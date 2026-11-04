package com.jaldishop.backend.notification.infrastructure.listener;

import com.jaldishop.backend.notification.application.command.CreateNotificationCommand;
import com.jaldishop.backend.notification.application.service.IsolatedOrderNotificationPersister;
import com.jaldishop.backend.notification.domain.port.NotificationRecipientResolver;
import com.jaldishop.backend.notification.domain.enums.NotificationType;
import com.jaldishop.backend.notification.infrastructure.persistence.NotificationDeduplicationExceptionClassifier;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.UUID;

/**
 * Listener síncrono encargado de generar la notificación persistente in-app (NEW_ORDER)
 * para el comerciante cuando se confirma una orden.
 *
 * <p><strong>Garantías y Comportamiento Transaccional:</strong></p>
 * <ul>
 *   <li>Ejecuta en fase {@code AFTER_COMMIT}, asegurando que la transacción de compra
 *       ya confirmó de manera definitiva en PostgreSQL antes de intentar notificar.</li>
 *   <li>Delega la persistencia a {@link IsolatedOrderNotificationPersister} anotado con
 *       {@code REQUIRES_NEW}, garantizando un ciclo de vida transaccional independiente (Tx2).</li>
 *   <li>La ejecución es síncrona en el hilo post-commit; cualquier excepción secundaria es capturada
 *       defensivamente para no alterar la respuesta HTTP exitosa de la orden confirmada.</li>
 *   <li>Clasifica con exactitud las violaciones UNIQUE de deduplicación (NEW_ORDER:{orderId})
 *       mediante {@link NotificationDeduplicationExceptionClassifier}.</li>
 * </ul>
 */
@Component
public class OrderNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationEventListener.class);

    private final NotificationRecipientResolver recipientResolver;
    private final IsolatedOrderNotificationPersister persister;

    public OrderNotificationEventListener(NotificationRecipientResolver recipientResolver,
                                          IsolatedOrderNotificationPersister persister) {
        this.recipientResolver = recipientResolver;
        this.persister = persister;
    }

    /**
     * Procesa la confirmación del pedido y persiste la notificación para el comerciante.
     *
     * @param event Evento ligero emitido tras el commit de checkout.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedDomainEvent event) {
        log.info("Procesando OrderConfirmedDomainEvent en listener de notificaciones para orden {}", event.orderNumber());
        try {
            Optional<UUID> merchantUserIdOpt = recipientResolver.resolveMerchantUserId(event.storeId());
            if (merchantUserIdOpt.isEmpty()) {
                log.warn("No se pudo determinar el merchantUserId para la tienda {}. Notificación omitida para orden {}.",
                        event.storeId(), event.orderNumber());
                return;
            }

            UUID merchantUserId = merchantUserIdOpt.get();
            String deduplicationKey = "NEW_ORDER:" + event.orderId();
            String title = "Nuevo pedido recibido: " + event.orderNumber();
            String message = "Has recibido una nueva orden. Ingresa a la sección de pedidos para gestionarla.";

            CreateNotificationCommand command = new CreateNotificationCommand(
                    merchantUserId,
                    NotificationType.NEW_ORDER,
                    title,
                    message,
                    deduplicationKey
            );

            persister.persistIsolated(command);
            log.info("Notificación persistente NEW_ORDER generada exitosamente para orden {} y comerciante {}",
                    event.orderNumber(), merchantUserId);

        } catch (Exception ex) {
            if (NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(ex)) {
                log.info("Notificación duplicada detectada para orden {} (deduplication_key: NEW_ORDER:{}). Inserción omitida de forma idempotente.",
                        event.orderNumber(), event.orderId());
            } else {
                log.error("Fallo no bloqueante al persistir notificación in-app para la orden {}: {}",
                        event.orderNumber(), ex.getMessage(), ex);
            }
        }
    }

}

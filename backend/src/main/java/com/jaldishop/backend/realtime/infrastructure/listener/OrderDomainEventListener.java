package com.jaldishop.backend.realtime.infrastructure.listener;

import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener transaccional de eventos de dominio de pedidos.
 *
 * <p><strong>Garantía de Consistencia Transaccional:</strong>
 * Anotado con {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)},
 * asegurando que la notificación a través de WebSocket solo se emita si la transacción
 * de base de datos se confirma satisfactoriamente. Si la transacción hace rollback
 * debido a cualquier fallo o excepción de negocio, el listener <strong>NUNCA</strong>
 * se ejecuta, garantizando la prevención absoluta de notificaciones fantasma (*Phantom Events*).</p>
 *
 * <p><strong>Limitación Arquitectural de Entrega:</strong>
 * Los eventos en memoria gestionados mediante {@code ApplicationEventPublisher} y el broker
 * simple no garantizan entrega persistente si el servidor se cae o reinicia inmediatamente
 * después del commit de la base de datos antes de que el frame WebSocket sea entregado.
 * Para garantizar entrega at-least-once y tolerancia a fallos entre instancias, se requerirá
 * en fases avanzadas implementar el patrón Transactional Outbox en PostgreSQL con un relay asíncrono.</p>
 */
@Component
public class OrderDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDomainEventListener.class);

    private final RealtimeNotificationPublisher realtimeNotificationPublisher;

    public OrderDomainEventListener(RealtimeNotificationPublisher realtimeNotificationPublisher) {
        this.realtimeNotificationPublisher = realtimeNotificationPublisher;
    }

    /**
     * Escucha la confirmación de la orden posterior al commit exitoso de la transacción.
     *
     * @param event Evento ligero con los identificadores de la compra.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedDomainEvent event) {
        log.info("Evento OrderConfirmedDomainEvent recibido AFTER_COMMIT para orden {}", event.orderNumber());
        try {
            realtimeNotificationPublisher.publishOrderConfirmed(event);
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al despachar notificación WebSocket para orden {}: {}",
                    event.orderNumber(), ex.getMessage(), ex);
        }
    }

    /**
     * Escucha el cambio de estado de la orden posterior al commit exitoso de la transacción.
     *
     * @param event Evento ligero con la transición de estado.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedDomainEvent event) {
        log.info("Evento OrderStatusChangedDomainEvent recibido AFTER_COMMIT para orden {} (nuevo estado: {})",
                event.orderNumber(), event.newStatus());
        try {
            realtimeNotificationPublisher.publishOrderStatusChanged(event);
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al despachar notificación WebSocket para orden {}: {}",
                    event.orderNumber(), ex.getMessage(), ex);
        }
    }

}

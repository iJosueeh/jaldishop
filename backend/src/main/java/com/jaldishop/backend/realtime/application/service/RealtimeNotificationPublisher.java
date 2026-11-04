package com.jaldishop.backend.realtime.application.service;

import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Servicio de publicación en tiempo real hacia destinos WebSocket / STOMP.
 *
 * <p><strong>Estrategia de Destinos:</strong>
 * <ul>
 *   <li>Comerciantes: Publicación al tópico de tienda {@code /topic/stores/{storeId}/orders}.</li>
 *   <li>Clientes: Publicación a la cola privada del usuario mediante {@code convertAndSendToUser(userId, "/queue/orders", payload)},
 *       la cual Spring STOMP enruta unívocamente a {@code /user/queue/orders} del cliente autenticado.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Limitación Arquitectural:</strong> Los mensajes emitidos mediante este publicador
 * utilizan el broker simple en memoria. En una arquitectura de un solo nodo, la entrega se
 * realiza en memoria; en despliegues con múltiples réplicas se requerirá un broker externo
 * distribuido (RabbitMQ o Redis Pub/Sub) para comunicar los eventos entre pods.</p>
 */
@Service
public class RealtimeNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationPublisher.class);

    private static final String STORE_ORDERS_TOPIC_PREFIX = "/topic/stores/";
    private static final String STORE_ORDERS_TOPIC_SUFFIX = "/orders";
    private static final String CUSTOMER_ORDERS_QUEUE = "/queue/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publica la notificación de orden confirmada en el canal de la tienda.
     *
     * @param event Evento ligero con los datos indispensables de la orden.
     */
    public void publishOrderConfirmed(OrderConfirmedDomainEvent event) {
        Objects.requireNonNull(event, "event no puede ser nulo");
        String destination = STORE_ORDERS_TOPIC_PREFIX + event.storeId() + STORE_ORDERS_TOPIC_SUFFIX;
        log.debug("Publicando evento ORDER_CONFIRMED a destino {} para orden {}", destination, event.orderNumber());
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Publica la notificación de cambio de estado a la cola privada del cliente dueño del pedido.
     *
     * @param event Evento ligero con la transición de estado.
     */
    public void publishOrderStatusChanged(OrderStatusChangedDomainEvent event) {
        Objects.requireNonNull(event, "event no puede ser nulo");
        String user = event.customerId().toString();
        log.debug("Publicando evento ORDER_STATUS_CHANGED a usuario {} en {} para orden {}",
                user, CUSTOMER_ORDERS_QUEUE, event.orderNumber());
        messagingTemplate.convertAndSendToUser(user, CUSTOMER_ORDERS_QUEUE, event);
    }

}

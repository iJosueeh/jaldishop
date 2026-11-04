package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.capacity.application.reservation.service.ReleaseCommittedCapacityReservationService;
import com.jaldishop.backend.inventory.application.service.RestockInventoryService;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestador transaccional de compensación para pedidos cancelados.
 *
 * <p>Orden de bloqueos canónico garantizado:
 * 1. Order (adquirido previamente con findByIdForUpdate en el llamador).
 * 2. Payment (adquirido mediante findByIdForUpdate).
 * 3. CapacityReservation (adquirido internamente por ReleaseCommittedCapacityReservationService).
 * 4. Inventories ordenados canónicamente por variant_id ASC (adquirido por RestockInventoryService).</p>
 */
@Service
@Transactional
public class OrderCancellationCompensator {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationCompensator.class);

    private final PaymentRepository paymentRepository;
    private final ReleaseCommittedCapacityReservationService releaseCapacityService;
    private final RestockInventoryService restockInventoryService;

    public OrderCancellationCompensator(
            PaymentRepository paymentRepository,
            ReleaseCommittedCapacityReservationService releaseCapacityService,
            RestockInventoryService restockInventoryService
    ) {
        this.paymentRepository = paymentRepository;
        this.releaseCapacityService = releaseCapacityService;
        this.restockInventoryService = restockInventoryService;
    }

    public void compensate(Order order, OrderStatus previousStatus, String reason) {
        if (order == null || previousStatus == null) {
            return;
        }

        // 1. Tratamiento financiero / Reembolso
        if (order.getPaymentId() != null) {
            paymentRepository.findByIdForUpdate(order.getPaymentId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.APPROVED) {
                    if (payment.getRefundStatus() == null || payment.getRefundStatus() == RefundStatus.NOT_REQUIRED) {
                        String refundReason = (reason != null && !reason.isBlank())
                                ? reason
                                : "Cancelación de pedido " + order.getOrderNumber();
                        payment.requestRefund(payment.getAmount(), refundReason);
                        paymentRepository.save(payment);
                        log.info("Reembolso PENDING registrado para pago {} asociado a pedido cancelado {}",
                                payment.getId(), order.getOrderNumber());
                    }
                }
            });
        }

        // 2. Compensación física de capacidad e inventario: ÚNICAMENTE si la cancelación fue en CONFIRMED (antes de cocina)
        if (previousStatus == OrderStatus.CONFIRMED) {
            if (order.getCapacityReservationId() != null) {
                // A. Liberar cupo de capacidad
                releaseCapacityService.execute(order.getCapacityReservationId());
                log.info("Cupo de capacidad {} liberado por cancelación de pedido {}",
                        order.getCapacityReservationId(), order.getOrderNumber());

                // B. Restituir inventario físico
                restockInventoryService.execute(order.getStoreId(), order.getCapacityReservationId());
                log.info("Inventario restituido por cancelación de pedido {}", order.getOrderNumber());
            }
        } else {
            log.info("Cancelación de pedido {} ocurrida en estado {}. No se restituye capacidad ni inventario según RN-ORD-13.",
                    order.getOrderNumber(), previousStatus);
        }
    }
}

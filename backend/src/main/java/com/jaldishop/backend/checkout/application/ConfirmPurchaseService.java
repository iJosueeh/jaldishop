package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.capacity.application.reservation.CommitCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.cart.application.ClearCartCommand;
import com.jaldishop.backend.cart.application.ClearCartService;
import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;
import com.jaldishop.backend.inventory.application.CommitInventoryReservationService;
import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Confirma una compra (BE-20): dado un pago aprobado, compromete la reserva de
 * capacidad, descuenta el inventario de forma atómica, crea el pedido CONFIRMED
 * y vacía el carrito — todo en una sola transacción.
 *
 * <p>Idempotencia: si el pedido ya existe para el pago o para la reserva se
 * devuelve la confirmación existente. La serialización real la garantiza el
 * lock pesimista de la reserva de capacidad: quien lo adquiere primero completa
 * reserva + inventario + pedido; el concurrente, al despertar, re-lee el pedido
 * existente y lo devuelve.</p>
 */
@Service
@Transactional
public class ConfirmPurchaseService {

    private final PaymentReader paymentReader;
    private final CommitCapacityReservationService commitCapacityReservationService;
    private final CommitInventoryReservationService commitInventoryReservationService;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CheckoutItemSnapshotAssembler snapshotAssembler;
    private final ClearCartService clearCartService;

    public ConfirmPurchaseService(
            PaymentReader paymentReader,
            CommitCapacityReservationService commitCapacityReservationService,
            CommitInventoryReservationService commitInventoryReservationService,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            ClearCartService clearCartService
    ) {
        this.paymentReader = paymentReader;
        this.commitCapacityReservationService = commitCapacityReservationService;
        this.commitInventoryReservationService = commitInventoryReservationService;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.snapshotAssembler = snapshotAssembler;
        this.clearCartService = clearCartService;
    }

    public OrderConfirmation execute(ConfirmPurchaseCommand command) {
        PaymentResume payment = readApprovedPayment(command.paymentId());

        Optional<Order> existingByPayment = orderRepository.findByPaymentId(payment.paymentId());
        if (existingByPayment.isPresent()) {
            return toConfirmation(existingByPayment.get());
        }
        Optional<Order> existingByReservation = orderRepository.findByCapacityReservationId(payment.capacityReservationId());
        if (existingByReservation.isPresent()) {
            return toConfirmation(existingByReservation.get());
        }

        CapacityReservation reservation = commitCapacityReservationService.execute(payment.capacityReservationId());
        if (reservation.getStatus() == CapacityReservationStatus.COMMITTED) {
            Optional<Order> raceWinner = orderRepository.findByCapacityReservationId(reservation.getId());
            if (raceWinner.isPresent()) {
                return toConfirmation(raceWinner.get());
            }
        }

        Store store = loadActiveStore(reservation.getStoreId());
        CheckoutFulfillmentValidator.validate(command.fulfillmentType(), command.deliveryAddress(), store);

        Cart cart = loadCart(reservation.getUserId(), reservation.getStoreId());
        List<CheckoutItemSnapshot> itemSnapshots = snapshotAssembler.assemble(cart, store.getId());
        String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.getFirst().currency();
        CheckoutPricing pricing = CheckoutPricing.calculate(
                itemSnapshots, command.fulfillmentType(), store.getDeliveryFeeAmount(), store.getTaxRate(), currency
        );

        commitInventoryReservationService.execute(reservation.getStoreId(), reservation.getId());

        Order order = buildOrder(command, reservation, store, pricing, itemSnapshots);
        Order savedOrder = orderRepository.save(order);
        clearCartService.execute(new ClearCartCommand(reservation.getUserId(), reservation.getStoreId()));
        return toConfirmation(savedOrder);
    }

    private PaymentResume readApprovedPayment(UUID paymentId) {
        PaymentResume payment = paymentReader.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado."));
        if (!"APPROVED".equals(payment.status())) {
            throw new ConflictException("RN-PAY-02", "La orden solo puede confirmarse con un pago aprobado.");
        }
        return payment;
    }

    private Store loadActiveStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada."));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }
        return store;
    }

    private Cart loadCart(UUID userId, UUID storeId) {
        Cart cart = cartRepository.findByUserIdAndStoreId(userId, storeId)
                .orElseThrow(() -> new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");
        }
        return cart;
    }

    private Order buildOrder(ConfirmPurchaseCommand command, CapacityReservation reservation, Store store,
                             CheckoutPricing pricing, List<CheckoutItemSnapshot> itemSnapshots) {
        CheckoutFulfillmentType fulfillmentType = command.fulfillmentType();
        String orderNumber = formatOrderNumber(orderRepository.nextOrderNumber());

        return Order.createConfirmed(new Order.CreateData(
                reservation.getUserId(),
                store.getId(),
                command.paymentId(),
                reservation.getId(),
                DeliveryMode.valueOf(fulfillmentType.name()),
                reservation.getServiceDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                command.customerName(),
                command.customerPhone(),
                command.customerEmail(),
                command.deliveryAddress(),
                command.deliveryReference(),
                command.deliveryLatitude(),
                command.deliveryLongitude(),
                pricing.currency(),
                pricing.productsSubtotalAmount(),
                pricing.discountAmount(),
                pricing.discountCode(),
                pricing.deliveryFeeAmount(),
                pricing.includedTaxAmount(),
                pricing.totalAmount(),
                itemSnapshots.stream()
                        .map(item -> new Order.ItemData(
                                item.variantId(),
                                item.productName(),
                                item.presentationName(),
                                item.quantity(),
                                item.unitPriceAmount()
                        ))
                        .toList()
        ), orderNumber);
    }

    private OrderConfirmation toConfirmation(Order order) {
        String storeName = storeRepository.findById(order.getStoreId())
                .map(Store::getName)
                .orElse("");
        return new OrderConfirmation(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getStoreId(),
                storeName,
                order.getCustomerName(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getConfirmedAt()
        );
    }

    private static String formatOrderNumber(long seq) {
        return "JAL-" + LocalDate.now().getYear() + "-" + String.format("%06d", seq);
    }

}
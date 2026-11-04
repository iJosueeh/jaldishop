package com.jaldishop.backend.checkout.application.service;

import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.port.PaymentReader;
import com.jaldishop.backend.checkout.application.port.PaymentResume;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;

import tools.jackson.databind.ObjectMapper;
import com.jaldishop.backend.capacity.application.reservation.service.CommitCapacityReservationService;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.cart.application.service.ClearCartService;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.model.CartItem;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.service.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.model.CheckoutPricing;
import com.jaldishop.backend.inventory.application.service.CommitInventoryReservationService;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.PaymentErrorCodes;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConfirmPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmPurchaseService.class);

    private final PaymentReader paymentReader;
    private final CommitCapacityReservationService commitCapacityReservationService;
    private final CommitInventoryReservationService commitInventoryReservationService;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CheckoutItemSnapshotAssembler snapshotAssembler;
    private final ClearCartService clearCartService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConfirmPurchaseService(
            PaymentReader paymentReader,
            CommitCapacityReservationService commitCapacityReservationService,
            CommitInventoryReservationService commitInventoryReservationService,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            ClearCartService clearCartService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.paymentReader = paymentReader;
        this.commitCapacityReservationService = commitCapacityReservationService;
        this.commitInventoryReservationService = commitInventoryReservationService;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.snapshotAssembler = snapshotAssembler;
        this.clearCartService = clearCartService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public ConfirmPurchaseService(
            PaymentReader paymentReader,
            CommitCapacityReservationService commitCapacityReservationService,
            CommitInventoryReservationService commitInventoryReservationService,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            ClearCartService clearCartService,
            ApplicationEventPublisher eventPublisher
    ) {
        this(
                paymentReader,
                commitCapacityReservationService,
                commitInventoryReservationService,
                orderRepository,
                cartRepository,
                storeRepository,
                snapshotAssembler,
                clearCartService,
                eventPublisher,
                new ObjectMapper()
        );
    }

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
        this(
                paymentReader,
                commitCapacityReservationService,
                commitInventoryReservationService,
                orderRepository,
                cartRepository,
                storeRepository,
                snapshotAssembler,
                clearCartService,
                null,
                new ObjectMapper()
        );
    }

    public OrderConfirmation execute(ConfirmPurchaseCommand command) {
        PaymentResume payment = readApprovedPayment(command.paymentId());

        // 1. Idempotencia: Verificar si ya existe un pedido asociado al pago o a la reserva
        Optional<Order> existingByPayment = orderRepository.findByPaymentId(payment.paymentId());
        if (existingByPayment.isPresent()) {
            log.info("Resolución idempotente: pedido ya confirmado para el pago {}", payment.paymentId());
            return toConfirmation(existingByPayment.get());
        }
        Optional<Order> existingByReservation = orderRepository.findByCapacityReservationId(payment.capacityReservationId());
        if (existingByReservation.isPresent()) {
            log.info("Resolución idempotente: pedido ya confirmado para la reserva {}", payment.capacityReservationId());
            return toConfirmation(existingByReservation.get());
        }

        // 2. Comprometer reserva de capacidad de forma atómica
        CapacityReservation reservation = commitCapacityReservationService.execute(payment.capacityReservationId());
        if (reservation.getStatus() == CapacityReservationStatus.COMMITTED) {
            Optional<Order> raceWinner = orderRepository.findByCapacityReservationId(reservation.getId());
            if (raceWinner.isPresent()) {
                log.info("Resolución de carrera: la reserva {} ya tenía un pedido generado concurrentemente.", reservation.getId());
                return toConfirmation(raceWinner.get());
            }
        }

        Store store = loadActiveStore(reservation.getStoreId());
        CheckoutFulfillmentType fulfillmentType = resolveFulfillmentType(command, payment);
        String deliveryAddress = resolveDeliveryAddress(command, payment);
        CheckoutFulfillmentValidator.validate(fulfillmentType, deliveryAddress, store);

        // 3. Resolución inmutable de ítems y precios: se usa el snapshot persistido en Payment
        List<CheckoutItemSnapshot> itemSnapshots;
        CheckoutPricing pricing;

        if (payment.itemsSnapshot() != null && !payment.itemsSnapshot().isBlank()) {
            itemSnapshots = deserializeAndValidateSnapshot(payment);
            pricing = new CheckoutPricing(
                    payment.productsSubtotalAmount() != null ? payment.productsSubtotalAmount() : payment.amount(),
                    payment.discountAmount() != null ? payment.discountAmount() : BigDecimal.ZERO,
                    payment.discountCode(),
                    payment.deliveryFeeAmount() != null ? payment.deliveryFeeAmount() : BigDecimal.ZERO,
                    store.getTaxRate(),
                    payment.includedTaxAmount() != null ? payment.includedTaxAmount() : BigDecimal.ZERO,
                    payment.amount(),
                    payment.currency() != null ? payment.currency() : "PEN"
            );
        } else {
            // Fallback para pruebas unitarias heredadas donde no se persistió itemsSnapshot
            Cart cart = loadCart(reservation.getUserId(), reservation.getStoreId());
            itemSnapshots = snapshotAssembler.assemble(cart, store.getId());
            String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.getFirst().currency();
            pricing = CheckoutPricing.calculate(
                    itemSnapshots, fulfillmentType, store.getDeliveryFeeAmount(), store.getTaxRate(), currency
            );
        }

        // 4. Comprometer inventario atómicamente
        commitInventoryReservationService.execute(reservation.getStoreId(), reservation.getId());

        // 5. Crear y persistir la orden en PostgreSQL
        Order order = buildOrder(command, payment, reservation, store, pricing, itemSnapshots);
        Order savedOrder = orderRepository.save(order);

        // 6. Limpieza selectiva del carrito mutable (únicamente cantidades e ítems comprometidos)
        cleanupCommittedItemsFromCart(reservation.getUserId(), reservation.getStoreId(), itemSnapshots);

        // 7. Notificación de dominio
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    savedOrder.getStoreId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getCurrency()
            ));
        }

        return toConfirmation(savedOrder);
    }

    private PaymentResume readApprovedPayment(UUID paymentId) {
        PaymentResume payment = paymentReader.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado."));
        if (!"APPROVED".equals(payment.status())) {
            throw new ConflictException(PaymentErrorCodes.RN_PAY_02, "La orden solo puede confirmarse con un pago aprobado.");
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

    private List<CheckoutItemSnapshot> deserializeAndValidateSnapshot(PaymentResume payment) {
        List<CheckoutItemSnapshot> snapshots;
        try {
            snapshots = objectMapper.readValue(
                    payment.itemsSnapshot(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CheckoutItemSnapshot.class)
            );
        } catch (Exception e) {
            log.error("Fallo al deserializar items_snapshot para el pago {}: {}", payment.paymentId(), e.getMessage());
            throw new ConflictException("INVALID_SNAPSHOT", "El snapshot de ítems en el pago está corrupto o malformado.");
        }

        if (snapshots == null || snapshots.isEmpty()) {
            throw new ConflictException("INVALID_SNAPSHOT", "El snapshot de ítems no contiene productos para confirmar la compra.");
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (CheckoutItemSnapshot item : snapshots) {
            if (item.variantId() == null) {
                throw new ConflictException("INVALID_SNAPSHOT", "Uno de los ítems en el snapshot carece de variantId.");
            }
            if (item.quantity() <= 0) {
                throw new ConflictException("INVALID_SNAPSHOT", "Cantidad inválida en el snapshot para la variante: " + item.variantId());
            }
            if (item.unitPriceAmount() == null || item.unitPriceAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new ConflictException("INVALID_SNAPSHOT", "Precio unitario inválido en el snapshot para la variante: " + item.variantId());
            }
            BigDecimal expectedItemSubtotal = item.unitPriceAmount().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.subtotalAmount() != null && item.subtotalAmount().compareTo(expectedItemSubtotal) != 0) {
                throw new ConflictException("INVALID_SNAPSHOT", "Subtotal inconsistente en el snapshot para la variante: " + item.variantId());
            }
            calculatedSubtotal = calculatedSubtotal.add(expectedItemSubtotal);
        }

        if (payment.productsSubtotalAmount() != null
                && calculatedSubtotal.compareTo(payment.productsSubtotalAmount()) != 0) {
            log.error("Discrepancia en snapshot para pago {}: subtotal calculado ({}) != registrado ({})",
                    payment.paymentId(), calculatedSubtotal, payment.productsSubtotalAmount());
            throw new ConflictException("INVALID_SNAPSHOT", "El subtotal del snapshot no coincide con el subtotal registrado en el pago.");
        }

        return snapshots;
    }

    private CheckoutFulfillmentType resolveFulfillmentType(ConfirmPurchaseCommand command, PaymentResume payment) {
        if (command.fulfillmentType() != null) {
            return command.fulfillmentType();
        }
        if (payment.fulfillmentType() != null && !payment.fulfillmentType().isBlank()) {
            return CheckoutFulfillmentType.valueOf(payment.fulfillmentType());
        }
        return CheckoutFulfillmentType.PICKUP;
    }

    private String resolveDeliveryAddress(ConfirmPurchaseCommand command, PaymentResume payment) {
        if (command.deliveryAddress() != null && !command.deliveryAddress().isBlank()) {
            return command.deliveryAddress();
        }
        return payment.deliveryAddress();
    }

    private Order buildOrder(
            ConfirmPurchaseCommand command,
            PaymentResume payment,
            CapacityReservation reservation,
            Store store,
            CheckoutPricing pricing,
            List<CheckoutItemSnapshot> itemSnapshots
    ) {
        CheckoutFulfillmentType fulfillmentType = resolveFulfillmentType(command, payment);
        String orderNumber = formatOrderNumber(orderRepository.nextOrderNumber());

        String customerName = command.customerName() != null && !command.customerName().isBlank()
                ? command.customerName()
                : (payment.customerName() != null ? payment.customerName() : "Cliente");
        String customerPhone = command.customerPhone() != null && !command.customerPhone().isBlank()
                ? command.customerPhone()
                : (payment.customerPhone() != null ? payment.customerPhone() : "");
        String customerEmail = command.customerEmail() != null && !command.customerEmail().isBlank()
                ? command.customerEmail()
                : (payment.customerEmail() != null ? payment.customerEmail() : "");
        String deliveryAddress = resolveDeliveryAddress(command, payment);
        String deliveryReference = command.deliveryReference() != null ? command.deliveryReference() : payment.deliveryReference();
        BigDecimal deliveryLatitude = command.deliveryLatitude() != null ? command.deliveryLatitude() : payment.deliveryLatitude();
        BigDecimal deliveryLongitude = command.deliveryLongitude() != null ? command.deliveryLongitude() : payment.deliveryLongitude();

        return Order.createConfirmed(new Order.CreateData(
                reservation.getUserId(),
                store.getId(),
                payment.paymentId(),
                reservation.getId(),
                DeliveryMode.valueOf(fulfillmentType.name()),
                reservation.getServiceDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                customerName,
                customerPhone,
                customerEmail,
                deliveryAddress,
                deliveryReference,
                deliveryLatitude,
                deliveryLongitude,
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

    private void cleanupCommittedItemsFromCart(UUID userId, UUID storeId, List<CheckoutItemSnapshot> committedItems) {
        try {
            Optional<Cart> cartOpt = cartRepository.findByUserIdAndStoreId(userId, storeId);
            if (cartOpt.isEmpty()) {
                return;
            }
            Cart cart = cartOpt.get();
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                return;
            }

            boolean cartModified = false;
            for (CheckoutItemSnapshot committedItem : committedItems) {
                Optional<CartItem> itemOpt = cart.findItemByVariantId(committedItem.variantId());
                if (itemOpt.isPresent()) {
                    CartItem item = itemOpt.get();
                    int currentQty = item.getQuantity();
                    int committedQty = committedItem.quantity();

                    if (currentQty == committedQty) {
                        // Coincidencia exacta: la línea correspondía a la compra y se remueve
                        cart.removeItem(committedItem.variantId());
                        cartModified = true;
                    } else if (currentQty > committedQty) {
                        // El usuario aumentó unidades tras el checkout: descontar solo lo comprado y conservar el excedente
                        cart.updateItemQuantity(committedItem.variantId(), currentQty - committedQty);
                        cartModified = true;
                    } else {
                        // currentQty < committedQty:
                        // El usuario redujo manualmente la cantidad tras el checkout.
                        // Para evitar ambigüedad y no borrar un ítem que el usuario desea conservar,
                        // preservamos la línea actual del carrito sin modificarla.
                        log.info("Conservando línea de carrito para variante {}: cantidad en carrito ({}) menor a la comprada ({})",
                                committedItem.variantId(), currentQty, committedQty);
                    }
                }
            }

            if (cartModified) {
                cartRepository.save(cart);
            }
        } catch (Exception ex) {
            log.warn("Limitación documentada: no se pudo limpiar selectivamente el carrito para usuario {} y tienda {}: {}",
                    userId, storeId, ex.getMessage());
        }
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

package com.jaldishop.backend.payment.application.preference;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.checkout.domain.service.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.model.CheckoutPricing;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio transaccional interno que prepara y persiste el agregado Payment y PaymentAttempt,
 * asegurando la protección de la reserva de capacidad y aislando el acceso a PostgreSQL
 * de las llamadas de red externas a Mercado Pago.
 */
@Service
public class PaymentPreferencePreparationService {

    private final CapacityReservationRepository capacityReservationRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final StoreRepository storeRepository;
    private final CartRepository cartRepository;
    private final CheckoutItemSnapshotAssembler snapshotAssembler;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ObjectMapper objectMapper;
    private final com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentPreferencePreparationService(
            CapacityReservationRepository capacityReservationRepository,
            InventoryReservationRepository inventoryReservationRepository,
            StoreRepository storeRepository,
            CartRepository cartRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ObjectMapper objectMapper,
            com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties properties
    ) {
        this.capacityReservationRepository = capacityReservationRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.storeRepository = storeRepository;
        this.cartRepository = cartRepository;
        this.snapshotAssembler = snapshotAssembler;
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PaymentPreferencePreparationService(
            CapacityReservationRepository capacityReservationRepository,
            InventoryReservationRepository inventoryReservationRepository,
            StoreRepository storeRepository,
            CartRepository cartRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ObjectMapper objectMapper
    ) {
        this(
                capacityReservationRepository,
                inventoryReservationRepository,
                storeRepository,
                cartRepository,
                snapshotAssembler,
                paymentRepository,
                paymentAttemptRepository,
                objectMapper,
                new com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties()
        );
    }

    @Transactional
    public PreparationResult prepareOrGetExistingPayment(CreatePaymentPreferenceCommand command) {
        // Bloqueo pesimista PESSIMISTIC_WRITE sobre la reserva de capacidad para serializar accesos concurrentes
        CapacityReservation reservation = capacityReservationRepository.findByIdForUpdate(command.capacityReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de capacidad no encontrada: " + command.capacityReservationId()));

        if (!reservation.getUserId().equals(command.userId())) {
            throw new ConflictException("RESERVATION_FORBIDDEN", "La reserva no pertenece al usuario autenticado.");
        }

        if (reservation.getStatus() == CapacityReservationStatus.COMMITTED) {
            throw new ConflictException("RESERVATION_COMMITTED", "La reserva ya ha sido confirmada en un pedido previo.");
        }
        if (reservation.getStatus() == CapacityReservationStatus.RELEASED) {
            throw new ConflictException("RESERVATION_RELEASED", "La reserva de capacidad fue cancelada.");
        }
        if (reservation.getStatus() == CapacityReservationStatus.EXPIRED || reservation.isExpired(Instant.now())) {
            throw new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada.");
        }

        // Validar reservas de inventario vinculadas a la reserva de capacidad
        List<InventoryReservation> inventoryReservations = inventoryReservationRepository.findByCapacityReservationId(reservation.getId());
        if (inventoryReservations != null && !inventoryReservations.isEmpty()) {
            Instant now = Instant.now();
            for (InventoryReservation ir : inventoryReservations) {
                if (ir.getStatus() == InventoryReservationStatus.EXPIRED || ir.isExpired(now)) {
                    throw new ConflictException("INVENTORY_RESERVATION_EXPIRED", "La reserva de inventario para los productos ha expirado.");
                }
                if (ir.getStatus() == InventoryReservationStatus.RELEASED) {
                    throw new ConflictException("INVENTORY_RESERVATION_RELEASED", "La reserva de inventario fue liberada.");
                }
            }
        }

        Store store = storeRepository.findById(reservation.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + reservation.getStoreId()));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }

        CheckoutFulfillmentValidator.validate(command.fulfillmentType(), command.deliveryAddress(), store);

        // Control de Idempotencia y Concurrencia: Verificar si ya existe un Payment para esta reserva
        Optional<Payment> existingPaymentOpt = paymentRepository.findByCapacityReservationId(reservation.getId());

        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();

            if (existingPayment.getStatus() == PaymentStatus.APPROVED) {
                throw new ConflictException("PAYMENT_ALREADY_APPROVED", "El pago para esta reserva ya fue aprobado.");
            }

            List<PaymentAttempt> existingAttempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(existingPayment.getId());

            // 1. Idempotencia: Si ya existe un intento previo con preference_id u order_id válido, retornarlo directamente
            for (int i = existingAttempts.size() - 1; i >= 0; i--) {
                PaymentAttempt attempt = existingAttempts.get(i);
                boolean hasValidProviderRef = (attempt.getPreferenceId() != null && !attempt.getPreferenceId().isBlank())
                        || (attempt.getProviderOrderId() != null && !attempt.getProviderOrderId().isBlank());
                if (hasValidProviderRef
                        && attempt.getStatus() != PaymentAttemptStatus.ERROR
                        && attempt.getStatus() != PaymentAttemptStatus.REJECTED) {
                    return new PreparationResult(existingPayment, attempt, true, store.getName(), null);
                }
            }

            // 2. Control de Concurrencia: Si existe una operación en curso (PROCESSING reciente), rechazar con respuesta controlada
            if (!existingAttempts.isEmpty()) {
                PaymentAttempt latestAttempt = existingAttempts.get(existingAttempts.size() - 1);
                Instant inFlightCutoff = Instant.now().minusSeconds(45);

                boolean isInFlight = (latestAttempt.getStatus() == PaymentAttemptStatus.PROCESSING
                        || latestAttempt.getStatus() == PaymentAttemptStatus.CREATED)
                        && latestAttempt.getPreferenceId() == null
                        && latestAttempt.getProviderOrderId() == null
                        && latestAttempt.getUpdatedAt().isAfter(inFlightCutoff);

                if (isInFlight) {
                    throw new ConflictException("PAYMENT_IN_PROGRESS",
                            "Ya existe una solicitud de pago o creación de preferencia en curso para esta reserva.");
                }

                // 3. Timeout ambiguo / Resultado desconocido:
                // No asumir rechazo ni crear un intento con nueva clave. Reutilizar el intento
                // existente con su misma idempotencyKey para permitir que Mercado Pago reconozca la operación.
                boolean isRecoverable = (latestAttempt.getStatus() == PaymentAttemptStatus.ERROR
                        && "MP_TIMEOUT".equals(latestAttempt.getErrorCode()))
                        || (latestAttempt.getPreferenceId() == null
                        && latestAttempt.getProviderOrderId() == null
                        && latestAttempt.getStatus() != PaymentAttemptStatus.REJECTED);

                if (isRecoverable) {
                    latestAttempt.markProcessing();
                    PaymentAttempt savedAttempt = paymentAttemptRepository.save(latestAttempt);
                    return new PreparationResult(existingPayment, savedAttempt, false, store.getName(), null);
                }
            }

            // 4. Si el intento previo fue formalmente REJECTED, crear un nuevo intento secuencial
            String currentMode = properties.getCheckoutMode() != null ? properties.getCheckoutMode().name() : "PREFERENCES";
            int nextAttemptNumber = paymentAttemptRepository.findNextAttemptNumber(existingPayment.getId());
            PaymentAttempt newAttempt = PaymentAttempt.create(
                    existingPayment.getId(),
                    nextAttemptNumber,
                    PaymentProvider.MERCADO_PAGO,
                    PaymentMethod.CARD,
                    UUID.randomUUID().toString(),
                    currentMode
            );
            newAttempt.markProcessing();
            PaymentAttempt savedAttempt = paymentAttemptRepository.save(newAttempt);

            return new PreparationResult(existingPayment, savedAttempt, false, store.getName(), null);
        }

        // Si no existe Payment, protegemos la reserva si está activa
        if (reservation.getStatus() == CapacityReservationStatus.ACTIVE) {
            reservation.protectPayment();
            capacityReservationRepository.save(reservation);
        }

        // Obtenemos snapshot de carrito y precios
        Cart cart = cartRepository.findByUserIdAndStoreId(command.userId(), store.getId())
                .orElseThrow(() -> new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");
        }

        List<CheckoutItemSnapshot> itemSnapshots = snapshotAssembler.assemble(cart, store.getId());
        String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.getFirst().currency();
        CheckoutPricing pricing = CheckoutPricing.calculate(
                itemSnapshots, command.fulfillmentType(), store.getDeliveryFeeAmount(), store.getTaxRate(), currency
        );

        String itemsSnapshotJson;
        try {
            itemsSnapshotJson = objectMapper.writeValueAsString(itemSnapshots);
        } catch (JacksonException e) {
            throw new IllegalStateException("Error al serializar el snapshot de ítems de checkout", e);
        }

        Payment payment = Payment.createPending(
                reservation.getId(),
                pricing.totalAmount(),
                "PEN",
                FulfillmentType.valueOf(command.fulfillmentType().name()),
                command.customerName(),
                command.customerPhone(),
                command.customerEmail(),
                command.deliveryAddress(),
                command.deliveryReference(),
                command.deliveryLatitude(),
                command.deliveryLongitude(),
                pricing.productsSubtotalAmount(),
                pricing.discountAmount(),
                pricing.discountCode(),
                pricing.deliveryFeeAmount(),
                pricing.includedTaxAmount(),
                itemsSnapshotJson
        );
        Payment savedPayment = paymentRepository.save(payment);

        String currentMode = properties.getCheckoutMode() != null ? properties.getCheckoutMode().name() : "PREFERENCES";
        PaymentAttempt attempt = PaymentAttempt.create(
                savedPayment.getId(),
                1,
                PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD,
                UUID.randomUUID().toString(),
                currentMode
        );
        attempt.markProcessing();
        PaymentAttempt savedAttempt = paymentAttemptRepository.save(attempt);

        return new PreparationResult(savedPayment, savedAttempt, false, store.getName(), itemSnapshots);
    }

    @Transactional
    public void recordPreferenceResult(UUID attemptId, String preferenceId, String initPoint) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento de pago no encontrado: " + attemptId));
        attempt.recordPreference(preferenceId, initPoint);
        paymentAttemptRepository.save(attempt);
    }

    @Transactional
    public void recordOrderResult(UUID attemptId, String providerOrderId, String initPoint) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento de pago no encontrado: " + attemptId));
        attempt.recordOrder(providerOrderId, initPoint);
        paymentAttemptRepository.save(attempt);
    }

    @Transactional
    public void recordAttemptError(UUID attemptId, String errorCode, String errorMessage) {
        paymentAttemptRepository.findById(attemptId).ifPresent(attempt -> {
            attempt.markError(errorCode, errorMessage);
            paymentAttemptRepository.save(attempt);
        });
    }

}

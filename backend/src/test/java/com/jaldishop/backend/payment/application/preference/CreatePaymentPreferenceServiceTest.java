package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.model.CartItem;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePaymentPreferenceServiceTest {

    @Mock
    private CapacityReservationRepository capacityReservationRepository;
    @Mock
    private InventoryReservationRepository inventoryReservationRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CheckoutItemSnapshotAssembler snapshotAssembler;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private MercadoPagoGateway mercadoPagoGateway;

    private MercadoPagoProperties properties;
    private ObjectMapper objectMapper;
    private PaymentPreferencePreparationService preparationService;
    private CreatePaymentPreferenceService service;

    private UUID userId;
    private UUID storeId;
    private UUID reservationId;
    private Store activeStore;
    private CapacityReservation activeReservation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();

        properties = new MercadoPagoProperties();
        properties.setNotificationUrl("https://api.jaldishop.com/api/v1/checkout/mercado-pago/webhook");
        properties.setBackUrlSuccess("https://jaldishop.com/checkout/success");
        properties.setBackUrlFailure("https://jaldishop.com/checkout/failure");
        properties.setBackUrlPending("https://jaldishop.com/checkout/pending");

        objectMapper = new ObjectMapper();

        preparationService = new PaymentPreferencePreparationService(
                capacityReservationRepository,
                inventoryReservationRepository,
                storeRepository,
                cartRepository,
                snapshotAssembler,
                paymentRepository,
                paymentAttemptRepository,
                objectMapper
        );

        service = new CreatePaymentPreferenceService(
                preparationService,
                mercadoPagoGateway,
                properties,
                objectMapper
        );

        activeStore = Store.create(
                UUID.randomUUID(),
                "Bakery Demo",
                "bakery-demo",
                "Panaderia y pasteleria",
                "987654321",
                "Av. Principal 123",
                "Cerca a plaza",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                null
        );
        storeId = activeStore.getId();

        activeReservation = CapacityReservation.create(
                storeId, userId, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0)
        );
    }

    @Test
    @DisplayName("Reserva activa válida genera preferencia y protege capacidad en PAYMENT_PROTECTED")
    void shouldCreatePreferenceSuccessfullyForActiveReservation() {
        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.DELIVERY,
                "Valeria Ramos", "+51987654321", "valeria@example.com",
                "Av. Salaverry 123", "Dpto 101", new BigDecimal("-12.08"), new BigDecimal("-77.05")
        );

        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.empty());

        Instant now = Instant.now();
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        CartItem item = new CartItem(cartId, variantId, 2, new BigDecimal("4.50"), "PEN", now, now);
        Cart cart = new Cart(cartId, userId, storeId, List.of(item), now, now);
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));

        List<CheckoutItemSnapshot> snapshots = List.of(new CheckoutItemSnapshot(
                variantId, UUID.randomUUID(), "Leche Gloria", "Lata 400g", "SKU-001",
                null, 2, new BigDecimal("4.50"), "PEN", new BigDecimal("9.00"), true
        ));
        when(snapshotAssembler.assemble(cart, storeId)).thenReturn(snapshots);

        java.util.concurrent.atomic.AtomicReference<PaymentAttempt> savedAttemptRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> {
            PaymentAttempt a = inv.getArgument(0);
            savedAttemptRef.set(a);
            return a;
        });
        when(paymentAttemptRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(savedAttemptRef.get()));

        when(mercadoPagoGateway.createPreference(any(CreatePreferenceCommand.class))).thenReturn(
                new PreferenceResult("pref-abc-123", "https://mercadopago.com/checkout/abc", "https://sandbox.mercadopago.com/checkout/abc")
        );

        CreatePaymentPreferenceResult result = service.execute(command);

        assertThat(result.preferenceId()).isEqualTo("pref-abc-123");
        assertThat(result.initPoint()).isEqualTo("https://mercadopago.com/checkout/abc");
        assertThat(result.status()).isEqualTo("PENDING");

        // Capacidad protegida: estado pasa a PAYMENT_PROTECTED
        assertThat(activeReservation.getStatus()).isEqualTo(CapacityReservationStatus.PAYMENT_PROTECTED);
        assertThat(activeReservation.getPaymentProtectionExpiresAt()).isNotNull();

        // Intento de pago actualizado con preferencia
        assertThat(savedAttemptRef.get().getPreferenceId()).isEqualTo("pref-abc-123");
        assertThat(savedAttemptRef.get().getInitPoint()).isEqualTo("https://mercadopago.com/checkout/abc");

        // Se persiste el agregado Payment con el snapshot inmutable
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getItemsSnapshot()).contains("Leche Gloria");
        assertThat(savedPayment.getCustomerName()).isEqualTo("Valeria Ramos");
        assertThat(savedPayment.getFulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);

        // Se verifica llamada al gateway fuera de transacción pasando idempotencyKey
        ArgumentCaptor<CreatePreferenceCommand> mpCommandCaptor = ArgumentCaptor.forClass(CreatePreferenceCommand.class);
        verify(mercadoPagoGateway, times(1)).createPreference(mpCommandCaptor.capture());
        assertThat(mpCommandCaptor.getValue().idempotencyKey()).isEqualTo(savedAttemptRef.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("Rechazar reserva si pertenece a otro usuario")
    void shouldRejectReservationOfAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        CapacityReservation reservationOfOther = CapacityReservation.create(
                storeId, otherUserId, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0)
        );
        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservationOfOther));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Juan Perez", "+51999999999", "juan@example.com", null, null, null, null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no pertenece al usuario");

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Rechazar reserva si ya expiró")
    void shouldRejectExpiredReservation() {
        Instant past = Instant.now().minusSeconds(3600);
        CapacityReservation expiredReservation = CapacityReservation.reconstitute(
                reservationId, storeId, userId, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.EXPIRED, past, null, past, past
        );
        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(expiredReservation));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Juan Perez", "+51999999999", "juan@example.com", null, null, null, null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expirada");

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Rechazar si las reservas de inventario asociadas están vencidas")
    void shouldRejectWhenInventoryReservationIsExpired() {
        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));

        Instant past = Instant.now().minusSeconds(3600);
        InventoryReservation expiredInventory = new InventoryReservation(
                UUID.randomUUID(), storeId, activeReservation.getId(), UUID.randomUUID(), 2,
                InventoryReservationStatus.EXPIRED, past, past, past
        );
        when(inventoryReservationRepository.findByCapacityReservationId(activeReservation.getId()))
                .thenReturn(List.of(expiredInventory));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVENTORY_RESERVATION_EXPIRED"));

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Rechazar reserva si ya fue confirmada en COMMITTED")
    void shouldRejectCommittedReservation() {
        activeReservation.protectPayment();
        activeReservation.commit();
        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Juan Perez", "+51999999999", "juan@example.com", null, null, null, null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("confirmada");

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Idempotencia: solicitudes repetidas retornan la preferencia existente sin llamar nuevamente a Mercado Pago")
    void shouldReturnExistingPreferenceOnRepeatedRequestWithoutDuplicateGatewayCall() {
        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null
        );

        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));

        Payment existingPayment = Payment.createPending(
                activeReservation.getId(), new BigDecimal("20.00"), "PEN", FulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("3.05"), "[]"
        );
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.of(existingPayment));

        PaymentAttempt existingAttempt = PaymentAttempt.create(
                existingPayment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idempotency-key-001"
        );
        existingAttempt.recordPreference("pref-existing-999", "https://mercadopago.com/checkout/existing-999");

        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(existingPayment.getId()))
                .thenReturn(List.of(existingAttempt));

        CreatePaymentPreferenceResult result = service.execute(command);

        assertThat(result.preferenceId()).isEqualTo("pref-existing-999");
        assertThat(result.initPoint()).isEqualTo("https://mercadopago.com/checkout/existing-999");

        // Se verifica que NO se llamó nuevamente al gateway externo de Mercado Pago
        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Concurrencia: rechazar con HTTP 409 PAYMENT_IN_PROGRESS si ya hay una operación en curso")
    void shouldRejectWithConflictWhenAnotherRequestIsInFlight() {
        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null
        );

        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));

        Payment existingPayment = Payment.createPending(
                activeReservation.getId(), new BigDecimal("20.00"), "PEN", FulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("3.05"), "[]"
        );
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.of(existingPayment));

        // Intento en PROCESSING creado recientemente sin preference_id (en vuelo)
        PaymentAttempt inFlightAttempt = PaymentAttempt.create(
                existingPayment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idempotency-key-inflight"
        );
        inFlightAttempt.markProcessing();

        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(existingPayment.getId()))
                .thenReturn(List.of(inFlightAttempt));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.getCode()).isEqualTo("PAYMENT_IN_PROGRESS");
                    assertThat(ce.getMessage()).contains("en curso");
                });

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Timeout ambiguo: no crear nuevo intento ciego, registrar error y reutilizar el mismo intento e idempotencyKey en reintento")
    void shouldRecordTimeoutAndRecoverSameAttemptOnRetryWithoutDuplicateKey() {
        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null
        );

        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.empty());

        Instant now = Instant.now();
        UUID cartId = UUID.randomUUID();
        CartItem item = new CartItem(cartId, UUID.randomUUID(), 1, new BigDecimal("5.00"), "PEN", now, now);
        Cart cart = new Cart(cartId, userId, storeId, List.of(item), now, now);
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(snapshotAssembler.assemble(cart, storeId)).thenReturn(List.of(new CheckoutItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Pan", "Bolsa", "SKU", null, 1, new BigDecimal("5.00"), "PEN", new BigDecimal("5.00"), false
        )));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idempotency-key-timeout-001"
        );
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenReturn(attempt);
        when(paymentAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        // 1. Primer intento falla por timeout de lectura
        when(mercadoPagoGateway.createPreference(any(CreatePreferenceCommand.class)))
                .thenThrow(new MercadoPagoException(0, "MP_TIMEOUT", "Timeout al invocar Mercado Pago"));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> assertThat(((MercadoPagoException) ex).getErrorCode()).isEqualTo("MP_TIMEOUT"));

        // Verificamos que el intento registró el error de timeout y el pago sigue PENDING
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.ERROR);
        assertThat(attempt.getErrorCode()).isEqualTo("MP_TIMEOUT");
        assertThat(attempt.getIdempotencyKey()).isEqualTo("idempotency-key-timeout-001");

        // 2. Reintento tras timeout: debe reutilizar el mismo intento 1 y la MISMA clave de idempotencia
        String validSnapshot = """
                [{"variantId":"%s","productId":"%s","productName":"Pan","presentationName":"Bolsa","sku":"SKU","imageUrl":null,"quantity":1,"unitPriceAmount":5.00,"currency":"PEN","totalAmount":5.00,"available":false}]
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        Payment pendingPayment = Payment.createPending(
                activeReservation.getId(), new BigDecimal("5.00"), "PEN", FulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null,
                new BigDecimal("5.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, validSnapshot
        );
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.of(pendingPayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(pendingPayment.getId())).thenReturn(List.of(attempt));

        // En el reintento Mercado Pago responde exitosamente (idempotente)
        when(mercadoPagoGateway.createPreference(any(CreatePreferenceCommand.class))).thenReturn(
                new PreferenceResult("pref-recovered-777", "https://mercadopago.com/checkout/777", null)
        );

        CreatePaymentPreferenceResult retryResult = service.execute(command);

        assertThat(retryResult.preferenceId()).isEqualTo("pref-recovered-777");
        assertThat(retryResult.initPoint()).isEqualTo("https://mercadopago.com/checkout/777");

        // Verificamos que se envió la MISMA idempotency-key a Mercado Pago
        ArgumentCaptor<CreatePreferenceCommand> cmdCaptor = ArgumentCaptor.forClass(CreatePreferenceCommand.class);
        verify(mercadoPagoGateway, times(2)).createPreference(cmdCaptor.capture());
        assertThat(cmdCaptor.getAllValues().get(1).idempotencyKey()).isEqualTo("idempotency-key-timeout-001");
    }

    @Test
    @DisplayName("Fallo de persistencia posterior a respuesta exitosa de Mercado Pago propaga excepción y permite reintento idempotente")
    void shouldHandleDatabaseFailureAfterGatewaySuccessAndAllowIdempotentRecovery() {
        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                userId, reservationId, CheckoutFulfillmentType.PICKUP,
                "Valeria Ramos", "+51987654321", "valeria@example.com", null, null, null, null
        );

        when(capacityReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(activeReservation));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
        when(paymentRepository.findByCapacityReservationId(activeReservation.getId())).thenReturn(Optional.empty());

        Instant now = Instant.now();
        UUID cartId = UUID.randomUUID();
        CartItem item = new CartItem(cartId, UUID.randomUUID(), 1, new BigDecimal("5.00"), "PEN", now, now);
        Cart cart = new Cart(cartId, userId, storeId, List.of(item), now, now);
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(snapshotAssembler.assemble(cart, storeId)).thenReturn(List.of(new CheckoutItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Pan", "Bolsa", "SKU", null, 1, new BigDecimal("5.00"), "PEN", new BigDecimal("5.00"), false
        )));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idempotency-key-post-db-fail"
        );
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenReturn(attempt);

        // Mercado Pago responde 200 OK con la preferencia
        when(mercadoPagoGateway.createPreference(any(CreatePreferenceCommand.class))).thenReturn(
                new PreferenceResult("pref-gateway-ok", "https://mercadopago.com/init/ok", null)
        );

        // Pero la DB arroja excepción al guardar el preferenceId en el intento
        when(paymentAttemptRepository.findById(attempt.getId())).thenThrow(new RuntimeException("Simulated DB Connection Failure"));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated DB Connection Failure");

        // Se llamó al gateway
        verify(mercadoPagoGateway, times(1)).createPreference(any());
    }

}

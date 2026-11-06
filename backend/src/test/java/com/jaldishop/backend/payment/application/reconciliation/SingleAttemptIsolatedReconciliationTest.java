package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.BackendApplication;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApplication.class)
@EnabledIfSystemProperty(named = "manual.reconcile.authorized", matches = "true")
@DisplayName("SingleAttemptIsolatedReconciliationTest — Runner controlado de conciliación puntual para jaldishop_test")
class SingleAttemptIsolatedReconciliationTest {

    private static final UUID RESIDUAL_ATTEMPT_ID = UUID.fromString("696f6184-ef80-41f0-84dc-554524cad38e");

    static UUID getValidatedTargetAttemptId() {
        String idStr = System.getProperty("targetAttemptId");
        if (idStr == null || idStr.isBlank()) {
            throw new IllegalStateException("FAIL-CLOSED: Requiere parámetro de sistema obligatorio -DtargetAttemptId=<UUID>");
        }
        UUID id;
        try {
            id = UUID.fromString(idStr.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FAIL-CLOSED: targetAttemptId no es un UUID válido: " + idStr, e);
        }
        if (RESIDUAL_ATTEMPT_ID.equals(id)) {
            throw new IllegalStateException("FAIL-CLOSED: Prohibido procesar el intento residual protegido ORD-RECON-008 (" + RESIDUAL_ATTEMPT_ID + ")");
        }
        return id;
    }

    @DynamicPropertySource
    static void configureIsolatedProperties(DynamicPropertyRegistry registry) {
        String authFlag = System.getProperty("manual.reconcile.authorized");
        if (!"true".equalsIgnoreCase(authFlag)) {
            throw new IllegalStateException("FAIL-CLOSED: Runner manual inactivo. Requiere estrictamente -Dmanual.reconcile.authorized=true");
        }

        // Valida que el targetAttemptId sea sintácticamente válido y no sea el intento residual
        getValidatedTargetAttemptId();

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("backend/.env.sandbox.local")) {
            props.load(fis);
        } catch (IOException e) {
            try (FileInputStream fis = new FileInputStream(".env.sandbox.local")) {
                props.load(fis);
            } catch (IOException ex) {
                throw new IllegalStateException("FAIL-CLOSED: No se encontró .env.sandbox.local", ex);
            }
        }

        String dbUrl = props.getProperty("DB_URL");
        if (dbUrl == null || !dbUrl.equals("jdbc:postgresql://127.0.0.1:5433/jaldishop_test")) {
            throw new IllegalStateException("FAIL-CLOSED: Requiere estrictamente jdbc:postgresql://127.0.0.1:5433/jaldishop_test. Recibido: " + dbUrl);
        }

        String token = props.getProperty("MERCADOPAGO_ACCESS_TOKEN");
        if (token == null || token.isBlank() || (!token.startsWith("APP_USR-") && !token.startsWith("TEST-"))) {
            throw new IllegalStateException("FAIL-CLOSED: MERCADOPAGO_ACCESS_TOKEN no configurado o no es credencial de prueba reconocida (debe iniciar con APP_USR- o TEST-)");
        }

        String checkoutMode = props.getProperty("MERCADOPAGO_CHECKOUT_MODE");
        if (checkoutMode == null || !"ORDERS".equalsIgnoreCase(checkoutMode.trim())) {
            throw new IllegalStateException("FAIL-CLOSED: Requiere estrictamente MERCADOPAGO_CHECKOUT_MODE=ORDERS. Recibido: " + checkoutMode);
        }

        registry.add("spring.datasource.url", () -> dbUrl);
        registry.add("spring.datasource.username", () -> props.getProperty("DB_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () -> props.getProperty("DB_PASSWORD", "postgres"));
        registry.add("mercadopago.access-token", () -> token);
        registry.add("mercadopago.checkout-mode", () -> "ORDERS");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
        registry.add("app.payment.reconciliation.orders-enabled", () -> "false");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
    }

    @Autowired
    private PaymentOrderReconciliationProcessor processor;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CapacityReservationRepository capacityReservationRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    @DisplayName("Ejecuta conciliación real puntual del intento autorizado con aislamiento absoluto de ORD-RECON-008")
    void shouldReconcileTargetAttemptWithoutTouchingResidualAttempt() {
        UUID targetAttemptId = getValidatedTargetAttemptId();

        // --- A. Verificaciones Previas (Estado Inicial) ---
        PaymentAttempt initialTarget = paymentAttemptRepository.findById(targetAttemptId)
                .orElseThrow(() -> new IllegalStateException("Intento objetivo no encontrado en base de datos: " + targetAttemptId));
        assertThat(initialTarget.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(initialTarget.getProviderOrderId()).isNotBlank();
        String expectedProviderOrderId = initialTarget.getProviderOrderId();
        UUID targetPaymentId = initialTarget.getPaymentId();

        // Guardia de concurrencia: no debe existir lease activo en progreso
        if (initialTarget.getReconciliationClaimToken() != null &&
                initialTarget.getReconciliationLeaseExpiresAt() != null &&
                initialTarget.getReconciliationLeaseExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("FAIL-CLOSED: El intento objetivo ya tiene un lease de conciliación activo hasta: "
                    + initialTarget.getReconciliationLeaseExpiresAt());
        }

        PaymentAttempt initialResidual = paymentAttemptRepository.findById(RESIDUAL_ATTEMPT_ID).orElseThrow();
        assertThat(initialResidual.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(initialResidual.getProviderOrderId()).isEqualTo("ORD-RECON-008");
        Instant residualInitialUpdatedAt = initialResidual.getUpdatedAt();

        Payment initialPayment = paymentRepository.findById(targetPaymentId).orElseThrow();
        assertThat(initialPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(initialPayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);

        // --- B. Fase 1: Claim Transaccional sobre el Intento Objetivo ---
        UUID workerToken = UUID.randomUUID();
        Instant now = Instant.now();
        Duration leaseDuration = Duration.ofMinutes(5);

        OrderReconciliationClaimResult claim = processor.claimAttempt(targetAttemptId, workerToken, leaseDuration, now);
        assertTrue(claim.claimed(), "El claim del intento objetivo debe ser exitoso");
        assertThat(claim.providerOrderId()).isEqualTo(expectedProviderOrderId);

        // --- C. Fase 2: Consulta Remota vía Gateway Real (HTTP GET /v1/orders/{id}) ---
        OrderQueryResult remoteOrder = processor.queryRemoteOrder(claim.providerOrderId());
        assertThat(remoteOrder).isNotNull();
        assertThat(remoteOrder.orderId()).isEqualTo(expectedProviderOrderId);

        // --- D. Fase 3: Consolidación Transaccional y Confirmación Comercial ---
        OrderReconciliationCandidateResult result = processor.consolidateOrderReconciliation(claim, remoteOrder, workerToken, now);

        // --- E. Verificaciones Posteriores (Estado Final) ---
        PaymentAttempt finalTarget = paymentAttemptRepository.findById(targetAttemptId).orElseThrow();
        Payment finalPayment = paymentRepository.findById(targetPaymentId).orElseThrow();

        if (result.status() == OrderReconciliationCandidateResult.Status.SUCCESS_CONFIRMED) {
            // Caso A: Pago Acreditado y Confirmado Comercial
            assertThat(finalTarget.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
            assertThat(finalTarget.getProviderPaymentId()).isNotBlank();
            assertThat(finalTarget.getCompletedAt()).isNotNull();
            assertThat(finalTarget.getReconciliationClaimToken()).isNull();

            assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(finalPayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);

            // Orden comercial creada
            Optional<Order> orderOpt = orderRepository.findByPaymentId(targetPaymentId);
            assertThat(orderOpt).isPresent();
            Order order = orderOpt.get();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getOrderNumber()).isNotBlank();
            assertThat(result.detail()).contains(order.getOrderNumber());

            // Capacidad e Inventario comprometidos
            if (initialPayment.getCapacityReservationId() != null) {
                CapacityReservation capRes = capacityReservationRepository.findById(initialPayment.getCapacityReservationId()).orElse(null);
                if (capRes != null) {
                    assertThat(capRes.getStatus()).isEqualTo(CapacityReservationStatus.COMMITTED);
                }
                List<InventoryReservation> invResList = inventoryReservationRepository.findByCapacityReservationId(initialPayment.getCapacityReservationId());
                for (InventoryReservation invRes : invResList) {
                    assertThat(invRes.getStatus()).isEqualTo(InventoryReservationStatus.COMMITTED);
                }
            }

            // Limpieza selectiva de carrito
            if (order.getUserId() != null && order.getStoreId() != null) {
                cartRepository.findByUserIdAndStoreId(order.getUserId(), order.getStoreId())
                        .ifPresent(cart -> {
                            if (cart.getItems() != null && order.getItems() != null) {
                                assertThat(cart.getItems()).noneMatch(ci ->
                                        order.getItems().stream().anyMatch(oi -> oi.variantId().equals(ci.getVariantId()))
                                );
                            }
                        });
            }

        } else if (result.status() == OrderReconciliationCandidateResult.Status.COMMERCIAL_INCIDENCE) {
            // Caso B: Pago Acreditado con Incidencia Comercial (ej. protección de reserva vencida)
            assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(finalPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(orderRepository.findByPaymentId(targetPaymentId)).isEmpty();

        } else if (result.status() == OrderReconciliationCandidateResult.Status.SUCCESS_CANCELLED_OR_EXPIRED) {
            // Caso C: Orden Cancelada o Expirada Remotamente
            assertThat(finalTarget.getStatus()).isEqualTo(PaymentAttemptStatus.REJECTED);
            assertThat(finalTarget.getErrorCode()).isEqualTo("ORDER_CANCELED");
            assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(finalPayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
            assertThat(orderRepository.findByPaymentId(targetPaymentId)).isEmpty();

        } else {
            throw new AssertionError("Resultado no esperado de conciliación puntual: " + result.status() + " - " + result.detail());
        }

        // --- F. Verificación de Aislamiento Invariable: ORD-RECON-008 no modificado ---
        PaymentAttempt finalResidual = paymentAttemptRepository.findById(RESIDUAL_ATTEMPT_ID).orElseThrow();
        assertThat(finalResidual.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(finalResidual.getProviderOrderId()).isEqualTo("ORD-RECON-008");
        assertThat(finalResidual.getUpdatedAt()).isEqualTo(residualInitialUpdatedAt);

        // --- G. Verificación de Idempotencia (Segunda Invocación) ---
        OrderReconciliationClaimResult secondClaim = processor.claimAttempt(targetAttemptId, UUID.randomUUID(), leaseDuration, Instant.now());
        assertFalse(secondClaim.claimed(), "Una segunda llamada a claimAttempt no debe adquirir lease");
    }
}

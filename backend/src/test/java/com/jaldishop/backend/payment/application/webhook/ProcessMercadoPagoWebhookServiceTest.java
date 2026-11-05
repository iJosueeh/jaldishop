package com.jaldishop.backend.payment.application.webhook;

import tools.jackson.databind.ObjectMapper;
import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.payment.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessMercadoPagoWebhookServiceTest {

    @Mock
    private MercadoPagoWebhookSignatureValidator signatureValidator;
    @Mock
    private MercadoPagoGateway mercadoPagoGateway;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    private MercadoPagoProperties properties;
    private ObjectMapper objectMapper;
    private PaymentWebhookPersistenceService persistenceService;
    private ProcessMercadoPagoWebhookService service;

    private final String secretKey = "test_webhook_secret";
    private UUID paymentId;
    private Payment activePayment;
    private PaymentAttempt activeAttempt;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        properties = new MercadoPagoProperties();
        properties.setWebhookSecret(secretKey);

        objectMapper = new ObjectMapper();

        persistenceService = new PaymentWebhookPersistenceService(
                paymentRepository,
                paymentAttemptRepository
        );

        service = new ProcessMercadoPagoWebhookService(
                signatureValidator,
                mercadoPagoGateway,
                persistenceService,
                properties,
                objectMapper
        );

        activePayment = Payment.createPending(
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "PEN",
                FulfillmentType.PICKUP,
                "Valeria Ramos",
                "+51987654321",
                "valeria@test.com",
                null,
                null,
                null,
                null,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "[]"
        );

        activeAttempt = PaymentAttempt.create(
                paymentId,
                1,
                PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD,
                "idemp-webhook-001"
        );
        activeAttempt.markProcessing();
    }

    @Test
    @DisplayName("Pago APPROVED en Mercado Pago actualiza Payment y PaymentAttempt a APPROVED")
    void shouldProcessApprovedPaymentWebhookSuccessfully() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        Instant approvedAt = Instant.now();
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), approvedAt
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.attemptStatus()).isEqualTo("APPROVED");

        // Verificamos persistencia
        verify(paymentRepository).save(activePayment);
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getApprovedAt()).isEqualTo(approvedAt);

        verify(paymentAttemptRepository).save(activeAttempt);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(activeAttempt.getProviderPaymentId()).isEqualTo(dataId);
        assertThat(activeAttempt.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("Pago in_process o pending en Mercado Pago actualiza estados a PROCESSING")
    void shouldProcessPendingPaymentWebhookSuccessfully() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "in_process", "pending_review_manual", new BigDecimal("50.00"), "PEN",
                "yape", "bank_transfer", paymentId.toString(), null
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("PROCESSING");
        assertThat(result.attemptStatus()).isEqualTo("PROCESSING");

        verify(paymentRepository).save(activePayment);
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        verify(paymentAttemptRepository).save(activeAttempt);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(activeAttempt.getPaymentMethod()).isEqualTo(PaymentMethod.YAPE);
    }

    @Test
    @DisplayName("Pago rejected en Mercado Pago actualiza intento a REJECTED manteniendo Payment en PENDING para reintento")
    void shouldProcessRejectedPaymentWebhookSuccessfully() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "rejected", "cc_rejected_insufficient_amount", new BigDecimal("50.00"), "PEN",
                "master", "credit_card", paymentId.toString(), null
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.attemptStatus()).isEqualTo("REJECTED");
        assertThat(result.paymentStatus()).isEqualTo("PENDING"); // Permite reintento

        verify(paymentAttemptRepository).save(activeAttempt);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.REJECTED);
        assertThat(activeAttempt.getErrorCode()).isEqualTo("rejected");
    }

    @Test
    @DisplayName("Protección contra eventos fuera de orden: evento pending extemporáneo no degrada un pago ya APPROVED")
    void shouldIgnoreOutOfOrderPendingEventIfPaymentAlreadyApproved() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        // El pago ya está aprobado
        activePayment.markApproved(Instant.now().minusSeconds(60));
        activeAttempt.markApproved(dataId, "approved", "accredited", PaymentMethod.CARD, Instant.now().minusSeconds(60));

        // Llega evento tardío con estado pending
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "pending", "waiting_payment", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), null
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("idempotencia aplicada sin degradación de estado");

        // El estado permanece inmutable en APPROVED
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
    }

    @Test
    @DisplayName("Discrepancia financiera en monto arroja ConflictException")
    void shouldRejectWebhookWhenAmountDiffers() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("20.00"), "PEN", // Monto incorrecto
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("PAYMENT_AMOUNT_MISMATCH"));
    }

    @Test
    @DisplayName("Discrepancia en divisa arroja ConflictException")
    void shouldRejectWebhookWhenCurrencyDiffers() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "USD", // Divisa incorrecta
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("PAYMENT_CURRENCY_MISMATCH"));
    }

    @Test
    @DisplayName("Firma inválida rechaza con SecurityException")
    void shouldRejectWhenSignatureIsInvalid() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=badhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(false);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Firma criptográfica x-signature inválida");

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Evento no soportado (ej. merchant_order) se ignora con respuesta segura")
    void shouldIgnoreUnsupportedEventsGracefully() {
        String dataId = "999";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "merchant_order"),
                null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isFalse();
        assertThat(result.message()).contains("Tipo de evento no soportado");

        verifyNoInteractions(mercadoPagoGateway);
    }

    @Test
    @DisplayName("Timeout o error de pasarela propaga excepción y NO marca el Payment como FAILED")
    void shouldNotMarkPaymentFailedWhenGatewayThrowsTimeoutOrError() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        when(mercadoPagoGateway.queryPayment(dataId))
                .thenThrow(new MercadoPagoException(0, "MP_TIMEOUT", "Timeout al consultar pago en MP"));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(MercadoPagoException.class);

        // El pago no se toca ni se marca como fallido
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("external_reference inexistente o incorrecta arroja excepción controlada")
    void shouldRejectInvalidExternalReference() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", "not-a-uuid", Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVALID_EXTERNAL_REFERENCE"));
    }

    @Test
    @DisplayName("H-02: Identificadores inconsistentes entre query params y cuerpo JSON son rechazados sin consultar Mercado Pago")
    void shouldRejectWhenQueryParamAndBodyDataIdAreInconsistent() {
        String xSignature = "ts=1700000000,v1=anyhash";
        String xRequestId = "req-111";
        String rawBody = "{\"data\":{\"id\":\"999888777\"},\"type\":\"payment\"}";

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", "111222333", "type", "payment"),
                rawBody
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconsistentes");

        verifyNoInteractions(signatureValidator);
        verifyNoInteractions(mercadoPagoGateway);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("H-02: Identificadores consistentes entre query params y cuerpo JSON son aceptados correctamente")
    void shouldAcceptWhenQueryParamAndBodyDataIdAreConsistent() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";
        String rawBody = "{\"data\":{\"id\":\"123456789\"},\"type\":\"payment\"}";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                rawBody
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);
        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        verify(paymentRepository).findByIdForUpdate(paymentId);
    }

    @Test
    @DisplayName("H-04: collector_id no coincide con la cuenta esperada rechaza con SecurityException")
    void shouldRejectWhenCollectorIdMismatchesExpectedAccount() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        properties.setExpectedCollectorId(99999999L);

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        // Pago pertenece a otra cuenta receptora (88888888L)
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now(),
                88888888L, false
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("collector_id");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("H-04: live_mode no coincide con el entorno configurado rechaza con SecurityException")
    void shouldRejectWhenLiveModeMismatchesExpectedEnvironment() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        properties.setExpectedLiveMode(true); // Se espera producción

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        // Pago realizado en sandbox (live_mode = false)
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now(),
                null, false
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("live_mode");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("H-01: Proceso del webhook adquiere bloqueo pesimista findByIdForUpdate sobre Payment")
    void shouldAcquirePessimisticLockOnPaymentDuringProcessing() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey)))
                .thenReturn(true);

        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature,
                xRequestId,
                Map.of("data.id", dataId, "type", "payment"),
                null
        );

        service.execute(command);

        // Verificamos que se ejecutó findByIdForUpdate y NO findById normal
        verify(paymentRepository).findByIdForUpdate(paymentId);
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Fase 4B: Webhook con pago APPROVED dispara ConfirmPurchaseService y confirma pedido")
    void shouldTriggerCommercialConfirmationWhenPaymentIsApproved() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        OrderConfirmation orderConfirmation = new OrderConfirmation(
                UUID.randomUUID(), "JAL-2026-000099", OrderStatus.CONFIRMED,
                UUID.randomUUID(), "Tienda Demo", "Valeria Ramos",
                new BigDecimal("50.00"), "PEN", Instant.now()
        );
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class))).thenReturn(orderConfirmation);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("JAL-2026-000099");

        verify(confirmPurchaseService).execute(argThat(cmd -> cmd.paymentId().equals(paymentId)));
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Fase 4B: Fallo comercial (reserva vencida) mantiene Payment APPROVED y registra incidencia duradera")
    void shouldPreserveApprovedFinancialStateAndRecordIncidenceWhenCommercialConfirmationFails() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        // La confirmación comercial falla porque la reserva expiró
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new ConflictException("RESERVATION_PROTECTION_EXPIRED", "Protección de pago vencida"));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        // El webhook responde éxito para no saturar reintentos en MP, pero registra la incidencia
        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("Protección de pago vencida");

        // El estado financiero se conserva estrictamente APPROVED
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        // Se registró la solicitud de reembolso/incidencia
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(activePayment.getRefundAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(activePayment.getRefundReference()).contains("INCIDENCE:Protección de pago vencida");

        // En el intento se registró la incidencia
        assertThat(activeAttempt.getErrorCode()).isEqualTo("COMMERCIAL_CONFIRMATION_FAILED");
    }

    @Test
    @DisplayName("Fase 4B: Webhook duplicado para pedido ya confirmado devuelve confirmación previa de forma idempotente")
    void shouldHandleDuplicateWebhookGracefullyWhenOrderAlreadyConfirmed() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        // El pago ya estaba aprobado
        activePayment.markApproved(Instant.now().minusSeconds(120));

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        OrderConfirmation existingOrder = new OrderConfirmation(
                UUID.randomUUID(), "JAL-2026-000010", OrderStatus.CONFIRMED,
                UUID.randomUUID(), "Tienda Demo", "Valeria Ramos",
                new BigDecimal("50.00"), "PEN", Instant.now().minusSeconds(110)
        );
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class))).thenReturn(existingOrder);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.message()).contains("JAL-2026-000010");
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("H-4B-01 Fallo Técnico: Error transitorio de DB (lock timeout / deadlock) se propaga para reintento y NO marca reembolso")
    void shouldRethrowTransientDatabaseExceptionWithoutRecordingRefund() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        // Simulamos un fallo transitorio de PostgreSQL (bloqueo / timeout)
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new CannotAcquireLockException("Lock wait timeout on PostgreSQL"));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        // Se propaga para que el controller responda 5xx y Mercado Pago reintente automáticamente
        assertThatThrownBy(() -> fullService.execute(command))
                .isInstanceOf(TransientDataAccessException.class);

        // El cobro sigue siendo APPROVED en base de datos
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        // NO se solicitó reembolso indebido
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("H-4B-01 Carrera Concurrente: Colisión UNIQUE con orden existente resuelve exitosamente sin marcar reembolso")
    void shouldResolveRaceWhenDataIntegrityViolationOccursAndOrderExists() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService, orderRepository
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        // confirmPurchaseService lanza colisión de unicidad uq_orders_payment_id
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_orders_payment_id"));

        // Al verificar en orderRepository, la orden SÍ fue creada por el hilo ganador
        Order winningOrder = mock(Order.class);
        when(winningOrder.getOrderNumber()).thenReturn("JAL-2026-000777");
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(winningOrder));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.message()).contains("JAL-2026-000777");
        assertThat(result.message()).contains("resolución concurrente");

        // Payment preserva APPROVED sin solicitud de reembolso
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("H-4B-01 Carrera Concurrente: Colisión UNIQUE sin orden asociada se propaga para revisión sin marcar reembolso")
    void shouldRethrowWhenDataIntegrityViolationOccursAndNoOrderExists() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService, orderRepository
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new DataIntegrityViolationException("foreign key error"));

        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        assertThatThrownBy(() -> fullService.execute(command))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("H-4B-01 Error Inesperado: RuntimeException se propaga para revisión y NO marca reembolso")
    void shouldRethrowUnexpectedExceptionWithoutRecordingRefund() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new IllegalStateException("Unexpected server error"));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        assertThatThrownBy(() -> fullService.execute(command))
                .isInstanceOf(IllegalStateException.class);

        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("H-4B-01 Fallo Comercial: Stock insuficiente registra incidencia duradera con refundStatus=PENDING y Payment APPROVED")
    void shouldRecordIncidenceAndPreserveApprovedWhenStockInsufficient() {
        String dataId = "123456789";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-111";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(dataId), eq(secretKey))).thenReturn(true);
        PaymentQueryResult queryResult = new PaymentQueryResult(
                dataId, "approved", "accredited", new BigDecimal("50.00"), "PEN",
                "visa", "credit_card", paymentId.toString(), Instant.now()
        );
        when(mercadoPagoGateway.queryPayment(dataId)).thenReturn(queryResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new ConflictException("INSUFFICIENT_STOCK", "Stock agotado para variante"));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", dataId, "type", "payment"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("Stock agotado para variante");

        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(activePayment.getRefundReference()).contains("INCIDENCE:Stock agotado para variante");
    }

    @Test
    @DisplayName("Orders API: Procesa exitosamente webhook de tipo order con pago aprobado y asocia providerOrderId y providerPaymentId")
    void shouldProcessOrderWebhookSuccessfullyWhenPaymentApproved() {
        String orderId = "ORD-MP-123456";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-01";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem paymentItem =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                        "PAY-TRANS-999", "approved", "accredited", new BigDecimal("50.00"), "PEN",
                        "visa", "credit_card", Instant.now()
                );

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "closed", "paid", new BigDecimal("50.00"), new BigDecimal("50.00"),
                        paymentId.toString(), List.of(paymentItem), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.providerPaymentId()).isEqualTo("PAY-TRANS-999");

        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(activeAttempt.getProviderPaymentId()).isEqualTo("PAY-TRANS-999");
    }

    @Test
    @DisplayName("Orders API: Ignora de forma segura orden sin transacciones de pago")
    void shouldIgnoreOrderWithoutTransactions() {
        String orderId = "ORD-EMPTY-001";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-02";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "opened", null, new BigDecimal("50.00"), BigDecimal.ZERO,
                        paymentId.toString(), List.of(), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "orders_v2"), null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isFalse();
        assertThat(result.message()).contains("sin transacciones de pago");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Orders API: Rechaza webhook con discrepancia de monto frente a pago registrado")
    void shouldRejectOrderWebhookOnAmountMismatch() {
        String orderId = "ORD-MISMATCH-001";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-03";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem paymentItem =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                        "PAY-TRANS-111", "approved", "accredited", new BigDecimal("30.00"), "PEN",
                        "visa", "credit_card", Instant.now()
                );

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "closed", null, new BigDecimal("30.00"), new BigDecimal("30.00"),
                        paymentId.toString(), List.of(paymentItem), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("El monto notificado (30.00) no coincide con el monto esperado (50.00)");
    }

    @Test
    @DisplayName("Orders API: Rechaza webhook con external_reference inválido o no correspondiente a UUID")
    void shouldRejectOrderWebhookOnInvalidExternalReference() {
        String orderId = "ORD-BAD-REF";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-04";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem paymentItem =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                        "PAY-1", "approved", "accredited", new BigDecimal("50.00"), "PEN", "visa", "credit_card", Instant.now()
                );

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "closed", null, new BigDecimal("50.00"), new BigDecimal("50.00"),
                        "not-a-valid-uuid", List.of(paymentItem), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("external_reference no posee un formato de UUID válido");
    }

    @Test
    @DisplayName("Orders API: Rechaza webhook si collector_id no coincide con el esperado")
    void shouldRejectOrderWebhookOnCollectorMismatch() {
        properties.setExpectedCollectorId(999888777L);
        String orderId = "ORD-COLLECTOR-MISMATCH";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-05";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "closed", null, new BigDecimal("50.00"), new BigDecimal("50.00"),
                        paymentId.toString(), List.of(), 111222333L, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("collector_id=111222333");
    }

    @Test
    @DisplayName("Orders API: Orden con estado in_process no aprueba pago y actualiza intento a PENDING")
    void shouldProcessOrderPendingPaymentWithoutApproving() {
        String orderId = "ORD-PENDING-001";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-06";

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem paymentItem =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                        "PAY-PEND-123", "in_process", "pending_review_manual", new BigDecimal("50.00"), "PEN",
                        "visa", "credit_card", null
                );

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "opened", null, new BigDecimal("50.00"), BigDecimal.ZERO,
                        paymentId.toString(), List.of(paymentItem), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        ProcessMercadoPagoWebhookResult result = service.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("PROCESSING");
        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(activeAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(activeAttempt.getProviderPaymentId()).isEqualTo("PAY-PEND-123");
    }

    @Test
    @DisplayName("Orders API: Confirmación comercial con reserva vencida genera incidencia duradera y conserva APPROVED")
    void shouldRecordIncidenceWhenOrderConfirmedWithExpiredReservation() {
        String orderId = "ORD-EXPIRED-RESERVATION";
        String xSignature = "ts=1700000000,v1=validhash";
        String xRequestId = "req-orders-07";

        ConfirmPurchaseService confirmPurchaseService = mock(ConfirmPurchaseService.class);
        ProcessMercadoPagoWebhookService fullService = new ProcessMercadoPagoWebhookService(
                signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService
        );

        when(signatureValidator.isValid(eq(xSignature), eq(xRequestId), eq(orderId), eq(secretKey))).thenReturn(true);

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem paymentItem =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                        "PAY-APPR-999", "approved", "accredited", new BigDecimal("50.00"), "PEN",
                        "visa", "credit_card", Instant.now()
                );

        com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult orderResult =
                new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                        orderId, "closed", null, new BigDecimal("50.00"), new BigDecimal("50.00"),
                        paymentId.toString(), List.of(paymentItem), null, null
                );

        when(mercadoPagoGateway.queryOrder(orderId)).thenReturn(orderResult);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(activePayment));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(activeAttempt));

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada."));

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, xRequestId, Map.of("data.id", orderId, "type", "order"), null
        );

        ProcessMercadoPagoWebhookResult result = fullService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("La reserva de capacidad se encuentra expirada.");

        assertThat(activePayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(activePayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(activePayment.getRefundReference()).contains("INCIDENCE:La reserva de capacidad se encuentra expirada.");
    }

}

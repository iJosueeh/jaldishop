package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.OrderResult;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderRefundResponse;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderRequest;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpPaymentResponse;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpPreferenceRequest;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpPreferenceResponse;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpRefundRequest;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpRefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@Component
public class MercadoPagoRestClientAdapter implements MercadoPagoGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoRestClientAdapter.class);

    private static final String PREFERENCES_PATH = "/checkout/preferences";
    private static final String PAYMENTS_PATH = "/v1/payments/{id}";
    private static final String REFUNDS_PATH = "/v1/payments/{id}/refunds";
    private static final String ORDERS_PATH = "/v1/orders";
    private static final String ORDER_DETAIL_PATH = "/v1/orders/{id}";
    private static final String ORDER_REFUND_PATH = "/v1/orders/{id}/refund";

    private final RestClient restClient;
    private final MercadoPagoProperties properties;
    private final CheckoutCreationRequestStore creationRequestStore;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoRestClientAdapter(MercadoPagoProperties properties, CheckoutCreationRequestStore creationRequestStore,
                                        ObjectMapper objectMapper) {
        this(buildDefaultRestClient(RestClient.builder(), properties), properties, creationRequestStore, objectMapper);
    }

    public MercadoPagoRestClientAdapter(MercadoPagoProperties properties) {
        this(RestClient.builder(), properties);
    }

    public MercadoPagoRestClientAdapter(RestClient.Builder restClientBuilder, MercadoPagoProperties properties) {
        this(buildDefaultRestClient(restClientBuilder, properties), properties);
    }

    public MercadoPagoRestClientAdapter(RestClient restClient, MercadoPagoProperties properties) {
        this(restClient, properties, null, new ObjectMapper());
    }

    public MercadoPagoRestClientAdapter(RestClient restClient, MercadoPagoProperties properties,
                                       CheckoutCreationRequestStore creationRequestStore, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.creationRequestStore = creationRequestStore;
        this.objectMapper = objectMapper;
    }

    private static RestClient buildDefaultRestClient(RestClient.Builder builder, MercadoPagoProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public PreferenceResult createPreference(CreatePreferenceCommand command, UUID attemptId) {
        validateAccessToken();

        Object requestBody = toPreferenceRequest(command);
        requestBody = creationRequestStore.saveBeforeSend(attemptId, "PREFERENCES", command.idempotencyKey(),
                objectMapper.writeValueAsBytes(requestBody));
        log.info("Creando preferencia de Checkout Pro para externalReference: {}", command.externalReference());

        try {
            var requestSpec = restClient.post()
                    .uri(PREFERENCES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
                requestSpec.header("X-Idempotency-Key", command.idempotencyKey());
            }

            MpPreferenceResponse response = requestSpec
                    .body(requestBody)
                    .retrieve()
                    .body(MpPreferenceResponse.class);

            if (response == null || response.id() == null || response.id().isBlank() ||
                    response.initPoint() == null || response.initPoint().isBlank()) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al crear preferencia: faltan id o init_point."
                );
            }

            log.info("Preferencia creada exitosamente con ID: {}", response.id());
            return new PreferenceResult(response.id(), response.initPoint(), response.sandboxInitPoint());

        } catch (HttpClientErrorException ex) {
            handleClientError(ex, "crear preferencia");
            throw ex; // inalcanzable, handleClientError siempre lanza excepción
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "crear preferencia");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al comunicarse con Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
        } catch (SecurityException ex) {
            throw ex;
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.error("Timeout o error de conexión al comunicarse con Mercado Pago: {}", ex.getMessage());
                throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
            }
            log.error("Error inesperado al comunicarse con Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);
        }
    }

    @Override
    public PaymentQueryResult queryPayment(String providerPaymentId) {
        validateAccessToken();

        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            throw new IllegalArgumentException("providerPaymentId no puede ser nulo ni estar vacío");
        }

        log.info("Consultando pago en Mercado Pago con ID: {}", providerPaymentId);

        try {
            MpPaymentResponse response = restClient.get()
                    .uri(PAYMENTS_PATH, providerPaymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MpPaymentResponse.class);

            if (response == null || response.id() == null || response.status() == null || response.status().isBlank()) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al consultar pago: faltan id o status."
                );
            }

            Instant dateApproved = parseIsoInstant(response.dateApproved());

            log.info("Pago {} consultado exitosamente con status: {}", response.id(), response.status());

            return new PaymentQueryResult(
                    String.valueOf(response.id()),
                    response.status(),
                    response.statusDetail(),
                    response.transactionAmount(),
                    response.currencyId(),
                    response.paymentMethodId(),
                    response.paymentTypeId(),
                    response.externalReference(),
                    dateApproved,
                    response.collectorId(),
                    response.liveMode()
            );

        } catch (HttpClientErrorException ex) {
            handleClientError(ex, "consultar pago");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "consultar pago");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al consultar pago en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado al consultar pago en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);
        }
    }

    @Override
    public RefundResult executeRefund(ExecuteRefundCommand command) {
        validateAccessToken();

        if ("ORDERS".equalsIgnoreCase(command.checkoutMode())) {
            return executeOrdersRefund(command);
        }
        return executePreferencesRefund(command);
    }

    private RefundResult executePreferencesRefund(ExecuteRefundCommand command) {
        log.info("Ejecutando reembolso PREFERENCES para providerPaymentId: {} con idempotencyKey: {}",
                command.providerPaymentId(), command.idempotencyKey());

        try {
            var requestSpec = restClient.post()
                    .uri(REFUNDS_PATH, command.providerPaymentId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .header("X-Idempotency-Key", command.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (command.amount() != null) {
                requestSpec.body(new MpRefundRequest(command.amount()));
            } else {
                requestSpec.body("{}");
            }

            MpRefundResponse response = requestSpec
                    .retrieve()
                    .body(MpRefundResponse.class);

            if (response == null || response.id() == null) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al crear reembolso: falta id de reembolso."
                );
            }

            Instant dateCreated = parseIsoInstant(response.dateCreated());
            log.info("Reembolso {} creado/obtenido exitosamente para pago {} con status: {}",
                    response.id(), response.paymentId(), response.status());

            return new RefundResult(
                    String.valueOf(response.id()),
                    String.valueOf(response.paymentId()),
                    response.amount(),
                    response.status(),
                    dateCreated != null ? dateCreated : Instant.now()
            );

        } catch (HttpClientErrorException ex) {
            handleClientError(ex, "ejecutar reembolso");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "ejecutar reembolso");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al ejecutar reembolso en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso en Mercado Pago", ex);
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.error("Timeout o error de conexión al ejecutar reembolso en Mercado Pago: {}", ex.getMessage());
                throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso en Mercado Pago", ex);
            }
            log.error("Error inesperado al ejecutar reembolso en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar reembolso en Mercado Pago", ex);
        }
    }

    private RefundResult executeOrdersRefund(ExecuteRefundCommand command) {
        log.info("Ejecutando reembolso ORDERS para providerOrderId: {} con idempotencyKey: {}",
                command.providerOrderId(), command.idempotencyKey());

        try {
            var requestSpec = restClient.post()
                    .uri(ORDER_REFUND_PATH, command.providerOrderId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .header("X-Idempotency-Key", command.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (command.amount() != null) {
                requestSpec.body(new MpRefundRequest(command.amount()));
            } else {
                requestSpec.body("{}");
            }

            MpOrderRefundResponse response = requestSpec
                    .retrieve()
                    .body(MpOrderRefundResponse.class);

            if (response == null || response.id() == null) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al crear reembolso de orden: falta id de orden."
                );
            }

            String refundId = response.id();
            String paymentId = command.providerPaymentId();
            BigDecimal amount = command.amount();
            String status = response.statusDetail() != null ? response.statusDetail() : response.status();
            Instant dateCreated = Instant.now();

            if (response.transactions() != null && response.transactions().refunds() != null && !response.transactions().refunds().isEmpty()) {
                var refundItem = response.transactions().refunds().getFirst();
                if (refundItem.id() != null) {
                    refundId = String.valueOf(refundItem.id());
                }
                if (refundItem.paymentId() != null) {
                    paymentId = String.valueOf(refundItem.paymentId());
                }
                if (refundItem.amount() != null) {
                    amount = refundItem.amount();
                }
                if (refundItem.status() != null) {
                    status = refundItem.status();
                }
                if (refundItem.dateCreated() != null) {
                    Instant parsed = parseIsoInstant(refundItem.dateCreated());
                    if (parsed != null) {
                        dateCreated = parsed;
                    }
                }
            }

            log.info("Reembolso {} creado/obtenido exitosamente para orden {} (pago {}) con status: {}",
                    refundId, command.providerOrderId(), paymentId, status);

            return new RefundResult(
                    refundId,
                    paymentId,
                    amount,
                    status,
                    dateCreated
            );

        } catch (HttpClientErrorException ex) {
            handleClientError(ex, "ejecutar reembolso de orden");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "ejecutar reembolso de orden");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al ejecutar reembolso de orden en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso de orden en Mercado Pago", ex);
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.error("Timeout o error de conexión al ejecutar reembolso de orden en Mercado Pago: {}", ex.getMessage());
                throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso de orden en Mercado Pago", ex);
            }
            log.error("Error inesperado al ejecutar reembolso de orden en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar reembolso de orden en Mercado Pago", ex);
        }
    }

    @Override
    public List<RefundQueryResult> queryPaymentRefunds(String providerPaymentId) {
        validateAccessToken();

        log.info("Consultando reembolsos existentes para providerPaymentId: {}", providerPaymentId);

        try {
            MpRefundResponse[] response = restClient.get()
                    .uri(REFUNDS_PATH, providerPaymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MpRefundResponse[].class);

            if (response == null || response.length == 0) {
                return List.of();
            }

            return java.util.Arrays.stream(response)
                    .map(r -> new RefundQueryResult(
                            String.valueOf(r.id()),
                            String.valueOf(r.paymentId()),
                            r.amount(),
                            r.status(),
                            parseIsoInstant(r.dateCreated())
                    ))
                    .toList();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                // Si el pago no existe o no tiene reembolsos en 404
                return List.of();
            }
            handleClientError(ex, "consultar reembolsos de pago");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "consultar reembolsos de pago");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al consultar reembolsos en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al consultar reembolsos en Mercado Pago", ex);
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado al consultar reembolsos en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al consultar reembolsos en Mercado Pago", ex);
        }
    }

    @Override
    public OrderResult createOrder(CreateOrderCommand command, UUID attemptId) {
        validateAccessToken();

        Object requestBody = toOrderRequest(command);
        requestBody = creationRequestStore.saveBeforeSend(attemptId, "ORDERS", command.idempotencyKey(),
                objectMapper.writeValueAsBytes(requestBody));
        log.info("Creando orden de Checkout Pro mediante Orders API para externalReference: {}", command.externalReference());

        try {
            var requestSpec = restClient.post()
                    .uri(ORDERS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
                requestSpec.header("X-Idempotency-Key", command.idempotencyKey());
            }

            MpOrderResponse response = requestSpec
                    .body(requestBody)
                    .retrieve()
                    .body(MpOrderResponse.class);

            if (response == null || response.id() == null || response.id().isBlank() ||
                    response.checkoutUrl() == null || response.checkoutUrl().isBlank()) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al crear orden: faltan id o checkout_url."
                );
            }

            validateCreatedOrderEnvironment(response, attemptId);

            log.info("Orden creada exitosamente en Mercado Pago con ID: {}", response.id());
            return new OrderResult(response.id(), response.checkoutUrl(), response.status(), response.totalAmount());

        } catch (HttpClientErrorException ex) {
            if (OrderCreationRejection.isInvalidTotalAmount(ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), objectMapper)) {
                throw new MercadoPagoException(400, OrderCreationRejection.INVALID_TOTAL_AMOUNT,
                        "Order creation rejected: invalid_total_amount");
            }
            handleClientError(ex, "crear orden");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "crear orden");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al comunicarse con Mercado Pago (crear orden): {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
        } catch (SecurityException ex) {
            throw ex;
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.error("Timeout o error de conexión al comunicarse con Mercado Pago (crear orden): {}", ex.getMessage());
                throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
            }
            log.error("Error inesperado al comunicarse con Mercado Pago (crear orden): {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);
        }
    }

    @Override
    public OrderQueryResult queryOrder(String orderId) {
        validateAccessToken();

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId no puede ser nulo ni estar vacío");
        }

        log.info("Consultando orden en Mercado Pago con ID: {}", orderId);

        try {
            MpOrderResponse response = restClient.get()
                    .uri(ORDER_DETAIL_PATH, orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MpOrderResponse.class);

            if (response == null || response.id() == null || response.status() == null || response.status().isBlank()) {
                throw new MercadoPagoException(
                        200,
                        "MP_INVALID_RESPONSE",
                        "Respuesta incompleta de Mercado Pago al consultar orden: faltan id o status."
                );
            }
            if (!orderId.equals(response.id())) throw invalidOrderField("id");

            List<OrderQueryResult.OrderPaymentItem> payments = List.of();
            if (response.transactions() != null && response.transactions().payments() != null) {
                payments = response.transactions().payments().stream()
                        .map(p -> normalizeOrderPayment(response, p))
                        .toList();
                var identities = new java.util.HashSet<String>();
                for (var payment : payments) {
                    if (!identities.add(payment.providerPaymentId())) throw invalidOrderField("payments.id");
                }
            }

            List<OrderQueryResult.OrderRefundItem> refunds = List.of();
            if (response.transactions() != null && response.transactions().refunds() != null) {
                refunds = response.transactions().refunds().stream()
                        .filter(r -> r != null && r.id() != null)
                        .map(r -> new OrderQueryResult.OrderRefundItem(
                                String.valueOf(r.id()),
                                r.paymentId() != null ? String.valueOf(r.paymentId()) : null,
                                r.amount(),
                                r.status(),
                                parseIsoInstant(r.dateCreated())
                        ))
                        .toList();
            }

            log.info("Orden {} consultada exitosamente con status: {}, total pagos: {}, total reembolsos: {}",
                    response.id(), response.status(), payments.size(), refunds.size());

            return new OrderQueryResult(
                    response.id(),
                    response.status(),
                    response.statusDetail(),
                    response.totalAmount(),
                    response.totalPaidAmount(),
                    response.externalReference(),
                    payments,
                    refunds,
                    normalizeOrderCollector(response),
                    response.liveMode(),
                    response.checkoutUrl()
            );

        } catch (HttpClientErrorException ex) {
            handleClientError(ex, "consultar orden");
            throw ex;
        } catch (HttpServerErrorException ex) {
            handleServerError(ex, "consultar orden");
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout o error de conexión al consultar orden en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (RestClientException ex) {
            // Decoding failures may contain response fragments, so neither log nor attach them.
            log.error("Respuesta de Orders no interpretable al consultar orden en Mercado Pago");
            throw invalidOrderField("response");
        } catch (Exception ex) {
            log.error("Error inesperado al consultar orden en Mercado Pago");
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al consultar orden en Mercado Pago");
        }
    }

    private OrderQueryResult.OrderPaymentItem normalizeOrderPayment(
            MpOrderResponse order, MpOrderResponse.MpOrderPayment payment) {
        if (payment == null) throw invalidOrderField("payments");
        Object rawId = payment.id();
        if (!(rawId instanceof String || rawId instanceof Integer || rawId instanceof Long
                || rawId instanceof java.math.BigInteger)) throw invalidOrderField("payments.id");
        String id = String.valueOf(rawId);
        if (id.isBlank() || (rawId instanceof Number && new BigDecimal(id).signum() <= 0))
            throw invalidOrderField("payments.id");
        if (payment.status() == null || payment.status().isBlank()) throw invalidOrderField("payments.status");
        // Same semantic amount under two wire names; never substitute paid_amount or order totals.
        BigDecimal amount = payment.amount();
        if (amount != null && payment.transactionAmount() != null
                && amount.compareTo(payment.transactionAmount()) != 0) throw invalidOrderField("payments.amount");
        if (amount == null) amount = payment.transactionAmount();
        if (amount == null || amount.signum() <= 0) throw invalidOrderField("payments.amount");
        if (payment.paidAmount() != null && payment.paidAmount().signum() < 0)
            throw invalidOrderField("payments.paid_amount");
        String currency = compatibleOrderText(order.currency(), payment.currencyId(), "currency", true);
        if (currency == null) throw invalidOrderField("currency");
        var method = payment.paymentMethod();
        return new OrderQueryResult.OrderPaymentItem(id, payment.status(), payment.statusDetail(), amount,
                currency,
                compatibleOrderText(method == null ? null : method.id(), payment.paymentMethodId(), "payment_method.id", false),
                compatibleOrderText(method == null ? null : method.type(), payment.paymentTypeId(), "payment_method.type", false),
                parseIsoInstant(payment.dateApproved()), payment.paidAmount());
    }

    private void validateCreatedOrderEnvironment(MpOrderResponse response, UUID attemptId) {
        if (Boolean.FALSE.equals(properties.getExpectedLiveMode())) {
            // 1. live_mode explícito en true está estrictamente prohibido
            if (Boolean.TRUE.equals(response.liveMode())) {
                log.error("CRÍTICO DE SEGURIDAD: Orden creada en Mercado Pago con ID {} contiene live_mode=true en entorno TEST. Intento {}. Bloqueando operación.",
                        response.id(), attemptId);
                throw new MercadoPagoOrderSecurityException(
                        "Entorno remoto no autorizado al crear orden: live_mode=true explícito.",
                        response.id(),
                        true
                );
            }

            // 2. Verificación de la cuenta colectora/vendedora esperada
            Long sellerId;
            try {
                sellerId = response.resolvedCollectorId();
            } catch (IllegalStateException ex) {
                log.error("CRÍTICO DE SEGURIDAD: Orden creada en Mercado Pago con ID {} presenta inconsistencia de vendedor: {}. Intento {}. Bloqueando operación.",
                        response.id(), ex.getMessage(), attemptId);
                throw new MercadoPagoOrderSecurityException(
                        "Cuenta de vendedor contradictoria o inválida al crear orden: " + ex.getMessage(),
                        response.id(),
                        response.liveMode()
                );
            }

            if (properties.getExpectedCollectorId() != null && properties.getExpectedCollectorId() > 0) {
                if (sellerId == null || !properties.getExpectedCollectorId().equals(sellerId)) {
                    log.error("CRÍTICO DE SEGURIDAD: Orden creada en Mercado Pago con ID {} tiene user_id/collector_id={} (esperado {}). Intento {}. Bloqueando operación.",
                            response.id(), sellerId, properties.getExpectedCollectorId(), attemptId);
                    throw new MercadoPagoOrderSecurityException(
                            "Cuenta de vendedor ausente o no autorizada al crear orden (user_id=" + sellerId + ", esperado=" + properties.getExpectedCollectorId() + ").",
                            response.id(),
                            response.liveMode()
                    );
                }
            }

            // 3. Compatibilidad del formato de identificador remoto con entorno TEST
            if (!com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidator.isCompatibleTestOrderId(response.id())) {
                log.error("CRÍTICO DE SEGURIDAD: Orden creada en Mercado Pago con ID {} no posee identificador compatible con entorno TEST. Intento {}. Bloqueando operación.",
                        response.id(), attemptId);
                throw new MercadoPagoOrderSecurityException(
                        "Identificador de orden incompatible con entorno TEST (id=" + response.id() + ").",
                        response.id(),
                        response.liveMode()
                );
            }
        }
    }

    private Long normalizeOrderCollector(MpOrderResponse order) {
        try {
            return order.resolvedCollectorId();
        } catch (IllegalStateException ex) {
            throw invalidOrderField(ex.getMessage().contains("user_id") ? "user_id" : "collector_id");
        }
    }

    private String compatibleOrderText(String canonical, String historical, String field, boolean ignoreCase) {
        if ((canonical != null && canonical.isBlank()) || (historical != null && historical.isBlank()))
            throw invalidOrderField(field);
        if (canonical != null && historical != null
                && !(ignoreCase ? canonical.equalsIgnoreCase(historical) : canonical.equals(historical)))
            throw invalidOrderField(field);
        return canonical != null ? canonical : historical;
    }

    private MercadoPagoException invalidOrderField(String field) {
        return new MercadoPagoException(200, "MP_INVALID_RESPONSE",
                "Respuesta de Orders incompleta o contradictoria: " + field);
    }

    private void validateAccessToken() {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new MercadoPagoException(
                    401,
                    "MP_MISSING_CREDENTIALS",
                    "No se ha configurado el access token de Mercado Pago (MERCADOPAGO_ACCESS_TOKEN)."
            );
        }
    }

    private void handleClientError(HttpClientErrorException ex, String operation) {
        int status = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();
        log.warn("Mercado Pago rechazó solicitud ({}) con HTTP {}: {}", operation, status, sanitizeLog(responseBody));

        if (status == 400) {
            throw new MercadoPagoException(400, "MP_BAD_REQUEST", "Solicitud rechazada por Mercado Pago: " + sanitizeLog(responseBody), ex);
        } else if (status == 401) {
            throw new MercadoPagoException(401, "MP_UNAUTHORIZED", "Credenciales inválidas o no autorizadas en Mercado Pago", ex);
        } else if (status == 404) {
            throw new MercadoPagoException(404, "MP_NOT_FOUND", "Recurso no encontrado en Mercado Pago", ex);
        } else if (status == 429) {
            throw new MercadoPagoException(429, "MP_RATE_LIMIT", "Límite de solicitudes alcanzado en Mercado Pago", ex);
        } else {
            throw new MercadoPagoException(status, "MP_CLIENT_ERROR", "Error de cliente devuelto por Mercado Pago: " + status, ex);
        }
    }

    private void handleServerError(HttpServerErrorException ex, String operation) {
        int status = ex.getStatusCode().value();
        log.error("Error de servidor en Mercado Pago ({}) con HTTP {}: {}", operation, status, sanitizeLog(ex.getResponseBodyAsString()));
        throw new MercadoPagoException(status, "MP_SERVER_ERROR", "Error de servidor en Mercado Pago: HTTP " + status, ex);
    }

    private MpPreferenceRequest toPreferenceRequest(CreatePreferenceCommand cmd) {
        List<MpPreferenceRequest.MpItem> mpItems = cmd.items().stream()
                .map(item -> new MpPreferenceRequest.MpItem(
                        item.id(),
                        item.title(),
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.currency() != null ? item.currency() : "PEN"
                ))
                .toList();

        MpPreferenceRequest.MpPayer mpPayer = null;
        if (cmd.payer() != null) {
            mpPayer = new MpPreferenceRequest.MpPayer(
                    cmd.payer().name(),
                    cmd.payer().surname(),
                    cmd.payer().email(),
                    cmd.payer().phone() != null ? new MpPreferenceRequest.MpPhone("", cmd.payer().phone()) : null
            );
        }

        MpPreferenceRequest.MpBackUrls backUrls = null;
        if (cmd.successBackUrl() != null || cmd.failureBackUrl() != null || cmd.pendingBackUrl() != null) {
            backUrls = new MpPreferenceRequest.MpBackUrls(
                    cmd.successBackUrl(),
                    cmd.pendingBackUrl(),
                    cmd.failureBackUrl()
            );
        }

        return new MpPreferenceRequest(
                mpItems,
                mpPayer,
                backUrls,
                "approved",
                cmd.notificationUrl(),
                cmd.externalReference(),
                cmd.statementDescriptor() != null ? cmd.statementDescriptor() : "JALDI SHOP"
        );
    }

    private MpOrderRequest toOrderRequest(CreateOrderCommand cmd) {
        List<MpOrderRequest.MpOrderItem> mpItems = cmd.items().stream()
                .map(item -> new MpOrderRequest.MpOrderItem(
                        item.title(),
                        StringUtils.hasText(item.description()) ? item.description().trim() : null,
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        MpOrderRequest.MpOrderPayer mpPayer = null;
        if (cmd.payer() != null) {
            String firstName = StringUtils.hasText(cmd.payer().name()) ? cmd.payer().name().trim() : null;
            String lastName = StringUtils.hasText(cmd.payer().surname()) ? cmd.payer().surname().trim() : null;
            String email = StringUtils.hasText(cmd.payer().email()) ? cmd.payer().email().trim() : null;

            MpOrderRequest.MpOrderPhone phone = null;
            if (StringUtils.hasText(cmd.payer().phone())) {
                phone = new MpOrderRequest.MpOrderPhone(null, cmd.payer().phone().trim());
            }

            if (firstName != null || lastName != null || email != null || phone != null) {
                mpPayer = new MpOrderRequest.MpOrderPayer(
                        firstName,
                        lastName,
                        email,
                        phone
                );
            }
        }

        String successUrl = StringUtils.hasText(cmd.successBackUrl()) ? cmd.successBackUrl().trim() : null;
        String failureUrl = StringUtils.hasText(cmd.failureBackUrl()) ? cmd.failureBackUrl().trim() : null;
        String pendingUrl = StringUtils.hasText(cmd.pendingBackUrl()) ? cmd.pendingBackUrl().trim() : null;

        MpOrderRequest.MpOrderConfig config = null;
        if (successUrl != null || failureUrl != null || pendingUrl != null) {
            config = new MpOrderRequest.MpOrderConfig(
                    new MpOrderRequest.MpOnlineConfig(
                            successUrl,
                            failureUrl,
                            pendingUrl,
                            "approved"
                    )
            );
        }

        return new MpOrderRequest(
                "online",
                "manual",
                "automatic",
                cmd.totalAmount(),
                cmd.externalReference(),
                mpItems,
                mpPayer,
                config,
                cmd.expirationTime()
        );
    }

    private Instant parseIsoInstant(String isoDateString) {
        if (isoDateString == null || isoDateString.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(isoDateString);
        } catch (DateTimeParseException e) {
            // Intentar offset date-time si viene con zona
            try {
                return java.time.OffsetDateTime.parse(isoDateString).toInstant();
            } catch (DateTimeParseException ex) {
                log.warn("No se pudo parsear fecha de Mercado Pago '{}': {}", isoDateString, ex.getMessage());
                return null;
            }
        }
    }

    private String sanitizeLog(String raw) {
        if (raw == null) {
            return "";
        }
        // Truncar respuestas muy largas y remover tokens si aparecieran
        String sanitized = raw.replaceAll("(?i)bearer\\s+[a-zA-Z0-9_.-]+", "Bearer [REDACTED]");
        if (sanitized.length() > 300) {
            return sanitized.substring(0, 300) + "... [truncado]";
        }
        return sanitized;
    }

    private boolean isTimeoutOrConnectionError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.springframework.web.client.ResourceAccessException
                    || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.ConnectException
                    || (current.getMessage() != null && current.getMessage().toLowerCase(java.util.Locale.ROOT).contains("timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}

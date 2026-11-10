package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.OrderResult;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoRestClientAdapterTest {

    private static final String BASE_URL = "https://api.mercadopago.com";
    private static final String ACCESS_TOKEN = "TEST-ACCESS-TOKEN-xyz123";

    private MockRestServiceServer mockServer;
    private MercadoPagoRestClientAdapter adapter;
    private MercadoPagoProperties properties;

    @Test
    void documentsConfirmedOrdersValidationRejectionWithoutPersistingResponse() {
        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"code\":\"invalid_total_amount\",\"message\":\"private data\"}]}"));
        var command = new CreateOrderCommand("payment-id", new BigDecimal("10.00"),
                List.of(new CreateOrderCommand.OrderItem("item", "Item", null, 1, new BigDecimal("10.00"), "PEN")),
                null, null, null, null, null, "JALDI SHOP", "key");
        assertThatThrownBy(() -> adapter.createOrder(command, UUID.randomUUID()))
                .isInstanceOfSatisfying(MercadoPagoException.class, ex -> {
                    assertThat(OrderCreationRejection.isConfirmed(ex)).isTrue();
                    assertThat(ex.getMessage()).doesNotContain("private data");
                    assertThat(ex.getCause()).isNull();
                });
        mockServer.verify();
    }

    @Test
    void persistsExactPreferenceBodyBeforeSending() {
        var store = mock(CheckoutCreationRequestStore.class);
        var saved = new AtomicReference<byte[]>();
        var attemptId = UUID.randomUUID();
        when(store.saveBeforeSend(eq(attemptId), eq("PREFERENCES"), eq("key"), any(byte[].class)))
                .thenAnswer(inv -> { byte[] bytes = inv.getArgument(3); saved.set(bytes); return bytes; });
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var tested = new MercadoPagoRestClientAdapter(builder.build(), properties, store, new ObjectMapper());
        server.expect(requestTo(BASE_URL + "/checkout/preferences"))
                .andExpect(request -> assertThat(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsBytes())
                        .isEqualTo(saved.get()))
                .andExpect(jsonPath("$.external_reference").value("payment-id"))
                .andRespond(withSuccess("{\"id\":\"pref\",\"init_point\":\"https://example.test\"}", MediaType.APPLICATION_JSON));
        var cmd = new CreatePreferenceCommand("payment-id", "title", List.of(
                new CreatePreferenceCommand.PreferenceItem("id", "item", null, 1, BigDecimal.TEN, "PEN")),
                null, null, null, null, null, null, "key");
        tested.createPreference(cmd, attemptId);
        server.verify();
        verify(store).saveBeforeSend(eq(attemptId), eq("PREFERENCES"), eq("key"), any(byte[].class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void persistsExactOrderBodyBeforeSending(boolean explicitAttempt) {
        var store = mock(CheckoutCreationRequestStore.class);
        var saved = new AtomicReference<byte[]>();
        var attemptId = UUID.randomUUID();
        var reference = explicitAttempt ? "pa1_" + attemptId : UUID.randomUUID().toString();
        when(store.saveBeforeSend(eq(attemptId), eq("ORDERS"), eq("key"), any(byte[].class)))
                .thenAnswer(inv -> { byte[] original = inv.getArgument(3);
                    byte[] persisted = (" " + new String(original, java.nio.charset.StandardCharsets.UTF_8)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    saved.set(persisted); return persisted; });
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var tested = new MercadoPagoRestClientAdapter(builder.build(), properties, store, new ObjectMapper());
        server.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(request -> assertThat(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsBytes())
                        .isEqualTo(saved.get()))
                .andExpect(jsonPath("$.external_reference").value(reference))
                .andExpect(jsonPath("$.expiration_time").value("PT9M30S"))
                .andRespond(withSuccess("{\"id\":\"order\",\"checkout_url\":\"https://example.test\"}", MediaType.APPLICATION_JSON));
        var cmd = new CreateOrderCommand(reference, BigDecimal.TEN, List.of(
                new CreateOrderCommand.OrderItem("id", "item", null, 1, BigDecimal.TEN, "PEN")),
                null, null, null, null, null, null, "key", "PT9M30S");
        tested.createOrder(cmd, attemptId);
        server.verify();
        verify(store).saveBeforeSend(eq(attemptId), eq("ORDERS"), eq("key"), any(byte[].class));
    }

    @Test
    void doesNotSendWhenPersistenceRejectsDuplicate() {
        var store = mock(CheckoutCreationRequestStore.class);
        when(store.saveBeforeSend(any(), any(), any(), any())).thenThrow(
                new com.jaldishop.backend.shared.exception.ConflictException("PAYMENT_IN_PROGRESS", "Revisión"));
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var tested = new MercadoPagoRestClientAdapter(builder.build(), properties, store, new ObjectMapper());
        var cmd = new CreatePreferenceCommand("payment-id", "title", List.of(
                new CreatePreferenceCommand.PreferenceItem("id", "item", null, 1, BigDecimal.TEN, "PEN")),
                null, null, null, null, null, null, "key");
        assertThatThrownBy(() -> tested.createPreference(cmd, UUID.randomUUID()))
                .isInstanceOf(com.jaldishop.backend.shared.exception.ConflictException.class);
        server.verify();
    }

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setAccessToken(ACCESS_TOKEN);
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(3000);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        var store = mock(CheckoutCreationRequestStore.class);
        lenient().when(store.saveBeforeSend(any(), any(), any(), any())).thenAnswer(inv -> inv.getArgument(3));
        adapter = new MercadoPagoRestClientAdapter(restClient, properties, store, new ObjectMapper());
    }

    @Test
    @DisplayName("Crear preferencia exitosamente con respuesta válida de Mercado Pago")
    void shouldCreatePreferenceSuccessfully() {
        String responseJson = """
                {
                  "id": "pref-123456789",
                  "init_point": "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-123456789",
                  "sandbox_init_point": "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-123456789",
                  "external_reference": "ext-pay-001"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.external_reference").value("ext-pay-001"))
                .andExpect(jsonPath("$.items[0].title").value("Producto Test"))
                .andExpect(jsonPath("$.items[0].unit_price").value(55.0))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-pay-001",
                "Compra JaldiShop",
                List.of(new CreatePreferenceCommand.PreferenceItem(
                        "var-1", "Producto Test", "Desc", 1, new BigDecimal("55.00"), "PEN"
                )),
                new CreatePreferenceCommand.PayerInfo("Juan", "Perez", "juan@example.com", "987654321"),
                "https://api.jaldishop.com/webhooks/mercadopago",
                "https://jaldishop.com/checkout/success",
                "https://jaldishop.com/checkout/failure",
                "https://jaldishop.com/checkout/pending",
                "JALDI SHOP"
        );

        PreferenceResult result = adapter.createPreference(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.preferenceId()).isEqualTo("pref-123456789");
        assertThat(result.initPoint()).contains("mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-123456789");
        assertThat(result.sandboxInitPoint()).contains("sandbox.mercadopago.com.pe");
    }

    @Test
    @DisplayName("Crear preferencia envía cabecera X-Idempotency-Key cuando se proporciona")
    void shouldSendIdempotencyKeyHeaderWhenProvided() {
        String responseJson = """
                {
                  "id": "pref-idemp-001",
                  "init_point": "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-idemp-001",
                  "sandbox_init_point": "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-idemp-001",
                  "external_reference": "ext-pay-001"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("X-Idempotency-Key", "idemp-key-test-123"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-pay-001",
                "Compra JaldiShop",
                List.of(new CreatePreferenceCommand.PreferenceItem(
                        "var-1", "Producto Test", "Desc", 1, new BigDecimal("55.00"), "PEN"
                )),
                new CreatePreferenceCommand.PayerInfo("Juan", "Perez", "juan@example.com", "987654321"),
                "https://api.jaldishop.com/webhooks/mercadopago",
                "https://jaldishop.com/checkout/success",
                "https://jaldishop.com/checkout/failure",
                "https://jaldishop.com/checkout/pending",
                "JALDI SHOP",
                "idemp-key-test-123"
        );

        PreferenceResult result = adapter.createPreference(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.preferenceId()).isEqualTo("pref-idemp-001");
    }

    @Test
    @DisplayName("Consultar pago APPROVED devuelve datos estructurados y fecha aprobada parseada")
    void shouldQueryApprovedPaymentSuccessfully() {
        String responseJson = """
                {
                  "id": 987654321,
                  "status": "approved",
                  "status_detail": "accredited",
                  "currency_id": "PEN",
                  "transaction_amount": 75.50,
                  "payment_method_id": "visa",
                  "payment_type_id": "credit_card",
                  "external_reference": "ext-pay-002",
                  "date_approved": "2026-11-02T19:00:00.000Z"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/payments/987654321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        PaymentQueryResult result = adapter.queryPayment("987654321");

        mockServer.verify();
        assertThat(result.providerPaymentId()).isEqualTo("987654321");
        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.statusDetail()).isEqualTo("accredited");
        assertThat(result.transactionAmount()).isEqualByComparingTo("75.50");
        assertThat(result.currency()).isEqualTo("PEN");
        assertThat(result.paymentMethodId()).isEqualTo("visa");
        assertThat(result.paymentTypeId()).isEqualTo("credit_card");
        assertThat(result.externalReference()).isEqualTo("ext-pay-002");
        assertThat(result.dateApproved()).isEqualTo(Instant.parse("2026-11-02T19:00:00Z"));
    }

    @Test
    @DisplayName("Consultar pago PENDING devuelve estado pendiente sin fecha de aprobación")
    void shouldQueryPendingPaymentSuccessfully() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/111"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 111,
                          "status": "pending",
                          "status_detail": "pending_waiting_payment",
                          "currency_id": "PEN",
                          "transaction_amount": 30.00,
                          "payment_method_id": "ticket",
                          "payment_type_id": "ticket"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentQueryResult pendingResult = adapter.queryPayment("111");

        mockServer.verify();
        assertThat(pendingResult.status()).isEqualTo("pending");
        assertThat(pendingResult.statusDetail()).isEqualTo("pending_waiting_payment");
        assertThat(pendingResult.dateApproved()).isNull();
    }

    @Test
    @DisplayName("Consultar pago REJECTED devuelve estado rechazado con detalle")
    void shouldQueryRejectedPaymentSuccessfully() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/222"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 222,
                          "status": "rejected",
                          "status_detail": "cc_rejected_insufficient_amount",
                          "currency_id": "PEN",
                          "transaction_amount": 100.00,
                          "payment_method_id": "master",
                          "payment_type_id": "credit_card"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentQueryResult rejectedResult = adapter.queryPayment("222");

        mockServer.verify();
        assertThat(rejectedResult.status()).isEqualTo("rejected");
        assertThat(rejectedResult.statusDetail()).isEqualTo("cc_rejected_insufficient_amount");
    }

    @Test
    @DisplayName("Manejo de HTTP 400 Bad Request devuelve MercadoPagoException con código MP_BAD_REQUEST")
    void shouldHandle400BadRequest() {
        mockServer.expect(requestTo(BASE_URL + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Invalid items price\", \"error\": \"bad_request\", \"status\": 400}"));

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-1", "Titulo",
                List.of(new CreatePreferenceCommand.PreferenceItem("1", "Item", null, 1, new BigDecimal("10.00"), "PEN")),
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> adapter.createPreference(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(400);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_BAD_REQUEST");
                });
    }

    @Test
    @DisplayName("Manejo de HTTP 401 Unauthorized devuelve MercadoPagoException con código MP_UNAUTHORIZED")
    void shouldHandle401Unauthorized() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Invalid token\", \"error\": \"unauthorized\", \"status\": 401}"));

        assertThatThrownBy(() -> adapter.queryPayment("999"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(401);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_UNAUTHORIZED");
                });
    }

    @Test
    @DisplayName("Manejo de HTTP 429 Too Many Requests devuelve MercadoPagoException con código MP_RATE_LIMIT")
    void shouldHandle429RateLimit() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/429"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Rate limit exceeded\", \"status\": 429}"));

        assertThatThrownBy(() -> adapter.queryPayment("429"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(429);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_RATE_LIMIT");
                });
    }

    @Test
    @DisplayName("Manejo de HTTP 500 Server Error devuelve MercadoPagoException con código MP_SERVER_ERROR")
    void shouldHandle500ServerError() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/500"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Internal Server Error\", \"status\": 500}"));

        assertThatThrownBy(() -> adapter.queryPayment("500"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(500);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_SERVER_ERROR");
                });
    }

    @Test
    @DisplayName("Respuesta incompleta al crear preferencia (sin id o init_point) lanza MP_INVALID_RESPONSE")
    void shouldHandleMissingRequiredFieldsInPreferenceResponse() {
        mockServer.expect(requestTo(BASE_URL + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-2", "Titulo",
                List.of(new CreatePreferenceCommand.PreferenceItem("1", "Item", null, 1, new BigDecimal("10.00"), "PEN")),
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> adapter.createPreference(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_INVALID_RESPONSE");
                });
    }

    @Test
    @DisplayName("Respuesta incompleta al consultar pago (sin id o status) lanza MP_INVALID_RESPONSE")
    void shouldHandleMissingRequiredFieldsInPaymentResponse() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/333"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"transaction_amount\": 50.00}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.queryPayment("333"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_INVALID_RESPONSE");
                });
    }

    @Test
    @DisplayName("Falla rápido si el access token no está configurado sin hacer llamada HTTP")
    void shouldFailFastWhenAccessTokenIsMissing() {
        properties.setAccessToken("");

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-3", "Titulo",
                List.of(new CreatePreferenceCommand.PreferenceItem("1", "Item", null, 1, new BigDecimal("10.00"), "PEN")),
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> adapter.createPreference(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(401);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_MISSING_CREDENTIALS");
                });

        assertThatThrownBy(() -> adapter.queryPayment("123"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_MISSING_CREDENTIALS");
                });

        // Verificamos que no se intentó ninguna llamada HTTP
        mockServer.verify();
    }

    @Test
    @DisplayName("Manejo de timeout y falla de red lanza MP_TIMEOUT")
    void shouldHandleTimeout() {
        RestClient failingRestClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.ClientHttpRequestFactory() {
                    @Override
                    public org.springframework.http.client.ClientHttpRequest createRequest(java.net.URI uri, HttpMethod httpMethod) {
                        throw new ResourceAccessException("I/O error: Read timed out", new SocketTimeoutException("Read timed out"));
                    }
                })
                .baseUrl(BASE_URL)
                .build();

        MercadoPagoRestClientAdapter timeoutAdapter = new MercadoPagoRestClientAdapter(failingRestClient, properties);

        assertThatThrownBy(() -> timeoutAdapter.queryPayment("timeout-id"))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_TIMEOUT");
                });
    }

    @Test
    @DisplayName("Excepciones y logs no exponen credenciales ni tokens sensibles")
    void shouldNotExposeSensitiveDataInExceptions() {
        mockServer.expect(requestTo(BASE_URL + "/v1/payments/777"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body("{\"message\": \"Invalid request\"}"));

        try {
            adapter.queryPayment("777");
        } catch (MercadoPagoException ex) {
            assertThat(ex.getMessage()).doesNotContain(ACCESS_TOKEN);
            assertThat(ex.toString()).doesNotContain(ACCESS_TOKEN);
        }
    }

    @Test
    @DisplayName("Crear orden en Checkout Pro mediante Orders API (/v1/orders) exitosamente")
    void shouldCreateOrderSuccessfully() {
        String responseJson = """
                {
                  "id": "ORD01J67CQQH5904WDBVZEM4JMEP3",
                  "type": "online",
                  "processing_mode": "manual",
                  "capture_mode": "automatic",
                  "total_amount": 120.00,
                  "external_reference": "ext-order-001",
                  "checkout_url": "https://www.mercadopago.com.pe/checkout/v1/redirect?order_id=ORD01J67CQQH5904WDBVZEM4JMEP3",
                  "live_mode": false,
                  "status": "created"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("X-Idempotency-Key", "idemp-order-999"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.type").value("online"))
                .andExpect(jsonPath("$.processing_mode").value("manual"))
                .andExpect(jsonPath("$.capture_mode").value("automatic"))
                .andExpect(jsonPath("$.total_amount").value(120.00))
                .andExpect(jsonPath("$.external_reference").value("ext-order-001"))
                .andExpect(jsonPath("$.items[0].title").value("Producto Order Test"))
                .andExpect(jsonPath("$.items[0].unit_price").value(120.00))
                .andExpect(jsonPath("$.payer.email").value("juan@example.com"))
                .andExpect(jsonPath("$.config.online.success_url").value("https://jaldishop.com/checkout/success"))
                .andExpect(jsonPath("$.config.online.failure_url").value("https://jaldishop.com/checkout/failure"))
                .andExpect(jsonPath("$.config.online.pending_url").value("https://jaldishop.com/checkout/pending"))
                .andExpect(jsonPath("$.config.online.auto_return").value("approved"))
                .andExpect(jsonPath("$.back_urls").doesNotExist())
                .andExpect(jsonPath("$.notification_url").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-order-001",
                new BigDecimal("120.00"),
                List.of(new CreateOrderCommand.OrderItem(
                        "item-1", "Producto Order Test", "Detalle", 1, new BigDecimal("120.00"), "PEN"
                )),
                new CreateOrderCommand.PayerInfo("Juan", "Perez", "juan@example.com", "987654321"),
                "https://api.jaldishop.com/webhooks/mercadopago",
                "https://jaldishop.com/checkout/success",
                "https://jaldishop.com/checkout/failure",
                "https://jaldishop.com/checkout/pending",
                "JALDI SHOP",
                "idemp-order-999"
        );

        OrderResult result = adapter.createOrder(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD01J67CQQH5904WDBVZEM4JMEP3");
        assertThat(result.checkoutUrl()).contains("order_id=ORD01J67CQQH5904WDBVZEM4JMEP3");
        assertThat(result.status()).isEqualTo("created");
        assertThat(result.totalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Crear orden rechaza con SecurityException si expectedLiveMode=false pero Mercado Pago retorna live_mode=true")
    void shouldRejectCreateOrderWhenExpectedLiveModeFalseAndResponseLiveModeTrue() {
        properties.setExpectedLiveMode(false);

        String liveOrderResponseJson = """
                {
                  "id": "ORD-LIVE-001",
                  "status": "created",
                  "total_amount": 120.00,
                  "checkout_url": "https://mercadopago.com.pe/checkout/orders/ORD-LIVE-001",
                  "live_mode": true
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(liveOrderResponseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-live-001",
                new BigDecimal("120.00"),
                List.of(new CreateOrderCommand.OrderItem("i-1", "Item", "Desc", 1, new BigDecimal("120.00"), "PEN")),
                null, null, null, null, null, "JALDI", "idemp-live-001"
        );

        assertThatThrownBy(() -> adapter.createOrder(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoOrderSecurityException.class)
                .hasMessageContaining("live_mode=true")
                .satisfies(ex -> assertThat(((MercadoPagoOrderSecurityException) ex).getRemoteOrderId()).isEqualTo("ORD-LIVE-001"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Crear orden rechaza con SecurityException si expectedLiveMode=false pero Mercado Pago no incluye live_mode (null)")
    void shouldRejectCreateOrderWhenExpectedLiveModeFalseAndResponseLiveModeNull() {
        properties.setExpectedLiveMode(false);

        String nullLiveModeResponseJson = """
                {
                  "id": "ORD-NULL-LIVE",
                  "status": "created",
                  "total_amount": 120.00,
                  "checkout_url": "https://mercadopago.com.pe/checkout/orders/ORD-NULL-LIVE"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nullLiveModeResponseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-null-001",
                new BigDecimal("120.00"),
                List.of(new CreateOrderCommand.OrderItem("i-1", "Item", "Desc", 1, new BigDecimal("120.00"), "PEN")),
                null, null, null, null, null, "JALDI", "idemp-null-001"
        );

        assertThatThrownBy(() -> adapter.createOrder(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoOrderSecurityException.class)
                .hasMessageContaining("live_mode=null")
                .satisfies(ex -> assertThat(((MercadoPagoOrderSecurityException) ex).getRemoteOrderId()).isEqualTo("ORD-NULL-LIVE"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Crear orden exitosa cuando expectedLiveMode=false y Mercado Pago retorna live_mode=false")
    void shouldAllowCreateOrderWhenExpectedLiveModeFalseAndResponseLiveModeFalse() {
        properties.setExpectedLiveMode(false);

        String sandboxOrderResponseJson = """
                {
                  "id": "ORD-SANDBOX-001",
                  "status": "created",
                  "total_amount": 120.00,
                  "checkout_url": "https://sandbox.mercadopago.com.pe/checkout/orders/ORD-SANDBOX-001",
                  "live_mode": false
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(sandboxOrderResponseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-sandbox-001",
                new BigDecimal("120.00"),
                List.of(new CreateOrderCommand.OrderItem("i-1", "Item", "Desc", 1, new BigDecimal("120.00"), "PEN")),
                null, null, null, null, null, "JALDI", "idemp-sandbox-001"
        );

        OrderResult result = adapter.createOrder(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD-SANDBOX-001");
        assertThat(result.checkoutUrl()).isEqualTo("https://sandbox.mercadopago.com.pe/checkout/orders/ORD-SANDBOX-001");
    }

    @Test
    @DisplayName("Crear orden falla cuando Mercado Pago responde con datos incompletos (sin checkout_url)")
    void shouldFailCreateOrderWhenResponseIncomplete() {
        String invalidResponseJson = """
                {
                  "id": "ORD-ONLY-ID",
                  "status": "created"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(invalidResponseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-order-002",
                new BigDecimal("80.00"),
                List.of(new CreateOrderCommand.OrderItem("i-1", "Test", "Desc", 1, new BigDecimal("80.00"), "PEN")),
                null, null, null, null, null, null, "idemp-002"
        );

        assertThatThrownBy(() -> adapter.createOrder(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_INVALID_RESPONSE");
                });
    }

    @Test
    @DisplayName("Crear orden traduce 400 Bad Request de Mercado Pago a MP_BAD_REQUEST")
    void shouldHandle400BadRequestOnCreateOrder() {
        mockServer.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"message\": \"Invalid items or total_amount mismatch\"}"));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-order-003",
                new BigDecimal("50.00"),
                List.of(new CreateOrderCommand.OrderItem("i-2", "Test Item", "Desc", 1, new BigDecimal("50.00"), "PEN")),
                null, null, null, null, null, null, "idemp-003"
        );

        assertThatThrownBy(() -> adapter.createOrder(command, UUID.randomUUID()))
                .isInstanceOf(MercadoPagoException.class)
                .satisfies(ex -> {
                    MercadoPagoException mpe = (MercadoPagoException) ex;
                    assertThat(mpe.getStatusCode()).isEqualTo(400);
                    assertThat(mpe.getErrorCode()).isEqualTo("MP_BAD_REQUEST");
                });
    }

    @Test
    @DisplayName("Consultar orden por ID (/v1/orders/{id}) con pagos asociados exitosamente")
    void shouldQueryOrderSuccessfullyWithPayments() {
        String responseJson = """
                {
                  "id": "ORD01J67CQQH5904WDBVZEM4JMEP3",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": "120.00",
                  "total_paid_amount": "120.00",
                  "external_reference": "ext-order-001",
                  "currency": "PEN",
                  "user_id": "987654321",
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY01J67CQQ",
                        "status": "processed",
                        "status_detail": "accredited",
                        "amount": "120.00",
                        "paid_amount": "120.00",
                        "payment_method": {"id": "visa", "type": "credit_card"},
                        "date_approved": "2026-11-05T12:00:00Z"
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders/ORD01J67CQQH5904WDBVZEM4JMEP3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OrderQueryResult result = adapter.queryOrder("ORD01J67CQQH5904WDBVZEM4JMEP3");

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD01J67CQQH5904WDBVZEM4JMEP3");
        assertThat(result.status()).isEqualTo("processed");
        assertThat(result.statusDetail()).isEqualTo("accredited");
        assertThat(result.totalAmount()).isEqualByComparingTo("120.00");
        assertThat(result.totalPaidAmount()).isEqualByComparingTo("120.00");
        assertThat(result.externalReference()).isEqualTo("ext-order-001");
        assertThat(result.collectorId()).isEqualTo(987654321L);
        assertThat(result.liveMode()).isFalse();
        assertThat(result.payments()).hasSize(1);

        OrderQueryResult.OrderPaymentItem paymentItem = result.payments().getFirst();
        assertThat(paymentItem.providerPaymentId()).isEqualTo("PAY01J67CQQ");
        assertThat(paymentItem.status()).isEqualTo("processed");
        assertThat(paymentItem.statusDetail()).isEqualTo("accredited");
        assertThat(paymentItem.transactionAmount()).isEqualByComparingTo("120.00");
        assertThat(paymentItem.paidAmount()).isEqualByComparingTo("120.00");
        assertThat(paymentItem.currency()).isEqualTo("PEN");
        assertThat(paymentItem.paymentMethodId()).isEqualTo("visa");
        assertThat(paymentItem.paymentTypeId()).isEqualTo("credit_card");
        assertThat(paymentItem.dateApproved()).isEqualTo(Instant.parse("2026-11-05T12:00:00Z"));
    }

    @Test
    @DisplayName("Consultar orden con transactions nulas o vacías no lanza NullPointerException")
    void shouldQueryOrderSuccessfullyWithEmptyTransactions() {
        String responseJson = """
                {
                  "id": "ORD-EMPTY-001",
                  "status": "created",
                  "total_amount": 50.00,
                  "external_reference": "ext-order-empty"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders/ORD-EMPTY-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OrderQueryResult result = adapter.queryOrder("ORD-EMPTY-001");

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD-EMPTY-001");
        assertThat(result.status()).isEqualTo("created");
        assertThat(result.payments()).isEmpty();
    }

    private tools.jackson.databind.node.ObjectNode orderFixture() throws Exception {
        try (var input = getClass().getResourceAsStream("/mercadopago/orders/get-order-accredited.json")) {
            return (tools.jackson.databind.node.ObjectNode) new ObjectMapper().readTree(input);
        }
    }

    private OrderQueryResult queryFixture(tools.jackson.databind.node.ObjectNode response) {
        mockServer.expect(requestTo(BASE_URL + "/v1/orders/ORD-MAPPING"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));
        return adapter.queryOrder("ORD-MAPPING");
    }

    @Test void ordersMappingKeepsAllFourAmountsSeparateAndDoesNotInventApprovalDate() throws Exception {
        var root = orderFixture();
        var payment = (tools.jackson.databind.node.ObjectNode) root.path("transactions").path("payments").get(0);
        root.put("total_amount", "70.00").put("total_paid_amount", "74.00");
        payment.put("paid_amount", "41.00");
        var result = queryFixture(root);
        assertThat(result.totalAmount()).isEqualByComparingTo("70.00");
        assertThat(result.totalPaidAmount()).isEqualByComparingTo("74.00");
        assertThat(result.collectorId()).isEqualTo(1234L);
        var item = result.payments().getFirst();
        assertThat(item.transactionAmount()).isEqualByComparingTo("39.00");
        assertThat(item.paidAmount()).isEqualByComparingTo("41.00");
        assertThat(item.providerPaymentId()).isEqualTo("PAY-MAPPING");
        assertThat(item.currency()).isEqualTo("PEN");
        assertThat(item.paymentMethodId()).isEqualTo("visa");
        assertThat(item.paymentTypeId()).isEqualTo("credit_card");
        assertThat(item.dateApproved()).isNull();
        mockServer.verify();
    }

    @Test void explicitHistoricalOrdersFieldsRemainCompatibleWithoutInventingPaidAmount() throws Exception {
        var root = orderFixture();
        var payment = (tools.jackson.databind.node.ObjectNode) root.path("transactions").path("payments").get(0);
        root.remove("currency");
        root.remove("user_id");
        root.put("collector_id", 1234);
        payment.remove("amount");
        payment.remove("paid_amount");
        payment.remove("payment_method");
        payment.put("transaction_amount", 39).put("currency_id", "PEN")
                .put("payment_method_id", "visa").put("payment_type_id", "credit_card");
        var result = queryFixture(root);
        assertThat(result.collectorId()).isEqualTo(1234L);
        assertThat(result.payments().getFirst().transactionAmount()).isEqualByComparingTo("39.00");
        assertThat(result.payments().getFirst().paidAmount()).isNull();
        mockServer.verify();
    }

    @Test void matchingCanonicalAndHistoricalFieldsAreAccepted() throws Exception {
        var root = orderFixture();
        root.put("collector_id", 1234);
        var payment = (tools.jackson.databind.node.ObjectNode) root.path("transactions").path("payments").get(0);
        payment.put("transaction_amount", 39).put("currency_id", "PEN")
                .put("payment_method_id", "visa").put("payment_type_id", "credit_card");
        assertThat(queryFixture(root).payments().getFirst().transactionAmount()).isEqualByComparingTo("39.00");
        mockServer.verify();
    }

    @Test void absentOptionalPaidAmountAndTrustMetadataAreNeverInferred() throws Exception {
        var root = orderFixture();
        root.remove("user_id");
        root.remove("live_mode");
        var payment = (tools.jackson.databind.node.ObjectNode) root.path("transactions").path("payments").get(0);
        payment.remove("paid_amount");
        properties.setExpectedCollectorId(1234L);
        properties.setExpectedLiveMode(false);
        var result = queryFixture(root);
        assertThat(result.payments().getFirst().paidAmount()).isNull();
        assertThat(result.collectorId()).isNull();
        assertThat(result.liveMode()).isNull();
        assertThatThrownBy(() -> new com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidator(properties)
                .validateOrder("ORD-MAPPING", result)).isInstanceOf(SecurityException.class);
        mockServer.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"missing_amount", "zero_amount", "negative_amount",
            "conflicting_amount", "missing_currency", "conflicting_currency", "conflicting_method", "conflicting_type",
            "conflicting_seller", "invalid_seller", "missing_id", "blank_id", "object_id", "missing_order_id",
            "other_order_id", "missing_status", "null_payment", "duplicate_id", "negative_paid", "malformed_amount"})
    void invalidOrdersAreRejectedWithoutDiscardingPaymentsOrExposingResponse(String scenario) throws Exception {
        var root = orderFixture();
        var payments = (tools.jackson.databind.node.ArrayNode) root.path("transactions").path("payments");
        var payment = (tools.jackson.databind.node.ObjectNode) payments.get(0);
        switch (scenario) {
            case "missing_amount" -> payment.remove("amount"); // paid_amount/total_amount must not substitute it.
            case "zero_amount" -> payment.put("amount", "0");
            case "negative_amount" -> payment.put("amount", "-39");
            case "conflicting_amount" -> payment.put("transaction_amount", "38.00");
            case "missing_currency" -> root.remove("currency");
            case "conflicting_currency" -> payment.put("currency_id", "USD");
            case "conflicting_method" -> payment.put("payment_method_id", "master");
            case "conflicting_type" -> payment.put("payment_type_id", "account_money");
            case "conflicting_seller" -> root.put("collector_id", 5678);
            case "invalid_seller" -> root.put("user_id", "not-a-seller");
            case "missing_id" -> payment.remove("id");
            case "blank_id" -> payment.put("id", " ");
            case "object_id" -> payment.putObject("id").put("private", "synthetic-private-value");
            case "missing_order_id" -> root.remove("id");
            case "other_order_id" -> root.put("id", "OTHER-ORDER");
            case "missing_status" -> payment.remove("status");
            case "null_payment" -> payments.addNull(); // One valid transaction must not hide an invalid second one.
            case "duplicate_id" -> payments.add(payment.deepCopy());
            case "negative_paid" -> payment.put("paid_amount", "-1");
            case "malformed_amount" -> payment.put("amount", "synthetic-private-value");
            default -> throw new AssertionError(scenario);
        }
        assertThatThrownBy(() -> queryFixture(root)).isInstanceOfSatisfying(MercadoPagoException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo("MP_INVALID_RESPONSE");
            assertThat(ex.getCause()).isNull();
            assertThat(ex.getMessage()).doesNotContain("synthetic-private-value", "legacy-payment-reference");
        });
        mockServer.verify();
    }

    @Test
    @DisplayName("Ejecutar reembolso exitosamente en modo PREFERENCES envía POST a /v1/payments/{id}/refunds")
    void shouldExecuteRefundSuccessfullyForPreferencesMode() {
        String responseJson = """
                {
                  "id": 88990011,
                  "payment_id": 12345678,
                  "amount": 55.00,
                  "status": "approved",
                  "date_created": "2026-11-05T18:00:00.000Z"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/payments/12345678/refunds"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("X-Idempotency-Key", "idemp-ref-pref-01"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.amount").value(55.0))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        ExecuteRefundCommand command = new ExecuteRefundCommand(
                "PREFERENCES",
                null,
                "12345678",
                new BigDecimal("55.00"),
                "idemp-ref-pref-01"
        );

        RefundResult result = adapter.executeRefund(command);

        mockServer.verify();
        assertThat(result.providerRefundId()).isEqualTo("88990011");
        assertThat(result.providerPaymentId()).isEqualTo("12345678");
        assertThat(result.amount()).isEqualByComparingTo("55.00");
        assertThat(result.status()).isEqualTo("approved");
    }

    @Test
    @DisplayName("Ejecutar reembolso exitosamente en modo ORDERS envía POST a /v1/orders/{order_id}/refund")
    void shouldExecuteRefundSuccessfullyForOrdersMode() {
        String responseJson = """
                {
                  "id": "ORD01J67CQQH5904WDBVZEM4JMEP3",
                  "status": "processed",
                  "status_detail": "refunded",
                  "transactions": {
                    "refunds": [
                      {
                        "id": 99887766,
                        "payment_id": 55443322,
                        "amount": 120.00,
                        "status": "approved",
                        "date_created": "2026-11-05T18:30:00.000Z"
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders/ORD01J67CQQH5904WDBVZEM4JMEP3/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("X-Idempotency-Key", "idemp-ref-orders-01"))
                .andExpect(header("Content-Type", "application/json"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        ExecuteRefundCommand command = new ExecuteRefundCommand(
                "ORDERS",
                "ORD01J67CQQH5904WDBVZEM4JMEP3",
                null,
                new BigDecimal("120.00"),
                "idemp-ref-orders-01"
        );

        RefundResult result = adapter.executeRefund(command);

        mockServer.verify();
        assertThat(result.providerRefundId()).isEqualTo("99887766");
        assertThat(result.providerPaymentId()).isEqualTo("55443322");
        assertThat(result.amount()).isEqualByComparingTo("120.00");
        assertThat(result.status()).isEqualTo("approved");
    }

    @Test
    @DisplayName("Consultar orden mapea array transactions.refunds a OrderRefundItem")
    void shouldQueryOrderWithRefundsSuccessfully() {
        String responseJson = """
                {
                  "id": "ORD-WITH-REFUNDS",
                  "currency": "PEN",
                  "status": "processed",
                  "status_detail": "refunded",
                  "total_amount": 100.00,
                  "total_paid_amount": 100.00,
                  "external_reference": "ext-order-ref",
                  "transactions": {
                    "payments": [
                      {
                        "id": 11112222,
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 100.00
                      }
                    ],
                    "refunds": [
                      {
                        "id": 33334444,
                        "payment_id": 11112222,
                        "amount": 100.00,
                        "status": "approved",
                        "date_created": "2026-11-05T18:45:00.000Z"
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/orders/ORD-WITH-REFUNDS"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OrderQueryResult result = adapter.queryOrder("ORD-WITH-REFUNDS");

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD-WITH-REFUNDS");
        assertThat(result.status()).isEqualTo("processed");
        assertThat(result.statusDetail()).isEqualTo("refunded");
        assertThat(result.payments()).hasSize(1);
        assertThat(result.refunds()).hasSize(1);
        assertThat(result.refunds().getFirst().providerRefundId()).isEqualTo("33334444");
        assertThat(result.refunds().getFirst().providerPaymentId()).isEqualTo("11112222");
        assertThat(result.refunds().getFirst().amount()).isEqualByComparingTo("100.00");
        assertThat(result.refunds().getFirst().status()).isEqualTo("approved");
    }

    @Test
    @DisplayName("Crear orden incluye expiration_time en el JSON de solicitud cuando está configurado")
    void shouldIncludeExpirationTimeInOrderRequestWhenPresent() {
        String responseJson = """
                {
                  "id": "ORD-EXP-001",
                  "status": "created",
                  "total_amount": 50.00,
                  "checkout_url": "https://mercadopago.com.pe/checkout/orders/ORD-EXP-001",
                  "live_mode": false
                }
                """;

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(jsonPath("$.expiration_time").value("PT9M30S"))
                .andExpect(jsonPath("$.type").value("online"))
                .andExpect(jsonPath("$.processing_mode").value("manual"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-order-exp",
                new BigDecimal("50.00"),
                List.of(new CreateOrderCommand.OrderItem(
                        "item-exp", "Item Expirable", "Detalle", 1, new BigDecimal("50.00"), "PEN"
                )),
                new CreateOrderCommand.PayerInfo("Ana", "Gomez", "ana@example.com", "987111222"),
                "https://api.jaldishop.com/webhooks/mercadopago",
                "https://jaldishop.com/checkout/success",
                "https://jaldishop.com/checkout/failure",
                "https://jaldishop.com/checkout/pending",
                "JALDI SHOP",
                "idemp-exp-001",
                "PT9M30S"
        );

        OrderResult result = adapter.createOrder(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD-EXP-001");
        assertThat(result.status()).isEqualTo("created");
    }

    @Test
    @DisplayName("Crear orden omite expiration_time en el JSON cuando es nulo")
    void shouldOmitExpirationTimeWhenNull() {
        String responseJson = """
                {
                  "id": "ORD-EXP-NULL",
                  "status": "created",
                  "total_amount": 30.00,
                  "checkout_url": "https://mercadopago.com.pe/checkout/orders/ORD-EXP-NULL",
                  "live_mode": false
                }
                """;

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.expiration_time").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreateOrderCommand command = new CreateOrderCommand(
                "ext-order-no-exp",
                new BigDecimal("30.00"),
                List.of(new CreateOrderCommand.OrderItem(
                        "item-no-exp", "Item Sin Exp", "Detalle", 1, new BigDecimal("30.00"), "PEN"
                )),
                null,
                null,
                null,
                null,
                null,
                "JALDI SHOP",
                "idemp-no-exp",
                null
        );

        OrderResult result = adapter.createOrder(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.orderId()).isEqualTo("ORD-EXP-NULL");
    }

    @Test
    @DisplayName("Crear preferencia no incluye expiration_time en el JSON de Preferences")
    void shouldNotIncludeExpirationTimeInPreferenceRequest() {
        String responseJson = """
                {
                  "id": "pref-no-exp",
                  "init_point": "https://mercadopago.com.pe/checkout/pref-no-exp"
                }
                """;

        mockServer.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.expiration_time").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        CreatePreferenceCommand command = new CreatePreferenceCommand(
                "ext-pref-check",
                "JALDI SHOP",
                List.of(new CreatePreferenceCommand.PreferenceItem(
                        "item-1", "Prod", "Desc", 1, new BigDecimal("25.00"), "PEN"
                )),
                null,
                null,
                null,
                null,
                null,
                "JALDI SHOP"
        );

        PreferenceResult result = adapter.createPreference(command, UUID.randomUUID());

        mockServer.verify();
        assertThat(result.preferenceId()).isEqualTo("pref-no-exp");
    }

}

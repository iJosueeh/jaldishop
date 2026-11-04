package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        adapter = new MercadoPagoRestClientAdapter(restClient, properties);
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

        PreferenceResult result = adapter.createPreference(command);

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

        PreferenceResult result = adapter.createPreference(command);

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

        assertThatThrownBy(() -> adapter.createPreference(command))
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

        assertThatThrownBy(() -> adapter.createPreference(command))
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

        assertThatThrownBy(() -> adapter.createPreference(command))
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

}

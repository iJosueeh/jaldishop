package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Adaptador HTTP para la API de Mercado Pago Checkout Pro.
 * No utiliza transacciones de base de datos ni expone secretos en logs.
 */
@Component
public class MercadoPagoRestClientAdapter implements MercadoPagoGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoRestClientAdapter.class);

    private static final String PREFERENCES_PATH = "/checkout/preferences";
    private static final String PAYMENTS_PATH = "/v1/payments/{id}";
    private static final String REFUNDS_PATH = "/v1/payments/{id}/refunds";

    private final RestClient restClient;
    private final MercadoPagoProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoRestClientAdapter(MercadoPagoProperties properties) {
        this(RestClient.builder(), properties);
    }

    public MercadoPagoRestClientAdapter(RestClient.Builder restClientBuilder, MercadoPagoProperties properties) {
        this(buildDefaultRestClient(restClientBuilder, properties), properties);
    }

    public MercadoPagoRestClientAdapter(RestClient restClient, MercadoPagoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
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
    public PreferenceResult createPreference(CreatePreferenceCommand command) {
        validateAccessToken();

        MpPreferenceRequest requestBody = toPreferenceRequest(command);
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
        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
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

        log.info("Ejecutando reembolso para providerPaymentId: {} con idempotencyKey: {}",
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
            log.error("Error inesperado al ejecutar reembolso en Mercado Pago: {}", ex.getMessage());
            throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar reembolso en Mercado Pago", ex);
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

}

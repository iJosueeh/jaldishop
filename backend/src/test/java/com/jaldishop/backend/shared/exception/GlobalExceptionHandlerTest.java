package com.jaldishop.backend.shared.exception;

import com.jaldishop.backend.payment.domain.PaymentErrorCodes;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test/handler")
    static class TestDummyController {

        @GetMapping("/only-get")
        public String onlyGet() {
            return "ok";
        }

        @GetMapping("/required-param")
        public String requiredParam(@RequestParam("storeId") UUID storeId) {
            return storeId.toString();
        }

        @PostMapping("/validation-body")
        public String validationBody(@Valid @RequestBody TestRequestDto dto) {
            return dto.name();
        }

        @GetMapping("/type-mismatch")
        public String typeMismatch(@RequestParam("id") UUID id) {
            return id.toString();
        }

        @GetMapping("/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("Argumento inválido de prueba");
        }

        @GetMapping("/business-rule")
        public String businessRule() {
            throw new BusinessRuleException("RULE_VIOLATION", "Regla de negocio infringida");
        }

        @GetMapping("/conflict")
        public String conflict() {
            throw new ConflictException("DUPLICATE_RESOURCE", "El recurso ya existe");
        }

        @GetMapping("/resource-not-found")
        public String resourceNotFound() {
            throw new ResourceNotFoundException("Tienda", "uuid-123");
        }

        @GetMapping("/unmapped-resource")
        public String unmappedResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/api/v1/no-existe", "api/v1/no-existe");
        }

        @GetMapping("/bad-credentials")
        public String badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/access-denied")
        public String accessDenied() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/generic-error")
        public String genericError() {
            throw new RuntimeException("Error inesperado no controlado");
        }

        @GetMapping("/mp-timeout")
        public String mpTimeout() {
            throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout al invocar Mercado Pago");
        }

        @GetMapping("/mp-rate-limit")
        public String mpRateLimit() {
            throw new MercadoPagoException(429, "MP_RATE_LIMIT", "Rate limit alcanzado");
        }

        @GetMapping("/mp-config-error")
        public String mpConfigError() {
            throw new MercadoPagoException(401, "MP_MISSING_CREDENTIALS", "Credenciales no configuradas");
        }

        @GetMapping("/mp-bad-request")
        public String mpBadRequest() {
            throw new MercadoPagoException(400, "MP_BAD_REQUEST", "Solicitud rechazada: {\"internal_code\":\"MALFORMED\"}");
        }

        @GetMapping("/mp-server-error")
        public String mpServerError() {
            throw new MercadoPagoException(500, "MP_SERVER_ERROR", "Error de servidor en Mercado Pago");
        }

        @GetMapping("/optimistic-lock")
        public String optimisticLock() {
            throw new OptimisticLockingFailureException("Row updated by another transaction");
        }

        @GetMapping("/cannot-acquire-lock")
        public String cannotAcquireLock() {
            throw new CannotAcquireLockException("Lock wait timeout on PostgreSQL");
        }

        @GetMapping("/data-integrity-unique")
        public String dataIntegrityUnique() {
            SQLException sqlEx = new SQLException("duplicate key value violates unique constraint uq_users_email", "23505");
            throw new DataIntegrityViolationException("Unique collision", sqlEx);
        }

        @GetMapping("/data-integrity-fk")
        public String dataIntegrityFk() {
            SQLException sqlEx = new SQLException("violates foreign key constraint fk_orders_user", "23503");
            throw new DataIntegrityViolationException("FK collision", sqlEx);
        }
    }

    record TestRequestDto(@NotBlank(message = "El nombre no puede estar vacío") String name) {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestDummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HAL-01: HttpRequestMethodNotSupportedException retorna HTTP 405 con código METHOD_NOT_ALLOWED")
    void shouldReturn405MethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/handler/only-get"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("El método HTTP 'POST' no está soportado para esta ruta"))
                .andExpect(jsonPath("$.errors").value(nullValue()));
    }

    @Test
    @DisplayName("HAL-01: MissingServletRequestParameterException retorna HTTP 400 con código MISSING_REQUIRED_PARAMETER")
    void shouldReturn400MissingRequiredParameter() throws Exception {
        mockMvc.perform(get("/test/handler/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_PARAMETER"))
                .andExpect(jsonPath("$.message").value("El parámetro de consulta obligatorio 'storeId' no fue proporcionado"))
                .andExpect(jsonPath("$.errors").value(nullValue()));
    }

    @Test
    @DisplayName("HAL-01: NoResourceFoundException retorna HTTP 404 con código RESOURCE_NOT_FOUND")
    void shouldReturn404NoResourceFound() throws Exception {
        mockMvc.perform(get("/test/handler/unmapped-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El recurso solicitado 'api/v1/no-existe' no fue encontrado"))
                .andExpect(jsonPath("$.errors").value(nullValue()));
    }

    @Test
    @DisplayName("Regresión: MethodArgumentNotValidException retorna HTTP 400 con VALIDATION_ERROR y mapa de errores")
    void shouldReturn400ValidationErrors() throws Exception {
        mockMvc.perform(post("/test/handler/validation-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Los datos enviados no son válidos"))
                .andExpect(jsonPath("$.errors.name").value("El nombre no puede estar vacío"));
    }

    @Test
    @DisplayName("Seguridad: MethodArgumentTypeMismatchException retorna HTTP 400 con INVALID_PARAMETER sin reflejar input")
    void shouldReturn400TypeMismatchSanitized() throws Exception {
        mockMvc.perform(get("/test/handler/type-mismatch")
                        .param("id", "<script>alert('xss')</script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un formato no válido"));
    }

    @Test
    @DisplayName("Regresión: HttpMessageNotReadableException retorna HTTP 400 con MALFORMED_JSON")
    void shouldReturn400MalformedJson() throws Exception {
        mockMvc.perform(post("/test/handler/validation-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    @DisplayName("Regresión: IllegalArgumentException retorna HTTP 400 con INVALID_ARGUMENT")
    void shouldReturn400IllegalArgument() throws Exception {
        mockMvc.perform(get("/test/handler/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Argumento inválido de prueba"));
    }

    @Test
    @DisplayName("Regresión: BusinessRuleException retorna HTTP 422 con código de negocio")
    void shouldReturn422BusinessRule() throws Exception {
        mockMvc.perform(get("/test/handler/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Regla de negocio infringida"));
    }

    @Test
    @DisplayName("Regresión: ConflictException retorna HTTP 409 con código CONFLICT")
    void shouldReturn409Conflict() throws Exception {
        mockMvc.perform(get("/test/handler/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("El recurso ya existe"));
    }

    @Test
    @DisplayName("Regresión: ResourceNotFoundException retorna HTTP 404 con RESOURCE_NOT_FOUND")
    void shouldReturn404ResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/handler/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Tienda con identificador 'uuid-123' no fue encontrado"));
    }

    @Test
    @DisplayName("Seguridad: BadCredentialsException retorna HTTP 401 con mensaje público seguro en español")
    void shouldReturn401BadCredentials() throws Exception {
        mockMvc.perform(get("/test/handler/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Credenciales no válidas"));
    }

    @Test
    @DisplayName("Seguridad: AccessDeniedException retorna HTTP 403 con mensaje público seguro en español")
    void shouldReturn403AccessDenied() throws Exception {
        mockMvc.perform(get("/test/handler/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta operación"));
    }

    @Test
    @DisplayName("Regresión: Excepción no controlada retorna HTTP 500 con INTERNAL_SERVER_ERROR sanitizado")
    void shouldReturn500GenericException() throws Exception {
        mockMvc.perform(get("/test/handler/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Ha ocurrido un error inesperado en el servidor"));
    }

    @Test
    @DisplayName("Mercado Pago: timeout retorna HTTP 504 GATEWAY_TIMEOUT con mensaje amigable")
    void shouldReturn504OnMercadoPagoTimeout() throws Exception {
        mockMvc.perform(get("/test/handler/mp-timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(PaymentErrorCodes.PAYMENT_GATEWAY_TIMEOUT))
                .andExpect(jsonPath("$.message").value("Tiempo de espera agotado al comunicarse con la pasarela de pagos. Por favor, intente nuevamente."));
    }

    @Test
    @DisplayName("Mercado Pago: rate limit retorna HTTP 429 TOO_MANY_REQUESTS")
    void shouldReturn429OnMercadoPagoRateLimit() throws Exception {
        mockMvc.perform(get("/test/handler/mp-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(PaymentErrorCodes.PAYMENT_RATE_LIMIT))
                .andExpect(jsonPath("$.message").value("Límite de solicitudes excedido con la pasarela de pagos. Por favor, espere antes de reintentar."));
    }

    @Test
    @DisplayName("Mercado Pago: credenciales no configuradas retorna HTTP 500 sanitizado")
    void shouldReturn500OnMercadoPagoConfigError() throws Exception {
        mockMvc.perform(get("/test/handler/mp-config-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR))
                .andExpect(jsonPath("$.message").value("El servicio de pagos no está disponible temporalmente por problemas de configuración."));
    }

    @Test
    @DisplayName("Mercado Pago: rechazo por solicitud incompatible retorna HTTP 400 sin filtrar payload")
    void shouldReturn400OnMercadoPagoBadRequestWithoutLeakingBody() throws Exception {
        mockMvc.perform(get("/test/handler/mp-bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR))
                .andExpect(jsonPath("$.message").value("La solicitud a la pasarela de pagos contiene parámetros incompatibles."));
    }

    @Test
    @DisplayName("Mercado Pago: fallo de servidor externo retorna HTTP 502 BAD_GATEWAY")
    void shouldReturn502OnMercadoPagoServerError() throws Exception {
        mockMvc.perform(get("/test/handler/mp-server-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR))
                .andExpect(jsonPath("$.message").value("La pasarela de pagos no se encuentra disponible temporalmente. Por favor, intente más tarde."));
    }

    @Test
    @DisplayName("Persistencia: OptimisticLockingFailureException retorna HTTP 409 CONCURRENT_MODIFICATION")
    void shouldReturn409OnOptimisticLocking() throws Exception {
        mockMvc.perform(get("/test/handler/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(SharedErrorCodes.CONCURRENT_MODIFICATION))
                .andExpect(jsonPath("$.message").value("El recurso fue modificado concurrentemente por otra operación. Por favor, intente nuevamente."));
    }

    @Test
    @DisplayName("Persistencia: CannotAcquireLockException retorna HTTP 409 CONCURRENT_MODIFICATION")
    void shouldReturn409OnCannotAcquireLock() throws Exception {
        mockMvc.perform(get("/test/handler/cannot-acquire-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(SharedErrorCodes.CONCURRENT_MODIFICATION))
                .andExpect(jsonPath("$.message").value("El recurso se encuentra temporalmente bloqueado por otra operación concurrente. Por favor, intente nuevamente."));
    }

    @Test
    @DisplayName("Persistencia: DataIntegrityViolationException por clave única retorna HTTP 409 RESOURCE_CONFLICT sin filtrar SQL")
    void shouldReturn409OnDataIntegrityUniqueViolation() throws Exception {
        mockMvc.perform(get("/test/handler/data-integrity-unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(SharedErrorCodes.RESOURCE_CONFLICT))
                .andExpect(jsonPath("$.message").value("El recurso entra en conflicto con un registro existente."));
    }

    @Test
    @DisplayName("Persistencia: DataIntegrityViolationException que no es unicidad retorna HTTP 500 sanitizado sin filtrar SQL")
    void shouldReturn500OnDataIntegrityNonUniqueViolation() throws Exception {
        mockMvc.perform(get("/test/handler/data-integrity-fk"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(SharedErrorCodes.INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.message").value("Ha ocurrido un error de integridad de datos en el servidor"));
    }
}

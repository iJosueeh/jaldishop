package com.jaldishop.backend.shared.exception;

import com.jaldishop.backend.payment.domain.PaymentErrorCodes;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired(required = false)
    private ObjectProvider<AdminAuditService> auditServiceProvider;

    public GlobalExceptionHandler() {}

    public GlobalExceptionHandler(ObjectProvider<AdminAuditService> auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                        ? fieldError.getDefaultMessage()
                        : "Valor invalido",
                        (first, second) -> first
                ));

        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.VALIDATION_ERROR, "Los datos enviados no son válidos", errors)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.VALIDATION_ERROR, "Parámetros de consulta o ruta no válidos", errors)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.VALIDATION_ERROR, "Parámetros de consulta o ruta no válidos")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.INVALID_ARGUMENT, ex.getMessage())
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
        HttpStatus status = "ADDRESS_ALREADY_EXISTS".equals(ex.getCode())
                ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(
                new ApiError(ex.getCode(), ex.getMessage())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.MALFORMED_JSON, "El cuerpo de la solicitud no tiene un formato JSON válido")
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Valor no convertible recibido para parámetro '{}'", ex.getName());
        String message = String.format("El parámetro '%s' tiene un formato no válido", ex.getName());
        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.INVALID_PARAMETER, message)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String message = String.format("El parámetro de consulta obligatorio '%s' no fue proporcionado", ex.getParameterName());
        return ResponseEntity.badRequest().body(
                new ApiError(SharedErrorCodes.MISSING_REQUIRED_PARAMETER, message)
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("El método HTTP '%s' no está soportado para esta ruta", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(ex.getHeaders()).body(
                new ApiError(SharedErrorCodes.METHOD_NOT_ALLOWED, message)
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(ex.getCode(), ex.getMessage())
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(ex.getCode(), ex.getMessage())
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        String message = String.format("El recurso solicitado '%s' no fue encontrado", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(SharedErrorCodes.RESOURCE_NOT_FOUND, message)
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFound(NoHandlerFoundException ex) {
        String message = String.format("El recurso solicitado '%s' no fue encontrado", ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(SharedErrorCodes.RESOURCE_NOT_FOUND, message)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank() || "Bad credentials".equalsIgnoreCase(msg.trim())) {
            msg = "Credenciales no válidas";
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiError(SharedErrorCodes.INVALID_CREDENTIALS, msg)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        if (request != null && request.getAttribute("ADMIN_AUDIT_DENIED_LOGGED") == null) {
            request.setAttribute("ADMIN_AUDIT_DENIED_LOGGED", Boolean.TRUE);
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/v1/admin") && auditServiceProvider != null) {
                auditServiceProvider.ifAvailable(auditService -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    UUID actorId = null;
                    if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                        actorId = principal.userId();
                    }
                    auditService.logDenied(
                            actorId,
                            null,
                            "ACCESS_DENIED",
                            "ADMIN_ROUTE",
                            null,
                            request.getRemoteAddr(),
                            ex.getMessage()
                    );
                });
            }
        }
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank() || "Access is denied".equalsIgnoreCase(msg.trim()) || "Access Denied".equalsIgnoreCase(msg.trim())) {
            msg = "No tienes permisos para realizar esta operación";
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiError(SharedErrorCodes.FORBIDDEN, msg)
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurityException(SecurityException ex) {
        log.warn("Violación de seguridad o entorno no autorizado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiError("UNAUTHORIZED_ENVIRONMENT", ex.getMessage())
        );
    }

    @ExceptionHandler(MercadoPagoException.class)
    public ResponseEntity<ApiError> handleMercadoPagoException(MercadoPagoException ex) {
        log.error("Error en invocación a pasarela Mercado Pago [status={}, errorCode={}]: {}",
                ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());

        String errorCode = ex.getErrorCode();
        int statusCode = ex.getStatusCode();

        if ("MP_TIMEOUT".equals(errorCode) || statusCode == 504) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
                    new ApiError(PaymentErrorCodes.PAYMENT_GATEWAY_TIMEOUT,
                            "Tiempo de espera agotado al comunicarse con la pasarela de pagos. Por favor, intente nuevamente.")
            );
        }

        if ("MP_RATE_LIMIT".equals(errorCode) || statusCode == 429) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    new ApiError(PaymentErrorCodes.PAYMENT_RATE_LIMIT,
                            "Límite de solicitudes excedido con la pasarela de pagos. Por favor, espere antes de reintentar.")
            );
        }

        if ("MP_MISSING_CREDENTIALS".equals(errorCode) || "MP_UNAUTHORIZED".equals(errorCode) || statusCode == 401) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiError(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR,
                            "El servicio de pagos no está disponible temporalmente por problemas de configuración.")
            );
        }

        if ("MP_BAD_REQUEST".equals(errorCode) || (statusCode >= 400 && statusCode < 500)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiError(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR,
                            "La solicitud a la pasarela de pagos contiene parámetros incompatibles.")
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                new ApiError(PaymentErrorCodes.PAYMENT_GATEWAY_ERROR,
                        "La pasarela de pagos no se encuentra disponible temporalmente. Por favor, intente más tarde.")
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia optimista detectado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(SharedErrorCodes.CONCURRENT_MODIFICATION,
                        "El recurso fue modificado concurrentemente por otra operación. Por favor, intente nuevamente.")
        );
    }

    @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleLockingFailure(Exception ex) {
        log.warn("Contención de bloqueo concurrente detectada en base de datos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(SharedErrorCodes.CONCURRENT_MODIFICATION,
                        "El recurso se encuentra temporalmente bloqueado por otra operación concurrente. Por favor, intente nuevamente.")
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos detectada: {}", ex.getMessage());
        if (isUniqueConstraintViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ApiError(SharedErrorCodes.RESOURCE_CONFLICT,
                            "El recurso entra en conflicto con un registro existente.")
            );
        }
        log.error("Violación de integridad de datos no recuperable: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError(SharedErrorCodes.INTERNAL_SERVER_ERROR,
                        "Ha ocurrido un error de integridad de datos en el servidor")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Error no controlado capturado en GlobalExceptionHandler: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError(SharedErrorCodes.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado en el servidor")
        );
    }

    private boolean isUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                if ("23505".equals(cve.getSQLState())) {
                    return true;
                }
            }
            if (current instanceof SQLException sqlEx) {
                if ("23505".equals(sqlEx.getSQLState())) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && (message.contains("23505")
                    || message.toLowerCase().contains("duplicate key")
                    || message.toLowerCase().contains("unique constraint"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}

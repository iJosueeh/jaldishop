package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.OrderCreationRejection;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador de aplicación para la creación de sesiones de pago en Checkout Pro.
 * Soporta tanto Preferences API (predeterminado) como Orders API según configuración.
 * Importante: La llamada a MercadoPagoGateway se ejecuta deliberadamente FUERA de cualquier
 * transacción de base de datos para no bloquear conexiones del pool de PostgreSQL.
 */
@Service
public class CreatePaymentPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentPreferenceService.class);

    public static final Duration SAFETY_MARGIN = Duration.ofSeconds(30);
    public static final Duration MIN_ORDER_VALIDITY = Duration.ofMinutes(1);

    private final PaymentPreferencePreparationService preparationService;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final MercadoPagoProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CreatePaymentPreferenceService(
            PaymentPreferencePreparationService preparationService,
            MercadoPagoGateway mercadoPagoGateway,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.preparationService = preparationService;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public CreatePaymentPreferenceService(
            PaymentPreferencePreparationService preparationService,
            MercadoPagoGateway mercadoPagoGateway,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper
    ) {
        this(preparationService, mercadoPagoGateway, properties, objectMapper, Clock.systemUTC());
    }

    public CreatePaymentPreferenceResult execute(CreatePaymentPreferenceCommand command) {
        // 1. Paso DB 1: Transaccional. Valida reserva, protege capacidad y persiste Payment y PaymentAttempt
        PreparationResult prep = preparationService.prepareOrGetExistingPayment(command);

        // Si ya existía un intento previo válido y con preferencia u orden generada (Idempotencia)
        if (prep.isAlreadyComplete()) {
            String referenceId = prep.attempt().getPreferenceId() != null
                    ? prep.attempt().getPreferenceId()
                    : prep.attempt().getProviderOrderId();
            log.info("Retornando sesión de checkout existente por idempotencia: {} para el pago: {}",
                    referenceId, prep.payment().getId());
            return new CreatePaymentPreferenceResult(
                    prep.payment().getId(),
                    referenceId,
                    prep.attempt().getInitPoint(),
                    prep.payment().getStatus().name()
            );
        }

        // Selección de modo de integración:
        // Si el intento ya tiene un checkoutMode explícito persistido en el intento, lo respeta (reintentos seguros).
        // Si no, recurre al valor configurado globalmente.
        boolean useOrders;
        if (prep.attempt().getCheckoutMode() != null) {
            useOrders = "ORDERS".equalsIgnoreCase(prep.attempt().getCheckoutMode());
        } else {
            useOrders = properties.getCheckoutMode() == MercadoPagoProperties.CheckoutIntegrationMode.ORDERS;
        }

        if (useOrders) {
            return executeWithOrdersApi(prep);
        } else {
            return executeWithPreferencesApi(prep);
        }
    }

    private CreatePaymentPreferenceResult executeWithPreferencesApi(PreparationResult prep) {
        // 2. Paso Gateway Externo: FUERA de transacción de PostgreSQL
        PreferenceResult preferenceResult;
        try {
            CreatePreferenceCommand mpCommand = buildGatewayPreferenceCommand(prep);
            preferenceResult = mercadoPagoGateway.createPreference(mpCommand, prep.attempt().getId());
        } catch (ConflictException ex) {
            throw ex;
        } catch (MercadoPagoException ex) {
            log.warn("Mercado Pago falló al crear preferencia para el intento {}: {}", prep.attempt().getId(), ex.getMessage());
            // Registrar error de intento en DB sin reintentar automáticamente
            preparationService.recordAttemptError(prep.attempt().getId(), ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado al invocar gateway de Mercado Pago: {}", ex.getMessage());
            preparationService.recordAttemptError(prep.attempt().getId(), "UNEXPECTED_ERROR", ex.getMessage());
            throw ex;
        }

        // 3. Paso DB 2: Transaccional. Almacenar preference_id e init_point devueltos
        try {
            preparationService.recordPreferenceResult(
                    prep.attempt().getId(),
                    preferenceResult.preferenceId(),
                    preferenceResult.initPoint()
            );
        } catch (Exception ex) {
            log.error("CRÍTICO: Preferencia creada en Mercado Pago con ID {} para intento {}, pero falló la persistencia local en DB: {}",
                    preferenceResult.preferenceId(), prep.attempt().getId(), ex.getMessage());
            throw ex;
        }

        return new CreatePaymentPreferenceResult(
                prep.payment().getId(),
                preferenceResult.preferenceId(),
                preferenceResult.initPoint(),
                prep.payment().getStatus().name()
        );
    }

    private CreatePaymentPreferenceResult executeWithOrdersApi(PreparationResult prep) {
        // 2. Paso Gateway Externo: FUERA de transacción de PostgreSQL
        OrderResult orderResult;
        try {
            CreateOrderCommand orderCommand = buildGatewayOrderCommand(prep);
            orderResult = mercadoPagoGateway.createOrder(orderCommand, prep.attempt().getId());
        } catch (ConflictException ex) {
            throw ex;
        } catch (MercadoPagoException ex) {
            log.warn("Mercado Pago falló al crear orden para el intento {}: {}", prep.attempt().getId(), ex.getMessage());
            preparationService.recordOrderCreationError(prep.attempt().getPaymentId(), prep.attempt().getId(),
                    OrderCreationRejection.isConfirmed(ex), ex.getErrorCode());
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado al invocar gateway de Mercado Pago (Orders): {}", ex.getMessage());
            preparationService.recordOrderCreationError(prep.attempt().getPaymentId(), prep.attempt().getId(), false, "UNEXPECTED_ERROR");
            throw ex;
        }

        // 3. Paso DB 2: Transaccional. Almacenar provider_order_id y checkout_url como init_point
        try {
            preparationService.recordOrderResult(
                    prep.attempt().getPaymentId(),
                    prep.attempt().getId(),
                    orderResult.orderId(),
                    orderResult.checkoutUrl()
            );
        } catch (Exception ex) {
            log.error("CRÍTICO: Orden creada en Mercado Pago con ID {} para intento {}, pero falló la persistencia local en DB: {}",
                    orderResult.orderId(), prep.attempt().getId(), ex.getMessage());
            throw ex;
        }

        return new CreatePaymentPreferenceResult(
                prep.payment().getId(),
                orderResult.orderId(),
                orderResult.checkoutUrl(),
                prep.payment().getStatus().name()
        );
    }

    private CreateOrderCommand buildGatewayOrderCommand(PreparationResult prep) {
        List<CreateOrderCommand.OrderItem> items = extractOrderItems(prep);

        CreateOrderCommand.PayerInfo payer = new CreateOrderCommand.PayerInfo(
                prep.payment().getCustomerName(),
                "",
                prep.payment().getCustomerEmail(),
                prep.payment().getCustomerPhone()
        );

        String expirationTime = calculateOrderExpirationTime(prep);

        return new CreateOrderCommand(
                "pa1_" + prep.attempt().getId(),
                prep.payment().getAmount(),
                items,
                payer,
                properties.getNotificationUrl(),
                properties.getBackUrlSuccess(),
                properties.getBackUrlFailure(),
                properties.getBackUrlPending(),
                "JALDI SHOP",
                prep.attempt().getIdempotencyKey(),
                expirationTime
        );
    }

    private String calculateOrderExpirationTime(PreparationResult prep) {
        Instant expiresAt = prep.paymentProtectionExpiresAt();
        if (expiresAt == null) {
            log.warn("paymentProtectionExpiresAt es nulo para el intento {}. No se enviará expiration_time.", prep.attempt().getId());
            return null;
        }

        Instant now = clock.instant();
        Duration remaining = Duration.between(now, expiresAt);

        if (remaining.isNegative() || remaining.isZero()) {
            throw new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada.");
        }

        Duration orderValidity = remaining.minus(SAFETY_MARGIN);
        if (orderValidity.compareTo(MIN_ORDER_VALIDITY) < 0) {
            throw new ConflictException(
                    "PAYMENT_PROTECTION_EXPIRING",
                    "El tiempo restante de la reserva (" + remaining.toSeconds() + "s) es insuficiente para completar el pago de forma segura."
            );
        }

        if (orderValidity.compareTo(remaining) > 0) {
            orderValidity = remaining;
        }

        String isoDuration = Duration.ofSeconds(orderValidity.toSeconds()).toString();
        log.info("Calculado expiration_time para orden Checkout Pro: {} (restante reserva: {}s, margen: {}s)",
                isoDuration, remaining.toSeconds(), SAFETY_MARGIN.toSeconds());
        return isoDuration;
    }

    private List<CreateOrderCommand.OrderItem> extractOrderItems(PreparationResult prep) {
        List<CreateOrderCommand.OrderItem> items = new ArrayList<>();

        if (prep.itemSnapshots() != null && !prep.itemSnapshots().isEmpty()) {
            for (CheckoutItemSnapshot item : prep.itemSnapshots()) {
                items.add(new CreateOrderCommand.OrderItem(
                        item.variantId() != null ? item.variantId().toString() : "item",
                        item.productName() + (item.presentationName() != null ? " - " + item.presentationName() : ""),
                        item.presentationName(),
                        item.quantity(),
                        item.unitPriceAmount(),
                        item.currency() != null ? item.currency() : "PEN"
                ));
            }
        } else if (prep.payment().getItemsSnapshot() != null) {
            try {
                List<CheckoutItemSnapshot> deserialized = objectMapper.readValue(
                        prep.payment().getItemsSnapshot(),
                        new TypeReference<>() {}
                );
                for (CheckoutItemSnapshot item : deserialized) {
                    items.add(new CreateOrderCommand.OrderItem(
                            item.variantId() != null ? item.variantId().toString() : "item",
                            item.productName() + (item.presentationName() != null ? " - " + item.presentationName() : ""),
                            item.presentationName(),
                            item.quantity(),
                            item.unitPriceAmount(),
                            item.currency() != null ? item.currency() : "PEN"
                    ));
                }
            } catch (Exception e) {
                items.add(new CreateOrderCommand.OrderItem(
                        "payment-" + prep.payment().getId(),
                        "Compra JaldiShop - " + prep.storeName(),
                        "Total de la compra",
                        1,
                        prep.payment().getAmount(),
                        prep.payment().getCurrency()
                ));
            }
        } else {
            items.add(new CreateOrderCommand.OrderItem(
                    "payment-" + prep.payment().getId(),
                    "Compra JaldiShop - " + prep.storeName(),
                    "Total de la compra",
                    1,
                    prep.payment().getAmount(),
                    prep.payment().getCurrency()
            ));
        }

        // Si existe costo de entrega, agregarlo como ítem si los ítems no suman ya el total
        BigDecimal itemsSum = items.stream()
                .map(it -> it.unitPrice().multiply(BigDecimal.valueOf(it.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (prep.payment().getDeliveryFeeAmount() != null
                && prep.payment().getDeliveryFeeAmount().signum() > 0
                && prep.payment().getAmount().compareTo(itemsSum) > 0) {
            items.add(new CreateOrderCommand.OrderItem(
                    "delivery-fee",
                    "Costo de Envío",
                    "Servicio de despacho",
                    1,
                    prep.payment().getDeliveryFeeAmount(),
                    prep.payment().getCurrency()
            ));
        }

        return items;
    }

    private CreatePreferenceCommand buildGatewayPreferenceCommand(PreparationResult prep) {
        List<CreatePreferenceCommand.PreferenceItem> items = new ArrayList<>();

        if (prep.itemSnapshots() != null && !prep.itemSnapshots().isEmpty()) {
            for (CheckoutItemSnapshot item : prep.itemSnapshots()) {
                items.add(new CreatePreferenceCommand.PreferenceItem(
                        item.variantId() != null ? item.variantId().toString() : "item",
                        item.productName() + (item.presentationName() != null ? " - " + item.presentationName() : ""),
                        item.presentationName(),
                        item.quantity(),
                        item.unitPriceAmount(),
                        item.currency() != null ? item.currency() : "PEN"
                ));
            }
        } else if (prep.payment().getItemsSnapshot() != null) {
            try {
                List<CheckoutItemSnapshot> deserialized = objectMapper.readValue(
                        prep.payment().getItemsSnapshot(),
                        new TypeReference<>() {}
                );
                for (CheckoutItemSnapshot item : deserialized) {
                    items.add(new CreatePreferenceCommand.PreferenceItem(
                            item.variantId() != null ? item.variantId().toString() : "item",
                            item.productName() + (item.presentationName() != null ? " - " + item.presentationName() : ""),
                            item.presentationName(),
                            item.quantity(),
                            item.unitPriceAmount(),
                            item.currency() != null ? item.currency() : "PEN"
                    ));
                }
            } catch (Exception e) {
                // Fallback a ítem consolidado si falla deserialización
                items.add(new CreatePreferenceCommand.PreferenceItem(
                        "payment-" + prep.payment().getId(),
                        "Compra JaldiShop - " + prep.storeName(),
                        "Total de la compra",
                        1,
                        prep.payment().getAmount(),
                        prep.payment().getCurrency()
                ));
            }
        } else {
            // Fallback consolidado
            items.add(new CreatePreferenceCommand.PreferenceItem(
                    "payment-" + prep.payment().getId(),
                    "Compra JaldiShop - " + prep.storeName(),
                    "Total de la compra",
                    1,
                    prep.payment().getAmount(),
                    prep.payment().getCurrency()
            ));
        }

        // Si existe costo de entrega, agregarlo como ítem de servicio si no está en subtotal
        if (prep.payment().getDeliveryFeeAmount() != null && prep.payment().getDeliveryFeeAmount().signum() > 0) {
            items.add(new CreatePreferenceCommand.PreferenceItem(
                    "delivery-fee",
                    "Costo de Envío",
                    "Servicio de despacho",
                    1,
                    prep.payment().getDeliveryFeeAmount(),
                    prep.payment().getCurrency()
            ));
        }

        CreatePreferenceCommand.PayerInfo payer = new CreatePreferenceCommand.PayerInfo(
                prep.payment().getCustomerName(),
                "",
                prep.payment().getCustomerEmail(),
                prep.payment().getCustomerPhone()
        );

        return new CreatePreferenceCommand(
                prep.payment().getId().toString(),
                "Pedido JaldiShop - " + prep.storeName(),
                items,
                payer,
                properties.getNotificationUrl(),
                properties.getBackUrlSuccess(),
                properties.getBackUrlFailure(),
                properties.getBackUrlPending(),
                "JALDI SHOP",
                prep.attempt().getIdempotencyKey()
        );
    }

}

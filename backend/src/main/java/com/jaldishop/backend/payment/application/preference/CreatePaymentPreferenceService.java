package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador de aplicación para la creación de preferencias de pago en Checkout Pro.
 * Importante: La llamada a MercadoPagoGateway se ejecuta deliberadamente FUERA de cualquier
 * transacción de base de datos para no bloquear conexiones del pool de PostgreSQL.
 */
@Service
public class CreatePaymentPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentPreferenceService.class);

    private final PaymentPreferencePreparationService preparationService;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final MercadoPagoProperties properties;
    private final ObjectMapper objectMapper;

    public CreatePaymentPreferenceService(
            PaymentPreferencePreparationService preparationService,
            MercadoPagoGateway mercadoPagoGateway,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper
    ) {
        this.preparationService = preparationService;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public CreatePaymentPreferenceResult execute(CreatePaymentPreferenceCommand command) {
        // 1. Paso DB 1: Transaccional. Valida reserva, protege capacidad y persiste Payment y PaymentAttempt
        PreparationResult prep = preparationService.prepareOrGetExistingPayment(command);

        // Si ya existía un intento previo válido y con preferencia generada (Idempotencia)
        if (prep.isAlreadyComplete()) {
            log.info("Retornando preferencia existente por idempotencia: {} para el pago: {}",
                    prep.attempt().getPreferenceId(), prep.payment().getId());
            return new CreatePaymentPreferenceResult(
                    prep.payment().getId(),
                    prep.attempt().getPreferenceId(),
                    prep.attempt().getInitPoint(),
                    prep.payment().getStatus().name()
            );
        }

        // 2. Paso Gateway Externo: FUERA de transacción de PostgreSQL
        PreferenceResult preferenceResult;
        try {
            CreatePreferenceCommand mpCommand = buildGatewayCommand(prep);
            preferenceResult = mercadoPagoGateway.createPreference(mpCommand);
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

    private CreatePreferenceCommand buildGatewayCommand(PreparationResult prep) {
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

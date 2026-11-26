package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CreatePaymentPreferenceCommand(
        UUID userId,
        UUID capacityReservationId,
        CheckoutFulfillmentType fulfillmentType,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String returnBaseUrl,
        String discountCode,
        String requestedReceiptType,
        String customerDocType,
        String customerDocNumber,
        String customerLegalName
) {
    public CreatePaymentPreferenceCommand(
            UUID userId,
            UUID capacityReservationId,
            CheckoutFulfillmentType fulfillmentType,
            String customerName,
            String customerPhone,
            String customerEmail,
            String deliveryAddress,
            String deliveryReference,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            String returnBaseUrl,
            String discountCode
    ) {
        this(
                userId, capacityReservationId, fulfillmentType, customerName, customerPhone,
                customerEmail, deliveryAddress, deliveryReference, deliveryLatitude,
                deliveryLongitude, returnBaseUrl, discountCode, null, null, null, null
        );
    }
    public CreatePaymentPreferenceCommand(
            UUID userId,
            UUID capacityReservationId,
            CheckoutFulfillmentType fulfillmentType,
            String customerName,
            String customerPhone,
            String customerEmail,
            String deliveryAddress,
            String deliveryReference,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            String returnBaseUrl
    ) {
        this(
                userId,
                capacityReservationId,
                fulfillmentType,
                customerName,
                customerPhone,
                customerEmail,
                deliveryAddress,
                deliveryReference,
                deliveryLatitude,
                deliveryLongitude,
                returnBaseUrl,
                null
        );
    }

    public CreatePaymentPreferenceCommand(
            UUID userId,
            UUID capacityReservationId,
            CheckoutFulfillmentType fulfillmentType,
            String customerName,
            String customerPhone,
            String customerEmail,
            String deliveryAddress,
            String deliveryReference,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude
    ) {
        this(
                userId,
                capacityReservationId,
                fulfillmentType,
                customerName,
                customerPhone,
                customerEmail,
                deliveryAddress,
                deliveryReference,
                deliveryLatitude,
                deliveryLongitude,
                null,
                null
        );
    }

    public CreatePaymentPreferenceCommand {
        Objects.requireNonNull(userId, "userId no puede ser nulo");
        Objects.requireNonNull(capacityReservationId, "capacityReservationId no puede ser nulo");
        Objects.requireNonNull(fulfillmentType, "fulfillmentType no puede ser nulo");
        Objects.requireNonNull(customerName, "customerName no puede ser nulo");
        Objects.requireNonNull(customerPhone, "customerPhone no puede ser nulo");
        Objects.requireNonNull(customerEmail, "customerEmail no puede ser nulo");
    }
}

package com.jaldishop.backend.checkout.web.mapper;

import com.jaldishop.backend.checkout.application.CheckoutResult;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;
import com.jaldishop.backend.checkout.web.dto.CheckoutCustomerResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutItemResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutPricingResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckoutResponseMapper {

    public CheckoutResponse toResponse(CheckoutResult result) {
        if (result == null) {
            return null;
        }

        List<CheckoutItemResponse> itemResponses = result.items().stream()
                .map(this::toItemResponse)
                .toList();

        CheckoutPricingResponse pricingResponse = toPricingResponse(result.pricing());
        CheckoutCustomerResponse customerResponse = new CheckoutCustomerResponse(
                result.customerName(),
                result.customerPhone(),
                result.customerEmail(),
                result.deliveryAddress(),
                result.deliveryReference()
        );

        return new CheckoutResponse(
                result.reservationId(),
                result.reservationExpiresAt(),
                result.storeId(),
                result.storeName(),
                result.userId(),
                result.fulfillmentType(),
                result.serviceDate(),
                result.startTime(),
                result.endTime(),
                customerResponse,
                itemResponses,
                pricingResponse,
                result.createdAt()
        );
    }

    private CheckoutItemResponse toItemResponse(CheckoutItemSnapshot item) {
        return new CheckoutItemResponse(
                item.variantId(),
                item.productId(),
                item.productName(),
                item.presentationName(),
                item.sku(),
                item.imageUrl(),
                item.quantity(),
                item.unitPriceAmount(),
                item.currency(),
                item.subtotalAmount(),
                item.tracksInventory()
        );
    }

    private CheckoutPricingResponse toPricingResponse(CheckoutPricing pricing) {
        if (pricing == null) {
            return null;
        }
        return new CheckoutPricingResponse(
                pricing.productsSubtotalAmount(),
                pricing.discountAmount(),
                pricing.discountCode(),
                pricing.deliveryFeeAmount(),
                pricing.taxRate(),
                pricing.includedTaxAmount(),
                pricing.totalAmount(),
                pricing.currency()
        );
    }
}

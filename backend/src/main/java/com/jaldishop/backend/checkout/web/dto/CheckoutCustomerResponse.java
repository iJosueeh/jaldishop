package com.jaldishop.backend.checkout.web.dto;

public record CheckoutCustomerResponse(
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference
) {}

package com.jaldishop.backend.identity.application;

public record RegisterMerchantCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phone,
        String storeName,
        String businessType,
        String storeContactPhone,
        String address,
        boolean pickupEnabled,
        boolean deliveryEnabled
) { }

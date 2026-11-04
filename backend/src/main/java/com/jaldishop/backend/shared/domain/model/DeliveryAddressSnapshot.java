package com.jaldishop.backend.shared.domain.model;

import java.math.BigDecimal;

public record DeliveryAddressSnapshot(
        String address,
        String reference,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public DeliveryAddressSnapshot {
        address = address != null ? address.trim() : null;
        reference = reference != null ? reference.trim() : null;
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw new IllegalArgumentException("Latitud y longitud deben especificarse conjuntamente.");
        }
    }

    public static DeliveryAddressSnapshot of(String address, String reference, BigDecimal latitude, BigDecimal longitude) {
        return new DeliveryAddressSnapshot(address, reference, latitude, longitude);
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}

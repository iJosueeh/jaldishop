package com.jaldishop.backend.store.domain.model;

import java.math.BigDecimal;

/**
 * Value Object inmutable que modela la dirección física y geolocalización de una tienda.
 * Garantiza que latitud y longitud sean coherentes (ambas presentes o ambas ausentes).
 */
public record StoreLocation(
        String address,
        String addressReference,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public StoreLocation {
        address = address != null ? address.trim() : null;
        addressReference = addressReference != null ? addressReference.trim() : null;
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitud y longitud deben ser ambos nulos o ambos especificados.");
        }
    }

    public static StoreLocation of(String address, String addressReference, BigDecimal latitude, BigDecimal longitude) {
        return new StoreLocation(address, addressReference, latitude, longitude);
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}

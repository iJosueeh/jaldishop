package com.jaldishop.backend.address.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CustomerAddress {

    private final UUID id;
    private final UUID userId;
    private String alias;
    private String recipientName;
    private String recipientPhone;
    private String streetAddress;
    private String department;
    private String province;
    private String district;
    private String ubigeoCode;
    private String reference;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isDefault;
    private final Instant createdAt;
    private Instant updatedAt;

    public CustomerAddress(UUID id, UUID userId, String alias, String recipientName, String recipientPhone,
                           String streetAddress, String department, String province, String district,
                           String ubigeoCode, String reference, BigDecimal latitude, BigDecimal longitude,
                           boolean isDefault, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "El identificador de la dirección es obligatorio.");
        this.userId = Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        this.alias = Objects.requireNonNull(alias, "El alias es obligatorio.").trim();
        this.recipientName = Objects.requireNonNull(recipientName, "El nombre del destinatario es obligatorio.").trim();
        this.recipientPhone = Objects.requireNonNull(recipientPhone, "El teléfono de contacto es obligatorio.").trim();
        this.streetAddress = Objects.requireNonNull(streetAddress, "La dirección exacta es obligatoria.").trim();
        this.department = Objects.requireNonNull(department, "El departamento es obligatorio.").trim();
        this.province = Objects.requireNonNull(province, "La provincia es obligatoria.").trim();
        this.district = Objects.requireNonNull(district, "El distrito es obligatorio.").trim();
        this.ubigeoCode = Objects.requireNonNull(ubigeoCode, "El código UBIGEO es obligatorio.").trim();
        this.reference = reference != null ? reference.trim() : null;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isDefault = isDefault;
        this.createdAt = Objects.requireNonNull(createdAt, "La fecha de creación es obligatoria.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "La fecha de actualización es obligatoria.");
    }

    public static CustomerAddress create(UUID userId, String alias, String recipientName, String recipientPhone,
                                         String streetAddress, String department, String province, String district,
                                         String ubigeoCode, String reference, BigDecimal latitude, BigDecimal longitude,
                                         boolean isDefault) {
        Instant now = Instant.now();
        return new CustomerAddress(
                UUID.randomUUID(),
                userId,
                alias,
                recipientName,
                recipientPhone,
                streetAddress,
                department,
                province,
                district,
                ubigeoCode,
                reference,
                latitude,
                longitude,
                isDefault,
                now,
                now
        );
    }

    public void update(String alias, String recipientName, String recipientPhone,
                       String streetAddress, String department, String province, String district,
                       String ubigeoCode, String reference, BigDecimal latitude, BigDecimal longitude,
                       boolean isDefault) {
        this.alias = Objects.requireNonNull(alias, "El alias es obligatorio.").trim();
        this.recipientName = Objects.requireNonNull(recipientName, "El nombre del destinatario es obligatorio.").trim();
        this.recipientPhone = Objects.requireNonNull(recipientPhone, "El teléfono de contacto es obligatorio.").trim();
        this.streetAddress = Objects.requireNonNull(streetAddress, "La dirección exacta es obligatoria.").trim();
        this.department = Objects.requireNonNull(department, "El departamento es obligatorio.").trim();
        this.province = Objects.requireNonNull(province, "La provincia es obligatoria.").trim();
        this.district = Objects.requireNonNull(district, "El distrito es obligatorio.").trim();
        this.ubigeoCode = Objects.requireNonNull(ubigeoCode, "El código UBIGEO es obligatorio.").trim();
        this.reference = reference != null ? reference.trim() : null;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isDefault = isDefault;
        this.updatedAt = Instant.now();
    }

    public void markAsDefault() {
        this.isDefault = true;
        this.updatedAt = Instant.now();
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAlias() {
        return alias;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getDepartment() {
        return department;
    }

    public String getProvince() {
        return province;
    }

    public String getDistrict() {
        return district;
    }

    public String getUbigeoCode() {
        return ubigeoCode;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

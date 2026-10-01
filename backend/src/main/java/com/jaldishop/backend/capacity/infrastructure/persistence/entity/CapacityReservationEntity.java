package com.jaldishop.backend.capacity.infrastructure.persistence.entity;

import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_reservations")
public class CapacityReservationEntity {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CapacityReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "payment_protection_expires_at")
    private Instant paymentProtectionExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CapacityReservationEntity() {}

    public CapacityReservationEntity(UUID id, UUID storeId, UUID userId, LocalDate serviceDate,
                                     LocalTime startTime, LocalTime endTime, CapacityReservationStatus status,
                                     Instant expiresAt, Instant paymentProtectionExpiresAt,
                                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.expiresAt = expiresAt;
        this.paymentProtectionExpiresAt = paymentProtectionExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStoreId() { return storeId; }
    public UUID getUserId() { return userId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public CapacityReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaymentProtectionExpiresAt() { return paymentProtectionExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
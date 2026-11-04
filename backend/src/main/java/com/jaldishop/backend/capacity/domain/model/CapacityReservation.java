package com.jaldishop.backend.capacity.domain.model;

import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class CapacityReservation {

    public static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    private final UUID id;
    private final UUID storeId;
    private final UUID userId;
    private final LocalDate serviceDate;
    private final CapacityPeriod period;
    private CapacityReservationStatus status;
    private Instant expiresAt;
    private Instant paymentProtectionExpiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private CapacityReservation(UUID id, UUID storeId, UUID userId, LocalDate serviceDate,
                                CapacityPeriod period, CapacityReservationStatus status,
                                Instant expiresAt, Instant paymentProtectionExpiresAt,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.period = period;
        this.status = status;
        this.expiresAt = expiresAt;
        this.paymentProtectionExpiresAt = paymentProtectionExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapacityReservation create(UUID storeId, UUID userId, LocalDate serviceDate,
                                             CapacityPeriod period) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId no puede ser nulo.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser nulo.");
        }
        if (serviceDate == null) {
            throw new IllegalArgumentException("serviceDate no puede ser nulo.");
        }
        if (period == null || period.isAllDay()) {
            throw new IllegalArgumentException("startTime y endTime son obligatorios para reservar la franja.");
        }

        Instant now = Instant.now();
        return new CapacityReservation(
                UUID.randomUUID(), storeId, userId, serviceDate, period,
                CapacityReservationStatus.ACTIVE, now.plus(RESERVATION_TTL), null, now, now);
    }

    public static CapacityReservation create(UUID storeId, UUID userId, LocalDate serviceDate,
                                             LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime y endTime son obligatorios para reservar la franja.");
        }
        return create(storeId, userId, serviceDate, CapacityPeriod.of(startTime, endTime));
    }

    public static CapacityReservation reconstitute(UUID id, UUID storeId, UUID userId, LocalDate serviceDate,
                                                   CapacityPeriod period,
                                                   CapacityReservationStatus status, Instant expiresAt,
                                                   Instant paymentProtectionExpiresAt,
                                                   Instant createdAt, Instant updatedAt) {
        return new CapacityReservation(id, storeId, userId, serviceDate, period,
                status, expiresAt, paymentProtectionExpiresAt, createdAt, updatedAt);
    }

    public static CapacityReservation reconstitute(UUID id, UUID storeId, UUID userId, LocalDate serviceDate,
                                                   LocalTime startTime, LocalTime endTime,
                                                   CapacityReservationStatus status, Instant expiresAt,
                                                   Instant paymentProtectionExpiresAt,
                                                   Instant createdAt, Instant updatedAt) {
        return reconstitute(id, storeId, userId, serviceDate, CapacityPeriod.of(startTime, endTime),
                status, expiresAt, paymentProtectionExpiresAt, createdAt, updatedAt);
    }

    public boolean isExpired(Instant now) {
        if (status == CapacityReservationStatus.ACTIVE) {
            return now.isAfter(expiresAt) || now.equals(expiresAt);
        }
        if (status == CapacityReservationStatus.PAYMENT_PROTECTED) {
            return paymentProtectionExpiresAt == null
                    || now.isAfter(paymentProtectionExpiresAt)
                    || now.equals(paymentProtectionExpiresAt);
        }
        return false;
    }

    public void expireIfDue(Instant now) {
        if (isExpired(now)) {
            this.status = CapacityReservationStatus.EXPIRED;
            this.updatedAt = now;
        }
    }

    public void release() {
        if (status == CapacityReservationStatus.COMMITTED) {
            throw new IllegalStateException("Una reserva comprometida no puede liberarse sin cancelar el pedido.");
        }
        if (status == CapacityReservationStatus.EXPIRED
                || status == CapacityReservationStatus.RELEASED) {
            throw new IllegalStateException("La reserva ya se encuentra en un estado terminal.");
        }
        this.status = CapacityReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public void protectPayment() {
        if (status != CapacityReservationStatus.ACTIVE) {
            throw new IllegalStateException("Solo una reserva activa puede protegerse por pago.");
        }
        if (isExpired(Instant.now())) {
            throw new IllegalStateException("La reserva venció y no puede protegerse por pago.");
        }
        this.status = CapacityReservationStatus.PAYMENT_PROTECTED;
        this.paymentProtectionExpiresAt = Instant.now().plus(RESERVATION_TTL);
        this.updatedAt = Instant.now();
    }

    public void commit() {
        if (status != CapacityReservationStatus.PAYMENT_PROTECTED) {
            throw new IllegalStateException("Solo una reserva protegida por pago puede comprometerse.");
        }
        this.status = CapacityReservationStatus.COMMITTED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStoreId() { return storeId; }
    public UUID getUserId() { return userId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public CapacityPeriod getPeriod() { return period; }
    public LocalTime getStartTime() { return period != null ? period.startTime() : null; }
    public LocalTime getEndTime() { return period != null ? period.endTime() : null; }
    public CapacityReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaymentProtectionExpiresAt() { return paymentProtectionExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
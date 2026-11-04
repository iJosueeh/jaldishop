package com.jaldishop.backend.capacity.domain.model;

import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class CapacityException {

    private final UUID id;
    private final UUID storeId;
    private LocalDate serviceDate;
    private CapacityPeriod period;
    private int exceptionCapacity;
    private String reason;
    private CapacityExceptionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private CapacityException(UUID id, UUID storeId, LocalDate serviceDate, CapacityPeriod period,
                              int exceptionCapacity, String reason,
                              CapacityExceptionStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.serviceDate = serviceDate;
        this.period = period != null ? period : CapacityPeriod.allDay();
        this.exceptionCapacity = exceptionCapacity;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapacityException create(UUID storeId, LocalDate serviceDate, CapacityPeriod period,
                                           int exceptionCapacity, String reason) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId no puede ser nulo.");
        }
        if (serviceDate == null) {
            throw new IllegalArgumentException("serviceDate no puede ser nulo.");
        }
        if (exceptionCapacity < 0) {
            throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");
        }
        CapacityPeriod effectivePeriod = period != null ? period : CapacityPeriod.allDay();

        Instant now = Instant.now();
        return new CapacityException(
                UUID.randomUUID(), storeId, serviceDate,
                effectivePeriod, exceptionCapacity, reason,
                CapacityExceptionStatus.ACTIVE, now, now);
    }

    public static CapacityException create(UUID storeId, LocalDate serviceDate, LocalTime startTime,
                                           LocalTime endTime, int exceptionCapacity, String reason) {
        return create(storeId, serviceDate, CapacityPeriod.of(startTime, endTime), exceptionCapacity, reason);
    }

    public static CapacityException reconstitute(UUID id, UUID storeId, LocalDate serviceDate,
                                                  CapacityPeriod period,
                                                  int exceptionCapacity, String reason,
                                                  CapacityExceptionStatus status,
                                                  Instant createdAt, Instant updatedAt) {
        return new CapacityException(
                id, storeId, serviceDate, period,
                exceptionCapacity, reason, status, createdAt, updatedAt);
    }

    public static CapacityException reconstitute(UUID id, UUID storeId, LocalDate serviceDate,
                                                  LocalTime startTime, LocalTime endTime,
                                                  int exceptionCapacity, String reason,
                                                  CapacityExceptionStatus status,
                                                  Instant createdAt, Instant updatedAt) {
        return reconstitute(id, storeId, serviceDate, CapacityPeriod.of(startTime, endTime),
                exceptionCapacity, reason, status, createdAt, updatedAt);
    }

    public void update(LocalDate serviceDate, CapacityPeriod period,
                       int exceptionCapacity, String reason) {
        if (serviceDate == null) {
            throw new IllegalArgumentException("serviceDate no puede ser nulo.");
        }
        if (exceptionCapacity < 0) {
            throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");
        }
        this.serviceDate = serviceDate;
        this.period = period != null ? period : CapacityPeriod.allDay();
        this.exceptionCapacity = exceptionCapacity;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    public void update(LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
                       int exceptionCapacity, String reason) {
        update(serviceDate, CapacityPeriod.of(startTime, endTime), exceptionCapacity, reason);
    }

    public void activate() {
        this.status = CapacityExceptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CapacityExceptionStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean hasTimeSlot() {
        return this.period != null && this.period.hasTimeSlot();
    }

    public boolean appliesTo(LocalTime reqStartTime, LocalTime reqEndTime) {
        if (status != CapacityExceptionStatus.ACTIVE) {
            return false;
        }
        return this.period != null && this.period.contains(reqStartTime, reqEndTime);
    }

    public UUID getId() { return id; }
    public UUID getStoreId() { return storeId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public CapacityPeriod getPeriod() { return period; }
    public LocalTime getStartTime() { return period != null ? period.startTime() : null; }
    public LocalTime getEndTime() { return period != null ? period.endTime() : null; }
    public int getExceptionCapacity() { return exceptionCapacity; }
    public String getReason() { return reason; }
    public CapacityExceptionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.jaldishop.backend.capacity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class CapacityException {

    private final UUID id;
    private final UUID storeId;
    private LocalDate serviceDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int exceptionCapacity;
    private String reason;
    private CapacityExceptionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private CapacityException(UUID id, UUID storeId, LocalDate serviceDate, LocalTime startTime,
                              LocalTime endTime, int exceptionCapacity, String reason,
                              CapacityExceptionStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.serviceDate = serviceDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exceptionCapacity = exceptionCapacity;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapacityException create(UUID storeId, LocalDate serviceDate, LocalTime startTime,
                                           LocalTime endTime, int exceptionCapacity, String reason) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId no puede ser nulo.");
        }
        if (serviceDate == null) {
            throw new IllegalArgumentException("serviceDate no puede ser nulo.");
        }
        if (exceptionCapacity < 0) {
            throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");
        }
        if ((startTime != null) == (endTime == null)) {
            throw new IllegalArgumentException("startTime y endTime deben ser ambos nulos o ambos con valor.");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime.");
        }

        Instant now = Instant.now();
        return new CapacityException(
                UUID.randomUUID(), storeId, serviceDate,
                startTime, endTime, exceptionCapacity, reason,
                CapacityExceptionStatus.ACTIVE, now, now);
    }

    public static CapacityException reconstitute(UUID id, UUID storeId, LocalDate serviceDate,
                                                  LocalTime startTime, LocalTime endTime,
                                                  int exceptionCapacity, String reason,
                                                  CapacityExceptionStatus status,
                                                  Instant createdAt, Instant updatedAt) {
        return new CapacityException(
                id, storeId, serviceDate, startTime, endTime,
                exceptionCapacity, reason, status, createdAt, updatedAt);
    }

    public void update(LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
                       int exceptionCapacity, String reason) {
        if (serviceDate == null) {
            throw new IllegalArgumentException("serviceDate no puede ser nulo.");
        }
        if (exceptionCapacity < 0) {
            throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");
        }
        if ((startTime != null) == (endTime == null)) {
            throw new IllegalArgumentException("startTime y endTime deben ser ambos nulos o ambos con valor.");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime.");
        }
        this.serviceDate = serviceDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exceptionCapacity = exceptionCapacity;
        this.reason = reason;
        this.updatedAt = Instant.now();
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
        return this.startTime != null && this.endTime != null;
    }

    public boolean appliesTo(LocalTime reqStartTime, LocalTime reqEndTime) {
        if (status != CapacityExceptionStatus.ACTIVE) {
            return false;
        }
        if (!hasTimeSlot()) {
            return true;
        }
        if (reqStartTime == null || reqEndTime == null) {
            return false;
        }
        return !reqStartTime.isBefore(startTime) && !reqEndTime.isAfter(endTime);
    }

    public UUID getId() { return id; }
    public UUID getStoreId() { return storeId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getExceptionCapacity() { return exceptionCapacity; }
    public String getReason() { return reason; }
    public CapacityExceptionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

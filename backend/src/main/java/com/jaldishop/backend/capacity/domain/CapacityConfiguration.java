package com.jaldishop.backend.capacity.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public class CapacityConfiguration {

    private final UUID id;
    private final UUID storeId;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCapacity;
    private CapacityConfigurationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private CapacityConfiguration(UUID id, UUID storeId, int dayOfWeek, LocalTime startTime,
                                  LocalTime endTime, int maxCapacity, CapacityConfigurationStatus status,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapacityConfiguration create(UUID storeId, int dayOfWeek, LocalTime startTime,
                                               LocalTime endTime, int maxCapacity) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId no puede ser nulo.");
        }
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity no puede ser negativo.");
        }
        if (startTime != null && endTime == null) {
            throw new IllegalArgumentException("Si se define startTime, se debe definir endTime.");
        }
        if (startTime == null && endTime != null) {
            throw new IllegalArgumentException("Si se define endTime, se debe definir startTime.");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime.");
        }

        Instant now = Instant.now();
        return new CapacityConfiguration(UUID.randomUUID(), storeId, dayOfWeek, startTime, endTime,
                maxCapacity, CapacityConfigurationStatus.ACTIVE, now, now);
    }

    public static CapacityConfiguration reconstitute(UUID id, UUID storeId, int dayOfWeek,
                                                     LocalTime startTime, LocalTime endTime,
                                                     int maxCapacity, CapacityConfigurationStatus status,
                                                     Instant createdAt, Instant updatedAt) {
        return new CapacityConfiguration(id, storeId, dayOfWeek, startTime, endTime, maxCapacity,
                status, createdAt, updatedAt);
    }

    public void update(int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxCapacity) {
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity no puede ser negativo.");
        }
        if (startTime != null && endTime == null) {
            throw new IllegalArgumentException("Si se define startTime, se debe definir endTime.");
        }
        if (startTime == null && endTime != null) {
            throw new IllegalArgumentException("Si se define endTime, se debe definir startTime.");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime.");
        }

        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = CapacityConfigurationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CapacityConfigurationStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean hasTimeSlot() {
        return startTime != null && endTime != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public CapacityConfigurationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

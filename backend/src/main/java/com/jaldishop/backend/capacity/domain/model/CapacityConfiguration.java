package com.jaldishop.backend.capacity.domain.model;

import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public class CapacityConfiguration {

    private final UUID id;
    private final UUID storeId;
    private int dayOfWeek;
    private CapacityPeriod period;
    private int maxCapacity;
    private CapacityConfigurationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private CapacityConfiguration(UUID id, UUID storeId, int dayOfWeek, CapacityPeriod period,
                                  int maxCapacity, CapacityConfigurationStatus status,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.dayOfWeek = dayOfWeek;
        this.period = period != null ? period : CapacityPeriod.allDay();
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapacityConfiguration create(UUID storeId, int dayOfWeek, CapacityPeriod period,
                                               int maxCapacity) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId no puede ser nulo.");
        }
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity no puede ser negativo.");
        }
        CapacityPeriod effectivePeriod = period != null ? period : CapacityPeriod.allDay();

        Instant now = Instant.now();
        return new CapacityConfiguration(UUID.randomUUID(), storeId, dayOfWeek, effectivePeriod,
                maxCapacity, CapacityConfigurationStatus.ACTIVE, now, now);
    }

    public static CapacityConfiguration create(UUID storeId, int dayOfWeek, LocalTime startTime,
                                               LocalTime endTime, int maxCapacity) {
        return create(storeId, dayOfWeek, CapacityPeriod.of(startTime, endTime), maxCapacity);
    }

    public static CapacityConfiguration reconstitute(UUID id, UUID storeId, int dayOfWeek,
                                                     CapacityPeriod period, int maxCapacity,
                                                     CapacityConfigurationStatus status,
                                                     Instant createdAt, Instant updatedAt) {
        return new CapacityConfiguration(id, storeId, dayOfWeek, period, maxCapacity,
                status, createdAt, updatedAt);
    }

    public static CapacityConfiguration reconstitute(UUID id, UUID storeId, int dayOfWeek,
                                                     LocalTime startTime, LocalTime endTime,
                                                     int maxCapacity, CapacityConfigurationStatus status,
                                                     Instant createdAt, Instant updatedAt) {
        return reconstitute(id, storeId, dayOfWeek, CapacityPeriod.of(startTime, endTime), maxCapacity,
                status, createdAt, updatedAt);
    }

    public void update(int dayOfWeek, CapacityPeriod period, int maxCapacity) {
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity no puede ser negativo.");
        }

        this.dayOfWeek = dayOfWeek;
        this.period = period != null ? period : CapacityPeriod.allDay();
        this.maxCapacity = maxCapacity;
        this.updatedAt = Instant.now();
    }

    public void update(int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxCapacity) {
        update(dayOfWeek, CapacityPeriod.of(startTime, endTime), maxCapacity);
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
        return period != null && period.hasTimeSlot();
    }

    public boolean appliesTo(int dayOfWeek, LocalTime reqStartTime, LocalTime reqEndTime) {
        if (status != CapacityConfigurationStatus.ACTIVE) {
            return false;
        }
        if (this.dayOfWeek != dayOfWeek) {
            return false;
        }
        return period != null && period.contains(reqStartTime, reqEndTime);
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

    public CapacityPeriod getPeriod() {
        return period;
    }

    public LocalTime getStartTime() {
        return period != null ? period.startTime() : null;
    }

    public LocalTime getEndTime() {
        return period != null ? period.endTime() : null;
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

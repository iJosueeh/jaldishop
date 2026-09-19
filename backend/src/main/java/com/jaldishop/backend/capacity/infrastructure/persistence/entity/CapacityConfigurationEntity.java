package com.jaldishop.backend.capacity.infrastructure.persistence.entity;

import com.jaldishop.backend.capacity.domain.CapacityConfigurationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_configurations")
public class CapacityConfigurationEntity {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CapacityConfigurationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CapacityConfigurationEntity() {}

    public CapacityConfigurationEntity(UUID id, UUID storeId, int dayOfWeek, LocalTime startTime,
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

    public UUID getId() { return id; }
    public UUID getStoreId() { return storeId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getMaxCapacity() { return maxCapacity; }
    public CapacityConfigurationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

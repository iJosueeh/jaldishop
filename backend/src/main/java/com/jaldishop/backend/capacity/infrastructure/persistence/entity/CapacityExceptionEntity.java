package com.jaldishop.backend.capacity.infrastructure.persistence.entity;

import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_exceptions")
public class CapacityExceptionEntity {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "exception_capacity", nullable = false)
    private int exceptionCapacity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CapacityExceptionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CapacityExceptionEntity() {}

    public CapacityExceptionEntity(UUID id, UUID storeId, LocalDate serviceDate, LocalTime startTime,
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

package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityReservationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityReservationPersistenceMapperTest {

    private CapacityReservationPersistenceMapper mapper;

    private UUID testId;
    private UUID testStoreId;
    private UUID testUserId;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;

    @BeforeEach
    void setUp() {
        mapper = new CapacityReservationPersistenceMapper();
        testId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCreatedAt = Instant.parse("2026-09-22T10:00:00Z");
        testUpdatedAt = Instant.parse("2026-09-22T10:00:05Z");
    }

    @Test
    @DisplayName("toEntity() debe mapear todos los campos correctamente")
    void toEntityShouldMapAllFields() {
        CapacityReservation domain = CapacityReservation.reconstitute(
                testId, testStoreId, testUserId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.ACTIVE, Instant.parse("2026-09-22T10:10:00Z"), null,
                testCreatedAt, testUpdatedAt);

        CapacityReservationEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testStoreId, entity.getStoreId());
        assertEquals(testUserId, entity.getUserId());
        assertEquals(LocalDate.of(2026, 9, 22), entity.getServiceDate());
        assertEquals(LocalTime.of(10, 0), entity.getStartTime());
        assertEquals(LocalTime.of(12, 0), entity.getEndTime());
        assertEquals(CapacityReservationStatus.ACTIVE, entity.getStatus());
        assertEquals(Instant.parse("2026-09-22T10:10:00Z"), entity.getExpiresAt());
        assertNull(entity.getPaymentProtectionExpiresAt());
        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe mapear todos los campos correctamente")
    void toDomainShouldMapAllFields() {
        CapacityReservationEntity entity = new CapacityReservationEntity(
                testId, testStoreId, testUserId, LocalDate.of(2026, 12, 25),
                LocalTime.of(18, 0), LocalTime.of(20, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED,
                Instant.parse("2026-12-25T18:10:00Z"), Instant.parse("2026-12-25T18:20:00Z"),
                testCreatedAt, testUpdatedAt);

        CapacityReservation domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals(testStoreId, domain.getStoreId());
        assertEquals(testUserId, domain.getUserId());
        assertEquals(LocalDate.of(2026, 12, 25), domain.getServiceDate());
        assertEquals(LocalTime.of(18, 0), domain.getStartTime());
        assertEquals(LocalTime.of(20, 0), domain.getEndTime());
        assertEquals(CapacityReservationStatus.PAYMENT_PROTECTED, domain.getStatus());
        assertEquals(Instant.parse("2026-12-25T18:10:00Z"), domain.getExpiresAt());
        assertEquals(Instant.parse("2026-12-25T18:20:00Z"), domain.getPaymentProtectionExpiresAt());
        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertEquals(testUpdatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Roundtrip: toDomain(toEntity(domain)) debe preservar todos los campos")
    void roundtripShouldPreserveAllFields() {
        CapacityReservation original = CapacityReservation.reconstitute(
                testId, testStoreId, testUserId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.COMMITTED, Instant.parse("2026-09-22T10:10:00Z"),
                Instant.parse("2026-09-22T10:20:00Z"), testCreatedAt, testUpdatedAt);

        CapacityReservationEntity entity = mapper.toEntity(original);
        CapacityReservation roundtripped = mapper.toDomain(entity);

        assertEquals(original.getId(), roundtripped.getId());
        assertEquals(original.getStoreId(), roundtripped.getStoreId());
        assertEquals(original.getUserId(), roundtripped.getUserId());
        assertEquals(original.getServiceDate(), roundtripped.getServiceDate());
        assertEquals(original.getStartTime(), roundtripped.getStartTime());
        assertEquals(original.getEndTime(), roundtripped.getEndTime());
        assertEquals(original.getStatus(), roundtripped.getStatus());
        assertEquals(original.getExpiresAt(), roundtripped.getExpiresAt());
        assertEquals(original.getPaymentProtectionExpiresAt(), roundtripped.getPaymentProtectionExpiresAt());
        assertEquals(original.getCreatedAt(), roundtripped.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundtripped.getUpdatedAt());
    }
}
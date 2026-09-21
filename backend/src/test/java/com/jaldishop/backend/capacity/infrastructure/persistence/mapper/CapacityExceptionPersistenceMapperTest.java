package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityExceptionPersistenceMapperTest {

    private CapacityExceptionPersistenceMapper mapper;

    private UUID testId;
    private UUID testStoreId;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;

    @BeforeEach
    void setUp() {
        mapper = new CapacityExceptionPersistenceMapper();
        testId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        testCreatedAt = Instant.parse("2026-01-10T08:00:00Z");
        testUpdatedAt = Instant.parse("2026-01-10T12:00:00Z");
    }

    @Test
    @DisplayName("toEntity() debe mapear todos los campos correctamente")
    void toEntityShouldMapAllFields() {
        CapacityException domain = CapacityException.reconstitute(
                testId, testStoreId, LocalDate.of(2026, 7, 20),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 15, "Verano",
                CapacityExceptionStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityExceptionEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testStoreId, entity.getStoreId());
        assertEquals(LocalDate.of(2026, 7, 20), entity.getServiceDate());
        assertEquals(LocalTime.of(9, 0), entity.getStartTime());
        assertEquals(LocalTime.of(17, 0), entity.getEndTime());
        assertEquals(15, entity.getExceptionCapacity());
        assertEquals("Verano", entity.getReason());
        assertEquals(CapacityExceptionStatus.ACTIVE, entity.getStatus());
        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe mapear todos los campos correctamente")
    void toDomainShouldMapAllFields() {
        CapacityExceptionEntity entity = new CapacityExceptionEntity(
                testId, testStoreId, LocalDate.of(2026, 12, 25),
                null, null, 0, "Navidad cerrado",
                CapacityExceptionStatus.INACTIVE, testCreatedAt, testUpdatedAt);

        CapacityException domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals(testStoreId, domain.getStoreId());
        assertEquals(LocalDate.of(2026, 12, 25), domain.getServiceDate());
        assertNull(domain.getStartTime());
        assertNull(domain.getEndTime());
        assertEquals(0, domain.getExceptionCapacity());
        assertEquals("Navidad cerrado", domain.getReason());
        assertEquals(CapacityExceptionStatus.INACTIVE, domain.getStatus());
        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertEquals(testUpdatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Roundtrip: toDomain(toEntity(domain)) debe preservar todos los campos")
    void roundtripShouldPreserveAllFields() {
        CapacityException original = CapacityException.reconstitute(
                testId, testStoreId, LocalDate.of(2026, 3, 8),
                LocalTime.of(8, 0), LocalTime.of(20, 0), 30, "Día de la mujer",
                CapacityExceptionStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityExceptionEntity entity = mapper.toEntity(original);
        CapacityException roundtripped = mapper.toDomain(entity);

        assertEquals(original.getId(), roundtripped.getId());
        assertEquals(original.getStoreId(), roundtripped.getStoreId());
        assertEquals(original.getServiceDate(), roundtripped.getServiceDate());
        assertEquals(original.getStartTime(), roundtripped.getStartTime());
        assertEquals(original.getEndTime(), roundtripped.getEndTime());
        assertEquals(original.getExceptionCapacity(), roundtripped.getExceptionCapacity());
        assertEquals(original.getReason(), roundtripped.getReason());
        assertEquals(original.getStatus(), roundtripped.getStatus());
        assertEquals(original.getCreatedAt(), roundtripped.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundtripped.getUpdatedAt());
    }

    @Test
    @DisplayName("Full day: startTime y endTime deben ser null")
    void fullDayShouldHaveNullTimeFields() {
        CapacityException domain = CapacityException.reconstitute(
                testId, testStoreId, LocalDate.of(2026, 1, 1),
                null, null, 0, null,
                CapacityExceptionStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityExceptionEntity entity = mapper.toEntity(domain);
        assertNull(entity.getStartTime());
        assertNull(entity.getEndTime());

        CapacityException roundtripped = mapper.toDomain(entity);
        assertNull(roundtripped.getStartTime());
        assertNull(roundtripped.getEndTime());
    }
}

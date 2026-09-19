package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationStatus;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityConfigurationPersistenceMapperTest {

    private CapacityConfigurationPersistenceMapper mapper;

    private UUID testId;
    private UUID testStoreId;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;

    @BeforeEach
    void setUp() {
        mapper = new CapacityConfigurationPersistenceMapper();
        testId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        testCreatedAt = Instant.parse("2026-01-10T08:00:00Z");
        testUpdatedAt = Instant.parse("2026-01-10T12:00:00Z");
    }

    @Test
    @DisplayName("toEntity() debe mapear todos los campos correctamente")
    void toEntityShouldMapAllFields() {
        CapacityConfiguration domain = CapacityConfiguration.reconstitute(
                testId, testStoreId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), 20,
                CapacityConfigurationStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityConfigurationEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testStoreId, entity.getStoreId());
        assertEquals(3, entity.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), entity.getStartTime());
        assertEquals(LocalTime.of(17, 0), entity.getEndTime());
        assertEquals(20, entity.getMaxCapacity());
        assertEquals(CapacityConfigurationStatus.ACTIVE, entity.getStatus());
        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe mapear todos los campos correctamente")
    void toDomainShouldMapAllFields() {
        CapacityConfigurationEntity entity = new CapacityConfigurationEntity(
                testId, testStoreId, 5, LocalTime.of(10, 0), LocalTime.of(14, 0), 8,
                CapacityConfigurationStatus.INACTIVE, testCreatedAt, testUpdatedAt);

        CapacityConfiguration domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals(testStoreId, domain.getStoreId());
        assertEquals(5, domain.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), domain.getStartTime());
        assertEquals(LocalTime.of(14, 0), domain.getEndTime());
        assertEquals(8, domain.getMaxCapacity());
        assertEquals(CapacityConfigurationStatus.INACTIVE, domain.getStatus());
        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertEquals(testUpdatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Roundtrip: toDomain(toEntity(domain)) debe preservar todos los campos")
    void roundtripShouldPreserveAllFields() {
        CapacityConfiguration original = CapacityConfiguration.reconstitute(
                testId, testStoreId, 2, LocalTime.of(8, 0), LocalTime.of(20, 0), 50,
                CapacityConfigurationStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityConfigurationEntity entity = mapper.toEntity(original);
        CapacityConfiguration roundtripped = mapper.toDomain(entity);

        assertEquals(original.getId(), roundtripped.getId());
        assertEquals(original.getStoreId(), roundtripped.getStoreId());
        assertEquals(original.getDayOfWeek(), roundtripped.getDayOfWeek());
        assertEquals(original.getStartTime(), roundtripped.getStartTime());
        assertEquals(original.getEndTime(), roundtripped.getEndTime());
        assertEquals(original.getMaxCapacity(), roundtripped.getMaxCapacity());
        assertEquals(original.getStatus(), roundtripped.getStatus());
        assertEquals(original.getCreatedAt(), roundtripped.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundtripped.getUpdatedAt());
    }

    @Test
    @DisplayName("Full day: startTime y endTime deben ser null")
    void fullDayShouldHaveNullTimeFields() {
        CapacityConfiguration domain = CapacityConfiguration.reconstitute(
                testId, testStoreId, 0, null, null, 10,
                CapacityConfigurationStatus.ACTIVE, testCreatedAt, testUpdatedAt);

        CapacityConfigurationEntity entity = mapper.toEntity(domain);
        assertNull(entity.getStartTime());
        assertNull(entity.getEndTime());

        CapacityConfiguration roundtripped = mapper.toDomain(entity);
        assertNull(roundtripped.getStartTime());
        assertNull(roundtripped.getEndTime());
    }
}

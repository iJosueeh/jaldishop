package com.jaldishop.backend.capacity.domain.repository;

import com.jaldishop.backend.capacity.domain.model.*;
import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapacityConfigurationRepository {
    Optional<CapacityConfiguration> findById(UUID id);
    Optional<CapacityConfiguration> findByIdForUpdate(UUID id);
    List<CapacityConfiguration> findByStoreId(UUID storeId);
    boolean existsOverlappingByStoreIdAndDayOfWeek(
            UUID storeId, int dayOfWeek, LocalTime startTime, LocalTime endTime, UUID excludeId);
    CapacityConfiguration save(CapacityConfiguration config);
}

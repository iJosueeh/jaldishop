package com.jaldishop.backend.capacity.domain.repository;

import com.jaldishop.backend.capacity.domain.model.*;
import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapacityExceptionRepository {
    Optional<CapacityException> findById(UUID id);
    Optional<CapacityException> findByIdForUpdate(UUID id);
    List<CapacityException> findByStoreId(UUID storeId);
    List<CapacityException> findByStoreIdAndServiceDate(UUID storeId, LocalDate serviceDate);
    CapacityException save(CapacityException exception);
}

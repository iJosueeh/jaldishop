package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.repository.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DeleteCapacityExceptionService {

    private final CapacityExceptionRepository repository;

    public DeleteCapacityExceptionService(CapacityExceptionRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID id, UUID storeId) {
        CapacityException exception = repository.findByIdForUpdate(id)
                .filter(e -> e.getStoreId().equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("Excepción de capacidad", id.toString()));

        if (repository.hasActiveOrCommittedReservations(
                storeId,
                exception.getServiceDate(),
                exception.getStartTime(),
                exception.getEndTime())) {
            throw new ResourceConflictException(
                    "CAPACITY_HAS_RESERVATIONS",
                    "Esta excepción tiene reservas activas o confirmadas y no puede eliminarse. Puedes desactivarla en su lugar.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", exception.getStatus().name(),
                            "blockingReason", "CAPACITY_HAS_RESERVATIONS"
                    )
            );
        }

        repository.deleteByIdAndStoreId(id, storeId);
    }
}

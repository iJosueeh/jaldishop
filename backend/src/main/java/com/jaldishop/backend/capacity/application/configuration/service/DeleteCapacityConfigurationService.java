package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DeleteCapacityConfigurationService {

    private final CapacityConfigurationRepository repository;

    public DeleteCapacityConfigurationService(CapacityConfigurationRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID id, UUID storeId) {
        CapacityConfiguration config = repository.findByIdForUpdate(id)
                .filter(c -> c.getStoreId().equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("Configuración de capacidad", id.toString()));

        if (repository.hasActiveOrCommittedReservations(
                storeId,
                config.getDayOfWeek(),
                config.getStartTime(),
                config.getEndTime())) {
            throw new ResourceConflictException(
                    "CAPACITY_HAS_RESERVATIONS",
                    "Este horario tiene reservas activas o confirmadas y no puede eliminarse. Puedes desactivarlo para que no reciba nuevas reservas.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", config.getStatus().name(),
                            "blockingReason", "CAPACITY_HAS_RESERVATIONS"
                    )
            );
        }

        repository.deleteByIdAndStoreId(id, storeId);
    }
}

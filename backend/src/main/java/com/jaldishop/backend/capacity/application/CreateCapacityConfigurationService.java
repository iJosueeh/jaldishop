package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCapacityConfigurationService {

    private final CapacityConfigurationRepository repository;

    public CreateCapacityConfigurationService(CapacityConfigurationRepository repository) {
        this.repository = repository;
    }

    public CapacityConfiguration execute(CreateCapacityConfigurationCommand command) {
        if (command.startTime() != null && command.endTime() != null) {
            boolean overlaps = repository.existsOverlappingByStoreIdAndDayOfWeek(
                    command.storeId(),
                    command.dayOfWeek(),
                    command.startTime(),
                    command.endTime(),
                    null);

            if (overlaps) {
                throw new BusinessRuleException("CAPACITY_OVERLAP",
                        "Ya existe una configuración activa con franja horaria solapada para este día.");
            }
        }

        CapacityConfiguration config = CapacityConfiguration.create(
                command.storeId(),
                command.dayOfWeek(),
                command.startTime(),
                command.endTime(),
                command.maxCapacity());

        return repository.save(config);
    }
}

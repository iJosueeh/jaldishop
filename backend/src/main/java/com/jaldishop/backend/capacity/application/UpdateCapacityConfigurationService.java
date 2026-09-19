package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@Transactional
public class UpdateCapacityConfigurationService {

    private final CapacityConfigurationRepository repository;

    public UpdateCapacityConfigurationService(CapacityConfigurationRepository repository) {
        this.repository = repository;
    }

    public CapacityConfiguration execute(UpdateCapacityConfigurationCommand command) {
        CapacityConfiguration config = repository.findById(command.configId())
                .orElseThrow(() -> new ResourceNotFoundException("CapacityConfiguration", command.configId()));

        if (!config.getStoreId().equals(command.storeId())) {
            throw new ResourceNotFoundException("CapacityConfiguration", command.configId());
        }

        boolean dayOrTimeChanged = config.getDayOfWeek() != command.dayOfWeek()
                || !safeEquals(config.getStartTime(), command.startTime())
                || !safeEquals(config.getEndTime(), command.endTime());

        if (dayOrTimeChanged && command.startTime() != null && command.endTime() != null) {
            boolean overlaps = repository.existsOverlappingByStoreIdAndDayOfWeek(
                    command.storeId(),
                    command.dayOfWeek(),
                    command.startTime(),
                    command.endTime(),
                    command.configId());

            if (overlaps) {
                throw new BusinessRuleException("CAPACITY_OVERLAP",
                        "Ya existe una configuración activa con franja horaria solapada para este día.");
            }
        }

        config.update(command.dayOfWeek(), command.startTime(), command.endTime(), command.maxCapacity());
        return repository.save(config);
    }

    private boolean safeEquals(LocalTime a, LocalTime b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}

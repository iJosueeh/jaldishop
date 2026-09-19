package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ToggleCapacityConfigurationStatusService {

    private final CapacityConfigurationRepository repository;

    public ToggleCapacityConfigurationStatusService(CapacityConfigurationRepository repository) {
        this.repository = repository;
    }

    public CapacityConfiguration activate(UUID configId, UUID storeId) {
        CapacityConfiguration config = findByIdAndStore(configId, storeId);
        config.activate();
        return repository.save(config);
    }

    public CapacityConfiguration deactivate(UUID configId, UUID storeId) {
        CapacityConfiguration config = findByIdAndStore(configId, storeId);
        config.deactivate();
        return repository.save(config);
    }

    private CapacityConfiguration findByIdAndStore(UUID configId, UUID storeId) {
        CapacityConfiguration config = repository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityConfiguration", configId));

        if (!config.getStoreId().equals(storeId)) {
            throw new ResourceNotFoundException("CapacityConfiguration", configId);
        }

        return config;
    }
}

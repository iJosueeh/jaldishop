package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.application.configuration.command.*;

import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetStoreCapacityConfigurationsService {

    private final CapacityConfigurationRepository repository;

    public GetStoreCapacityConfigurationsService(CapacityConfigurationRepository repository) {
        this.repository = repository;
    }

    public List<CapacityConfiguration> execute(UUID storeId) {
        return repository.findByStoreId(storeId);
    }
}

package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetStoreCapacityExceptionsService {

    private final CapacityExceptionRepository repository;

    public GetStoreCapacityExceptionsService(CapacityExceptionRepository repository) {
        this.repository = repository;
    }

    public List<CapacityException> execute(UUID storeId) {
        return repository.findByStoreId(storeId);
    }
}

package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ToggleCapacityExceptionStatusService {

    private final CapacityExceptionRepository repository;

    public ToggleCapacityExceptionStatusService(CapacityExceptionRepository repository) {
        this.repository = repository;
    }

    public CapacityException activate(UUID exceptionId, UUID storeId) {
        CapacityException exception = findByIdAndStore(exceptionId, storeId);
        exception.activate();
        return repository.save(exception);
    }

    public CapacityException deactivate(UUID exceptionId, UUID storeId) {
        CapacityException exception = findByIdAndStore(exceptionId, storeId);
        exception.deactivate();
        return repository.save(exception);
    }

    private CapacityException findByIdAndStore(UUID exceptionId, UUID storeId) {
        CapacityException exception = repository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAPACITY_EXCEPTION_NOT_FOUND",
                        "No se encontró la excepción de capacidad con el ID especificado."));
        if (!exception.getStoreId().equals(storeId)) {
            throw new ResourceNotFoundException(
                    "CAPACITY_EXCEPTION_NOT_FOUND",
                    "La excepción de capacidad no pertenece a esta tienda.");
        }
        return exception;
    }
}

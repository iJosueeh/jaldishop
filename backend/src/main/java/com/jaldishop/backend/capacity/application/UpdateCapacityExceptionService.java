package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateCapacityExceptionService {

    private final CapacityExceptionRepository repository;

    public UpdateCapacityExceptionService(CapacityExceptionRepository repository) {
        this.repository = repository;
    }

    public CapacityException execute(UpdateCapacityExceptionCommand command) {
        CapacityException exception = repository.findById(command.exceptionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAPACITY_EXCEPTION_NOT_FOUND",
                        "No se encontró la excepción de capacidad con el ID especificado."));

        if (!exception.getStoreId().equals(command.storeId())) {
            throw new ResourceNotFoundException(
                    "CAPACITY_EXCEPTION_NOT_FOUND",
                    "La excepción de capacidad no pertenece a esta tienda.");
        }

        exception.update(command.serviceDate(), command.startTime(), command.endTime(),
                command.exceptionCapacity(), command.reason());

        return repository.save(exception);
    }
}

package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.application.configuration.command.*;

import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.repository.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.enums.CapacityExceptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCapacityExceptionService {

    private final CapacityExceptionRepository repository;

    public CreateCapacityExceptionService(CapacityExceptionRepository repository) {
        this.repository = repository;
    }

    public CapacityException execute(CreateCapacityExceptionCommand command) {
        CapacityException exception = CapacityException.create(
                command.storeId(), command.serviceDate(),
                command.startTime(), command.endTime(),
                command.exceptionCapacity(), command.reason());

        return repository.save(exception);
    }
}

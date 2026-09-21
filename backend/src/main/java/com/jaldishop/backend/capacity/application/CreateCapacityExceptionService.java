package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
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

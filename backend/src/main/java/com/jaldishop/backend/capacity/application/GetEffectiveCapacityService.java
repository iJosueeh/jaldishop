package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetEffectiveCapacityService {

    private final CapacityConfigurationRepository configurationRepository;
    private final CapacityExceptionRepository exceptionRepository;

    public GetEffectiveCapacityService(CapacityConfigurationRepository configurationRepository,
                                       CapacityExceptionRepository exceptionRepository) {
        this.configurationRepository = configurationRepository;
        this.exceptionRepository = exceptionRepository;
    }

    public EffectiveCapacityResult execute(EffectiveCapacityQuery query) {
        validateQuery(query);

        CapacityException exception = findApplicableException(
                query.storeId(), query.serviceDate(), query.startTime(), query.endTime());
        if (exception != null) {
            return new EffectiveCapacityResult(exception.getExceptionCapacity(), EffectiveCapacitySource.EXCEPTION);
        }

        CapacityConfiguration configuration = findApplicableConfiguration(
                query.storeId(), query.serviceDate(), query.startTime(), query.endTime());
        if (configuration != null) {
            return new EffectiveCapacityResult(configuration.getMaxCapacity(), EffectiveCapacitySource.BASE);
        }

        return new EffectiveCapacityResult(0, EffectiveCapacitySource.NONE);
    }

    private void validateQuery(EffectiveCapacityQuery query) {
        if (query.serviceDate() == null) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha es obligatoria para consultar la capacidad efectiva.");
        }
        if (query.startTime() == null || query.endTime() == null) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "Debe indicar la franja horaria (startTime y endTime).");
        }
        if (!query.startTime().isBefore(query.endTime())) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "startTime debe ser anterior a endTime.");
        }
    }

    private CapacityException findApplicableException(UUID storeId, LocalDate serviceDate,
                                                      LocalTime startTime, LocalTime endTime) {
        List<CapacityException> exceptions = exceptionRepository.findByStoreIdAndServiceDate(storeId, serviceDate);
        CapacityException fullDay = null;
        CapacityException slot = null;

        for (CapacityException exception : exceptions) {
            if (!exception.appliesTo(startTime, endTime)) {
                continue;
            }
            if (exception.hasTimeSlot()) {
                if (slot == null) {
                    slot = exception;
                }
            } else {
                fullDay = exception;
            }
        }

        return fullDay != null ? fullDay : slot;
    }

    private CapacityConfiguration findApplicableConfiguration(UUID storeId, LocalDate serviceDate,
                                                             LocalTime startTime, LocalTime endTime) {
        int dayOfWeek = serviceDate.getDayOfWeek().getValue() % 7;
        List<CapacityConfiguration> configurations = configurationRepository.findByStoreId(storeId);
        CapacityConfiguration fullDay = null;
        CapacityConfiguration slot = null;

        for (CapacityConfiguration configuration : configurations) {
            if (!configuration.appliesTo(dayOfWeek, startTime, endTime)) {
                continue;
            }
            if (configuration.hasTimeSlot()) {
                if (slot == null) {
                    slot = configuration;
                }
            } else {
                fullDay = configuration;
            }
        }

        return slot != null ? slot : fullDay;
    }
}
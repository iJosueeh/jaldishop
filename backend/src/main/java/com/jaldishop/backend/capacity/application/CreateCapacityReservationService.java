package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CreateCapacityReservationService {

    private final GetEffectiveCapacityService getEffectiveCapacityService;
    private final CapacityConfigurationRepository configurationRepository;
    private final CapacityExceptionRepository exceptionRepository;
    private final CapacityReservationRepository reservationRepository;

    public CreateCapacityReservationService(GetEffectiveCapacityService getEffectiveCapacityService,
                                            CapacityConfigurationRepository configurationRepository,
                                            CapacityExceptionRepository exceptionRepository,
                                            CapacityReservationRepository reservationRepository) {
        this.getEffectiveCapacityService = getEffectiveCapacityService;
        this.configurationRepository = configurationRepository;
        this.exceptionRepository = exceptionRepository;
        this.reservationRepository = reservationRepository;
    }

    public CapacityReservation execute(CreateCapacityReservationCommand command) {
        EffectiveCapacityQuery query = new EffectiveCapacityQuery(
                command.storeId(), command.serviceDate(), command.startTime(), command.endTime());
        EffectiveCapacityResolution resolution = getEffectiveCapacityService.resolve(query);

        if (resolution.capacity() <= 0) {
            throw new ConflictException("CAPACITY_UNAVAILABLE",
                    "No hay capacidad disponible para la franja horaria seleccionada.");
        }

        lockAnchor(resolution);

        Instant now = Instant.now();
        if (reservationRepository.existsReservedByUserAndWindow(
                command.userId(), command.storeId(), command.serviceDate(),
                command.startTime(), command.endTime(), now)) {
            throw new ConflictException("ALREADY_RESERVED",
                    "Ya existe una reserva activa para esta franja horaria.");
        }

        int reserved = reservationRepository.countReserved(
                command.storeId(), command.serviceDate(), command.startTime(), command.endTime(), now);
        int committed = reservationRepository.countCommitted(
                command.storeId(), command.serviceDate(), command.startTime(), command.endTime());
        if (resolution.capacity() - reserved - committed <= 0) {
            throw new ConflictException("CAPACITY_EXHAUSTED",
                    "No quedan cupos disponibles para la franja horaria seleccionada.");
        }

        CapacityReservation reservation = CapacityReservation.create(
                command.storeId(), command.userId(), command.serviceDate(),
                command.startTime(), command.endTime());
        return reservationRepository.save(reservation);
    }

    private void lockAnchor(EffectiveCapacityResolution resolution) {
        if (resolution.exceptionId() != null) {
            exceptionRepository.findByIdForUpdate(resolution.exceptionId())
                    .orElseThrow(() -> new ConflictException("CAPACITY_UNAVAILABLE",
                            "No hay capacidad disponible para la franja horaria seleccionada."));
            return;
        }
        if (resolution.configurationId() != null) {
            configurationRepository.findByIdForUpdate(resolution.configurationId())
                    .orElseThrow(() -> new ConflictException("CAPACITY_UNAVAILABLE",
                            "No hay capacidad disponible para la franja horaria seleccionada."));
            return;
        }
        throw new ConflictException("CAPACITY_UNAVAILABLE",
                "No hay capacidad disponible para la franja horaria seleccionada.");
    }
}
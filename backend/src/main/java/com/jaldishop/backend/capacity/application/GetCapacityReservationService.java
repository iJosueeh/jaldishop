package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class GetCapacityReservationService {

    private final CapacityReservationRepository repository;

    public GetCapacityReservationService(CapacityReservationRepository repository) {
        this.repository = repository;
    }

    public CapacityReservation execute(UUID id, UUID userId) {
        CapacityReservation reservation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("reserva de capacidad", id));
        if (!reservation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("reserva de capacidad", id);
        }

        reservation.expireIfDue(Instant.now());
        if (reservation.getStatus() == CapacityReservationStatus.EXPIRED) {
            return repository.save(reservation);
        }
        return reservation;
    }
}
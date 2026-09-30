package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ReleaseCapacityReservationService {

    private final CapacityReservationRepository repository;

    public ReleaseCapacityReservationService(CapacityReservationRepository repository) {
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
        if (reservation.getStatus() == CapacityReservationStatus.COMMITTED) {
            throw new ConflictException("RESERVATION_COMMITTED",
                    "La reserva ya está comprometida y no puede liberarse.");
        }
        if (reservation.getStatus() == CapacityReservationStatus.RELEASED) {
            return reservation;
        }

        reservation.release();
        return repository.save(reservation);
    }
}
package com.jaldishop.backend.capacity.application.reservation.service;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Libera de forma transaccional una reserva de capacidad COMMITTED asociada a un pedido cancelado.
 * Idempotente: si la reserva ya no está en COMMITTED, no realiza cambios.
 */
@Service
@Transactional
public class ReleaseCommittedCapacityReservationService {

    private final CapacityReservationRepository repository;

    public ReleaseCommittedCapacityReservationService(CapacityReservationRepository repository) {
        this.repository = repository;
    }

    public CapacityReservation execute(UUID reservationId) {
        if (reservationId == null) {
            return null;
        }

        CapacityReservation reservation = repository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("reserva de capacidad", reservationId));

        boolean released = reservation.releaseFromCancelledOrder();
        if (released) {
            return repository.save(reservation);
        }
        return reservation;
    }
}

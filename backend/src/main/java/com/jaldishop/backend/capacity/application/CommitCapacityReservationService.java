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

/**
 * Valida y compromete una reserva de capacidad para ConfirmPurchase.
 *
 * <p>Regla BE-20: la reserva DEBE estar en PAYMENT_PROTECTED con protección
 * vigente. Una reserva ACTIVE nunca se promueve aquí (eso corresponde al
 * inicio válido del pago, BE-PAYMENTS-01). Si ya está COMMITTED se retorna
 * sin cambios para permitir la resolución idempotente del orquestador.</p>
 */
@Service
@Transactional
public class CommitCapacityReservationService {

    private final CapacityReservationRepository repository;

    public CommitCapacityReservationService(CapacityReservationRepository repository) {
        this.repository = repository;
    }

    public CapacityReservation execute(UUID reservationId) {
        CapacityReservation reservation = repository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("reserva de capacidad", reservationId));

        if (reservation.getStatus() == CapacityReservationStatus.COMMITTED) {
            return reservation;
        }

        reservation.expireIfDue(Instant.now());

        if (reservation.getStatus() != CapacityReservationStatus.PAYMENT_PROTECTED) {
            throw conflictForState(reservation);
        }

        if (reservation.isExpired(Instant.now())) {
            throw new ConflictException(
                    "RESERVATION_PROTECTION_EXPIRED",
                    "La protección de pago de la reserva de capacidad ya venció; la compra no puede confirmarse automáticamente."
            );
        }

        reservation.commit();
        return repository.save(reservation);
    }

    private ConflictException conflictForState(CapacityReservation reservation) {
        return switch (reservation.getStatus()) {
            case ACTIVE -> new ConflictException(
                    "RESERVATION_NOT_PAYMENT_PROTECTED",
                    "La reserva de capacidad no tiene protección de pago activa y no puede comprometerse."
            );
            case EXPIRED -> new ConflictException(
                    "RESERVATION_EXPIRED",
                    "La reserva de capacidad expiró y no puede comprometerse."
            );
            case RELEASED -> new ConflictException(
                    "RESERVATION_RELEASED",
                    "La reserva de capacidad fue liberada y no puede comprometerse."
            );
            case COMMITTED -> new ConflictException(
                    "RESERVATION_ALREADY_COMMITTED",
                    "La reserva de capacidad ya fue comprometida."
            );
            case PAYMENT_PROTECTED -> new ConflictException(
                    "RESERVATION_PROTECTION_EXPIRED",
                    "La protección de pago de la reserva de capacidad ya venció."
            );
        };
    }

}
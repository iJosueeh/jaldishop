package com.jaldishop.backend.capacity.application.reservation.service;

import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseCommittedCapacityReservationService — Pruebas unitarias de liberación de cupo cancelado")
class ReleaseCommittedCapacityReservationServiceTest {

    @Mock
    private CapacityReservationRepository repository;

    private ReleaseCommittedCapacityReservationService service;

    private UUID reservationId;
    private UUID storeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ReleaseCommittedCapacityReservationService(repository);
        reservationId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Liberación exitosa: reserva COMMITTED pasa a RELEASED y se persiste")
    void shouldReleaseCommittedCapacityReservation() {
        CapacityReservation reservation = CapacityReservation.reconstitute(
                reservationId, storeId, userId, LocalDate.now().plusDays(1),
                CapacityPeriod.of(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                CapacityReservationStatus.COMMITTED, Instant.now().plusSeconds(600), null,
                Instant.now(), Instant.now()
        );

        when(repository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(repository.save(any(CapacityReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        CapacityReservation result = service.execute(reservationId);

        assertThat(result.getStatus()).isEqualTo(CapacityReservationStatus.RELEASED);
        verify(repository).save(reservation);
    }

    @Test
    @DisplayName("Idempotencia: si la reserva ya no está en COMMITTED, no se persiste de nuevo")
    void shouldBeIdempotentWhenAlreadyReleased() {
        CapacityReservation reservation = CapacityReservation.reconstitute(
                reservationId, storeId, userId, LocalDate.now().plusDays(1),
                CapacityPeriod.of(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                CapacityReservationStatus.RELEASED, Instant.now().plusSeconds(600), null,
                Instant.now(), Instant.now()
        );

        when(repository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));

        CapacityReservation result = service.execute(reservationId);

        assertThat(result.getStatus()).isEqualTo(CapacityReservationStatus.RELEASED);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotencia: identificador nulo no realiza consultas")
    void shouldDoNothingWhenReservationIdIsNull() {
        CapacityReservation result = service.execute(null);

        assertThat(result).isNull();
        verify(repository, never()).findByIdForUpdate(any());
    }
}

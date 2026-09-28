package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseCapacityReservationServiceTest {

    @Mock
    private CapacityReservationRepository repository;

    private ReleaseCapacityReservationService service;
    private UUID storeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ReleaseCapacityReservationService(repository);
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Liberar reserva activa pasa a RELEASED")
    void releaseActiveReservation() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = fixture(id, userId, CapacityReservationStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));
        when(repository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation result = service.execute(id, userId);

        assertEquals(CapacityReservationStatus.RELEASED, result.getStatus());
        verify(repository).save(reservation);
    }

    @Test
    @DisplayName("Liberar reserva protegida por pago pasa a RELEASED")
    void releaseProtectedReservation() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = fixture(id, userId, CapacityReservationStatus.PAYMENT_PROTECTED);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));
        when(repository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation result = service.execute(id, userId);

        assertEquals(CapacityReservationStatus.RELEASED, result.getStatus());
    }

    @Test
    @DisplayName("Reserva comprometida rechaza con RESERVATION_COMMITTED")
    void committedReservationRejected() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = fixture(id, userId, CapacityReservationStatus.COMMITTED);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.execute(id, userId));

        assertEquals("RESERVATION_COMMITTED", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Reserva inexistente lanza ResourceNotFoundException")
    void missingReservationThrows() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.execute(id, userId));
    }

    @Test
    @DisplayName("Reserva de otro cliente lanza ResourceNotFoundException")
    void foreignReservationThrows() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = fixture(id, UUID.randomUUID(), CapacityReservationStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(id, userId));
    }

    @Test
    @DisplayName("Reserva ya liberada es idempotente (200)")
    void releasedReservationIdempotent() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = fixture(id, userId, CapacityReservationStatus.RELEASED);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        CapacityReservation result = service.execute(id, userId);

        assertEquals(CapacityReservationStatus.RELEASED, result.getStatus());
        verify(repository, never()).save(any());
    }

    private CapacityReservation fixture(UUID id, UUID ownerId, CapacityReservationStatus status) {
        Instant createdAt = Instant.now().minusSeconds(300);
        return CapacityReservation.reconstitute(
                id, storeId, ownerId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0), status,
                createdAt.plusSeconds(600), null, createdAt, createdAt);
    }
}
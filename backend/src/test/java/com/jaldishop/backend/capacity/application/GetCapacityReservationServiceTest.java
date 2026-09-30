package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
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
class GetCapacityReservationServiceTest {

    @Mock
    private CapacityReservationRepository repository;

    private GetCapacityReservationService service;
    private UUID storeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new GetCapacityReservationService(repository);
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Obtener la reserva propia activa")
    void getOwnReservation() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = activeFixture(id, userId, Instant.now().plusSeconds(600));
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        CapacityReservation result = service.execute(id, userId);

        assertEquals(id, result.getId());
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
        CapacityReservation reservation = activeFixture(id, UUID.randomUUID(), Instant.now().plusSeconds(600));
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(id, userId));
    }

    @Test
    @DisplayName("Reserva vencida se persiste como EXPIRADA al consultarla")
    void getExpiredReservationPersistsExpiry() {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = activeFixture(id, userId, Instant.now().minusSeconds(60));
        when(repository.findById(id)).thenReturn(Optional.of(reservation));
        when(repository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation result = service.execute(id, userId);

        assertEquals(CapacityReservationStatus.EXPIRED, result.getStatus());
        verify(repository).save(reservation);
    }

    private CapacityReservation activeFixture(UUID id, UUID ownerId, Instant expiresAt) {
        return CapacityReservation.reconstitute(
                id, storeId, ownerId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.ACTIVE, expiresAt, null,
                expiresAt.minusSeconds(600), expiresAt.minusSeconds(600));
    }
}
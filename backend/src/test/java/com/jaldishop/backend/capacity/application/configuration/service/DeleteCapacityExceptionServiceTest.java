package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.domain.enums.CapacityExceptionStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.repository.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
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
class DeleteCapacityExceptionServiceTest {

    @Mock
    private CapacityExceptionRepository repository;

    private DeleteCapacityExceptionService service;
    private UUID id;
    private UUID storeId;
    private CapacityException exception;

    @BeforeEach
    void setUp() {
        service = new DeleteCapacityExceptionService(repository);
        id = UUID.randomUUID();
        storeId = UUID.randomUUID();
        exception = CapacityException.reconstitute(id, storeId, LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0), 5, "Feriado", CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Bloquea eliminación de excepción si existen reservas activas o confirmadas")
    void blockDeleteWhenReservationsExist() {
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(exception));
        when(repository.hasActiveOrCommittedReservations(storeId, exception.getServiceDate(), exception.getStartTime(), exception.getEndTime())).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.execute(id, storeId));

        assertEquals("CAPACITY_HAS_RESERVATIONS", ex.getCode());
        verify(repository, never()).deleteByIdAndStoreId(any(), any());
    }

    @Test
    @DisplayName("Elimina excepción cuando no tiene reservas")
    void deleteWhenNoReservations() {
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(exception));
        when(repository.hasActiveOrCommittedReservations(storeId, exception.getServiceDate(), exception.getStartTime(), exception.getEndTime())).thenReturn(false);

        service.execute(id, storeId);

        verify(repository).deleteByIdAndStoreId(id, storeId);
    }
}

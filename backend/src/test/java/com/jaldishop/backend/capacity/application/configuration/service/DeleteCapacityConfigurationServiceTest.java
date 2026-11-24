package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.domain.enums.CapacityConfigurationStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCapacityConfigurationServiceTest {

    @Mock
    private CapacityConfigurationRepository repository;

    private DeleteCapacityConfigurationService service;
    private UUID id;
    private UUID storeId;
    private CapacityConfiguration config;

    @BeforeEach
    void setUp() {
        service = new DeleteCapacityConfigurationService(repository);
        id = UUID.randomUUID();
        storeId = UUID.randomUUID();
        config = CapacityConfiguration.reconstitute(id, storeId, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), 10, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Bloquea eliminación si existen reservas activas o confirmadas (409 CAPACITY_HAS_RESERVATIONS)")
    void blockDeleteWhenReservationsExist() {
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(config));
        when(repository.hasActiveOrCommittedReservations(storeId, 1, LocalTime.of(9, 0), LocalTime.of(12, 0))).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.execute(id, storeId));

        assertEquals("CAPACITY_HAS_RESERVATIONS", ex.getCode());
        verify(repository, never()).deleteByIdAndStoreId(any(), any());
    }

    @Test
    @DisplayName("No filtra existencia si la configuración pertenece a otra tienda")
    void throwNotFoundWhenStoreMismatch() {
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(config));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(id, UUID.randomUUID()));
        verify(repository, never()).deleteByIdAndStoreId(any(), any());
    }

    @Test
    @DisplayName("Elimina configuración cuando no tiene reservas")
    void deleteWhenNoReservations() {
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(config));
        when(repository.hasActiveOrCommittedReservations(storeId, 1, LocalTime.of(9, 0), LocalTime.of(12, 0))).thenReturn(false);

        service.execute(id, storeId);

        verify(repository).deleteByIdAndStoreId(id, storeId);
    }
}

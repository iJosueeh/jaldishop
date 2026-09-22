package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCapacityExceptionServiceTest {

    @Mock
    private CapacityExceptionRepository repository;

    private UpdateCapacityExceptionService service;

    private UUID storeId;
    private UUID exceptionId;

    @BeforeEach
    void setUp() {
        service = new UpdateCapacityExceptionService(repository);
        storeId = UUID.randomUUID();
        exceptionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Actualizar excepción exitosamente")
    void updateSuccessfully() {
        CapacityException existing = CapacityException.create(
                storeId, LocalDate.of(2026, 1, 1), null, null, 5, "Original");

        when(repository.findById(exceptionId)).thenReturn(Optional.of(existing));
        when(repository.save(any(CapacityException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCapacityExceptionCommand command = new UpdateCapacityExceptionCommand(
                exceptionId, storeId, LocalDate.of(2026, 6, 20), null, null, 15, "Actualizada");

        CapacityException updated = service.execute(command);

        assertEquals(LocalDate.of(2026, 6, 20), updated.getServiceDate());
        assertEquals(15, updated.getExceptionCapacity());
        assertEquals("Actualizada", updated.getReason());
    }

    @Test
    @DisplayName("Lanzar excepción cuando no se encuentra la excepción")
    void throwExceptionWhenNotFound() {
        when(repository.findById(exceptionId)).thenReturn(Optional.empty());

        UpdateCapacityExceptionCommand command = new UpdateCapacityExceptionCommand(
                exceptionId, storeId, LocalDate.of(2026, 1, 1), null, null, 5, null);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(command));
    }

    @Test
    @DisplayName("Lanzar excepción cuando la excepción no pertenece a la tienda")
    void throwExceptionWhenNotFromStore() {
        UUID otherStoreId = UUID.randomUUID();
        CapacityException existing = CapacityException.create(
                otherStoreId, LocalDate.of(2026, 1, 1), null, null, 5, null);

        when(repository.findById(exceptionId)).thenReturn(Optional.of(existing));

        UpdateCapacityExceptionCommand command = new UpdateCapacityExceptionCommand(
                exceptionId, storeId, LocalDate.of(2026, 1, 1), null, null, 5, null);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(command));
    }
}

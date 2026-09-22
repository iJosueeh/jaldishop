package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
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
class CreateCapacityExceptionServiceTest {

    @Mock
    private CapacityExceptionRepository repository;

    private CreateCapacityExceptionService service;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        service = new CreateCapacityExceptionService(repository);
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crear excepción full day exitosamente")
    void createFullDaySuccessfully() {
        CreateCapacityExceptionCommand command = new CreateCapacityExceptionCommand(
                storeId, LocalDate.of(2026, 12, 25), null, null, 0, "Navidad");

        when(repository.save(any(CapacityException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityException exception = service.execute(command);

        assertNotNull(exception);
        assertEquals(storeId, exception.getStoreId());
        assertEquals(LocalDate.of(2026, 12, 25), exception.getServiceDate());
        assertEquals(0, exception.getExceptionCapacity());
        assertEquals(CapacityExceptionStatus.ACTIVE, exception.getStatus());
        verify(repository).save(any(CapacityException.class));
    }

    @Test
    @DisplayName("Crear excepción con franja horaria exitosamente")
    void createTimeSlotSuccessfully() {
        CreateCapacityExceptionCommand command = new CreateCapacityExceptionCommand(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        when(repository.save(any(CapacityException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityException exception = service.execute(command);

        assertNotNull(exception);
        assertEquals(LocalDate.of(2026, 7, 15), exception.getServiceDate());
        assertEquals(LocalTime.of(10, 0), exception.getStartTime());
        assertEquals(LocalTime.of(14, 0), exception.getEndTime());
        assertEquals(5, exception.getExceptionCapacity());
    }
}

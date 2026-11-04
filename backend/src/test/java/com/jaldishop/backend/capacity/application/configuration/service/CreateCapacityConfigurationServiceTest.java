package com.jaldishop.backend.capacity.application.configuration.service;

import com.jaldishop.backend.capacity.domain.model.*;
import com.jaldishop.backend.capacity.domain.enums.*;
import com.jaldishop.backend.capacity.domain.repository.*;
import com.jaldishop.backend.capacity.application.availability.query.*;
import com.jaldishop.backend.capacity.application.availability.result.*;
import com.jaldishop.backend.capacity.application.availability.service.*;
import com.jaldishop.backend.capacity.application.configuration.command.*;
import com.jaldishop.backend.capacity.application.configuration.service.*;
import com.jaldishop.backend.capacity.application.reservation.command.*;
import com.jaldishop.backend.capacity.application.reservation.service.*;

import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.enums.CapacityConfigurationStatus;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCapacityConfigurationServiceTest {

    @Mock
    private CapacityConfigurationRepository repository;

    private CreateCapacityConfigurationService service;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        service = new CreateCapacityConfigurationService(repository);
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crear configuración full day exitosamente")
    void createFullDaySuccessfully() {
        CreateCapacityConfigurationCommand command = new CreateCapacityConfigurationCommand(
                storeId, 1, null, null, 10);

        when(repository.save(any(CapacityConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityConfiguration config = service.execute(command);

        assertNotNull(config);
        assertEquals(storeId, config.getStoreId());
        assertEquals(1, config.getDayOfWeek());
        assertNull(config.getStartTime());
        assertEquals(10, config.getMaxCapacity());
        assertEquals(CapacityConfigurationStatus.ACTIVE, config.getStatus());
        verify(repository).save(any(CapacityConfiguration.class));
    }

    @Test
    @DisplayName("Crear configuración con franja horaria exitosamente")
    void createTimeSlotSuccessfully() {
        CreateCapacityConfigurationCommand command = new CreateCapacityConfigurationCommand(
                storeId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        when(repository.existsOverlappingByStoreIdAndDayOfWeek(
                storeId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), null))
                .thenReturn(false);
        when(repository.save(any(CapacityConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityConfiguration config = service.execute(command);

        assertNotNull(config);
        assertEquals(3, config.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), config.getStartTime());
        assertEquals(LocalTime.of(17, 0), config.getEndTime());
    }

    @Test
    @DisplayName("Lanzar excepción cuando hay solapamiento de franjas")
    void throwExceptionWhenOverlap() {
        CreateCapacityConfigurationCommand command = new CreateCapacityConfigurationCommand(
                storeId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        when(repository.existsOverlappingByStoreIdAndDayOfWeek(
                storeId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), null))
                .thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> service.execute(command));

        assertEquals("CAPACITY_OVERLAP", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("No verificar solapamiento para configuración full day")
    void noOverlapCheckForFullDay() {
        CreateCapacityConfigurationCommand command = new CreateCapacityConfigurationCommand(
                storeId, 0, null, null, 5);

        when(repository.save(any(CapacityConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityConfiguration config = service.execute(command);

        assertNotNull(config);
        verify(repository, never()).existsOverlappingByStoreIdAndDayOfWeek(any(), anyInt(), any(), any(), any());
    }
}

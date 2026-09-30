package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCapacityAvailabilityServiceTest {

    @Mock
    private GetEffectiveCapacityService getEffectiveCapacityService;
    @Mock
    private CapacityReservationRepository reservationRepository;

    private GetCapacityAvailabilityService service;
    private UUID storeId;
    private final LocalDate tuesday = LocalDate.of(2026, 9, 22);

    @BeforeEach
    void setUp() {
        service = new GetCapacityAvailabilityService(getEffectiveCapacityService, reservationRepository);
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Disponible = efectiva - reservada - comprometida")
    void availableCapacityFormula() {
        EffectiveCapacityQuery query = query();
        when(getEffectiveCapacityService.resolve(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResolution(20, EffectiveCapacitySource.BASE, UUID.randomUUID(), null));
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), any(), any(), any())).thenReturn(4);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), any(), any())).thenReturn(2);

        CapacityAvailabilityResult result = service.execute(query);

        assertEquals(20, result.effectiveCapacity());
        assertEquals(4, result.reservedCapacity());
        assertEquals(2, result.committedCapacity());
        assertEquals(14, result.availableCapacity());
    }

    @Test
    @DisplayName("Disponibilidad no negativa cuando se satura")
    void availableCapacityClampsToZero() {
        EffectiveCapacityQuery query = query();
        when(getEffectiveCapacityService.resolve(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResolution(5, EffectiveCapacitySource.EXCEPTION, null, UUID.randomUUID()));
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), any(), any(), any())).thenReturn(5);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), any(), any())).thenReturn(2);

        CapacityAvailabilityResult result = service.execute(query);

        assertEquals(0, result.availableCapacity());
    }

    @Test
    @DisplayName("Sin cobertura de capacidad devuelve disponible 0")
    void noCoverageReturnsZero() {
        EffectiveCapacityQuery query = query();
        when(getEffectiveCapacityService.resolve(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResolution(0, EffectiveCapacitySource.NONE, null, null));
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), any(), any(), any())).thenReturn(0);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), any(), any())).thenReturn(0);

        CapacityAvailabilityResult result = service.execute(query);

        assertEquals(0, result.effectiveCapacity());
        assertEquals(0, result.availableCapacity());
    }

    private EffectiveCapacityQuery query() {
        return new EffectiveCapacityQuery(storeId, tuesday, LocalTime.of(10, 0), LocalTime.of(12, 0));
    }
}
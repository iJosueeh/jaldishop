package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCapacityReservationServiceTest {

    @Mock
    private GetEffectiveCapacityService getEffectiveCapacityService;
    @Mock
    private CapacityConfigurationRepository configurationRepository;
    @Mock
    private CapacityExceptionRepository exceptionRepository;
    @Mock
    private CapacityReservationRepository reservationRepository;

    private CreateCapacityReservationService service;
    private UUID storeId;
    private UUID userId;
    private final LocalDate tuesday = LocalDate.of(2026, 9, 22);
    private final LocalTime start = LocalTime.of(10, 0);
    private final LocalTime end = LocalTime.of(12, 0);

    @BeforeEach
    void setUp() {
        service = new CreateCapacityReservationService(
                getEffectiveCapacityService, configurationRepository,
                exceptionRepository, reservationRepository);
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crear reserva exitosamente desde excepción de capacidad")
    void createFromExceptionSuccessfully() {
        UUID exceptionId = UUID.randomUUID();
        EffectiveCapacityQuery query = query();
        stubResolution(new EffectiveCapacityResolution(5, EffectiveCapacitySource.EXCEPTION, null, exceptionId));
        when(exceptionRepository.findByIdForUpdate(exceptionId)).thenReturn(Optional.of(
                com.jaldishop.backend.capacity.domain.CapacityException.reconstitute(
                        exceptionId, storeId, tuesday, null, null, 5, null,
                        com.jaldishop.backend.capacity.domain.CapacityExceptionStatus.ACTIVE,
                        java.time.Instant.now(), java.time.Instant.now())));
        when(reservationRepository.existsReservedByUserAndWindow(eq(userId), eq(storeId), eq(tuesday), eq(start), eq(end), any()))
                .thenReturn(false);
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), eq(start), eq(end), any())).thenReturn(1);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), eq(start), eq(end))).thenReturn(1);
        when(reservationRepository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation reservation = service.execute(newCommand());

        verify(getEffectiveCapacityService).resolve(query);
        verify(exceptionRepository).findByIdForUpdate(exceptionId);
        assertNotNull(reservation);
        assertEquals(CapacityReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(userId, reservation.getUserId());
        verify(reservationRepository).save(any(CapacityReservation.class));
    }

    @Test
    @DisplayName("Crear reserva exitosamente desde capacidad base")
    void createFromBaseSuccessfully() {
        UUID configId = UUID.randomUUID();
        stubResolution(new EffectiveCapacityResolution(20, EffectiveCapacitySource.BASE, configId, null));
        when(configurationRepository.findByIdForUpdate(configId)).thenReturn(Optional.of(
                com.jaldishop.backend.capacity.domain.CapacityConfiguration.reconstitute(
                        configId, storeId, 2, LocalTime.of(9, 0), LocalTime.of(17, 0), 20,
                        com.jaldishop.backend.capacity.domain.CapacityConfigurationStatus.ACTIVE,
                        java.time.Instant.now(), java.time.Instant.now())));
        when(reservationRepository.existsReservedByUserAndWindow(eq(userId), eq(storeId), eq(tuesday), eq(start), eq(end), any()))
                .thenReturn(false);
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), eq(start), eq(end), any())).thenReturn(0);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), eq(start), eq(end))).thenReturn(0);
        when(reservationRepository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation reservation = service.execute(newCommand());

        verify(configurationRepository).findByIdForUpdate(configId);
        assertNotNull(reservation);
        assertEquals(CapacityReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    @DisplayName("Capacidad efectiva 0 rechaza con CAPACITY_UNAVAILABLE")
    void noCapacityRejected() {
        stubResolution(new EffectiveCapacityResolution(0, EffectiveCapacitySource.NONE, null, null));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.execute(newCommand()));

        assertEquals("CAPACITY_UNAVAILABLE", exception.getCode());
        verify(exceptionRepository, never()).findByIdForUpdate(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Último cupo disponible permite reservar")
    void lastSlotAvailable() {
        UUID exceptionId = UUID.randomUUID();
        stubResolution(new EffectiveCapacityResolution(3, EffectiveCapacitySource.EXCEPTION, null, exceptionId));
        when(exceptionRepository.findByIdForUpdate(exceptionId)).thenReturn(Optional.of(
                com.jaldishop.backend.capacity.domain.CapacityException.reconstitute(
                        exceptionId, storeId, tuesday, null, null, 3, null,
                        com.jaldishop.backend.capacity.domain.CapacityExceptionStatus.ACTIVE,
                        java.time.Instant.now(), java.time.Instant.now())));
        when(reservationRepository.existsReservedByUserAndWindow(eq(userId), eq(storeId), eq(tuesday), eq(start), eq(end), any()))
                .thenReturn(false);
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), eq(start), eq(end), any())).thenReturn(2);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), eq(start), eq(end))).thenReturn(0);
        when(reservationRepository.save(any(CapacityReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CapacityReservation reservation = service.execute(newCommand());

        assertNotNull(reservation);
        assertEquals(CapacityReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    @DisplayName("Franja saturada rechaza con CAPACITY_EXHAUSTED")
    void exhaustedSlotRejected() {
        UUID exceptionId = UUID.randomUUID();
        stubResolution(new EffectiveCapacityResolution(3, EffectiveCapacitySource.EXCEPTION, null, exceptionId));
        when(exceptionRepository.findByIdForUpdate(exceptionId)).thenReturn(Optional.of(
                com.jaldishop.backend.capacity.domain.CapacityException.reconstitute(
                        exceptionId, storeId, tuesday, null, null, 3, null,
                        com.jaldishop.backend.capacity.domain.CapacityExceptionStatus.ACTIVE,
                        java.time.Instant.now(), java.time.Instant.now())));
        when(reservationRepository.existsReservedByUserAndWindow(eq(userId), eq(storeId), eq(tuesday), eq(start), eq(end), any()))
                .thenReturn(false);
        when(reservationRepository.countReserved(eq(storeId), eq(tuesday), eq(start), eq(end), any())).thenReturn(3);
        when(reservationRepository.countCommitted(eq(storeId), eq(tuesday), eq(start), eq(end))).thenReturn(1);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.execute(newCommand()));

        assertEquals("CAPACITY_EXHAUSTED", exception.getCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cliente con reserva activa en la franja rechaza con ALREADY_RESERVED")
    void duplicateActiveReservationRejected() {
        UUID exceptionId = UUID.randomUUID();
        stubResolution(new EffectiveCapacityResolution(10, EffectiveCapacitySource.EXCEPTION, null, exceptionId));
        when(exceptionRepository.findByIdForUpdate(exceptionId)).thenReturn(Optional.of(
                com.jaldishop.backend.capacity.domain.CapacityException.reconstitute(
                        exceptionId, storeId, tuesday, null, null, 10, null,
                        com.jaldishop.backend.capacity.domain.CapacityExceptionStatus.ACTIVE,
                        java.time.Instant.now(), java.time.Instant.now())));
        when(reservationRepository.existsReservedByUserAndWindow(eq(userId), eq(storeId), eq(tuesday), eq(start), eq(end), any()))
                .thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.execute(newCommand()));

        assertEquals("ALREADY_RESERVED", exception.getCode());
        verify(reservationRepository, never()).countReserved(any(), any(), any(), any(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ancla de excepción inexistente rechaza con CAPACITY_UNAVAILABLE")
    void missingAnchorRejected() {
        UUID exceptionId = UUID.randomUUID();
        stubResolution(new EffectiveCapacityResolution(5, EffectiveCapacitySource.EXCEPTION, null, exceptionId));
        when(exceptionRepository.findByIdForUpdate(exceptionId)).thenReturn(Optional.empty());

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.execute(newCommand()));

        assertEquals("CAPACITY_UNAVAILABLE", exception.getCode());
        verify(reservationRepository, never()).save(any());
    }

    private CreateCapacityReservationCommand newCommand() {
        return new CreateCapacityReservationCommand(storeId, userId, tuesday, start, end);
    }

    private EffectiveCapacityQuery query() {
        return new EffectiveCapacityQuery(storeId, tuesday, start, end);
    }

    private void stubResolution(EffectiveCapacityResolution resolution) {
        when(getEffectiveCapacityService.resolve(any(EffectiveCapacityQuery.class))).thenReturn(resolution);
    }
}
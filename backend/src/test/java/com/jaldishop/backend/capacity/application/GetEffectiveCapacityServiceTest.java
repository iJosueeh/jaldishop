package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEffectiveCapacityServiceTest {

    @Mock
    private CapacityConfigurationRepository configurationRepository;
    @Mock
    private CapacityExceptionRepository exceptionRepository;

    private GetEffectiveCapacityService service;
    private UUID storeId;
    private final LocalDate tuesday = LocalDate.of(2026, 9, 22);

    @BeforeEach
    void setUp() {
        service = new GetEffectiveCapacityService(configurationRepository, exceptionRepository);
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Aplica capacidad base cuando existe configuración de franja para el día")
    void appliesBaseSlot() {
        stubConfigs(slotConfig(2, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(20, result.capacity());
        assertEquals(EffectiveCapacitySource.BASE, result.source());
    }

    @Test
    @DisplayName("Aplica capacidad base full day")
    void appliesBaseFullDay() {
        stubConfigs(fullDayConfig(2, 10));
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(10, result.capacity());
        assertEquals(EffectiveCapacitySource.BASE, result.source());
    }

    @Test
    @DisplayName("Excepción reemplaza la capacidad base")
    void exceptionOverridesBase() {
        stubExceptions(fullDayException(4));

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(4, result.capacity());
        assertEquals(EffectiveCapacitySource.EXCEPTION, result.source());
    }

    @Test
    @DisplayName("Excepción con capacidad 0 devuelve capacidad efectiva 0")
    void exceptionCapacityZero() {
        stubExceptions(fullDayException(0));

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(0, result.capacity());
        assertEquals(EffectiveCapacitySource.EXCEPTION, result.source());
    }

    @Test
    @DisplayName("Sin configuración ni excepción la tienda está cerrada (0)")
    void noConfigNoException() {
        stubConfigs();
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(0, result.capacity());
        assertEquals(EffectiveCapacitySource.NONE, result.source());
    }

    @Test
    @DisplayName("Franja fuera de horario devuelve 0")
    void franjaFueraDeHorario() {
        stubConfigs(slotConfig(2, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(18, 0), LocalTime.of(19, 0)));

        assertEquals(0, result.capacity());
        assertEquals(EffectiveCapacitySource.NONE, result.source());
    }

    @Test
    @DisplayName("Excepción full day tiene prioridad sobre excepción de franja")
    void fullDayExceptionBeatsSlotException() {
        stubExceptions(fullDayException(8), slotException(LocalTime.of(12, 0), LocalTime.of(14, 0), 3));

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(12, 30), LocalTime.of(13, 30)));

        assertEquals(8, result.capacity());
        assertEquals(EffectiveCapacitySource.EXCEPTION, result.source());
    }

    @Test
    @DisplayName("Excepción de franja reemplaza la capacidad base")
    void slotExceptionBeatsBase() {
        stubExceptions(slotException(LocalTime.of(12, 0), LocalTime.of(14, 0), 6));

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(12, 30), LocalTime.of(13, 30)));

        assertEquals(6, result.capacity());
        assertEquals(EffectiveCapacitySource.EXCEPTION, result.source());
    }

    @Test
    @DisplayName("Configuración INACTIVE no aplica")
    void inactiveConfigIgnored() {
        CapacityConfiguration config = fullDayConfig(2, 10);
        config.deactivate();
        stubConfigs(config);
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(0, result.capacity());
        assertEquals(EffectiveCapacitySource.NONE, result.source());
    }

    @Test
    @DisplayName("Excepción INACTIVE es ignorada y aplica la base")
    void inactiveExceptionIgnored() {
        CapacityException exception = fullDayException(4);
        exception.deactivate();
        stubConfigs(fullDayConfig(2, 10));
        stubExceptions(exception);

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(10, result.capacity());
        assertEquals(EffectiveCapacitySource.BASE, result.source());
    }

    @Test
    @DisplayName("Día de la semana no configurado devuelve 0")
    void differentDayConfigNotApplied() {
        stubConfigs(fullDayConfig(3, 10));
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(0, result.capacity());
        assertEquals(EffectiveCapacitySource.NONE, result.source());
    }

    @Test
    @DisplayName("Configuración de franja tiene prioridad sobre full day dentro de la base")
    void baseSlotBeatsBaseFullDay() {
        stubConfigs(fullDayConfig(2, 10), slotConfig(2, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        stubNoExceptions();

        EffectiveCapacityResult result = service.execute(query(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(20, result.capacity());
        assertEquals(EffectiveCapacitySource.BASE, result.source());
    }

    @Test
    @DisplayName("Rango horario inválido (start >= end) lanza BusinessRuleException")
    void invalidTimeRange() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> service.execute(query(LocalTime.of(12, 0), LocalTime.of(10, 0))));

        assertEquals("INVALID_CAPACITY_QUERY", exception.getCode());
    }

    @Test
    @DisplayName("Franja incompleta lanza BusinessRuleException")
    void missingFranja() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> service.execute(new EffectiveCapacityQuery(storeId, tuesday, null, null)));

        assertEquals("INVALID_CAPACITY_QUERY", exception.getCode());
    }

    @Test
    @DisplayName("Fecha faltante lanza BusinessRuleException")
    void missingDate() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> service.execute(new EffectiveCapacityQuery(
                        storeId, null, LocalTime.of(10, 0), LocalTime.of(12, 0))));

        assertEquals("INVALID_CAPACITY_QUERY", exception.getCode());
    }

    private EffectiveCapacityQuery query(LocalTime start, LocalTime end) {
        return new EffectiveCapacityQuery(storeId, tuesday, start, end);
    }

    private CapacityConfiguration slotConfig(int dayOfWeek, LocalTime start, LocalTime end, int capacity) {
        return CapacityConfiguration.create(storeId, dayOfWeek, start, end, capacity);
    }

    private CapacityConfiguration fullDayConfig(int dayOfWeek, int capacity) {
        return CapacityConfiguration.create(storeId, dayOfWeek, null, null, capacity);
    }

    private CapacityException slotException(LocalTime start, LocalTime end, int capacity) {
        return CapacityException.create(storeId, tuesday, start, end, capacity, null);
    }

    private CapacityException fullDayException(int capacity) {
        return CapacityException.create(storeId, tuesday, null, null, capacity, null);
    }

    private void stubNoExceptions() {
        stubExceptions();
    }

    private void stubExceptions(CapacityException... exceptions) {
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, tuesday))
                .thenReturn(List.of(exceptions));
    }

    private void stubConfigs(CapacityConfiguration... configs) {
        when(configurationRepository.findByStoreId(storeId))
                .thenReturn(Arrays.asList(configs));
    }
}
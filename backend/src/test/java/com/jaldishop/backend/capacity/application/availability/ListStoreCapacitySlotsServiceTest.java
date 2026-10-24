package com.jaldishop.backend.capacity.application.availability;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Servicio compuesto con GetEffectiveCapacityService y GetCapacityAvailabilityService
 * REALES (solo repos mockeados) para ejercitar de verdad las reglas de excepciones.
 */
@ExtendWith(MockitoExtension.class)
class ListStoreCapacitySlotsServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    @Mock
    private CapacityConfigurationRepository configurationRepository;
    @Mock
    private CapacityExceptionRepository exceptionRepository;
    @Mock
    private CapacityReservationRepository reservationRepository;

    private ListStoreCapacitySlotsService service;
    private UUID storeId;
    /** Fecha futura para que la validación de "sin fechas pasadas" no dispare. */
    private LocalDate futureDate;
    private int futureDayOfWeek;

    @BeforeEach
    void setUp() {
        GetEffectiveCapacityService effectiveService =
                new GetEffectiveCapacityService(configurationRepository, exceptionRepository);
        GetCapacityAvailabilityService availabilityService =
                new GetCapacityAvailabilityService(effectiveService, reservationRepository);
        service = new ListStoreCapacitySlotsService(
                configurationRepository, exceptionRepository, availabilityService);
        storeId = UUID.randomUUID();
        futureDate = LocalDate.now(BUSINESS_ZONE).plusDays(7);
        futureDayOfWeek = futureDate.getDayOfWeek().getValue() % 7;
    }

    @Test
    @DisplayName("Lista las configuraciones del día ordenadas por hora de inicio")
    void listsDayConfigurationsSorted() {
        stubConfigs(
                slotConfig(futureDayOfWeek, LocalTime.of(14, 0), LocalTime.of(18, 0), 8),
                slotConfig(futureDayOfWeek, LocalTime.of(9, 0), LocalTime.of(12, 0), 10));
        stubExceptions();
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).startTime());
        assertEquals(LocalTime.of(12, 0), slots.get(0).endTime());
        assertEquals(LocalTime.of(14, 0), slots.get(1).startTime());
        assertEquals(LocalTime.of(18, 0), slots.get(1).endTime());
        assertEquals(10, slots.get(0).availability().effectiveCapacity());
        assertEquals(8, slots.get(1).availability().effectiveCapacity());
    }

    @Test
    @DisplayName("Configuraciones de otro día de la semana se ignoran")
    void configsOfOtherDayIgnored() {
        stubConfigs(slotConfig((futureDayOfWeek + 1) % 7, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        stubExceptions();

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Configuraciones INACTIVE se ignoran")
    void inactiveConfigsIgnored() {
        CapacityConfiguration config = slotConfig(futureDayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);
        config.deactivate();
        stubConfigs(config);
        stubExceptions();

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Excepción de día completo aplica como reemplazo y no agrega franja")
    void fullDayExceptionAppliesAsReplacement() {
        stubConfigs(slotConfig(futureDayOfWeek, LocalTime.of(14, 0), LocalTime.of(18, 0), 8));
        stubExceptions(fullDayException(0));
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(1, slots.size());
        // RN-CAP-05: la excepción de día completo reemplaza la base en toda franja candidata.
        assertEquals(0, slots.get(0).availability().effectiveCapacity());
        assertEquals(0, slots.get(0).availability().availableCapacity());
    }

    @Test
    @DisplayName("Excepción de día completo sin configuración base no inventa franja")
    void fullDayExceptionWithoutBaseCreatesNoSlot() {
        stubConfigs();
        stubExceptions(fullDayException(5));

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Excepción de franja se lista como ventana propia con su propio pool")
    void slotExceptionListedAsOwnWindow() {
        stubConfigs(slotConfig(futureDayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        stubExceptions(slotException(LocalTime.of(12, 0), LocalTime.of(14, 0), 6));
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(2, slots.size());
        // La ventana base mantiene la capacidad base (la excepción no la contiene).
        assertEquals(LocalTime.of(9, 0), slots.get(0).startTime());
        assertEquals(20, slots.get(0).availability().effectiveCapacity());
        // La ventana de la excepción conserva su capacidad sustituida (RN-CAP-05).
        assertEquals(LocalTime.of(12, 0), slots.get(1).startTime());
        assertEquals(6, slots.get(1).availability().effectiveCapacity());
    }

    @Test
    @DisplayName("Ventana idéntica entre configuración y excepción se deduplica")
    void identicalWindowDeduplicated() {
        stubConfigs(slotConfig(futureDayOfWeek, LocalTime.of(14, 0), LocalTime.of(18, 0), 8));
        stubExceptions(slotException(LocalTime.of(14, 0), LocalTime.of(18, 0), 3));
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(1, slots.size());
        // Al ser la misma ventana, la excepción sustituye a la base (RN-CAP-05).
        assertEquals(3, slots.get(0).availability().effectiveCapacity());
    }

    @Test
    @DisplayName("Excepción INACTIVE de franja no se lista")
    void inactiveSlotExceptionNotListed() {
        stubConfigs(slotConfig(futureDayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));
        CapacityException exception = slotException(LocalTime.of(12, 0), LocalTime.of(14, 0), 6);
        exception.deactivate();
        stubExceptions(exception);
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).startTime());
    }

    @Test
    @DisplayName("Configuración sin horas se materializa como 00:00:00-23:59:59")
    void fullDayConfigMaterializedAsSingleWindow() {
        stubConfigs(fullDayConfig(futureDayOfWeek, 10));
        stubExceptions();
        stubCounts(0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(0, 0, 0), slots.get(0).startTime());
        assertEquals(LocalTime.of(23, 59, 59), slots.get(0).endTime());
        assertEquals(10, slots.get(0).availability().effectiveCapacity());
    }

    @Test
    @DisplayName("Domingo usa la convención dayOfWeek = 0 (valor ISO 7 % 7)")
    void sundayUsesZeroBasedConvention() {
        LocalDate sunday = nextSunday();
        stubConfigs(slotConfig(0, LocalTime.of(10, 0), LocalTime.of(13, 0), 5));
        stubExceptionsFor(sunday);
        stubCountsFor(sunday, 0, 0);

        List<CapacitySlotAvailability> slots = service.execute(storeId, sunday);

        assertEquals(1, slots.size());
        assertEquals(5, slots.get(0).availability().effectiveCapacity());
    }

    @Test
    @DisplayName("occupied = reserved + committed y disponible respeta RN-CAP-07")
    void countsFeedAvailabilityFormula() {
        stubConfigs(slotConfig(futureDayOfWeek, LocalTime.of(14, 0), LocalTime.of(18, 0), 8));
        stubExceptions();
        stubCounts(4, 2);

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertEquals(1, slots.size());
        CapacityAvailabilityResult result = slots.get(0).availability();
        assertEquals(8, result.effectiveCapacity());
        assertEquals(4, result.reservedCapacity());
        assertEquals(2, result.committedCapacity());
        // disponible = max(0, efectiva - reservadas - comprometidas)
        assertEquals(2, result.availableCapacity());
    }

    @Test
    @DisplayName("Sin configuraciones ni excepciones devuelve lista vacía")
    void noCandidatesReturnsEmptyList() {
        stubConfigs();
        stubExceptions();

        List<CapacitySlotAvailability> slots = service.execute(storeId, futureDate);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Fecha pasada lanza INVALID_CAPACITY_QUERY (422)")
    void pastDateRejected() {
        LocalDate pastDate = LocalDate.now(BUSINESS_ZONE).minusDays(1);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> service.execute(storeId, pastDate));

        assertEquals("INVALID_CAPACITY_QUERY", exception.getCode());
    }

    @Test
    @DisplayName("Fecha y storeId faltantes lanzan INVALID_CAPACITY_QUERY (422)")
    void missingArgumentsRejected() {
        BusinessRuleException noStore = assertThrows(BusinessRuleException.class,
                () -> service.execute(null, futureDate));
        BusinessRuleException noDate = assertThrows(BusinessRuleException.class,
                () -> service.execute(storeId, null));

        assertEquals("INVALID_CAPACITY_QUERY", noStore.getCode());
        assertEquals("INVALID_CAPACITY_QUERY", noDate.getCode());
    }

    private LocalDate nextSunday() {
        LocalDate date = futureDate;
        while (date.getDayOfWeek().getValue() != 7) {
            date = date.plusDays(1);
        }
        return date;
    }

    private CapacityConfiguration slotConfig(int dayOfWeek, LocalTime start, LocalTime end, int capacity) {
        return CapacityConfiguration.create(storeId, dayOfWeek, start, end, capacity);
    }

    private CapacityConfiguration fullDayConfig(int dayOfWeek, int capacity) {
        return CapacityConfiguration.create(storeId, dayOfWeek, null, null, capacity);
    }

    private CapacityException slotException(LocalTime start, LocalTime end, int capacity) {
        return CapacityException.create(storeId, futureDate, start, end, capacity, null);
    }

    private CapacityException fullDayException(int capacity) {
        return CapacityException.create(storeId, futureDate, null, null, capacity, null);
    }

    private void stubConfigs(CapacityConfiguration... configs) {
        when(configurationRepository.findByStoreId(storeId)).thenReturn(Arrays.asList(configs));
    }

    private void stubExceptions(CapacityException... exceptions) {
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, futureDate))
                .thenReturn(List.of(exceptions));
    }

    private void stubExceptionsFor(LocalDate date, CapacityException... exceptions) {
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, date))
                .thenReturn(List.of(exceptions));
    }

    private void stubCounts(int reserved, int committed) {
        stubCountsFor(futureDate, reserved, committed);
    }

    private void stubCountsFor(LocalDate date, int reserved, int committed) {
        when(reservationRepository.countReserved(eq(storeId), eq(date), any(LocalTime.class),
                any(LocalTime.class), any(Instant.class))).thenReturn(reserved);
        when(reservationRepository.countCommitted(eq(storeId), eq(date), any(LocalTime.class),
                any(LocalTime.class))).thenReturn(committed);
    }
}

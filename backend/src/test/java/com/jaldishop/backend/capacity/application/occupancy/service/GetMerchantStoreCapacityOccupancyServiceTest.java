package com.jaldishop.backend.capacity.application.occupancy.service;

import com.jaldishop.backend.capacity.application.availability.query.EffectiveCapacityQuery;
import com.jaldishop.backend.capacity.application.availability.result.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.availability.service.GetCapacityAvailabilityService;
import com.jaldishop.backend.capacity.domain.enums.CapacityConfigurationStatus;
import com.jaldishop.backend.capacity.domain.enums.CapacityExceptionStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.repository.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.capacity.web.dto.MerchantStoreCapacityOccupancyResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMerchantStoreCapacityOccupancyService — Pruebas unitarias de casos de negocio")
class GetMerchantStoreCapacityOccupancyServiceTest {

    @Mock
    private CapacityConfigurationRepository configurationRepository;

    @Mock
    private CapacityExceptionRepository exceptionRepository;

    @Mock
    private CapacityReservationRepository reservationRepository;

    @Mock
    private GetCapacityAvailabilityService availabilityService;

    private GetMerchantStoreCapacityOccupancyService service;

    private final UUID storeId = UUID.randomUUID();
    private final LocalDate testDate = LocalDate.of(2026, 11, 16); // Lunes (dayOfWeek = 1)

    @BeforeEach
    void setUp() {
        service = new GetMerchantStoreCapacityOccupancyService(
                configurationRepository,
                exceptionRepository,
                reservationRepository,
                availabilityService
        );
    }

    @Test
    @DisplayName("C1: Franjas disjuntas estándar con ocupación normal")
    void testC1_DisjointSlotsStandard() {
        CapacityConfiguration config1 = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                10, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());
        CapacityConfiguration config2 = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, LocalTime.of(14, 0), LocalTime.of(18, 0),
                15, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        when(configurationRepository.findByStoreId(storeId)).thenReturn(List.of(config1, config2));
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(Collections.emptyList());
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(3);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(7);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .thenReturn(new CapacityAvailabilityResult(10, 5, 2, 3));
        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(14, 0), LocalTime.of(18, 0)))))
                .thenReturn(new CapacityAvailabilityResult(15, 10, 1, 4));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.storeId()).isEqualTo(storeId);
        assertThat(response.date()).isEqualTo(testDate);
        assertThat(response.isClosed()).isFalse();
        assertThat(response.effectiveCapacity()).isEqualTo(25);
        assertThat(response.occupiedCapacity()).isEqualTo(10);
        assertThat(response.availableCapacity()).isEqualTo(15);
        assertThat(response.reservedCapacity()).isEqualTo(3);
        assertThat(response.committedCapacity()).isEqualTo(7);
        assertThat(response.occupancyPercentage()).isEqualTo(40);
        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().get(0).occupancyPercentage()).isEqualTo(50);
        assertThat(response.slots().get(1).occupancyPercentage()).isEqualTo(33);
    }

    @Test
    @DisplayName("C2: Excepción reduce capacidad bajo reservas existentes (sobrecapacidad)")
    void testC2_ExceptionReducesCapacityBelowCommitted() {
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                10, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        CapacityException exception = CapacityException.reconstitute(
                UUID.randomUUID(), storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0),
                3, "Falta personal", CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());

        when(configurationRepository.findByStoreId(storeId)).thenReturn(List.of(config));
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(List.of(exception));
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(0);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(7);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .thenReturn(new CapacityAvailabilityResult(3, 0, 0, 7));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.isClosed()).isFalse();
        assertThat(response.effectiveCapacity()).isEqualTo(3);
        assertThat(response.occupiedCapacity()).isEqualTo(7);
        assertThat(response.availableCapacity()).isEqualTo(0); // Nunca negativo
        assertThat(response.occupancyPercentage()).isEqualTo(233); // Real > 100%
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().getFirst().availableCapacity()).isEqualTo(0);
        assertThat(response.slots().getFirst().occupancyPercentage()).isEqualTo(233);
    }

    @Test
    @DisplayName("C3: Excepción de jornada completa cierra la tienda con pedidos previos")
    void testC3_FullDayExceptionClosesStoreWithExistingOrders() {
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                10, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        CapacityException fullDayExc = CapacityException.reconstitute(
                UUID.randomUUID(), storeId, testDate, null, null,
                0, "Feriado", CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());

        when(configurationRepository.findByStoreId(storeId)).thenReturn(List.of(config));
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(List.of(fullDayExc));
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(0);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(4);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .thenReturn(new CapacityAvailabilityResult(0, 0, 0, 4));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.isClosed()).isTrue();
        assertThat(response.effectiveCapacity()).isEqualTo(0);
        assertThat(response.occupiedCapacity()).isEqualTo(4); // Ocupación no se oculta
        assertThat(response.availableCapacity()).isEqualTo(0);
        assertThat(response.occupancyPercentage()).isEqualTo(0);
    }

    @Test
    @DisplayName("C4: Configuración desactivada con reservas vivas (ventana huérfana descubierta)")
    void testC4_DeactivatedConfigWithLivingReservations() {
        // No hay configuraciones activas ni excepciones
        when(configurationRepository.findByStoreId(storeId)).thenReturn(Collections.emptyList());
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(Collections.emptyList());

        // La base de datos tiene una ventana con reservas vivas
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(List.of(CapacityPeriod.of(LocalTime.of(9, 0), LocalTime.of(13, 0))));

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(1);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(4);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .thenReturn(new CapacityAvailabilityResult(0, 0, 1, 4));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.isClosed()).isTrue();
        assertThat(response.effectiveCapacity()).isEqualTo(0);
        assertThat(response.occupiedCapacity()).isEqualTo(5);
        assertThat(response.availableCapacity()).isEqualTo(0);
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.slots().getFirst().endTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(response.slots().getFirst().occupiedCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("C5: Configuración de jornada completa sin franjas")
    void testC5_FullDayConfiguration() {
        CapacityConfiguration fullDayConfig = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, null, null,
                20, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        when(configurationRepository.findByStoreId(storeId)).thenReturn(List.of(fullDayConfig));
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(Collections.emptyList());
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(2);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(6);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(0, 0), LocalTime.of(23, 59, 59)))))
                .thenReturn(new CapacityAvailabilityResult(20, 12, 2, 6));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.isClosed()).isFalse();
        assertThat(response.effectiveCapacity()).isEqualTo(20);
        assertThat(response.occupiedCapacity()).isEqualTo(8);
        assertThat(response.availableCapacity()).isEqualTo(12);
        assertThat(response.occupancyPercentage()).isEqualTo(40);
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().getFirst().startTime()).isEqualTo(LocalTime.of(0, 0));
        assertThat(response.slots().getFirst().endTime()).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    @DisplayName("C6: Solapamiento entre configuración y excepción (ambas ventanas se conservan)")
    void testC6_OverlappingConfigAndExceptionBothPreserved() {
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                UUID.randomUUID(), storeId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                10, CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        CapacityException exception = CapacityException.reconstitute(
                UUID.randomUUID(), storeId, testDate, LocalTime.of(11, 0), LocalTime.of(15, 0),
                8, "Turno especial", CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());

        when(configurationRepository.findByStoreId(storeId)).thenReturn(List.of(config));
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(List.of(exception));
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(1);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(4);

        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .thenReturn(new CapacityAvailabilityResult(10, 8, 1, 1));
        when(availabilityService.execute(eq(new EffectiveCapacityQuery(storeId, testDate, LocalTime.of(11, 0), LocalTime.of(15, 0)))))
                .thenReturn(new CapacityAvailabilityResult(8, 5, 0, 3));

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.slots().get(0).endTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(response.slots().get(1).startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.slots().get(1).endTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(response.occupiedCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("C7: Tienda sin configuración ni excepciones (retorna ceros y cerrada)")
    void testC7_NoConfigAndNoExceptions() {
        when(configurationRepository.findByStoreId(storeId)).thenReturn(Collections.emptyList());
        when(exceptionRepository.findByStoreIdAndServiceDate(storeId, testDate)).thenReturn(Collections.emptyList());
        when(reservationRepository.findOccupiedWindows(eq(storeId), eq(testDate), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.countDailyReserved(eq(storeId), eq(testDate), any(Instant.class))).thenReturn(0);
        when(reservationRepository.countDailyCommitted(storeId, testDate)).thenReturn(0);

        MerchantStoreCapacityOccupancyResponse response = service.execute(storeId, testDate);

        assertThat(response.isClosed()).isTrue();
        assertThat(response.effectiveCapacity()).isEqualTo(0);
        assertThat(response.occupiedCapacity()).isEqualTo(0);
        assertThat(response.availableCapacity()).isEqualTo(0);
        assertThat(response.occupancyPercentage()).isEqualTo(0);
        assertThat(response.slots()).isEmpty();
    }

    @Test
    @DisplayName("Validación: storeId nulo lanza BusinessRuleException")
    void testValidation_NullStoreIdThrows() {
        assertThatThrownBy(() -> service.execute(null, testDate))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("identificador de la tienda es obligatorio");
    }
}

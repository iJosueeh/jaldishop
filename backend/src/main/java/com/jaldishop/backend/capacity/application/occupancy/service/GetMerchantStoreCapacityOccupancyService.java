package com.jaldishop.backend.capacity.application.occupancy.service;

import com.jaldishop.backend.capacity.application.availability.query.EffectiveCapacityQuery;
import com.jaldishop.backend.capacity.application.availability.result.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.availability.service.GetCapacityAvailabilityService;
import com.jaldishop.backend.capacity.domain.enums.CapacityExceptionStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.repository.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.repository.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.capacity.web.dto.MerchantCapacitySlotOccupancyResponse;
import com.jaldishop.backend.capacity.web.dto.MerchantStoreCapacityOccupancyResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class GetMerchantStoreCapacityOccupancyService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private static final LocalTime FULL_DAY_START = LocalTime.of(0, 0, 0);
    private static final LocalTime FULL_DAY_END = LocalTime.of(23, 59, 59);

    private final CapacityConfigurationRepository configurationRepository;
    private final CapacityExceptionRepository exceptionRepository;
    private final CapacityReservationRepository reservationRepository;
    private final GetCapacityAvailabilityService availabilityService;

    public GetMerchantStoreCapacityOccupancyService(
            CapacityConfigurationRepository configurationRepository,
            CapacityExceptionRepository exceptionRepository,
            CapacityReservationRepository reservationRepository,
            GetCapacityAvailabilityService availabilityService
    ) {
        this.configurationRepository = configurationRepository;
        this.exceptionRepository = exceptionRepository;
        this.reservationRepository = reservationRepository;
        this.availabilityService = availabilityService;
    }

    public MerchantStoreCapacityOccupancyResponse execute(UUID storeId, LocalDate queryDate) {
        if (storeId == null) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda es obligatorio.");
        }
        LocalDate date = queryDate != null ? queryDate : LocalDate.now(BUSINESS_ZONE);
        Instant now = Instant.now();

        // 1. Conteo directo de ocupación diaria viva en BD (nunca se oculta)
        int dailyReserved = reservationRepository.countDailyReserved(storeId, date, now);
        int dailyCommitted = reservationRepository.countDailyCommitted(storeId, date);
        int dailyOccupied = dailyReserved + dailyCommitted;

        // 2. Descubrir todas las ventanas operativas del día
        List<CapacityPeriod> windows = discoverWindows(storeId, date, now);

        // 3. Evaluar cada ventana con el motor canónico reutilizado
        List<MerchantCapacitySlotOccupancyResponse> slotResponses = new ArrayList<>(windows.size());
        int sumSlotsEffectiveCapacity = 0;

        for (CapacityPeriod window : windows) {
            LocalTime start = window.startTime();
            LocalTime end = window.endTime();
            EffectiveCapacityQuery query = new EffectiveCapacityQuery(storeId, date, start, end);
            CapacityAvailabilityResult result = availabilityService.execute(query);

            int slotOccupied = result.reservedCapacity() + result.committedCapacity();
            int slotPercentage = result.effectiveCapacity() > 0
                    ? (int) Math.round(((double) slotOccupied / result.effectiveCapacity()) * 100)
                    : 0;

            sumSlotsEffectiveCapacity += result.effectiveCapacity();

            slotResponses.add(new MerchantCapacitySlotOccupancyResponse(
                    start,
                    end,
                    result.effectiveCapacity(),
                    slotOccupied,
                    result.availableCapacity(),
                    result.reservedCapacity(),
                    result.committedCapacity(),
                    slotPercentage
            ));
        }

        // 4. Determinar capacidad efectiva diaria y cierre
        CapacityException fullDayException = findFullDayActiveException(storeId, date);
        CapacityConfiguration fullDayConfig = findFullDayActiveConfiguration(storeId, date);
        boolean isClosed;
        int dailyEffectiveCapacity;

        if (fullDayException != null) {
            dailyEffectiveCapacity = fullDayException.getExceptionCapacity();
            isClosed = (dailyEffectiveCapacity == 0);
        } else if (fullDayConfig != null) {
            dailyEffectiveCapacity = fullDayConfig.getMaxCapacity();
            isClosed = (dailyEffectiveCapacity == 0);
        } else if (windows.isEmpty()) {
            dailyEffectiveCapacity = 0;
            isClosed = true;
        } else {
            dailyEffectiveCapacity = sumSlotsEffectiveCapacity;
            isClosed = (dailyEffectiveCapacity == 0);
        }

        int dailyAvailable = Math.max(0, dailyEffectiveCapacity - dailyOccupied);
        int dailyPercentage = dailyEffectiveCapacity > 0
                ? (int) Math.round(((double) dailyOccupied / dailyEffectiveCapacity) * 100)
                : 0;

        return new MerchantStoreCapacityOccupancyResponse(
                storeId,
                date,
                isClosed,
                dailyEffectiveCapacity,
                dailyOccupied,
                dailyAvailable,
                dailyReserved,
                dailyCommitted,
                dailyPercentage,
                slotResponses
        );
    }

    private List<CapacityPeriod> discoverWindows(UUID storeId, LocalDate date, Instant now) {
        Map<CapacityPeriod, CapacityPeriod> unique = new LinkedHashMap<>();

        // A. Ventanas de configuraciones semanales activas
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        for (CapacityConfiguration config : configurationRepository.findByStoreId(storeId)) {
            if (!config.appliesTo(dayOfWeek, config.getStartTime(), config.getEndTime())) {
                continue;
            }
            LocalTime start = config.hasTimeSlot() ? config.getStartTime() : FULL_DAY_START;
            LocalTime end = config.hasTimeSlot() ? config.getEndTime() : FULL_DAY_END;
            CapacityPeriod period = CapacityPeriod.of(start, end);
            unique.putIfAbsent(period, period);
        }

        // B. Ventanas de excepciones activas para la fecha
        for (CapacityException exception : exceptionRepository.findByStoreIdAndServiceDate(storeId, date)) {
            if (!exception.hasTimeSlot() || exception.getStatus() != CapacityExceptionStatus.ACTIVE) {
                continue;
            }
            CapacityPeriod period = CapacityPeriod.of(exception.getStartTime(), exception.getEndTime());
            unique.putIfAbsent(period, period);
        }

        // C. Ventanas con reservas vivas en BD (evita ocultar reservas huérfanas)
        for (CapacityPeriod occupiedPeriod : reservationRepository.findOccupiedWindows(storeId, date, now)) {
            if (occupiedPeriod != null && occupiedPeriod.startTime() != null && occupiedPeriod.endTime() != null) {
                unique.putIfAbsent(occupiedPeriod, occupiedPeriod);
            }
        }

        List<CapacityPeriod> list = new ArrayList<>(unique.values());
        list.sort(Comparator.comparing(CapacityPeriod::startTime).thenComparing(CapacityPeriod::endTime));
        return list;
    }

    private CapacityException findFullDayActiveException(UUID storeId, LocalDate date) {
        for (CapacityException exception : exceptionRepository.findByStoreIdAndServiceDate(storeId, date)) {
            if (!exception.hasTimeSlot() && exception.getStatus() == CapacityExceptionStatus.ACTIVE) {
                return exception;
            }
        }
        return null;
    }

    private CapacityConfiguration findFullDayActiveConfiguration(UUID storeId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        for (CapacityConfiguration config : configurationRepository.findByStoreId(storeId)) {
            if (!config.hasTimeSlot() && config.appliesTo(dayOfWeek, FULL_DAY_START, FULL_DAY_END)) {
                return config;
            }
        }
        return null;
    }
}

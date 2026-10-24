package com.jaldishop.backend.capacity.application.availability;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListStoreCapacitySlotsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private static final LocalTime FULL_DAY_START = LocalTime.of(0, 0, 0);
    private static final LocalTime FULL_DAY_END = LocalTime.of(23, 59, 59);

    private final CapacityConfigurationRepository configurationRepository;
    private final CapacityExceptionRepository exceptionRepository;
    private final GetCapacityAvailabilityService availabilityService;

    public ListStoreCapacitySlotsService(CapacityConfigurationRepository configurationRepository,
                                         CapacityExceptionRepository exceptionRepository,
                                         GetCapacityAvailabilityService availabilityService) {
        this.configurationRepository = configurationRepository;
        this.exceptionRepository = exceptionRepository;
        this.availabilityService = availabilityService;
    }

    public List<CapacitySlotAvailability> execute(UUID storeId, LocalDate serviceDate) {
        if (storeId == null) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda es obligatorio.");
        }
        if (serviceDate == null) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha es obligatoria para consultar las franjas.");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (serviceDate.isBefore(today)) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY",
                    "No es posible consultar franjas de fechas pasadas.");
        }

        List<Window> candidates = candidateWindows(storeId, serviceDate);
        List<CapacitySlotAvailability> slots = new ArrayList<>(candidates.size());
        for (Window window : candidates) {
            EffectiveCapacityQuery query =
                    new EffectiveCapacityQuery(storeId, serviceDate, window.start(), window.end());
            slots.add(new CapacitySlotAvailability(
                    window.start(), window.end(), availabilityService.execute(query)));
        }
        return slots;
    }

    private List<Window> candidateWindows(UUID storeId, LocalDate serviceDate) {
        Map<Window, Window> unique = new LinkedHashMap<>();

        int dayOfWeek = serviceDate.getDayOfWeek().getValue() % 7;
        for (CapacityConfiguration configuration : configurationRepository.findByStoreId(storeId)) {
            if (!configuration.appliesTo(dayOfWeek, configuration.getStartTime(), configuration.getEndTime())) {
                continue;
            }
            LocalTime start = configuration.hasTimeSlot() ? configuration.getStartTime() : FULL_DAY_START;
            LocalTime end = configuration.hasTimeSlot() ? configuration.getEndTime() : FULL_DAY_END;
            Window window = new Window(start, end);
            unique.putIfAbsent(window, window);
        }

        for (CapacityException exception : exceptionRepository.findByStoreIdAndServiceDate(storeId, serviceDate)) {
            if (!exception.hasTimeSlot()) {
                continue;
            }
            if (exception.getStatus() != CapacityExceptionStatus.ACTIVE) {
                continue;
            }
            Window window = new Window(exception.getStartTime(), exception.getEndTime());
            unique.putIfAbsent(window, window);
        }

        List<Window> windows = new ArrayList<>(unique.values());
        windows.sort(Comparator.comparing(Window::start).thenComparing(Window::end));
        return windows;
    }

    private record Window(LocalTime start, LocalTime end) {}
}

package com.jaldishop.backend.capacity.application;

import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class GetCapacityAvailabilityService {

    private final GetEffectiveCapacityService getEffectiveCapacityService;
    private final CapacityReservationRepository reservationRepository;

    public GetCapacityAvailabilityService(GetEffectiveCapacityService getEffectiveCapacityService,
                                          CapacityReservationRepository reservationRepository) {
        this.getEffectiveCapacityService = getEffectiveCapacityService;
        this.reservationRepository = reservationRepository;
    }

    public CapacityAvailabilityResult execute(EffectiveCapacityQuery query) {
        EffectiveCapacityResolution resolution = getEffectiveCapacityService.resolve(query);

        Instant now = Instant.now();
        int reserved = reservationRepository.countReserved(
                query.storeId(), query.serviceDate(), query.startTime(), query.endTime(), now);
        int committed = reservationRepository.countCommitted(
                query.storeId(), query.serviceDate(), query.startTime(), query.endTime());

        int available = Math.max(0, resolution.capacity() - reserved - committed);
        return new CapacityAvailabilityResult(resolution.capacity(), available, reserved, committed);
    }
}
package com.jaldishop.backend.capacity.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de GET /api/v1/capacity/slots. La consulta es informativa y no
 * garantiza un cupo: el checkout vuelve a revalidar la disponibilidad.
 */
public record CapacitySlotsResponse(
        UUID storeId,
        LocalDate serviceDate,
        List<CapacitySlotResponse> slots
) {}

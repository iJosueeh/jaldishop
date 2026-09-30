package com.jaldishop.backend.capacity.application;

import java.util.UUID;

public record EffectiveCapacityResolution(
        int capacity,
        EffectiveCapacitySource source,
        UUID configurationId,
        UUID exceptionId
) {}
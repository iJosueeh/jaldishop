package com.jaldishop.backend.identity.web.dto;

import java.util.UUID;

public record AdminUserStoreSummaryResponse(
        UUID id,
        String name,
        String slug,
        String status
) {}

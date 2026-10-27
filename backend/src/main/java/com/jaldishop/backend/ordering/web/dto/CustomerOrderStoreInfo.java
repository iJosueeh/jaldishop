package com.jaldishop.backend.ordering.web.dto;

import java.util.UUID;

public record CustomerOrderStoreInfo(
        UUID id,
        String name,
        String slug,
        String contactPhone,
        String whatsappNumber,
        String address,
        String addressReference,
        String logoUrl
) {}

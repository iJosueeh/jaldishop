package com.jaldishop.backend.shared.web.dto;

import java.util.List;

public record CustomerPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

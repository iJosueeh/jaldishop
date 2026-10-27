package com.jaldishop.backend.address.web.dto;

import java.util.List;

public record CustomerAddressListResponse(
        int total,
        int maxAllowed,
        List<CustomerAddressResponse> addresses
) {}

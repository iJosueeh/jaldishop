package com.jaldishop.backend.address.infrastructure.ubigeo;

public record UbigeoEntry(
        String code,
        String department,
        String province,
        String district
) {}

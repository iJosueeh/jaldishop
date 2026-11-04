package com.jaldishop.backend.capacity.domain;

public final class CapacityErrorCodes {

    private CapacityErrorCodes() {
    }

    public static final String INVALID_CAPACITY_QUERY = "INVALID_CAPACITY_QUERY";
    public static final String CAPACITY_UNAVAILABLE = "CAPACITY_UNAVAILABLE";
    public static final String CAPACITY_EXHAUSTED = "CAPACITY_EXHAUSTED";
    public static final String CAPACITY_OVERLAP = "CAPACITY_OVERLAP";
    public static final String ALREADY_RESERVED = "ALREADY_RESERVED";
    public static final String RESERVATION_EXPIRED = "RESERVATION_EXPIRED";
    public static final String RESERVATION_COMMITTED = "RESERVATION_COMMITTED";
    public static final String RESERVATION_RELEASED = "RESERVATION_RELEASED";
    public static final String RESERVATION_NOT_PAYMENT_PROTECTED = "RESERVATION_NOT_PAYMENT_PROTECTED";
    public static final String RESERVATION_PROTECTION_EXPIRED = "RESERVATION_PROTECTION_EXPIRED";
}

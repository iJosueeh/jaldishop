package com.jaldishop.backend.ordering.domain;

import java.util.Set;

public enum OrderStatus {
    CONFIRMED,
    IN_PREPARATION,
    READY,
    OUT_FOR_DELIVERY,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CONFIRMED -> Set.of(IN_PREPARATION, CANCELLED).contains(next);
            case IN_PREPARATION -> Set.of(READY, CANCELLED).contains(next);
            case READY -> Set.of(OUT_FOR_DELIVERY, COMPLETED).contains(next);
            case OUT_FOR_DELIVERY -> Set.of(COMPLETED).contains(next);
            case COMPLETED, CANCELLED -> false;
        };
    }

}
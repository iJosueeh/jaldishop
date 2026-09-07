package com.jaldishop.backend.ordering.domain;

import java.util.Set;

public enum OrderStatus {
    CONFIRMADO,
    EN_PREPARACION,
    LISTO,
    EN_ENTREGA,
    COMPLETADO,
    CANCELADO;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CONFIRMADO -> Set.of(EN_PREPARACION, CANCELADO).contains(next);
            case EN_PREPARACION -> Set.of(LISTO, CANCELADO).contains(next);
            case LISTO -> Set.of(EN_ENTREGA, COMPLETADO).contains(next);
            case EN_ENTREGA -> Set.of(COMPLETADO).contains(next);
            case COMPLETADO, CANCELADO -> false;
        };
    }

}

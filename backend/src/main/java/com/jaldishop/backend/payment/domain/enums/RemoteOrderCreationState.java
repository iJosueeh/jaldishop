package com.jaldishop.backend.payment.domain.enums;

/** Creation evidence only; never implies financial approval or permission to resend. */
public enum RemoteOrderCreationState {
    PREPARED, UNKNOWN, CREATED, REJECTED_CONFIRMED
}

package com.jaldishop.backend.store.application;

import java.util.UUID;

public record CloseMyStoreCommand(
        UUID merchantUserId,
        String reason
) {
    public CloseMyStoreCommand {
        if (merchantUserId == null) {
            throw new IllegalArgumentException("El ID del comerciante no puede ser nulo.");
        }
    }
}

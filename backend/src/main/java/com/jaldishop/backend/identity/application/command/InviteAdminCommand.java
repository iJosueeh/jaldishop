package com.jaldishop.backend.identity.application.command;

import java.util.UUID;

public record InviteAdminCommand(
        String email,
        String firstName,
        String lastName,
        String phone,
        UUID currentAdminId
) {
    public InviteAdminCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio.");
        }
    }
}

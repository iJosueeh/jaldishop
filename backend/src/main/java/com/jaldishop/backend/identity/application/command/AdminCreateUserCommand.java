package com.jaldishop.backend.identity.application.command;

import java.util.UUID;

public record AdminCreateUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phone,
        UUID currentAdminId
) {
    public AdminCreateUserCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio.");
        }
    }
}

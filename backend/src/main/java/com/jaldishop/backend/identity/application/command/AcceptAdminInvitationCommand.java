package com.jaldishop.backend.identity.application.command;

public record AcceptAdminInvitationCommand(
        String token,
        String newPassword
) {
    public AcceptAdminInvitationCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de invitación es obligatorio.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }
}

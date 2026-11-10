package com.jaldishop.backend.identity.application.command;

public record ResetPasswordCommand(String token, String newPassword) {
    @Override public String toString() { return "ResetPasswordCommand[REDACTED]"; }
}

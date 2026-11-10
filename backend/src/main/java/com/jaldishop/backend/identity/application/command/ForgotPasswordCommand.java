package com.jaldishop.backend.identity.application.command;

public record ForgotPasswordCommand(String email) {
    @Override public String toString() { return "ForgotPasswordCommand[REDACTED]"; }
}

package com.jaldishop.backend.identity.application.command;

public record ForgotPasswordCommand(String email, String origin) {
    public ForgotPasswordCommand(String email) {
        this(email, null);
    }

    @Override public String toString() { return "ForgotPasswordCommand[REDACTED]"; }
}

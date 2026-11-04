package com.jaldishop.backend.identity.application.command;

public record RegisterCustomerCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phone
) {}

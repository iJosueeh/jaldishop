package com.jaldishop.backend.identity.application;

import java.util.UUID;

public record UpdateProfileCommand(
        UUID userId,
        String firstName,
        String lastName,
        String phone
) {}

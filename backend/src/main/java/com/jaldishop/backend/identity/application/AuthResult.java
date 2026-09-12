package com.jaldishop.backend.identity.application;

import java.util.Set;
import java.util.UUID;

public record AuthResult(
   String token,
   UUID userId,
   String email,
   String fullName,
   Set<String> roles
) {}

package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserProfileResponse(
   UUID id,
   String email,
   String firstName,
   String lastName,
   String phone,
   UserStatus status,
   Set<String> roles,
   Instant createdAt
) {
    public static UserProfileResponse fromDomain(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                roleNames,
                user.getCreatedAt()
        );
    }
}

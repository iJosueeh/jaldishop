package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.identity.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "Debe proporcionar al menos un rol.")
        Set<RoleName> roles
) {}

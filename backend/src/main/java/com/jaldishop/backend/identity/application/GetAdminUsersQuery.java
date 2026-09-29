package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.UserStatus;

public record GetAdminUsersQuery(
        String query,
        RoleName role,
        UserStatus status
) {}

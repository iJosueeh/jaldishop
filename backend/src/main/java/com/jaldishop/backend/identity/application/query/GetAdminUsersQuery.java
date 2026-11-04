package com.jaldishop.backend.identity.application.query;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;

public record GetAdminUsersQuery(
        String query,
        RoleName role,
        UserStatus status
) {}

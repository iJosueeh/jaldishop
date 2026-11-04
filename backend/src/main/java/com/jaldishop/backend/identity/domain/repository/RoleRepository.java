package com.jaldishop.backend.identity.domain.repository;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByName(RoleName name);

}

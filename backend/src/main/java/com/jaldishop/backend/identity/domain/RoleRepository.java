package com.jaldishop.backend.identity.domain;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByName(RoleName name);

}

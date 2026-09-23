package com.jaldishop.backend.identity.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    User save(User user);
    boolean existsByEmail(String email);
    List<User> findAll(String query, RoleName role, UserStatus status);

}

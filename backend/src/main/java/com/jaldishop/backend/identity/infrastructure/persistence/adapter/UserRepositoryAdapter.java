package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.jaldishop.backend.identity.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository, UserPersistenceMapper mapper) {
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findWithRolesByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findWithRolesById(id)
                .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = userJpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll(String query, RoleName role, UserStatus status) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return userJpaRepository.searchUsers(cleanQuery, role, status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

package com.jaldishop.backend.identity.infrastructure.persistence.mapper;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles().stream()
                .map(roleEntity -> new Role(roleEntity.getId(), roleEntity.getName()))
                .collect(Collectors.toUnmodifiableSet());

        return User.reconstitute(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordEncoded(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getStatus(),
                roles,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(User user) {
        Set<RoleEntity> roles = user.getRoles().stream()
                .map(role -> new RoleEntity(
                        role.getId(),
                        role.getName()
                ))
                .collect(Collectors.toSet());

        return new UserEntity(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.getStatus(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roles
        );
    }

}

package com.jaldishop.backend.identity.infrastructure.persistence.entity;

import com.jaldishop.backend.identity.domain.RoleName;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;

    protected RoleEntity() {}

    public RoleEntity(Short id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public Short getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }

}

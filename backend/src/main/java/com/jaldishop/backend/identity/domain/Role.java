package com.jaldishop.backend.identity.domain;

public class Role {

    private final Short id;
    private final RoleName name;

    public Role(Short id, RoleName name) {
        if (id == null) {
            throw new IllegalArgumentException("ID no puede ser nulo.");
        }

        if (name == null) {
            throw new IllegalArgumentException("Nombre no puede ser nulo.");
        }

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

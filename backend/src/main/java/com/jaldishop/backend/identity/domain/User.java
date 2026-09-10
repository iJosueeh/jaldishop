package com.jaldishop.backend.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final String phone;
    private final Set<Role> roles;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, String firstName, String lastName, String email, String password, UserStatus status, String phone, Instant createdAt, Instant updatedAt, Set<Role> roles) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email.toLowerCase().trim();
        this.password = password;
        this.status = status;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = Set.copyOf(roles);
    }

    public static User create(String email, String password, String firstName, String lastName, String phone, Set<Role> roles) {
        validateRequired(email, "email");
        validateRequired(password, "password");
        validateRequired(firstName, "First Name");
        validateRequired(lastName, "Last Name");

        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe contener un rol minimo.");
        }

        Instant now = Instant.now();

        return new User(
                UUID.randomUUID(),
                firstName.trim(),
                lastName.trim(),
                email.trim().toLowerCase(Locale.ROOT),
                password,
                UserStatus.ACTIVE,
                phone,
                now,
                now,
                roles
        );
    }

    public static void validateRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " no puede ser nulo o vacio."
            );
        }
    }

    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

}

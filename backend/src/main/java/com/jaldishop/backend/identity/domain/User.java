package com.jaldishop.backend.identity.domain;

import java.time.Instant;
import java.util.*;

public class User {

    private final UUID id;
    private String firstName;
    private String lastName;
    private final String email;
    private final String password;
    private UserStatus status;
    private String phone;
    private final Set<Role> roles;
    private final Instant createdAt;
    private Instant updatedAt;

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
        this.roles = new HashSet<>(roles);
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

    public static User reconstitute(
            UUID id,
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            UserStatus status,
            Set<Role> roles,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new User(
                id,
                firstName,
                lastName,
                email,
                password,
                status,
                phone,
                createdAt,
                updatedAt,
                roles
        );
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        validateRequired(firstName, "First Name");
        validateRequired(lastName, "Last Name");

        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.phone = phone != null ? phone.trim() : null;
        this.updatedAt = Instant.now();
    }

    public static void validateRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " no puede ser nulo o vacio."
            );
        }
    }

    public void addRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo.");
        }
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public boolean hasRole(RoleName roleName) {
        if (roleName == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }

    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE;
    }

    public void suspend(UUID currentAdminId) {
        if (currentAdminId != null && this.id.equals(currentAdminId)) {
            throw new IllegalArgumentException("No puedes suspender tu propia cuenta de administrador.");
        }
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("El usuario ya se encuentra suspendido.");
        }
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("El usuario ya se encuentra activo.");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
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

    public String getPhone() {
        return phone;
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
        return Collections.unmodifiableSet(this.roles);
    }

}

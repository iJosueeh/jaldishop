package com.jaldishop.backend.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private final Role customerRole = new Role((short) 1, RoleName.CUSTOMER);
    private final Role merchantRole = new Role((short) 2, RoleName.MERCHANT);

    @Test
    @DisplayName("Crear usuario con valores válidos")
    void createUserWithValidValues() {
        User user = User.create(
                "juan@test.com",
                "password123",
                "Juan",
                "Perez",
                "+51999999999",
                Set.of(customerRole)
        );

        assertNotNull(user.getId());
        assertEquals("juan@test.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals("Juan", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(Set.of(customerRole), user.getRoles());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Crear usuario con múltiples roles")
    void createUserWithMultipleRoles() {
        User user = User.create(
                "admin@test.com",
                "password123",
                "Admin",
                "User",
                null,
                Set.of(customerRole, merchantRole)
        );

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(customerRole));
        assertTrue(user.getRoles().contains(merchantRole));
    }

    @Test
    @DisplayName("Lanzar excepción cuando email es nulo")
    void throwExceptionWhenEmailIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create(null, "password123", "Juan", "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando email está en blanco")
    void throwExceptionWhenEmailIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("   ", "password123", "Juan", "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando password es nulo")
    void throwExceptionWhenPasswordIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", null, "Juan", "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("password"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando password está en blanco")
    void throwExceptionWhenPasswordIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "  ", "Juan", "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("password"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando firstName es nulo")
    void throwExceptionWhenFirstNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", null, "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("First Name"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando firstName está en blanco")
    void throwExceptionWhenFirstNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", "  ", "Perez", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("First Name"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando lastName es nulo")
    void throwExceptionWhenLastNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", "Juan", null, null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("Last Name"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando lastName está en blanco")
    void throwExceptionWhenLastNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", "Juan", "  ", null, Set.of(customerRole))
        );
        assertTrue(exception.getMessage().contains("Last Name"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando roles es nulo")
    void throwExceptionWhenRolesIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", "Juan", "Perez", null, null)
        );
        assertTrue(exception.getMessage().contains("rol"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando roles está vacío")
    void throwExceptionWhenRolesIsEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.create("juan@test.com", "password123", "Juan", "Perez", null, Set.of())
        );
        assertTrue(exception.getMessage().contains("rol"));
    }

    @Test
    @DisplayName("Email debe convertirse a minúsculas")
    void emailShouldBeConvertedToLowercase() {
        User user = User.create(
                "JUAN@TEST.COM",
                "password123",
                "Juan",
                "Perez",
                null,
                Set.of(customerRole)
        );

        assertEquals("juan@test.com", user.getEmail());
    }

    @Test
    @DisplayName("Email debe recortar espacios en blanco")
    void emailShouldBeTrimmed() {
        User user = User.create(
                "  juan@test.com  ",
                "password123",
                "Juan",
                "Perez",
                null,
                Set.of(customerRole)
        );

        assertEquals("juan@test.com", user.getEmail());
    }

    @Test
    @DisplayName("Email con mayúsculas y espacios debe normalizarse correctamente")
    void emailShouldBeNormalizedCorrectly() {
        User user = User.create(
                "  JUAN@TEST.COM  ",
                "password123",
                "Juan",
                "Perez",
                null,
                Set.of(customerRole)
        );

        assertEquals("juan@test.com", user.getEmail());
    }

    @Test
    @DisplayName("canAuthenticate() debe retornar true cuando status es ACTIVE")
    void canAuthenticateShouldReturnTrueWhenActive() {
        User user = User.create(
                "juan@test.com",
                "password123",
                "Juan",
                "Perez",
                null,
                Set.of(customerRole)
        );

        assertTrue(user.canAuthenticate());
    }

    @Test
    @DisplayName("canAuthenticate() debe retornar false cuando status es INACTIVE")
    void canAuthenticateShouldReturnFalseWhenInactive() {
        User user = new User(
                java.util.UUID.randomUUID(),
                "Juan",
                "Perez",
                "juan@test.com",
                "password123",
                UserStatus.INACTIVE,
                null,
                Instant.now(),
                Instant.now(),
                Set.of(customerRole)
        );

        assertFalse(user.canAuthenticate());
    }

    @Test
    @DisplayName("canAuthenticate() debe retornar false cuando status es SUSPENDED")
    void canAuthenticateShouldReturnFalseWhenSuspended() {
        User user = new User(
                java.util.UUID.randomUUID(),
                "Juan",
                "Perez",
                "juan@test.com",
                "password123",
                UserStatus.SUSPENDED,
                null,
                Instant.now(),
                Instant.now(),
                Set.of(customerRole)
        );

        assertFalse(user.canAuthenticate());
    }

    @Test
    @DisplayName("getFullName() debe retornar firstName + espacio + lastName")
    void getFullNameShouldReturnCorrectFormat() {
        User user = User.create(
                "juan@test.com",
                "password123",
                "Juan",
                "Perez",
                null,
                Set.of(customerRole)
        );

        assertEquals("Juan Perez", user.getFullName());
    }

    @Test
    @DisplayName("getFullName() debe funcionar con nombres compuestos")
    void getFullNameShouldWorkWithCompoundNames() {
        User user = User.create(
                "maria@test.com",
                "password123",
                "Maria Jose",
                "Garcia Lopez",
                null,
                Set.of(customerRole)
        );

        assertEquals("Maria Jose Garcia Lopez", user.getFullName());
    }

    @Test
    @DisplayName("validateRequired() debe lanzar excepción con valor nulo")
    void validateRequiredShouldThrowExceptionWithNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.validateRequired(null, "testField")
        );
        assertTrue(exception.getMessage().contains("testField"));
    }

    @Test
    @DisplayName("validateRequired() debe lanzar excepción con valor vacío")
    void validateRequiredShouldThrowExceptionWithEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.validateRequired("", "testField")
        );
        assertTrue(exception.getMessage().contains("testField"));
    }

    @Test
    @DisplayName("validateRequired() debe lanzar excepción con espacios en blanco")
    void validateRequiredShouldThrowExceptionWithBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.validateRequired("   ", "testField")
        );
        assertTrue(exception.getMessage().contains("testField"));
    }

    @Test
    @DisplayName("validateRequired() no debe lanzar excepción con valor válido")
    void validateRequiredShouldNotThrowExceptionWithValidValue() {
        assertDoesNotThrow(() -> User.validateRequired("validValue", "testField"));
    }

}

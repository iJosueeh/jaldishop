package com.jaldishop.backend.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    @DisplayName("Crear Role con valores válidos")
    void createRoleWithValidValues() {
        Role role = new Role((short) 1, RoleName.CUSTOMER);

        assertEquals((short) 1, role.getId());
        assertEquals(RoleName.CUSTOMER, role.getName());
    }

    @Test
    @DisplayName("Crear Role con cada valor del enum")
    void createRoleWithAllEnumValues() {
        Role customer = new Role((short) 1, RoleName.CUSTOMER);
        Role merchant = new Role((short) 2, RoleName.MERCHANT);
        Role admin = new Role((short) 3, RoleName.ADMIN);

        assertEquals(RoleName.CUSTOMER, customer.getName());
        assertEquals(RoleName.MERCHANT, merchant.getName());
        assertEquals(RoleName.ADMIN, admin.getName());
    }

    @Test
    @DisplayName("Lanzar excepción cuando id es nulo")
    void throwExceptionWhenIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Role(null, RoleName.CUSTOMER)
        );
        assertEquals("ID no puede ser nulo.", exception.getMessage());
    }

    @Test
    @DisplayName("Lanzar excepción cuando name es nulo")
    void throwExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Role((short) 1, null)
        );
        assertEquals("Nombre no puede ser nulo.", exception.getMessage());
    }

}

package com.jaldishop.backend.identity.infrastructure.persistence.mapper;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPersistenceMapperTest {

    private UserPersistenceMapper mapper;

    private UUID testId;
    private String testEmail;
    private String testPassword;
    private String testFirstName;
    private String testLastName;
    private String testPhone;
    private UserStatus testStatus;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;
    private Set<Role> testRoles;
    private Set<RoleEntity> testRoleEntities;

    @BeforeEach
    void setUp() {
        mapper = new UserPersistenceMapper();

        testId = UUID.randomUUID();
        testEmail = "juan@test.com";
        testPassword = "$2a$10$encodedPassword123";
        testFirstName = "Juan";
        testLastName = "Perez";
        testPhone = "+51999999999";
        testStatus = UserStatus.ACTIVE;
        testCreatedAt = Instant.parse("2024-01-15T10:00:00Z");
        testUpdatedAt = Instant.parse("2024-01-15T12:00:00Z");

        Role customerRole = new Role((short) 1, RoleName.CUSTOMER);
        Role merchantRole = new Role((short) 2, RoleName.MERCHANT);
        testRoles = Set.of(customerRole, merchantRole);

        RoleEntity customerRoleEntity = new RoleEntity((short) 1, RoleName.CUSTOMER);
        RoleEntity merchantRoleEntity = new RoleEntity((short) 2, RoleName.MERCHANT);
        testRoleEntities = Set.of(customerRoleEntity, merchantRoleEntity);
    }

    @Test
    @DisplayName("toEntity() debe conservar el UUID del dominio")
    void toEntityShouldPreserveUuid() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testId, entity.getId());
    }

    @Test
    @DisplayName("toEntity() debe conservar el email")
    void toEntityShouldPreserveEmail() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testEmail, entity.getEmail());
    }

    @Test
    @DisplayName("toEntity() debe conservar el password encoded")
    void toEntityShouldPreservePassword() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testPassword, entity.getPasswordEncoded());
    }

    @Test
    @DisplayName("toEntity() debe conservar el status")
    void toEntityShouldPreserveStatus() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testStatus, entity.getStatus());
    }

    @Test
    @DisplayName("toEntity() debe mapear roles correctamente")
    void toEntityShouldMapRoles() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testRoles.size(), entity.getRoles().size());
        assertTrue(entity.getRoles().stream()
                .anyMatch(r -> r.getId().equals((short) 1) && r.getName() == RoleName.CUSTOMER));
        assertTrue(entity.getRoles().stream()
                .anyMatch(r -> r.getId().equals((short) 2) && r.getName() == RoleName.MERCHANT));
    }

    @Test
    @DisplayName("toEntity() debe conservar firstName y lastName")
    void toEntityShouldPreserveNames() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testFirstName, entity.getFirstName());
        assertEquals(testLastName, entity.getLastName());
    }

    @Test
    @DisplayName("toEntity() debe conservar phone")
    void toEntityShouldPreservePhone() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testPhone, entity.getPhone());
    }

    @Test
    @DisplayName("toEntity() debe conservar timestamps")
    void toEntityShouldPreserveTimestamps() {
        User domain = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(domain);

        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe reconstruir el UUID sin generar uno nuevo")
    void toDomainShouldReconstructUuid() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testId, domain.getId());
    }

    @Test
    @DisplayName("toDomain() debe conservar el email")
    void toDomainShouldPreserveEmail() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testEmail, domain.getEmail());
    }

    @Test
    @DisplayName("toDomain() debe conservar el password encoded")
    void toDomainShouldPreservePassword() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testPassword, domain.getPassword());
    }

    @Test
    @DisplayName("toDomain() debe conservar el status")
    void toDomainShouldPreserveStatus() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testStatus, domain.getStatus());
    }

    @Test
    @DisplayName("toDomain() debe reconstruir roles correctamente")
    void toDomainShouldReconstructRoles() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testRoleEntities.size(), domain.getRoles().size());
        assertTrue(domain.getRoles().stream()
                .anyMatch(r -> r.getId().equals((short) 1) && r.getName() == RoleName.CUSTOMER));
        assertTrue(domain.getRoles().stream()
                .anyMatch(r -> r.getId().equals((short) 2) && r.getName() == RoleName.MERCHANT));
    }

    @Test
    @DisplayName("toDomain() debe conservar createdAt y updatedAt")
    void toDomainShouldPreserveTimestamps() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertEquals(testUpdatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe conservar firstName y lastName")
    void toDomainShouldPreserveNames() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testFirstName, domain.getFirstName());
        assertEquals(testLastName, domain.getLastName());
    }

    @Test
    @DisplayName("toDomain() debe conservar phone")
    void toDomainShouldPreservePhone() {
        UserEntity entity = new UserEntity(
                testId, testFirstName, testLastName, testEmail, testPassword,
                testStatus, testPhone, testCreatedAt, testUpdatedAt, testRoleEntities
        );

        User domain = mapper.toDomain(entity);

        assertEquals(testPhone, domain.getPhone());
    }

    @Test
    @DisplayName("Roundtrip: Domain → Entity → Domain conserva todos los campos")
    void roundtripShouldPreserveAllFields() {
        User original = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(original);
        User reconstructed = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstructed.getId());
        assertEquals(original.getEmail(), reconstructed.getEmail());
        assertEquals(original.getPassword(), reconstructed.getPassword());
        assertEquals(original.getFirstName(), reconstructed.getFirstName());
        assertEquals(original.getLastName(), reconstructed.getLastName());
        assertEquals(original.getPhone(), reconstructed.getPhone());
        assertEquals(original.getStatus(), reconstructed.getStatus());
        assertEquals(original.getCreatedAt(), reconstructed.getCreatedAt());
        assertEquals(original.getUpdatedAt(), reconstructed.getUpdatedAt());
        assertEquals(original.getRoles().size(), reconstructed.getRoles().size());
    }

    @Test
    @DisplayName("Roundtrip: Domain → Entity → Domain para status INACTIVE")
    void roundtripShouldPreserveInactiveStatus() {
        User original = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, UserStatus.INACTIVE, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(original);
        User reconstructed = mapper.toDomain(entity);

        assertEquals(UserStatus.INACTIVE, reconstructed.getStatus());
    }

    @Test
    @DisplayName("Roundtrip: Domain → Entity → Domain para status SUSPENDED")
    void roundtripShouldPreserveSuspendedStatus() {
        User original = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                testPhone, UserStatus.SUSPENDED, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(original);
        User reconstructed = mapper.toDomain(entity);

        assertEquals(UserStatus.SUSPENDED, reconstructed.getStatus());
    }

    @Test
    @DisplayName("Roundtrip: Domain → Entity → Domain con phone nulo")
    void roundtripShouldPreserveNullPhone() {
        User original = User.reconstitute(
                testId, testEmail, testPassword, testFirstName, testLastName,
                null, testStatus, testRoles, testCreatedAt, testUpdatedAt
        );

        UserEntity entity = mapper.toEntity(original);
        User reconstructed = mapper.toDomain(entity);

        assertNull(reconstructed.getPhone());
    }

}

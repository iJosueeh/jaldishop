package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica contra PostgreSQL real, con los proxies @Transactional de Spring vivos,
 * que el registro de comerciante es atómico: cuando la creación de la tienda falla
 * (slug duplicado), el INSERT de usuario realizado ANTES en la misma transacción
 * se revierte por completo.
 *
 * IMPORTANTE: el método de prueba NO lleva @Transactional. Si lo llevara, JUnit
 * revertiría la transacción al terminar y enmascararía el rollback real que se
 * pretende observar. Usa @SpringBootTest (no JDBC puro) para ejercitar los
 * proxies transaccionales reales de RegisterMerchantService/CreateStoreService.
 */
@SpringBootTest
@Import(RegisterMerchantRollbackIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterMerchantRollbackIntegrationTest {

    private static final String STORE_NAME = "Tienda Rollback Test";
    private static final String STORE_SLUG = "tienda-rollback-test";
    private static final String EMAIL_OWNER = "rollback-owner@jaldi.test";
    private static final String EMAIL_CONFLICT = "rollback-conflict@jaldi.test";

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        // The static database field is validated before JUnit setup or Spring startup.
        // Override connection properties with the values used by direct JDBC checks.
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
    }

    @Autowired
    private RegisterMerchantService registerMerchantService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestDatabase {
        @Bean
        DataSource dataSource() {
            TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(config.url());
            dataSource.setUsername(config.username());
            dataSource.setPassword(config.password());
            return dataSource;
        }

        @Bean(initMethod = "migrate")
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .validateOnMigrate(TestPostgresConfig.flywayValidateOnMigrate())
                    .load();
        }
    }

    @BeforeAll
    static void cleanPreexistingData() throws SQLException {
        cleanTestData();
    }

    @AfterAll
    static void cleanTestDataAfterRun() throws SQLException {
        cleanTestData();
    }

    private static void cleanTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM store_category_assignments WHERE store_id IN (SELECT id FROM stores WHERE slug = ?)")) {
                ps.setString(1, STORE_SLUG);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?))")) {
                ps.setString(1, EMAIL_OWNER);
                ps.setString(2, EMAIL_CONFLICT);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM stores WHERE slug = ?")) {
                ps.setString(1, STORE_SLUG);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE email IN (?, ?)")) {
                ps.setString(1, EMAIL_OWNER);
                ps.setString(2, EMAIL_CONFLICT);
                ps.executeUpdate();
            }
        }
    }

    private static int count(String sql, String param) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Registro exitoso crea usuario, roles y tienda contra PostgreSQL real")
    void registerMerchantCreatesUserRolesAndStore() throws SQLException {
        try (Connection springConnection = dataSource.getConnection();
             Connection flywayConnection = flyway.getConfiguration().getDataSource().getConnection()) {
            assertEquals(database.url(), springConnection.getMetaData().getURL());
            assertEquals(database.url(), flywayConnection.getMetaData().getURL());
        }

        RegisterMerchantCommand command = new RegisterMerchantCommand(
                EMAIL_OWNER,
                "password123",
                "Rollback",
                "Owner",
                "987654321",
                STORE_NAME,
                "Prueba rollback",
                "987654321",
                "Av. Principal 123",
                true,
                true
        );

        AuthResult result = registerMerchantService.execute(command);

        assertNotNull(result);
        assertEquals(EMAIL_OWNER, result.email());
        assertTrue(result.roles().contains("MERCHANT"));
        assertTrue(result.roles().contains("CUSTOMER"));

        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE email = ?", EMAIL_OWNER));
        assertEquals(1, count("SELECT COUNT(*) FROM stores WHERE slug = ?", STORE_SLUG));
        assertEquals(2, count(
                "SELECT COUNT(*) FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.email = ?",
                EMAIL_OWNER));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM user_roles ur JOIN users u ON u.id = ur.user_id "
                        + "JOIN roles r ON r.id = ur.role_id "
                        + "WHERE u.email = ? AND r.name = 'MERCHANT' AND ur.store_id IS NOT NULL",
                EMAIL_OWNER));
    }

    @Test
    @Order(2)
    @DisplayName("Fallo por slug duplicado revierte el INSERT de usuario previo (rollback completo)")
    void storeCreationFailureRollsBackInsertedUser() throws SQLException {
        RegisterMerchantCommand command = new RegisterMerchantCommand(
                EMAIL_CONFLICT,
                "password123",
                "Rollback",
                "Conflict",
                "911111111",
                STORE_NAME,
                "Prueba rollback",
                "911111111",
                "Calle Secundaria 456",
                true,
                true
        );

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> registerMerchantService.execute(command)
        );
        assertEquals("STORE_SLUG_ALREADY_EXISTS", exception.getCode());

        // El INSERT del usuario (hecho ANTES de provisionar la tienda) se revirtió:
        assertEquals(0, count("SELECT COUNT(*) FROM users WHERE email = ?", EMAIL_CONFLICT));
        assertEquals(0, count(
                "SELECT COUNT(*) FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.email = ?",
                EMAIL_CONFLICT));

        // Solo existe la tienda del registro previo (no se creó ninguna nueva):
        assertEquals(1, count("SELECT COUNT(*) FROM stores WHERE slug = ?", STORE_SLUG));

        // El rollback quedó acotado a la transacción fallida: el registro previo
        // y su asignación MERCHANT con store_id siguen intactos.
        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE email = ?", EMAIL_OWNER));
        assertEquals(2, count(
                "SELECT COUNT(*) FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.email = ?",
                EMAIL_OWNER));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM user_roles ur JOIN users u ON u.id = ur.user_id "
                        + "JOIN roles r ON r.id = ur.role_id "
                        + "WHERE u.email = ? AND r.name = 'MERCHANT' AND ur.store_id IS NOT NULL",
                EMAIL_OWNER));
    }
}

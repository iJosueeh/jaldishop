package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.shared.exception.ConflictException;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterMerchantRollbackIntegrationTest {

    private static final String STORE_NAME = "Tienda Rollback Test";
    private static final String STORE_SLUG = "tienda-rollback-test";
    private static final String EMAIL_OWNER = "rollback-owner@jaldi.test";
    private static final String EMAIL_CONFLICT = "rollback-conflict@jaldi.test";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        String envDir = new File("backend/.env").exists() ? "./backend" : "./";
        Dotenv dotenv = Dotenv.configure()
                .directory(envDir)
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        dbUrl = dotenv.get("DB_URL", System.getenv("DB_URL"));
        dbUser = dotenv.get("DB_USERNAME", System.getenv("DB_USERNAME"));
        dbPassword = dotenv.get("DB_PASSWORD", System.getenv("DB_PASSWORD"));
    }

    @Autowired
    private RegisterMerchantService registerMerchantService;

    @BeforeAll
    static void cleanPreexistingData() throws SQLException {
        cleanTestData();
    }

    @AfterAll
    static void cleanTestDataAfterRun() throws SQLException {
        cleanTestData();
    }

    private static void cleanTestData() throws SQLException {
        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
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
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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

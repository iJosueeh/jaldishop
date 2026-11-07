package com.jaldishop.backend.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestPostgresConfigTest — Pruebas de validación estricta y aislamiento de base de datos")
class TestPostgresConfigTest {

    @Test
    void separateEnvironmentUrlsCanCoexistWithoutFallbackOrCrossing() {
        String general = "jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test";
        String phase2a = "jdbc:postgresql://127.0.0.1:5433/jaldishop_fase2a_test";
        Map<String, String> env = Map.of("DB_URL", general, "PHASE2A_DB_URL", phase2a,
                "DB_USERNAME", "test", "DB_PASSWORD", "test");
        assertEquals(general, TestPostgresConfig.fromEnvironment(env, false).url());
        assertEquals(phase2a, TestPostgresConfig.fromEnvironment(env, true).url());
        Map<String, String> missingPhase = Map.of("DB_URL", general, "DB_USERNAME", "test", "DB_PASSWORD", "test");
        assertTrue(assertThrows(IllegalStateException.class,
                () -> TestPostgresConfig.fromEnvironment(missingPhase, true)).getMessage().contains("PHASE2A_DB_URL"));
        Map<String, String> crossed = Map.of("DB_URL", phase2a, "PHASE2A_DB_URL", general,
                "DB_USERNAME", "test", "DB_PASSWORD", "test");
        assertThrows(IllegalStateException.class, () -> TestPostgresConfig.fromEnvironment(crossed, false));
        assertThrows(IllegalStateException.class, () -> TestPostgresConfig.fromEnvironment(crossed, true));
    }

    @Test
    @DisplayName("Acepta URL válida local exclusiva para integración jaldishop_fase2a_test")
    void shouldAcceptValidLocalIntegrationTestUrl() {
        TestPostgresConfig config = TestPostgresConfig.fromValuesForPhase2a(
                "jdbc:postgresql://127.0.0.1:5433/jaldishop_fase2a_test",
                "postgres",
                "postgres"
        );
        assertNotNull(config);
        assertEquals("jdbc:postgresql://127.0.0.1:5433/jaldishop_fase2a_test", config.url());
        assertEquals("postgres", config.username());
        assertEquals("postgres", config.password());
    }

    @Test
    void normalSuitesKeepTheirOriginalDatabaseAndRejectPhase2aDatabase() {
        assertEquals("jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test",
                TestPostgresConfig.fromValues("jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test", "test", "test").url());
        assertThrows(IllegalStateException.class, () -> TestPostgresConfig.fromValues(
                "jdbc:postgresql://127.0.0.1:5433/jaldishop_fase2a_test", "test", "test"));
        assertThrows(IllegalStateException.class, () -> TestPostgresConfig.fromValues(
                "jdbc:postgresql://remote.example:5433/jaldishop_integration_test", "test", "test"));
        assertThrows(IllegalStateException.class, () -> TestPostgresConfig.fromValues(
                "jdbc:postgresql://127.0.0.1:5433/jaldishop_test", "test", "test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_test", // Base de sandbox manual rechazada
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test",
            "jdbc:postgresql://localhost:5433/jaldishop_test",
            "jdbc:postgresql://ep-silent-dawn-1234.us-east-2.aws.neon.tech/jaldishop_fase2a_test", // Neon remoto
            "jdbc:postgresql://dpg-c12345-a.oregon-postgres.render.com/jaldishop_fase2a_test", // Render remoto
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_prod", // Producción
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_fase2a_test?sslmode=require", // Con parámetros no permitidos
            "jdbc:postgresql://192.168.1.100:5433/jaldishop_fase2a_test", // IP no loopback
            "jdbc:postgresql://127.0.0.1:99999/jaldishop_fase2a_test" // Puerto fuera de rango
    })
    @DisplayName("Rechaza categóricamente URLs de sandbox manual, remotas o no autorizadas")
    void shouldRejectInvalidOrUnsafeUrls(String unsafeUrl) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                TestPostgresConfig.fromValuesForPhase2a(unsafeUrl, "postgres", "postgres")
        );
        assertTrue(ex.getMessage().contains("jaldishop_fase2a_test") || ex.getMessage().contains("loopback"));
    }

    @Test
    @DisplayName("Rechaza variables nulas o en blanco de inmediato")
    void shouldRejectNullOrBlankCredentials() {
        assertThrows(IllegalStateException.class, () ->
                TestPostgresConfig.fromValues(null, "postgres", "postgres")
        );
        assertThrows(IllegalStateException.class, () ->
                TestPostgresConfig.fromValues("jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test", "", "postgres")
        );
        assertThrows(IllegalStateException.class, () ->
                TestPostgresConfig.fromValues("jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test", "postgres", "")
        );
    }
}

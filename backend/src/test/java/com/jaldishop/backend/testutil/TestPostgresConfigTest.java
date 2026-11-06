package com.jaldishop.backend.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestPostgresConfigTest — Pruebas de validación estricta y aislamiento de base de datos")
class TestPostgresConfigTest {

    @Test
    @DisplayName("Acepta URL válida local exclusiva para integración jaldishop_integration_test")
    void shouldAcceptValidLocalIntegrationTestUrl() {
        TestPostgresConfig config = TestPostgresConfig.fromValues(
                "jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test",
                "postgres",
                "postgres"
        );
        assertNotNull(config);
        assertEquals("jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test", config.url());
        assertEquals("postgres", config.username());
        assertEquals("postgres", config.password());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_test", // Base de sandbox manual rechazada
            "jdbc:postgresql://localhost:5433/jaldishop_test",
            "jdbc:postgresql://ep-silent-dawn-1234.us-east-2.aws.neon.tech/jaldishop_integration_test", // Neon remoto
            "jdbc:postgresql://dpg-c12345-a.oregon-postgres.render.com/jaldishop_integration_test", // Render remoto
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_prod", // Producción
            "jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test?sslmode=require", // Con parámetros no permitidos
            "jdbc:postgresql://192.168.1.100:5433/jaldishop_integration_test", // IP no loopback
            "jdbc:postgresql://127.0.0.1:99999/jaldishop_integration_test" // Puerto fuera de rango
    })
    @DisplayName("Rechaza categóricamente URLs de sandbox manual, remotas o no autorizadas")
    void shouldRejectInvalidOrUnsafeUrls(String unsafeUrl) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                TestPostgresConfig.fromValues(unsafeUrl, "postgres", "postgres")
        );
        assertTrue(ex.getMessage().contains("jaldishop_integration_test") || ex.getMessage().contains("loopback"));
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

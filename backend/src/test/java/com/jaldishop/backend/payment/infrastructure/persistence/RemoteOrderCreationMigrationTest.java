package com.jaldishop.backend.payment.infrastructure.persistence;

import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Dedicated loopback test DB only; minimal pre-V24 schema isolates the migration contract. */
@EnabledIfEnvironmentVariable(named = "PHASE2A_DB_URL", matches = "jdbc:postgresql://.*")
class RemoteOrderCreationMigrationTest {
    @Test void preservesHistoricalRowsAndAuditsAtomicTransitions() throws Exception {
        var database = TestPostgresConfig.fromEnvironmentForPhase2a();
        try (var connection = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                String schema = "phase2b1_" + UUID.randomUUID().toString().replace("-", "");
                sql.execute("CREATE SCHEMA " + schema);
                sql.execute("SET LOCAL search_path TO " + schema);
                sql.execute("CREATE TABLE payment_attempts (id UUID PRIMARY KEY, checkout_mode VARCHAR(20) NOT NULL, "
                        + "idempotency_key VARCHAR(64) NOT NULL, provider_order_id VARCHAR(100), init_point TEXT, status VARCHAR(30))");
                UUID historical = UUID.randomUUID();
                sql.execute("INSERT INTO payment_attempts VALUES ('" + historical + "', 'ORDERS', 'old', NULL, NULL, 'ERROR')");
                for (String migration : new String[]{"V24__create_checkout_creation_requests.sql", "V25__add_remote_order_creation_evidence.sql"}) {
                    sql.execute(new ClassPathResource("db/migration/" + migration).getContentAsString(StandardCharsets.UTF_8));
                }
                try (var rows = sql.executeQuery("SELECT status, remote_order_creation_state FROM payment_attempts")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("ERROR");
                    assertThat(rows.getString(2)).isEqualTo("UNKNOWN");
                }
                UUID fresh = UUID.randomUUID();
                sql.execute("INSERT INTO payment_attempts (id, checkout_mode, idempotency_key, status, remote_order_creation_state) "
                        + "VALUES ('" + fresh + "', 'ORDERS', 'new', 'PROCESSING', 'PREPARED')");
                sql.execute("UPDATE payment_attempts SET remote_order_creation_state = 'UNKNOWN' WHERE id = '" + fresh + "'");
                sql.execute("UPDATE payment_attempts SET provider_order_id = 'ORD-1', init_point = 'https://example.invalid', "
                        + "remote_order_creation_state = 'CREATED' WHERE id = '" + fresh + "'");
                try (var rows = sql.executeQuery("SELECT new_state FROM remote_order_creation_events WHERE payment_attempt_id = '" + fresh + "' ORDER BY id")) {
                    for (String expected : new String[]{"PREPARED", "UNKNOWN", "CREATED"}) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getString(1)).isEqualTo(expected);
                    }
                    assertThat(rows.next()).isFalse();
                }
                UUID rejected = UUID.randomUUID();
                sql.execute("INSERT INTO payment_attempts (id, checkout_mode, idempotency_key, remote_order_creation_state) "
                        + "VALUES ('" + rejected + "', 'ORDERS', 'rejected', 'UNKNOWN')");
                sql.execute("UPDATE payment_attempts SET remote_order_creation_state = 'REJECTED_CONFIRMED' WHERE id = '" + rejected + "'");
                try (var rows = sql.executeQuery("SELECT evidence_code FROM remote_order_creation_events WHERE payment_attempt_id = '" + rejected + "' ORDER BY id DESC LIMIT 1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("INVALID_TOTAL_AMOUNT");
                }
                UUID legacy = UUID.randomUUID();
                sql.execute("INSERT INTO payment_attempts (id, checkout_mode, idempotency_key) VALUES ('" + legacy + "', 'PREFERENCES', 'legacy')");
                try (var rows = sql.executeQuery("SELECT remote_order_creation_state FROM payment_attempts WHERE id = '" + legacy + "'")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isNull();
                }
                var savepoint = connection.setSavepoint();
                assertThatThrownBy(() -> sql.execute("UPDATE payment_attempts SET remote_order_creation_state = 'UNKNOWN' WHERE id = '" + fresh + "'"))
                        .isInstanceOf(java.sql.SQLException.class);
                connection.rollback(savepoint);
                savepoint = connection.setSavepoint();
                assertThatThrownBy(() -> sql.execute("UPDATE remote_order_creation_events SET evidence_code = 'tampered'"))
                        .isInstanceOf(java.sql.SQLException.class);
                connection.rollback(savepoint);
            } finally {
                connection.rollback(); // Includes schema and every fixture; never commits.
            }
        }
    }
}

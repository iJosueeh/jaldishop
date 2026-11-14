package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.*;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.domain.model.*;
import com.jaldishop.backend.identity.domain.enums.*;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.identity.infrastructure.security.PasswordResetTokens;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Requires the dedicated loopback database, validated before any Flyway or application startup. */
@SpringBootTest(properties = {"spring.config.import=", "app.password-reset.mail.mode=disabled",
        "app.payment.reconciliation.enabled=false", "payments.mercadopago.refund-scheduler-enabled=false"})
class PasswordRecoveryPostgresIntegrationTest {
    static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();
    static Map<UUID, String> passwordsBeforeMigration;
    static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", database::url);
        r.add("spring.datasource.username", database::username);
        r.add("spring.datasource.password", database::password);
        r.add("spring.flyway.url", database::url);
        r.add("spring.flyway.user", database::username);
        r.add("spring.flyway.password", database::password);
    }
    @BeforeAll static void migratePreviousVersion() {
        Flyway.configure().dataSource(database.url(), database.username(), database.password())
                .target("26").validateOnMigrate(database.validateOnMigrate()).load().migrate();
        passwordsBeforeMigration = new HashMap<>();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(database.url(), database.username(), database.password()));
        jdbc.query("SELECT id, password_encoded FROM users", rs -> {
            passwordsBeforeMigration.put(rs.getObject(1, UUID.class), rs.getString(2));
        });
    }
    @Autowired ForgotPasswordService forgot;
    @Autowired ResetPasswordService resetPassword;
    @Autowired AuthenticateUserService login;
    @Autowired RefreshTokenService refresh;
    @Autowired RegisterMerchantService registerMerchant;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @MockitoBean PasswordResetMail mail;
    @MockitoBean(name = "passwordRecoveryExecutor") ThreadPoolTaskExecutor executor;
    @MockitoBean(name = "passwordRecoveryClock") Clock clock;
    UUID id;
    String email;
    String token;
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(NOW);
        // Run jobs on each test caller thread; concurrent cases keep two real JDBC transactions.
        doAnswer(invocation -> { invocation.getArgument(0, Runnable.class).run(); return null; })
                .when(executor).execute(any(Runnable.class));
        email = "recovery-" + UUID.randomUUID() + "@example.test";
        // Use the seeded role ID, not an assumption about role sequence order.
        short customerId = jdbc.queryForObject("SELECT id FROM roles WHERE name='CUSTOMER'", Short.class);
        User user = User.create(email, encoder.encode("old-password"), "Recovery", "Test", null,
                Set.of(new Role(customerId, RoleName.CUSTOMER)));
        id = users.save(user).getId();
        doAnswer(invocation -> { token = invocation.getArgument(1); return null; }).when(mail).send(eq(email), any(), any());
        doAnswer(invocation -> { token = invocation.getArgument(1); return null; }).when(mail).send(eq(email), any(), any(), any(), any());
    }
    @AfterEach void cleanup() {
        if (id == null) return;
        jdbc.update("DELETE FROM store_category_assignments WHERE store_id IN (SELECT id FROM stores WHERE owner_user_id=?)", id);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?", id);
        jdbc.update("DELETE FROM stores WHERE owner_user_id=?", id);
        jdbc.update("DELETE FROM users WHERE id=?", id);
    }
    private void issueToken() {
        token = null;
        forgot.execute(new ForgotPasswordCommand(email));
        if (token == null) throw new AssertionError("Recovery mail was not captured");
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void fullFlowWorksForCustomerAndMerchantWithJwtRevocation(boolean merchant) {
        if (merchant) registerMerchant.execute(new RegisterMerchantCommand(email, "old-password", "Recovery", "Test",
                "987654321", "Recovery " + id, "Test", "987654321", "Av Test 1", true, false));
        var before = login.execute(new LoginCommand(email, "old-password"));
        issueToken();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        String hash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id=?", String.class, id);
        assertThat(hash).isEqualTo(new PasswordResetTokens().hash(token)).isNotEqualTo(token);
        resetPassword.execute(new ResetPasswordCommand(token, "new-password"));
        assertThatThrownBy(() -> login.execute(new LoginCommand(email, "old-password"))).isInstanceOf(BadCredentialsException.class);
        var after = login.execute(new LoginCommand(email, "new-password"));
        assertThat(after.roles()).contains("CUSTOMER");
        if (merchant) assertThat(after.roles()).contains("MERCHANT");
        assertThat(jwt.getPrincipal(after.token()).credentialsVersion()).isEqualTo(1);
        assertThatThrownBy(() -> refresh.execute(id, jwt.getPrincipal(before.token()).credentialsVersion()))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(token, "another-password")))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens WHERE user_id=?", Integer.class, id)).isZero();
    }
    @Test void expiresExactlyAtFifteenMinutesAndUnknownTokensHaveSameError() {
        issueToken();
        when(clock.instant()).thenReturn(NOW.plusSeconds(900));
        assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(token, "new-password")))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(new PasswordResetTokens().generate(), "new-password")))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        assertThat(login.execute(new LoginCommand(email, "old-password"))).isNotNull();
    }
    @Test void newRequestReplacesPreviousToken() {
        issueToken(); String previous = token;
        issueToken(); String latest = token;
        assertThat(latest).isNotEqualTo(previous);
        assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(previous, "new-password"))).isInstanceOf(RuntimeException.class);
        resetPassword.execute(new ResetPasswordCommand(latest, "new-password"));
    }
    @Test void concurrentResetsHaveExactlyOneWinner() throws Exception {
        issueToken();
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                ready.countDown(); assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                try { resetPassword.execute(new ResetPasswordCommand(token, "new-password")); return true; }
                catch (com.jaldishop.backend.shared.exception.BusinessRuleException invalid) {
                    assertThat(invalid.getCode()).isEqualTo("INVALID_PASSWORD_RESET_TOKEN"); return false;
                }
            };
            var a = pool.submit(attempt); var b = pool.submit(attempt);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(users.findById(id).orElseThrow().getCredentialsVersion()).isEqualTo(1);
    }
    @Test void concurrentRequestsLeaveOnlyOneUsableToken() throws Exception {
        var delivered = new java.util.concurrent.CopyOnWriteArrayList<String>();
        doAnswer(invocation -> { delivered.add(invocation.getArgument(1)); return null; })
                .when(mail).send(eq(email), any(), any());
        doAnswer(invocation -> { delivered.add(invocation.getArgument(1)); return null; })
                .when(mail).send(eq(email), any(), any(), any(), any());
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Void> issue = () -> {
                ready.countDown(); assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                forgot.execute(new ForgotPasswordCommand(email)); return null;
            };
            var a = pool.submit(issue); var b = pool.submit(issue);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        }
        assertThat(delivered).hasSize(2);
        String currentHash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id=?", String.class, id);
        String valid = delivered.stream().filter(value -> new PasswordResetTokens().hash(value).equals(currentHash))
                .findFirst().orElseThrow();
        String invalid = delivered.stream().filter(value -> !value.equals(valid)).findFirst().orElseThrow();
        assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(invalid, "new-password")))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        resetPassword.execute(new ResetPasswordCommand(valid, "new-password"));
    }

    @Test void failedCredentialUpdateRollsBackTokenConsumption() {
        issueToken();
        jdbc.execute("""
                CREATE FUNCTION password_recovery_test_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'test failure'; END $$
                """);
        jdbc.execute("CREATE TRIGGER password_recovery_test_failure BEFORE UPDATE OF password_encoded ON users "
                + "FOR EACH ROW WHEN (NEW.id = '" + id + "'::uuid) EXECUTE FUNCTION password_recovery_test_failure()");
        try { assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(token, "new-password"))).isInstanceOf(RuntimeException.class); }
        finally {
            jdbc.execute("DROP TRIGGER password_recovery_test_failure ON users");
            jdbc.execute("DROP FUNCTION password_recovery_test_failure()");
        }
        resetPassword.execute(new ResetPasswordCommand(token, "new-password"));
        assertThat(login.execute(new LoginCommand(email, "new-password"))).isNotNull();
    }
    @Test void staleProfileSaveCannotRestorePasswordOrRevokedSessions() {
        User stale = users.findById(id).orElseThrow();
        issueToken(); resetPassword.execute(new ResetPasswordCommand(token, "new-password"));
        stale.updateProfile("New", "Name", null); users.save(stale);
        assertThat(users.findById(id).orElseThrow().getCredentialsVersion()).isEqualTo(1);
        assertThat(login.execute(new LoginCommand(email, "new-password"))).isNotNull();
    }
    @Test void additiveMigrationPreservesAllPreexistingCredentials() {
        flyway.validate();
        passwordsBeforeMigration.forEach((key, password) -> assertThat(jdbc.queryForObject(
                "SELECT password_encoded FROM users WHERE id=?", String.class, key)).isEqualTo(password));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='27' AND success", Integer.class)).isEqualTo(1);
    }
}

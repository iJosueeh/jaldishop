package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.AcceptAdminInvitationCommand;
import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.AdminInvitationTokens;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.config.import=",
        "app.password-reset.mail.mode=disabled",
        "app.payment.reconciliation.enabled=false",
        "payments.mercadopago.refund-scheduler-enabled=false"
})
@Import({
        AdminInvitationConcurrencyPostgresIntegrationTest.SafeTestDatabase.class,
        AdminInvitationConcurrencyPostgresIntegrationTest.AuditRollbackHelper.class
})
class AdminInvitationConcurrencyPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
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

    @Component
    static class AuditRollbackHelper {
        @Autowired
        private AdminAuditService auditService;

        @Transactional
        public void executeWithRollback(UUID actorId, String action) {
            auditService.logSuccess(
                    actorId,
                    "admin@jaldi.test",
                    action,
                    "USER",
                    UUID.randomUUID(),
                    "127.0.0.1",
                    "Details test"
            );
            throw new RuntimeException("Simulated business error for transactional rollback");
        }

        @Transactional
        public void executeWithCommit(UUID actorId, String action) {
            auditService.logSuccess(
                    actorId,
                    "admin@jaldi.test",
                    action,
                    "USER",
                    UUID.randomUUID(),
                    "127.0.0.1",
                    "Details test"
            );
        }
    }

    @Autowired
    private AcceptAdminInvitationService acceptAdminInvitationService;

    @Autowired
    private AdminInvitationTokens tokenService;

    @Autowired
    private AdminInvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuditRollbackHelper auditRollbackHelper;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM admin_audit_logs WHERE action LIKE 'TEST_%'");
        jdbcTemplate.update("DELETE FROM admin_invitations WHERE email = 'concurrent-admin@jaldi.test'");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email = 'concurrent-admin@jaldi.test')");
        jdbcTemplate.update("DELETE FROM users WHERE email = 'concurrent-admin@jaldi.test'");
    }

    @Test
    @DisplayName("Concurrencia real: Dos transacciones concurrentes compitiendo por la misma invitación")
    void concurrentAcceptanceProducesExactlyOneSuccessAndOneConflictException() throws Exception {
        String rawToken = "concurrent-token-" + UUID.randomUUID();
        String tokenHash = tokenService.hash(rawToken);

        // Find an existing admin to act as invited_by
        UUID inviterId = jdbcTemplate.queryForObject(
                "SELECT id FROM users LIMIT 1",
                UUID.class
        );

        AdminInvitation invitation = AdminInvitation.create(
                "concurrent-admin@jaldi.test",
                tokenHash,
                "Concurrent",
                "Admin",
                "987654321",
                inviterId,
                Instant.now().plusSeconds(3600)
        );
        invitationRepository.save(invitation);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand(
                            rawToken,
                            "NewStrongP@ss123!"
                    );
                    User user = acceptAdminInvitationService.execute(command);
                    if (user != null) {
                        successCount.incrementAndGet();
                    }
                } catch (ConflictException ex) {
                    conflictCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Release both threads simultaneously
        startGate.countDown();
        boolean completed = doneGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Verify database state: exactly 1 accepted invitation and 1 user created
        Integer acceptedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_invitations WHERE email = 'concurrent-admin@jaldi.test' AND status = 'ACCEPTED'",
                Integer.class
        );
        assertThat(acceptedCount).isEqualTo(1);

        Integer usersCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'concurrent-admin@jaldi.test'",
                Integer.class
        );
        assertThat(usersCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Consistencia transaccional: logSuccess se revierte cuando la transacción hace rollback")
    void auditSuccessDoesNotPersistWhenTransactionRollsBack() {
        UUID actorId = jdbcTemplate.queryForObject(
                "SELECT id FROM users LIMIT 1",
                UUID.class
        );
        String rollbackAction = "TEST_ROLLBACK_ACTION";
        String commitAction = "TEST_COMMIT_ACTION";

        // 1. Transaction that fails and rolls back
        assertThrows(RuntimeException.class, () ->
                auditRollbackHelper.executeWithRollback(actorId, rollbackAction)
        );

        Integer rollbackLogsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_logs WHERE action = ?",
                Integer.class,
                rollbackAction
        );
        // CRITICAL PROOF: 0 rows persisted because MANDATORY joins transaction and rolls back
        assertThat(rollbackLogsCount).isEqualTo(0);

        // 2. Transaction that succeeds
        auditRollbackHelper.executeWithCommit(actorId, commitAction);

        Integer commitLogsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_logs WHERE action = ?",
                Integer.class,
                commitAction
        );
        assertThat(commitLogsCount).isEqualTo(1);
    }
}

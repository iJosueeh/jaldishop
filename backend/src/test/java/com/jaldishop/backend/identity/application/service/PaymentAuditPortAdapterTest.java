package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminAuditLogRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.payment.domain.port.PaymentAuditPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAuditPortAdapterTest {

    @Mock
    private AdminAuditService auditService;

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentAuditPortAdapter adapter;
    private UUID adminId;
    private UUID attemptId;

    @BeforeEach
    void setUp() {
        adapter = new PaymentAuditPortAdapter(auditService, auditLogRepository, userRepository, new ObjectMapper());
        adminId = UUID.randomUUID();
        attemptId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Registra investigación de pago como PAYMENT_INVESTIGATED con JSON estructurado")
    void recordInvestigationLogsSuccess() {
        User admin = User.reconstitute(adminId, "admin@jaldi.com", "hash", "Admin", "User", null, com.jaldishop.backend.identity.domain.enums.UserStatus.ACTIVE, java.util.Set.of(), java.time.Instant.now(), java.time.Instant.now(), true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(auditLogRepository.findLatestByActorActionAndTarget(adminId, "PAYMENT_INVESTIGATED", attemptId))
                .thenReturn(Optional.empty());

        adapter.recordInvestigation(adminId, attemptId, "VERIFIED", "REMOTE_PAID", "127.0.0.1");

        verify(auditLogRepository).acquireTransactionLock(adminId + ":PAYMENT_INVESTIGATED:" + attemptId);
        verify(auditService).logSuccess(
                eq(adminId),
                eq("admin@jaldi.com"),
                eq("PAYMENT_INVESTIGATED"),
                eq("PAYMENT_ATTEMPT"),
                eq(attemptId),
                eq("127.0.0.1"),
                contains("REMOTE_PAID")
        );
    }

    @Test
    @DisplayName("Deduplica investigación idéntica dentro del cooldown de 5 minutos")
    void skipDuplicateInvestigationWithinCooldown() {
        AdminAuditLog recent = AdminAuditLog.record(
                adminId, "admin@jaldi.com", "PAYMENT_INVESTIGATED", "PAYMENT_ATTEMPT",
                attemptId, AdminAuditStatus.SUCCESS, "127.0.0.1",
                "{\"outcome\":\"VERIFIED\",\"reason\":\"REMOTE_PAID\",\"reconciled\":false}"
        );
        when(auditLogRepository.findLatestByActorActionAndTarget(adminId, "PAYMENT_INVESTIGATED", attemptId))
                .thenReturn(Optional.of(recent));

        adapter.recordInvestigation(adminId, attemptId, "VERIFIED", "REMOTE_PAID", "127.0.0.1");

        verify(auditLogRepository).acquireTransactionLock(adminId + ":PAYMENT_INVESTIGATED:" + attemptId);
        verify(auditService, never()).logSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Registra nueva investigación si cambia el outcome aunque esté dentro del cooldown")
    void recordInvestigationWhenOutcomeChanges() {
        User admin = User.reconstitute(adminId, "admin@jaldi.com", "hash", "Admin", "User", null, com.jaldishop.backend.identity.domain.enums.UserStatus.ACTIVE, java.util.Set.of(), java.time.Instant.now(), java.time.Instant.now(), true);
        AdminAuditLog recent = AdminAuditLog.record(
                adminId, "admin@jaldi.com", "PAYMENT_INVESTIGATED", "PAYMENT_ATTEMPT",
                attemptId, AdminAuditStatus.SUCCESS, "127.0.0.1",
                "{\"outcome\":\"INCONCLUSIVE\",\"reason\":\"PENDING\",\"reconciled\":false}"
        );
        when(auditLogRepository.findLatestByActorActionAndTarget(adminId, "PAYMENT_INVESTIGATED", attemptId))
                .thenReturn(Optional.of(recent));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        adapter.recordInvestigation(adminId, attemptId, "VERIFIED", "REMOTE_PAID", "127.0.0.1");

        verify(auditService).logSuccess(
                eq(adminId), eq("admin@jaldi.com"), eq("PAYMENT_INVESTIGATED"),
                eq("PAYMENT_ATTEMPT"), eq(attemptId), eq("127.0.0.1"), contains("VERIFIED")
        );
    }

    @Test
    @DisplayName("Lee y parsea historial reciente de PAYMENT_INVESTIGATED y PAYMENT_RECONCILED")
    void findRecentHistoryParsesJsonDetails() {
        AdminAuditLog log = AdminAuditLog.record(
                adminId,
                "admin@jaldi.com",
                "PAYMENT_RECONCILED",
                "PAYMENT_ATTEMPT",
                attemptId,
                AdminAuditStatus.SUCCESS,
                "127.0.0.1",
                "{\"outcome\":\"RECONCILED\",\"reason\":\"ORDER_CREATED\",\"reconciled\":true,\"orderNumber\":\"ORD-100\"}"
        );
        when(auditLogRepository.findRecentByActions(List.of("PAYMENT_INVESTIGATED", "PAYMENT_RECONCILED"), 10))
                .thenReturn(List.of(log));

        List<PaymentAuditPort.HistoryItem> items = adapter.findRecentHistory(10);

        assertEquals(1, items.size());
        assertEquals(attemptId, items.getFirst().attemptId());
        assertEquals("RECONCILED", items.getFirst().outcome());
        assertEquals("ORDER_CREATED", items.getFirst().reason());
        assertTrue(items.getFirst().reconciled());
        assertEquals("ORD-100", items.getFirst().orderNumber());
    }
}

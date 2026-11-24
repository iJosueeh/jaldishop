package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminAuditLogRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.payment.domain.port.PaymentAuditPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentAuditPortAdapter implements PaymentAuditPort {

    private static final String ACTION_INVESTIGATED = "PAYMENT_INVESTIGATED";
    private static final String ACTION_RECONCILED = "PAYMENT_RECONCILED";
    private static final String TARGET_TYPE = "PAYMENT_ATTEMPT";
    private static final Duration INVESTIGATION_COOLDOWN = Duration.ofMinutes(5);
    private static final Duration REJECTED_RECONCILIATION_COOLDOWN = Duration.ofMinutes(1);

    private final AdminAuditService auditService;
    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PaymentAuditPortAdapter(
            AdminAuditService auditService,
            AdminAuditLogRepository auditLogRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordInvestigation(UUID actorId, UUID attemptId, String outcome, String reason, String ipAddress) {
        if (isDuplicateWithinCooldown(actorId, ACTION_INVESTIGATED, attemptId, outcome, reason, INVESTIGATION_COOLDOWN)) {
            return;
        }
        String actorEmail = resolveActorEmail(actorId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("outcome", outcome);
        payload.put("reason", reason);
        payload.put("reconciled", false);
        auditService.logSuccess(
                actorId,
                actorEmail,
                ACTION_INVESTIGATED,
                TARGET_TYPE,
                attemptId,
                ipAddress,
                serialize(payload)
        );
    }

    @Override
    public void recordReconciliation(
            UUID actorId,
            UUID attemptId,
            String outcome,
            String reason,
            boolean reconciled,
            String orderNumber,
            String ipAddress
    ) {
        if (!reconciled && isDuplicateWithinCooldown(
                actorId, ACTION_RECONCILED, attemptId, outcome, reason, REJECTED_RECONCILIATION_COOLDOWN)) {
            return;
        }
        String actorEmail = resolveActorEmail(actorId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("outcome", outcome);
        payload.put("reason", reason);
        payload.put("reconciled", reconciled);
        if (orderNumber != null) {
            payload.put("orderNumber", orderNumber);
        }
        auditService.logSuccess(
                actorId,
                actorEmail,
                ACTION_RECONCILED,
                TARGET_TYPE,
                attemptId,
                ipAddress,
                serialize(payload)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryItem> findRecentHistory(int limit) {
        return auditLogRepository
                .findRecentByActions(List.of(ACTION_INVESTIGATED, ACTION_RECONCILED), limit)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    private boolean isDuplicateWithinCooldown(
            UUID actorId,
            String action,
            UUID attemptId,
            String outcome,
            String reason,
            Duration cooldown
    ) {
        if (actorId == null || attemptId == null) {
            return false;
        }
        auditLogRepository.acquireTransactionLock(actorId + ":" + action + ":" + attemptId);
        Optional<AdminAuditLog> latestOpt =
                auditLogRepository.findLatestByActorActionAndTarget(actorId, action, attemptId);
        if (latestOpt.isEmpty()) {
            return false;
        }
        AdminAuditLog latest = latestOpt.get();
        if (latest.getCreatedAt() == null || latest.getCreatedAt().isBefore(Instant.now().minus(cooldown))) {
            return false;
        }
        HistoryItem parsed = toHistoryItem(latest);
        return Objects.equals(parsed.outcome(), outcome) && Objects.equals(parsed.reason(), reason);
    }

    private String resolveActorEmail(UUID actorId) {
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).map(User::getEmail).orElse(null);
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload.getOrDefault("outcome", "UNKNOWN"));
        }
    }

    private HistoryItem toHistoryItem(AdminAuditLog log) {
        String outcome = log.getAction();
        String reason = "";
        boolean reconciled = ACTION_RECONCILED.equals(log.getAction());
        String orderNumber = null;

        if (log.getDetails() != null && log.getDetails().startsWith("{")) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        log.getDetails(),
                        new TypeReference<Map<String, Object>>() {}
                );
                if (parsed.get("outcome") != null) {
                    outcome = String.valueOf(parsed.get("outcome"));
                }
                if (parsed.get("reason") != null) {
                    reason = String.valueOf(parsed.get("reason"));
                }
                if (parsed.get("reconciled") instanceof Boolean b) {
                    reconciled = b;
                }
                if (parsed.get("orderNumber") != null) {
                    orderNumber = String.valueOf(parsed.get("orderNumber"));
                }
            } catch (Exception ignored) {
                reason = log.getDetails();
            }
        } else if (log.getDetails() != null) {
            reason = log.getDetails();
        }

        return new HistoryItem(
                log.getTargetId(),
                log.getAction(),
                outcome,
                reason,
                reconciled,
                orderNumber,
                log.getActorEmail(),
                log.getCreatedAt()
        );
    }
}

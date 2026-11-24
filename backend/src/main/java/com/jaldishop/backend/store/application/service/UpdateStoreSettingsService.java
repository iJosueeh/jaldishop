package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.shared.security.TextSanitizer;
import com.jaldishop.backend.store.application.port.in.UpdateStoreSettingsUseCase;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UpdateStoreSettingsService implements UpdateStoreSettingsUseCase {

    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final AdminAuditService auditService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UpdateStoreSettingsService(StoreRepository storeRepository, StoreSettingsRepository storeSettingsRepository) {
        this(storeRepository, storeSettingsRepository, null, null, null);
    }

    @Autowired
    public UpdateStoreSettingsService(
            StoreRepository storeRepository,
            StoreSettingsRepository storeSettingsRepository,
            AdminAuditService auditService,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public StoreSettingsResponse execute(UUID userId, StoreSettingsRequest request) {
        return execute(userId, request, null);
    }

    @Override
    public StoreSettingsResponse execute(UUID userId, StoreSettingsRequest request, String ipAddress) {
        Store store = storeRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una tienda asociada a este usuario"));

        Optional<StoreSettings> existingOpt = storeSettingsRepository.findByStoreId(store.getId());
        boolean persisted = existingOpt.isPresent();
        StoreSettings existing = existingOpt.orElseGet(() -> StoreSettings.defaultSettings(store.getId()));

        if (persisted && (request.version() == null || !request.version().equals(existing.getVersion()))) {
            throw new OptimisticLockingFailureException("La configuración de la tienda fue modificada por otro usuario o falta la versión.");
        }

        String sanitizedCancellation = TextSanitizer.sanitizePlainText(request.cancellationPolicy());
        String sanitizedRefund = TextSanitizer.sanitizePlainText(request.refundPolicy());
        if (sanitizedCancellation == null || sanitizedCancellation.isBlank()) {
            throw new BusinessRuleException("INVALID_CANCELLATION_POLICY", "La política de cancelación no puede estar vacía.");
        }
        if (sanitizedRefund == null || sanitizedRefund.isBlank()) {
            throw new BusinessRuleException("INVALID_REFUND_POLICY", "La política de reembolso no puede estar vacía.");
        }

        StoreSettings updated = new StoreSettings(
                store.getId(),
                sanitizedCancellation,
                sanitizedRefund,
                request.requireCustomerDni(),
                request.allowOrderNotes(),
                request.onlinePaymentsEnabled(),
                request.cashOnDeliveryEnabled(),
                request.acceptsPosOnDelivery(),
                request.minDeliveryAmount(),
                request.estimatedPrepTimeMinutes(),
                request.orderSoundAlert(),
                request.soundVolume(),
                request.lowStockVisualAlert(),
                persisted ? request.version() : 0L,
                existing.getCreatedAt() != null ? existing.getCreatedAt() : Instant.now(),
                Instant.now()
        );

        StoreSettings saved = storeSettingsRepository.save(updated);

        if (auditService != null) {
            String actorEmail = (userRepository != null && userId != null)
                    ? userRepository.findById(userId).map(User::getEmail).orElse(null)
                    : null;
            auditService.logSuccess(
                    userId,
                    actorEmail,
                    "UPDATE_STORE_SETTINGS",
                    "STORE_SETTINGS",
                    store.getId(),
                    ipAddress,
                    buildAuditDetails(existing, saved)
            );
        }

        return StoreSettingsResponse.fromDomain(saved);
    }

    private String buildAuditDetails(StoreSettings before, StoreSettings after) {
        try {
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("before", Map.of(
                    "onlinePaymentsEnabled", before.isOnlinePaymentsEnabled(),
                    "cashOnDeliveryEnabled", before.isCashOnDeliveryEnabled(),
                    "acceptsPosOnDelivery", before.isAcceptsPosOnDelivery(),
                    "minDeliveryAmount", before.getMinDeliveryAmount() != null ? before.getMinDeliveryAmount() : "null",
                    "requireCustomerDni", before.isRequireCustomerDni(),
                    "allowOrderNotes", before.isAllowOrderNotes(),
                    "estimatedPrepTimeMinutes", before.getEstimatedPrepTimeMinutes(),
                    "version", before.getVersion() != null ? before.getVersion() : 0L
            ));
            diff.put("after", Map.of(
                    "onlinePaymentsEnabled", after.isOnlinePaymentsEnabled(),
                    "cashOnDeliveryEnabled", after.isCashOnDeliveryEnabled(),
                    "acceptsPosOnDelivery", after.isAcceptsPosOnDelivery(),
                    "minDeliveryAmount", after.getMinDeliveryAmount() != null ? after.getMinDeliveryAmount() : "null",
                    "requireCustomerDni", after.isRequireCustomerDni(),
                    "allowOrderNotes", after.isAllowOrderNotes(),
                    "estimatedPrepTimeMinutes", after.getEstimatedPrepTimeMinutes(),
                    "version", after.getVersion() != null ? after.getVersion() : 0L
            ));
            return objectMapper != null ? objectMapper.writeValueAsString(diff) : diff.toString();
        } catch (Exception e) {
            return "{\"storeId\":\"" + after.getStoreId() + "\",\"version\":" + after.getVersion() + "}";
        }
    }
}

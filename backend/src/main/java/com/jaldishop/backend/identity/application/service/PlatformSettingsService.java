package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.UpdatePlatformSettingsCommand;
import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.PlatformSettingsRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.security.TextSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlatformSettingsService {

    private final PlatformSettingsRepository repository;
    private final PlatformSettingsProvider provider;
    private final AdminAuditService auditService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlatformSettingsService(PlatformSettingsRepository repository,
                                   PlatformSettingsProvider provider,
                                   AdminAuditService auditService,
                                   UserRepository userRepository,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.provider = provider;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public PlatformSettingsService(PlatformSettingsRepository repository,
                                   PlatformSettingsProvider provider,
                                   AdminAuditService auditService) {
        this(repository, provider, auditService, null, null);
    }

    @Transactional(readOnly = true)
    public PlatformSettings getSettings() {
        return repository.findSingleton()
                .orElseGet(provider::getSettings);
    }

    @Transactional
    public PlatformSettings updateSettings(UpdatePlatformSettingsCommand command, UUID actorId, String actorEmail, String ipAddress) {
        Optional<PlatformSettings> existingOpt = repository.findSingleton();
        boolean persisted = existingOpt.isPresent();
        PlatformSettings beforeDomain = existingOpt.orElseGet(PlatformSettings::defaultSettings);

        if (persisted && (command.version() == null || !command.version().equals(beforeDomain.version()))) {
            throw new OptimisticLockingFailureException("La configuración de la plataforma fue modificada por otro usuario o falta la versión.");
        }

        String sanitizedNotice = beforeDomain.maintenanceNotice();
        if (command.maintenanceNotice() != null) {
            sanitizedNotice = TextSanitizer.sanitizePlainText(command.maintenanceNotice());
        }
        boolean maintenanceMode = Boolean.TRUE.equals(command.maintenanceMode());
        if (maintenanceMode && (sanitizedNotice == null || sanitizedNotice.isBlank())) {
            throw new BusinessRuleException("MAINTENANCE_NOTICE_REQUIRED", "El aviso de mantenimiento no puede estar vacío cuando el modo mantenimiento está activo.");
        }

        BigDecimal commissionRate = command.marketplaceCommissionRate() != null
                ? command.marketplaceCommissionRate()
                : beforeDomain.marketplaceCommissionRate();

        BigDecimal customerServiceFeeRate = command.customerServiceFeeRate() != null
                ? command.customerServiceFeeRate()
                : (beforeDomain.customerServiceFeeRate() != null ? beforeDomain.customerServiceFeeRate() : BigDecimal.ZERO);

        BigDecimal customerServiceFeeCap = command.customerServiceFeeCap();

        PlatformSettings toSave = new PlatformSettings(
                1,
                Boolean.TRUE.equals(command.allowMerchantRegistration()),
                Boolean.TRUE.equals(command.requireStoreApproval()),
                maintenanceMode,
                sanitizedNotice,
                commissionRate,
                customerServiceFeeRate,
                customerServiceFeeCap,
                persisted ? command.version() : 0L,
                beforeDomain.createdAt(),
                Instant.now()
        );

        PlatformSettings domain = repository.save(toSave);
        provider.updateSettingsCache(domain);

        String resolvedEmail = actorEmail;
        if ((resolvedEmail == null || (actorId != null && resolvedEmail.equals(actorId.toString()))) && userRepository != null && actorId != null) {
            resolvedEmail = userRepository.findById(actorId).map(User::getEmail).orElse(resolvedEmail);
        }

        String auditDetails = buildAuditDetails(beforeDomain, domain);

        auditService.logSuccess(
                actorId,
                resolvedEmail,
                "UPDATE_PLATFORM_SETTINGS",
                "PLATFORM_SETTINGS",
                null,
                ipAddress,
                auditDetails
        );

        return domain;
    }

    private String buildAuditDetails(PlatformSettings beforeDomain, PlatformSettings domain) {
        try {
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("before", Map.of(
                    "maintenanceMode", beforeDomain.maintenanceMode(),
                    "maintenanceNotice", beforeDomain.maintenanceNotice() != null ? beforeDomain.maintenanceNotice() : "",
                    "allowMerchantRegistration", beforeDomain.allowMerchantRegistration(),
                    "requireStoreApproval", beforeDomain.requireStoreApproval(),
                    "commissionRate", beforeDomain.marketplaceCommissionRate(),
                    "version", beforeDomain.version() != null ? beforeDomain.version() : 0L
            ));
            diff.put("after", Map.of(
                    "maintenanceMode", domain.maintenanceMode(),
                    "maintenanceNotice", domain.maintenanceNotice() != null ? domain.maintenanceNotice() : "",
                    "allowMerchantRegistration", domain.allowMerchantRegistration(),
                    "requireStoreApproval", domain.requireStoreApproval(),
                    "commissionRate", domain.marketplaceCommissionRate(),
                    "version", domain.version() != null ? domain.version() : 0L
            ));
            return objectMapper != null ? objectMapper.writeValueAsString(diff) : diff.toString();
        } catch (Exception e) {
            return "{\"maintenanceMode\":" + domain.maintenanceMode() +
                    ",\"allowMerchantRegistration\":" + domain.allowMerchantRegistration() +
                    ",\"requireStoreApproval\":" + domain.requireStoreApproval() +
                    ",\"commissionRate\":" + domain.marketplaceCommissionRate() +
                    ",\"version\":" + domain.version() + "}";
        }
    }
}

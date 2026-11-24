package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.UpdatePlatformSettingsCommand;
import com.jaldishop.backend.identity.application.service.PlatformSettingsService;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.web.dto.PlatformSettingsResponse;
import com.jaldishop.backend.identity.web.dto.UpdatePlatformSettingsRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.security.ratelimit.AuthenticatedRateLimits;
import com.jaldishop.backend.shared.security.ratelimit.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('ADMIN') and principal.primaryAdmin")
@Validated
public class AdminPlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;
    private final AuthenticatedRateLimits rateLimits;
    private final ClientIpResolver clientIpResolver;

    @Autowired
    public AdminPlatformSettingsController(PlatformSettingsService platformSettingsService,
                                           AuthenticatedRateLimits rateLimits,
                                           ClientIpResolver clientIpResolver) {
        this.platformSettingsService = platformSettingsService;
        this.rateLimits = rateLimits;
        this.clientIpResolver = clientIpResolver;
    }

    public AdminPlatformSettingsController(PlatformSettingsService platformSettingsService) {
        this(platformSettingsService, null, null);
    }

    @GetMapping
    public ResponseEntity<PlatformSettingsResponse> getSettings() {
        PlatformSettings settings = platformSettingsService.getSettings();
        return ResponseEntity.ok(toResponse(settings));
    }

    @PutMapping
    public ResponseEntity<PlatformSettingsResponse> updateSettings(
            @Valid @RequestBody UpdatePlatformSettingsRequest request,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        if (rateLimits != null && principal != null && principal.userId() != null) {
            rateLimits.adminSettingsUpdate(principal.userId());
        }
        String ipAddress = (clientIpResolver != null)
                ? clientIpResolver.resolveOrRemoteAddr(servletRequest)
                : (servletRequest != null ? servletRequest.getRemoteAddr() : "127.0.0.1");

        UpdatePlatformSettingsCommand command = new UpdatePlatformSettingsCommand(
                request.allowMerchantRegistration(),
                request.requireStoreApproval(),
                request.maintenanceMode(),
                request.maintenanceNotice(),
                request.marketplaceCommissionRate(),
                request.customerServiceFeeRate(),
                request.customerServiceFeeCap(),
                request.version()
        );

        PlatformSettings updated = platformSettingsService.updateSettings(
                command,
                principal != null ? principal.userId() : null,
                null,
                ipAddress
        );
        return ResponseEntity.ok(toResponse(updated));
    }

    private PlatformSettingsResponse toResponse(PlatformSettings settings) {
        return new PlatformSettingsResponse(
                settings.allowMerchantRegistration(),
                settings.requireStoreApproval(),
                settings.maintenanceMode(),
                settings.maintenanceNotice(),
                settings.marketplaceCommissionRate(),
                settings.customerServiceFeeRate() != null ? settings.customerServiceFeeRate() : java.math.BigDecimal.ZERO,
                settings.customerServiceFeeCap(),
                settings.version(),
                settings.updatedAt()
        );
    }
}

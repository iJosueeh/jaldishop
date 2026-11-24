package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.security.ratelimit.AuthenticatedRateLimits;
import com.jaldishop.backend.shared.security.ratelimit.ClientIpResolver;
import com.jaldishop.backend.store.application.port.in.GetStoreSettingsUseCase;
import com.jaldishop.backend.store.application.port.in.UpdateStoreSettingsUseCase;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores/me/settings")
@PreAuthorize("hasRole('MERCHANT')")
public class StoreSettingsController {

    private final GetStoreSettingsUseCase getStoreSettingsUseCase;
    private final UpdateStoreSettingsUseCase updateStoreSettingsUseCase;
    private final AuthenticatedRateLimits rateLimits;
    private final ClientIpResolver clientIpResolver;

    @Autowired
    public StoreSettingsController(
            GetStoreSettingsUseCase getStoreSettingsUseCase,
            UpdateStoreSettingsUseCase updateStoreSettingsUseCase,
            AuthenticatedRateLimits rateLimits,
            ClientIpResolver clientIpResolver
    ) {
        this.getStoreSettingsUseCase = getStoreSettingsUseCase;
        this.updateStoreSettingsUseCase = updateStoreSettingsUseCase;
        this.rateLimits = rateLimits;
        this.clientIpResolver = clientIpResolver;
    }

    public StoreSettingsController(
            GetStoreSettingsUseCase getStoreSettingsUseCase,
            UpdateStoreSettingsUseCase updateStoreSettingsUseCase
    ) {
        this(getStoreSettingsUseCase, updateStoreSettingsUseCase, null, null);
    }

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> getMySettings(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return ResponseEntity.ok(getStoreSettingsUseCase.execute(principal.userId()));
    }

    @PutMapping
    public ResponseEntity<StoreSettingsResponse> updateMySettings(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody StoreSettingsRequest request,
            HttpServletRequest servletRequest
    ) {
        if (rateLimits != null && principal != null && principal.userId() != null) {
            rateLimits.storeSettingsUpdate(principal.userId());
        }
        String ipAddress = (clientIpResolver != null)
                ? clientIpResolver.resolveOrRemoteAddr(servletRequest)
                : (servletRequest != null ? servletRequest.getRemoteAddr() : null);

        return ResponseEntity.ok(updateStoreSettingsUseCase.execute(principal.userId(), request, ipAddress));
    }

    public ResponseEntity<StoreSettingsResponse> updateMySettings(
            JwtPrincipal principal,
            StoreSettingsRequest request
    ) {
        return updateMySettings(principal, request, null);
    }
}

package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.port.in.GetStoreSettingsUseCase;
import com.jaldishop.backend.store.application.port.in.UpdateStoreSettingsUseCase;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores/me/settings")
public class StoreSettingsController {

    private final GetStoreSettingsUseCase getStoreSettingsUseCase;
    private final UpdateStoreSettingsUseCase updateStoreSettingsUseCase;

    public StoreSettingsController(
            GetStoreSettingsUseCase getStoreSettingsUseCase,
            UpdateStoreSettingsUseCase updateStoreSettingsUseCase
    ) {
        this.getStoreSettingsUseCase = getStoreSettingsUseCase;
        this.updateStoreSettingsUseCase = updateStoreSettingsUseCase;
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
            @Valid @RequestBody StoreSettingsRequest request
    ) {
        return ResponseEntity.ok(updateStoreSettingsUseCase.execute(principal.userId(), request));
    }
}

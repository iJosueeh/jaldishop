package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.GetMyProfileService;
import com.jaldishop.backend.identity.application.command.UpdateProfileCommand;
import com.jaldishop.backend.identity.application.service.UpdateProfileService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.UpdateUserRequest;
import com.jaldishop.backend.identity.web.dto.UserProfileResponse;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetMyProfileService getMyProfileService;
    private final UpdateProfileService updateProfileService;

    @Autowired(required = false)
    private StoreRepository storeRepository;

    public UserController(GetMyProfileService getMyProfileService, UpdateProfileService updateProfileService) {
        this.getMyProfileService = getMyProfileService;
        this.updateProfileService = updateProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        User user = getMyProfileService.execute(principal.userId());
        return ResponseEntity.ok(UserProfileResponse.fromDomain(user, resolveOwnedStoreId(user.getId())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UpdateProfileCommand command = new UpdateProfileCommand(
                principal.userId(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );

        User user = updateProfileService.execute(command);
        return ResponseEntity.ok(UserProfileResponse.fromDomain(user, resolveOwnedStoreId(user.getId())));
    }

    private UUID resolveOwnedStoreId(UUID userId) {
        if (storeRepository == null || userId == null) {
            return null;
        }
        return storeRepository.findByOwnerUserId(userId)
                .map(Store::getId)
                .orElse(null);
    }

}

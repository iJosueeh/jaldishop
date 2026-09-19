package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.GetMyProfileService;
import com.jaldishop.backend.identity.application.UpdateProfileCommand;
import com.jaldishop.backend.identity.application.UpdateProfileService;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.UpdateUserRequest;
import com.jaldishop.backend.identity.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetMyProfileService getMyProfileService;
    private final UpdateProfileService updateProfileService;

    public UserController(GetMyProfileService getMyProfileService, UpdateProfileService updateProfileService) {
        this.getMyProfileService = getMyProfileService;
        this.updateProfileService = updateProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        User user = getMyProfileService.execute(principal.userId());
        return ResponseEntity.ok(UserProfileResponse.fromDomain(user));
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
        return ResponseEntity.ok(UserProfileResponse.fromDomain(user));
    }

}

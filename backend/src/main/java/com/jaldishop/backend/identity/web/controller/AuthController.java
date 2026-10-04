package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.*;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.LoginRequest;
import com.jaldishop.backend.identity.web.dto.RegisterMerchantRequest;
import com.jaldishop.backend.identity.web.dto.RegisterRequest;
import com.jaldishop.backend.identity.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserService authenticateUserService;
    private final RegisterCustomerService registerCustomerService;
    private final RegisterMerchantService registerMerchantService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticateUserService authenticateUserService,
            RegisterCustomerService registerCustomerService,
            RegisterMerchantService registerMerchantService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticateUserService = authenticateUserService;
        this.registerCustomerService = registerCustomerService;
        this.registerMerchantService = registerMerchantService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCustomerCommand command = new RegisterCustomerCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );

        User user = registerCustomerService.execute(command);
        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    };

    @PostMapping("/login")
    public ResponseEntity<AuthResult> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        AuthResult authResult = authenticateUserService.execute(command);

        return ResponseEntity.ok(authResult);
    }

    @PostMapping("/register/merchant")
    public ResponseEntity<AuthResult> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        RegisterMerchantCommand command = new RegisterMerchantCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.storeName(),
                request.businessType(),
                request.storeContactPhone(),
                request.address(),
                request.addressReference(),
                request.latitude(),
                request.longitude(),
                request.pickupEnabled(),
                request.deliveryEnabled(),
                request.logoUrl(),
                request.bannerUrl(),
                request.categoryIds()
        );

        AuthResult result = registerMerchantService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResult> refresh(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }
        AuthResult result = refreshTokenService.execute(principal.userId());
        return ResponseEntity.ok(result);
    }

}

package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.*;
import com.jaldishop.backend.identity.application.result.*;
import com.jaldishop.backend.identity.application.service.*;

import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.LoginRequest;
import com.jaldishop.backend.identity.web.dto.RegisterMerchantRequest;
import com.jaldishop.backend.identity.web.dto.RegisterRequest;
import com.jaldishop.backend.identity.web.dto.ResendVerificationRequest;
import com.jaldishop.backend.identity.web.dto.UserResponse;
import com.jaldishop.backend.identity.web.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserService authenticateUserService;
    private final RegisterCustomerService registerCustomerService;
    private final RegisterMerchantService registerMerchantService;
    private final RefreshTokenService refreshTokenService;
    private final VerifyEmailService verifyEmailService;
    private final ResendVerificationEmailService resendVerificationEmailService;

    @Autowired
    public AuthController(
            AuthenticateUserService authenticateUserService,
            RegisterCustomerService registerCustomerService,
            RegisterMerchantService registerMerchantService,
            RefreshTokenService refreshTokenService,
            VerifyEmailService verifyEmailService,
            ResendVerificationEmailService resendVerificationEmailService
    ) {
        this.authenticateUserService = authenticateUserService;
        this.registerCustomerService = registerCustomerService;
        this.registerMerchantService = registerMerchantService;
        this.refreshTokenService = refreshTokenService;
        this.verifyEmailService = verifyEmailService;
        this.resendVerificationEmailService = resendVerificationEmailService;
    }

    public AuthController(
            AuthenticateUserService authenticateUserService,
            RegisterCustomerService registerCustomerService,
            RegisterMerchantService registerMerchantService,
            RefreshTokenService refreshTokenService
    ) {
        this(authenticateUserService, registerCustomerService, registerMerchantService, refreshTokenService, null, null);
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
                user.getCreatedAt(),
                user.isEmailVerified()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    };

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailService.execute(new VerifyEmailCommand(request.token()));
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationEmailService.execute(new ResendVerificationCommand(request.email()));
        return ResponseEntity.accepted()
                .header("Cache-Control", "no-store")
                .body(Map.of(
                        "message",
                        "Si tu correo corresponde a una cuenta pendiente de verificación, recibirás un nuevo enlace en unos momentos."
                ));
    }

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
                request.categoryIds(),
                request.legalBusinessName(),
                request.taxIdType(),
                request.taxIdNumber(),
                request.taxRegime(),
                Boolean.TRUE.equals(request.canIssueInvoices()),
                request.taxRate(),
                Boolean.TRUE.equals(request.sunatTermsAccepted())
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
        AuthResult result = refreshTokenService.execute(principal.userId(), principal.credentialsVersion());
        return ResponseEntity.ok(result);
    }

}

package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.ResetPasswordService;
import com.jaldishop.backend.identity.application.command.ForgotPasswordCommand;
import com.jaldishop.backend.identity.application.command.ResetPasswordCommand;
import com.jaldishop.backend.identity.application.service.ForgotPasswordService;
import com.jaldishop.backend.identity.web.dto.ForgotPasswordRequest;
import com.jaldishop.backend.identity.web.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordRecoveryController {
    private final ForgotPasswordService forgotPasswordService;
    private final ResetPasswordService resetPasswordService;
    public PasswordRecoveryController(ForgotPasswordService forgotPasswordService, ResetPasswordService resetPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
        this.resetPasswordService = resetPasswordService;
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(
            @Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {
        forgotPasswordService.execute(new ForgotPasswordCommand(request.email(), origin));
        return ResponseEntity.accepted().header("Cache-Control", "no-store").body(Map.of("message",
                "Si el correo corresponde a una cuenta habilitada, recibirás instrucciones para restablecer la contraseña."));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordService.execute(new ResetPasswordCommand(request.token(), request.newPassword()));
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}

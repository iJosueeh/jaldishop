package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.PasswordRecoveryService;
import com.jaldishop.backend.identity.application.service.RequestPasswordRecoveryService;
import com.jaldishop.backend.identity.web.dto.ForgotPasswordRequest;
import com.jaldishop.backend.identity.web.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordRecoveryController {
    private final RequestPasswordRecoveryService requests;
    private final PasswordRecoveryService recovery;
    public PasswordRecoveryController(RequestPasswordRecoveryService requests, PasswordRecoveryService recovery) {
        this.requests = requests; this.recovery = recovery;
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        requests.request(request.email());
        return ResponseEntity.accepted().header("Cache-Control", "no-store").body(Map.of("message",
                "Si el correo corresponde a una cuenta habilitada, recibirás instrucciones para restablecer la contraseña."));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        recovery.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}

package com.jaldishop.backend.media.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.media.application.GenerateUploadSignatureCommand;
import com.jaldishop.backend.media.application.GenerateUploadSignatureService;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import com.jaldishop.backend.media.web.dto.GenerateSignatureRequest;
import com.jaldishop.backend.media.web.dto.UploadSignatureResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final GenerateUploadSignatureService generateUploadSignatureService;

    public MediaController(GenerateUploadSignatureService generateUploadSignatureService) {
        this.generateUploadSignatureService = generateUploadSignatureService;
    }

    @PostMapping("/upload-signature")
    public ResponseEntity<UploadSignatureResponse> generateUploadSignature(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody GenerateSignatureRequest request
    ) {
        if (principal == null) {
            if (request.targetType() == MediaTargetType.STORE_LOGO || request.targetType() == MediaTargetType.STORE_BANNER) {
                GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                        null,
                        request.targetType(),
                        null
                );
                UploadSignature signature = generateUploadSignatureService.execute(command);
                return ResponseEntity.ok(UploadSignatureResponse.fromDomain(signature));
            }
            throw new AccessDeniedException("No autenticado.");
        }

        boolean isMerchant = principal.roles().contains("MERCHANT");
        boolean isAdmin = principal.roles().contains("ADMIN");

        if (!isMerchant && !isAdmin) {
            throw new AccessDeniedException("Solo comerciantes o administradores pueden generar firmas de subida.");
        }

        UUID explicitStoreId = isAdmin ? request.storeId() : null;

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                principal.userId(),
                request.targetType(),
                explicitStoreId
        );

        UploadSignature signature = generateUploadSignatureService.execute(command);
        return ResponseEntity.ok(UploadSignatureResponse.fromDomain(signature));
    }
}

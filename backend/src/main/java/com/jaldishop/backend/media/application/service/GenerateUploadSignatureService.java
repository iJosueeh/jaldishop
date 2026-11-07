package com.jaldishop.backend.media.application.service;

import com.jaldishop.backend.media.application.command.GenerateUploadSignatureCommand;

import com.jaldishop.backend.media.domain.repository.MediaStorageService;
import com.jaldishop.backend.media.domain.enums.MediaTargetType;
import com.jaldishop.backend.media.domain.model.UploadSignature;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GenerateUploadSignatureService {

    private final MediaStorageService mediaStorageService;
    private final StoreRepository storeRepository;

    public GenerateUploadSignatureService(MediaStorageService mediaStorageService, StoreRepository storeRepository) {
        this.mediaStorageService = mediaStorageService;
        this.storeRepository = storeRepository;
    }

    public UploadSignature execute(GenerateUploadSignatureCommand command) {
        UUID effectiveStoreId = null;
        if (command.userId() != null && command.roles().contains("MERCHANT")) {
            effectiveStoreId = storeRepository.findByMerchantUserId(command.userId())
                    .map(Store::getId)
                    .orElse(null);
        }

        if (command.explicitStoreId() != null) {
            if (command.userId() != null && command.roles().contains("ADMIN")) {
                effectiveStoreId = storeRepository.findById(command.explicitStoreId())
                        .map(Store::getId)
                        .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Tienda no encontrada."));
            } else if (!command.explicitStoreId().equals(effectiveStoreId)) {
                throw new ResourceNotFoundException("STORE_NOT_FOUND", "Tienda no encontrada.");
            }
        }

        if (effectiveStoreId == null && command.targetType() == MediaTargetType.PRODUCT_IMAGE) {
            throw new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda asociada a este usuario.");
        }

        return mediaStorageService.generateSignature(effectiveStoreId, command.targetType());
    }
}

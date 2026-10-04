package com.jaldishop.backend.media.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
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
        UUID effectiveStoreId = command.explicitStoreId();

        if (effectiveStoreId == null && command.userId() != null) {
            effectiveStoreId = storeRepository.findByMerchantUserId(command.userId())
                    .map(Store::getId)
                    .orElse(null);
        }

        if (effectiveStoreId == null && command.targetType() == MediaTargetType.PRODUCT_IMAGE) {
            throw new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda asociada a este usuario.");
        }

        return mediaStorageService.generateSignature(effectiveStoreId, command.targetType());
    }
}

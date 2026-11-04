package com.jaldishop.backend.media.domain.repository;

import com.jaldishop.backend.media.domain.enums.MediaTargetType;
import com.jaldishop.backend.media.domain.model.UploadSignature;

import java.util.UUID;

public interface MediaStorageService {

    UploadSignature generateSignature(UUID storeId, MediaTargetType targetType);

    void deleteMedia(String publicId);

    void deleteStoreMedia(UUID storeId);
}

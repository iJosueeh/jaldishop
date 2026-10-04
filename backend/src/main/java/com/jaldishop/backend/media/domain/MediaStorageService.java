package com.jaldishop.backend.media.domain;

import java.util.UUID;

public interface MediaStorageService {

    UploadSignature generateSignature(UUID storeId, MediaTargetType targetType);

    void deleteMedia(String publicId);

    void deleteStoreMedia(UUID storeId);
}

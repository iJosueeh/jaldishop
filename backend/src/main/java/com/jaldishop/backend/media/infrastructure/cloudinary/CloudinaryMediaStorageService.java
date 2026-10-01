package com.jaldishop.backend.media.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryMediaStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public UploadSignature generateSignature(UUID storeId, MediaTargetType targetType) {
        long timestamp = Instant.now().getEpochSecond();
        String folder = resolveFolder(storeId, targetType);

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("folder", folder);
        paramsToSign.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(paramsToSign, properties.getApiSecret());
        String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", properties.getCloudName());

        return new UploadSignature(
                properties.getCloudName(),
                properties.getApiKey(),
                timestamp,
                folder,
                signature,
                uploadUrl
        );
    }

    @Override
    public void deleteMedia(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar recurso multimedia de Cloudinary: " + e.getMessage(), e);
        }
    }

    private String resolveFolder(UUID storeId, MediaTargetType targetType) {
        String base = "jaldishop/tenants";
        if (storeId == null) {
            return String.format("%s/common/%s", base, targetType.getSubfolder());
        }
        return String.format("%s/%s/%s", base, storeId, targetType.getSubfolder());
    }
}

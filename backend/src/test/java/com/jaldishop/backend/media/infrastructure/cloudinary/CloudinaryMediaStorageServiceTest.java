package com.jaldishop.backend.media.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryMediaStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    private CloudinaryProperties properties;
    private CloudinaryMediaStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("test-key");
        properties.setApiSecret("test-secret");

        storageService = new CloudinaryMediaStorageService(cloudinary, properties);
    }

    @Test
    @DisplayName("Generar firma con ruta tenant y timestamp")
    void generateSignatureSuccessfully() {
        UUID storeId = UUID.randomUUID();
        when(cloudinary.apiSignRequest(any(Map.class), eq("test-secret"))).thenReturn("signed-sha-hash");

        UploadSignature signature = storageService.generateSignature(storeId, MediaTargetType.STORE_LOGO);

        assertNotNull(signature);
        assertEquals("test-cloud", signature.cloudName());
        assertEquals("test-key", signature.apiKey());
        assertEquals("signed-sha-hash", signature.signature());
        assertEquals("jaldishop/tenants/" + storeId + "/branding", signature.folder());
        assertEquals("https://api.cloudinary.com/v1_1/test-cloud/image/upload", signature.uploadUrl());
        assertTrue(signature.timestamp() > 0);
    }

    @Test
    @DisplayName("deleteStoreMedia con storeId nulo no realiza peticiones")
    void deleteStoreMediaWithNullStoreId() {
        assertDoesNotThrow(() -> storageService.deleteStoreMedia(null));
    }

    @Test
    @DisplayName("deleteStoreMedia captura excepciones de Cloudinary sin interrumpir flujo")
    void deleteStoreMediaHandlesCloudinaryExceptionGracefully() {
        UUID storeId = UUID.randomUUID();
        when(cloudinary.api()).thenThrow(new RuntimeException("Cloudinary API unavailable"));

        assertDoesNotThrow(() -> storageService.deleteStoreMedia(storeId));
    }
}

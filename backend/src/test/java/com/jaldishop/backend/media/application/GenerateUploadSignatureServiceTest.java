package com.jaldishop.backend.media.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateUploadSignatureServiceTest {

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private StoreRepository storeRepository;

    private GenerateUploadSignatureService service;

    @BeforeEach
    void setUp() {
        service = new GenerateUploadSignatureService(mediaStorageService, storeRepository);
    }

    @Test
    @DisplayName("Generar firma exitosamente para merchant resolviendo storeId")
    void generateSignatureForMerchant() {
        UUID merchantUserId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));

        UploadSignature expectedSignature = new UploadSignature(
                "demo-cloud",
                "demo-key",
                1700000000L,
                "jaldishop/tenants/" + storeId + "/branding",
                "signature-hash",
                "https://api.cloudinary.com/v1_1/demo-cloud/image/upload"
        );
        when(mediaStorageService.generateSignature(eq(storeId), eq(MediaTargetType.STORE_LOGO)))
                .thenReturn(expectedSignature);

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                merchantUserId,
                MediaTargetType.STORE_LOGO,
                null
        );

        UploadSignature result = service.execute(command);

        assertNotNull(result);
        assertEquals("demo-cloud", result.cloudName());
        assertEquals("signature-hash", result.signature());
        verify(mediaStorageService).generateSignature(storeId, MediaTargetType.STORE_LOGO);
    }

    @Test
    @DisplayName("Generar firma con explicitStoreId para admin")
    void generateSignatureWithExplicitStoreId() {
        UUID storeId = UUID.randomUUID();
        UploadSignature expectedSignature = new UploadSignature(
                "demo-cloud",
                "demo-key",
                1700000000L,
                "jaldishop/tenants/" + storeId + "/products",
                "signature-hash",
                "https://api.cloudinary.com/v1_1/demo-cloud/image/upload"
        );
        when(mediaStorageService.generateSignature(eq(storeId), eq(MediaTargetType.PRODUCT_IMAGE)))
                .thenReturn(expectedSignature);

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                UUID.randomUUID(),
                MediaTargetType.PRODUCT_IMAGE,
                storeId
        );

        UploadSignature result = service.execute(command);

        assertNotNull(result);
        verify(mediaStorageService).generateSignature(storeId, MediaTargetType.PRODUCT_IMAGE);
        verifyNoInteractions(storeRepository);
    }

    @Test
    @DisplayName("Lanzar excepción si merchant no tiene tienda asociada")
    void throwExceptionWhenStoreNotFound() {
        UUID merchantUserId = UUID.randomUUID();
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.empty());

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                merchantUserId,
                MediaTargetType.STORE_BANNER,
                null
        );

        assertThrows(ResourceNotFoundException.class, () -> service.execute(command));
        verifyNoInteractions(mediaStorageService);
    }
}

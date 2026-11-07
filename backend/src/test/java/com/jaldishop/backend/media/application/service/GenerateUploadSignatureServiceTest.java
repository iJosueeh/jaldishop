package com.jaldishop.backend.media.application.service;

import com.jaldishop.backend.media.application.command.GenerateUploadSignatureCommand;

import com.jaldishop.backend.media.domain.repository.MediaStorageService;
import com.jaldishop.backend.media.domain.enums.MediaTargetType;
import com.jaldishop.backend.media.domain.model.UploadSignature;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
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
                null, Set.of("MERCHANT")
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
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(mediaStorageService.generateSignature(eq(storeId), eq(MediaTargetType.PRODUCT_IMAGE)))
                .thenReturn(expectedSignature);

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                UUID.randomUUID(),
                MediaTargetType.PRODUCT_IMAGE,
                storeId, Set.of("ADMIN")
        );

        UploadSignature result = service.execute(command);

        assertNotNull(result);
        verify(mediaStorageService).generateSignature(storeId, MediaTargetType.PRODUCT_IMAGE);
        verify(storeRepository).findById(storeId);
    }

    @Test
    @DisplayName("Lanzar excepción si merchant no tiene tienda asociada para imágenes de producto")
    void throwExceptionWhenStoreNotFound() {
        UUID merchantUserId = UUID.randomUUID();
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.empty());

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                merchantUserId,
                MediaTargetType.PRODUCT_IMAGE,
                null, Set.of("MERCHANT")
        );

        assertThrows(ResourceNotFoundException.class, () -> service.execute(command));
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    @DisplayName("Permitir firma de branding aunque la tienda aún no exista")
    void allowBrandingSignatureWhenStoreNotFound() {
        UUID merchantUserId = UUID.randomUUID();
        UploadSignature expectedSignature = new UploadSignature(
                "demo-cloud",
                "demo-key",
                1700000000L,
                "jaldishop/tenants/common/branding",
                "signature-hash",
                "https://api.cloudinary.com/v1_1/demo-cloud/image/upload"
        );
        when(mediaStorageService.generateSignature(eq(null), eq(MediaTargetType.STORE_BANNER)))
                .thenReturn(expectedSignature);

        GenerateUploadSignatureCommand command = new GenerateUploadSignatureCommand(
                merchantUserId,
                MediaTargetType.STORE_BANNER,
                null, Set.of("CUSTOMER")
        );

        UploadSignature result = service.execute(command);

        assertNotNull(result);
        assertEquals("demo-cloud", result.cloudName());
        verify(mediaStorageService).generateSignature(null, MediaTargetType.STORE_BANNER);
    }
    @Test
    void merchantCanSignForOwnExplicitStore() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findByMerchantUserId(userId)).thenReturn(Optional.of(store));
        service.execute(new GenerateUploadSignatureCommand(userId, MediaTargetType.PRODUCT_IMAGE, storeId, Set.of("MERCHANT")));
        verify(mediaStorageService).generateSignature(storeId, MediaTargetType.PRODUCT_IMAGE);
    }

    @Test
    void foreignAndAnonymousStoreIdsAreRejectedWithoutSigning() {
        UUID userId = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(UUID.randomUUID());
        when(storeRepository.findByMerchantUserId(userId)).thenReturn(Optional.of(store));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(
                new GenerateUploadSignatureCommand(userId, MediaTargetType.PRODUCT_IMAGE, foreignId, Set.of("MERCHANT"))));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(
                new GenerateUploadSignatureCommand(null, MediaTargetType.STORE_LOGO, foreignId, Set.of())));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(
                new GenerateUploadSignatureCommand(userId, MediaTargetType.STORE_BANNER, foreignId, Set.of("CUSTOMER"))));
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void anonymousBrandingUsesCommonFolder() {
        service.execute(new GenerateUploadSignatureCommand(null, MediaTargetType.STORE_LOGO, null, Set.of()));
        verify(mediaStorageService).generateSignature(null, MediaTargetType.STORE_LOGO);
    }

    @Test
    void adminCannotSignForNonexistentStore() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.execute(
                new GenerateUploadSignatureCommand(UUID.randomUUID(), MediaTargetType.PRODUCT_IMAGE, storeId, Set.of("ADMIN"))));
        verifyNoInteractions(mediaStorageService);
    }
}

package com.jaldishop.backend.media.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreMediaCleanupAdapterTest {

    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private StoreMediaCleanupAdapter adapter;

    @Test
    @DisplayName("Delega la limpieza de medios de la tienda en MediaStorageService")
    void shouldDelegateToMediaStorageService() {
        UUID storeId = UUID.randomUUID();

        adapter.deleteStoreMedia(storeId);

        verify(mediaStorageService).deleteStoreMedia(storeId);
    }

    @Test
    @DisplayName("Propaga el fallo al llamante: el aislamiento de errores lo aplica quien cierra la tienda")
    void shouldPropagateFailureToCaller() {
        UUID storeId = UUID.randomUUID();
        doThrow(new RuntimeException("Cloudinary no disponible"))
                .when(mediaStorageService).deleteStoreMedia(storeId);

        assertThrows(RuntimeException.class, () -> adapter.deleteStoreMedia(storeId));
    }
}

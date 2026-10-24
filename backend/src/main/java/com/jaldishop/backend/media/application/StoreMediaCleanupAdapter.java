package com.jaldishop.backend.media.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.store.domain.StoreMediaCleaner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación del puerto {@link StoreMediaCleaner} de {@code store/domain},
 * delegando en el servicio de almacenamiento de medios (Cloudinary).
 * Mantiene la dirección de dependencia media → store (ya existente) y evita
 * que store dependa de media.
 */
@Component
public class StoreMediaCleanupAdapter implements StoreMediaCleaner {

    private final MediaStorageService mediaStorageService;

    public StoreMediaCleanupAdapter(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @Override
    public void deleteStoreMedia(UUID storeId) {
        mediaStorageService.deleteStoreMedia(storeId);
    }
}

package com.jaldishop.backend.media.application.service;

import com.jaldishop.backend.media.domain.repository.MediaStorageService;
import com.jaldishop.backend.store.domain.port.StoreMediaCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación del puerto {@link StoreMediaCleaner} de {@code store/domain}.
 * Encola la limpieza de medios en la tabla outbox transaccional cuando está disponible
 * para garantizar atomicidad con el borrado de tienda en PostgreSQL.
 */
@Component
public class StoreMediaCleanupAdapter implements StoreMediaCleaner {

    private final MediaStorageService mediaStorageService;
    private final MediaCleanupOutboxService outboxService;

    public StoreMediaCleanupAdapter(MediaStorageService mediaStorageService) {
        this(mediaStorageService, null);
    }

    @Autowired
    public StoreMediaCleanupAdapter(
            MediaStorageService mediaStorageService,
            MediaCleanupOutboxService outboxService
    ) {
        this.mediaStorageService = mediaStorageService;
        this.outboxService = outboxService;
    }

    @Override
    public void deleteStoreMedia(UUID storeId) {
        if (outboxService != null) {
            outboxService.enqueueStoreCleanup(storeId);
        } else {
            mediaStorageService.deleteStoreMedia(storeId);
        }
    }
}

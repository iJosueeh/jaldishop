package com.jaldishop.backend.media.application.service;

import com.jaldishop.backend.media.domain.repository.MediaStorageService;
import com.jaldishop.backend.media.infrastructure.persistence.entity.MediaCleanupOutboxEntity;
import com.jaldishop.backend.media.infrastructure.persistence.repository.MediaCleanupOutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MediaCleanupOutboxService {

    private static final Logger log = LoggerFactory.getLogger(MediaCleanupOutboxService.class);
    private static final int MAX_ATTEMPTS = 5;

    private final MediaCleanupOutboxJpaRepository outboxRepository;
    private final MediaStorageService mediaStorageService;

    public MediaCleanupOutboxService(
            MediaCleanupOutboxJpaRepository outboxRepository,
            MediaStorageService mediaStorageService
    ) {
        this.outboxRepository = outboxRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public void enqueueStoreCleanup(UUID storeId) {
        if (storeId == null) {
            return;
        }
        outboxRepository.save(MediaCleanupOutboxEntity.forPrefix("jaldishop/tenants/" + storeId));
    }

    @Transactional
    public void enqueuePublicIds(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }
        List<MediaCleanupOutboxEntity> batch = publicIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .map(MediaCleanupOutboxEntity::forPublicId)
                .toList();
        if (!batch.isEmpty()) {
            outboxRepository.saveAll(batch);
        }
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void processPendingBatch() {
        List<MediaCleanupOutboxEntity> batch = outboxRepository.findPendingBatchForUpdate();
        for (MediaCleanupOutboxEntity item : batch) {
            try {
                if (item.getPrefix() != null && item.getPrefix().startsWith("jaldishop/tenants/")) {
                    String rawId = item.getPrefix().substring("jaldishop/tenants/".length());
                    mediaStorageService.deleteStoreMedia(UUID.fromString(rawId));
                } else if (item.getPublicId() != null) {
                    mediaStorageService.deleteMedia(item.getPublicId());
                }
                item.markCompleted();
            } catch (Exception ex) {
                log.warn("Error procesando limpieza multimedia {}: {}", item.getId(), ex.getMessage());
                item.recordFailure(ex.getMessage(), MAX_ATTEMPTS);
            }
            outboxRepository.save(item);
        }
    }
}

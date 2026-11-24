package com.jaldishop.backend.media.infrastructure.persistence.repository;

import com.jaldishop.backend.media.infrastructure.persistence.entity.MediaCleanupOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MediaCleanupOutboxJpaRepository extends JpaRepository<MediaCleanupOutboxEntity, UUID> {

    @Query(value = """
            SELECT * FROM media_cleanup_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT 20
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<MediaCleanupOutboxEntity> findPendingBatchForUpdate();
}

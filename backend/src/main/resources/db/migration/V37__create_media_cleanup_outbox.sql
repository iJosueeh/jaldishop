-- ==============================================================================
-- JaldiShop Migration V37: Create Media Cleanup Outbox Table
-- Version: V37__create_media_cleanup_outbox.sql
-- Description:
--   Transactional outbox for reliable asynchronous Cloudinary asset cleanup
--   after hard-delete operations commit in PostgreSQL.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS media_cleanup_outbox (
    id UUID PRIMARY KEY,
    public_id VARCHAR(500),
    prefix VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,

    CONSTRAINT chk_media_cleanup_outbox_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_media_cleanup_outbox_target CHECK (public_id IS NOT NULL OR prefix IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_media_cleanup_outbox_pending
    ON media_cleanup_outbox (created_at)
    WHERE status = 'PENDING';

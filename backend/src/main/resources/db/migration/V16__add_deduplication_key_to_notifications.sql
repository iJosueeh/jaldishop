-- ====================================================================
-- V16: Agregar clave de deduplicación a la tabla notifications
-- Permite garantizar idempotencia estricta en la emisión de notificaciones in-app
-- ====================================================================

ALTER TABLE notifications
    ADD COLUMN deduplication_key VARCHAR(100);

ALTER TABLE notifications
    ADD CONSTRAINT uq_notifications_deduplication_key UNIQUE (deduplication_key);

COMMENT ON COLUMN notifications.deduplication_key IS 'Clave única para deduplicación idempotente (ej. NEW_ORDER:{orderId}). Nullable para registros históricos.';

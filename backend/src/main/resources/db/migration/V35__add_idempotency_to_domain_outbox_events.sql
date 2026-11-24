-- ====================================================================
-- V35: Agregar clave de idempotencia a domain_outbox_events
-- Deduplicación transaccional determinista para el patrón Transactional Outbox
-- ====================================================================

ALTER TABLE domain_outbox_events
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_domain_outbox_idempotency_key'
    ) THEN
        ALTER TABLE domain_outbox_events
            ADD CONSTRAINT uq_domain_outbox_idempotency_key UNIQUE (idempotency_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_domain_outbox_aggregate
    ON domain_outbox_events(aggregate_type, aggregate_id, event_type);

COMMENT ON COLUMN domain_outbox_events.idempotency_key IS 'Clave determinista para deduplicación (ej. ORDER:uuid:ORDER_CONFIRMED).';

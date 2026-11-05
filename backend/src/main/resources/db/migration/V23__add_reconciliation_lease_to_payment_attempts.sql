-- V23: Agregar columnas de lease/claim para conciliacion activa de ordenes en modo ORDERS
ALTER TABLE payment_attempts
    ADD COLUMN IF NOT EXISTS reconciliation_claim_token UUID,
    ADD COLUMN IF NOT EXISTS reconciliation_lease_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_payment_attempts_reconciliation_claim
    ON payment_attempts(checkout_mode, status, created_at)
    WHERE checkout_mode = 'ORDERS' AND status IN ('CREATED', 'PROCESSING') AND provider_order_id IS NOT NULL;

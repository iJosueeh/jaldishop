-- Version: V20__add_provider_order_id_to_payment_attempts.sql
-- Description: Añadir columna provider_order_id e índice condicional en payment_attempts para Orders API de Mercado Pago,
-- preservando preference_id para compatibilidad histórica con Preferences API.

ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_provider_order_id
    ON payment_attempts (provider_order_id)
    WHERE provider_order_id IS NOT NULL;

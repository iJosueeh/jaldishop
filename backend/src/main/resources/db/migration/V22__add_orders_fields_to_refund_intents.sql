-- Version: V22__add_orders_fields_to_refund_intents.sql
-- Description: Soporte bimodal para reembolsos en Orders API y Preferences API en payment_refund_intents.

ALTER TABLE payment_refund_intents
    ADD COLUMN IF NOT EXISTS checkout_mode VARCHAR(20) NOT NULL DEFAULT 'PREFERENCES',
    ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(100),
    ALTER COLUMN provider_payment_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_refund_intents_provider_order
    ON payment_refund_intents (provider_order_id);

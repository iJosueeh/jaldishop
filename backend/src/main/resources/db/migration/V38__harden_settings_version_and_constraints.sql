-- ==============================================================================
-- JaldiShop Migration V38: Add version column for optimistic locking and payment method constraints
-- Version: V38__harden_settings_version_and_constraints.sql
-- Description:
--   1. Adds 'version' column to platform_settings and store_settings for optimistic locking (@Version).
--   2. Ensures at least one payment method is active in store_settings before adding check constraint.
--   3. Adds check constraint ensuring store has online_payments_enabled OR cash_on_delivery_enabled.
-- ==============================================================================

ALTER TABLE platform_settings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Ensure valid payment methods for all existing store settings
UPDATE store_settings
SET online_payments_enabled = true
WHERE NOT online_payments_enabled AND NOT cash_on_delivery_enabled;

-- Add check constraint ensuring at least one payment method is enabled
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_store_settings_payment_method'
    ) THEN
        ALTER TABLE store_settings
        ADD CONSTRAINT chk_store_settings_payment_method
        CHECK (online_payments_enabled OR cash_on_delivery_enabled);
    END IF;
END $$;

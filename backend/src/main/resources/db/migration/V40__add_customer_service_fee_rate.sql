-- ==============================================================================
-- JaldiShop Migration V40: Add Customer Service Fee Rate & Snapshots
-- Version: V40__add_customer_service_fee_rate.sql
-- Description:
--   1. Adds customer_service_fee_rate to platform_settings (0.00% - 100.00%).
--   2. Adds immutable service_fee_amount snapshot columns to payments & orders.
-- ==============================================================================

ALTER TABLE platform_settings
    ADD COLUMN IF NOT EXISTS customer_service_fee_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE platform_settings
    DROP CONSTRAINT IF EXISTS chk_platform_settings_service_fee;

ALTER TABLE platform_settings
    ADD CONSTRAINT chk_platform_settings_service_fee
        CHECK (customer_service_fee_rate >= 0.00 AND customer_service_fee_rate <= 100.00);

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS service_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS service_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;

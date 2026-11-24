-- ==============================================================================
-- JaldiShop Migration V41: Add Customer Service Fee Cap
-- Version: V41__add_customer_service_fee_cap.sql
-- Description:
--   1. Adds nullable customer_service_fee_cap to platform_settings.
--   2. Enforces non-negative cap when configured.
-- ==============================================================================

ALTER TABLE platform_settings
    ADD COLUMN IF NOT EXISTS customer_service_fee_cap NUMERIC(12, 2) NULL;

ALTER TABLE platform_settings
    DROP CONSTRAINT IF EXISTS chk_platform_settings_service_fee_cap;

ALTER TABLE platform_settings
    ADD CONSTRAINT chk_platform_settings_service_fee_cap
        CHECK (customer_service_fee_cap IS NULL OR customer_service_fee_cap >= 0.00);

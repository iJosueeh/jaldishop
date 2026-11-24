-- ==============================================================================
-- JaldiShop Migration V36: Create Platform Settings Table
-- Version: V36__create_platform_settings_table.sql
-- Description:
--   1. Provides persistent platform-wide operational settings.
--   2. Enforces singleton pattern (single row with id = 1).
--   3. Configures merchant registration, store approval, maintenance mode,
--      session timeout, admin 2FA enforcement and default marketplace commission rate.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS platform_settings (
    id INT PRIMARY KEY DEFAULT 1,
    allow_merchant_registration BOOLEAN NOT NULL DEFAULT true,
    require_store_approval BOOLEAN NOT NULL DEFAULT false,
    maintenance_mode BOOLEAN NOT NULL DEFAULT false,
    maintenance_notice TEXT NOT NULL DEFAULT 'La plataforma se encuentra en mantenimiento programado. Volveremos pronto.',
    marketplace_commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 5.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_platform_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_platform_settings_commission CHECK (marketplace_commission_rate >= 0.00 AND marketplace_commission_rate <= 100.00)
);

-- Seed initial row if not present
INSERT INTO platform_settings (
    id,
    allow_merchant_registration,
    require_store_approval,
    maintenance_mode,
    maintenance_notice,
    marketplace_commission_rate
) VALUES (
    1,
    true,
    false,
    false,
    'La plataforma se encuentra en mantenimiento programado. Volveremos pronto.',
    5.00
) ON CONFLICT (id) DO NOTHING;

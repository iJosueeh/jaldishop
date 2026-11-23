-- ==============================================================================
-- JaldiShop Migration V33: Create Store Operational Settings Table
-- Version: V33__create_store_settings_table.sql
-- Description:
--   1. Replaces frontend localStorage with PostgreSQL persistence for operational settings.
--   2. Stores policies (cancellation, refunds INDECOPI), checkout requirements.
--   3. Stores customer payment methods & dispatch rules (Online vs COD, min amounts, prep time).
--   4. Stores live notification preferences (sound alert switch, volume, low stock).
-- ==============================================================================

CREATE TABLE IF NOT EXISTS store_settings (
    store_id UUID PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,

    -- Políticas y Checkout (INDECOPI)
    cancellation_policy TEXT NOT NULL DEFAULT 'Puedes solicitar la cancelación gratuita de tu pedido mientras se encuentre en estado Confirmado antes de entrar a preparación.',
    refund_policy TEXT NOT NULL DEFAULT 'Conforme a la normativa de INDECOPI, ante fallas o inconsistencias comprobadas se procederá con el reenvío o reembolso del producto dentro de las 24 horas.',
    require_customer_dni BOOLEAN NOT NULL DEFAULT false,
    allow_order_notes BOOLEAN NOT NULL DEFAULT true,

    -- Cobros y Despacho
    online_payments_enabled BOOLEAN NOT NULL DEFAULT true,
    cash_on_delivery_enabled BOOLEAN NOT NULL DEFAULT true,
    accepts_pos_on_delivery BOOLEAN NOT NULL DEFAULT false,
    min_delivery_amount NUMERIC(10, 2) DEFAULT NULL,
    estimated_prep_time_minutes INT NOT NULL DEFAULT 30,

    -- Alertas y Sonidos en Vivo
    order_sound_alert BOOLEAN NOT NULL DEFAULT true,
    sound_volume INT NOT NULL DEFAULT 80,
    low_stock_visual_alert BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_store_settings_sound_volume CHECK (sound_volume >= 0 AND sound_volume <= 100),
    CONSTRAINT chk_store_settings_prep_time CHECK (estimated_prep_time_minutes >= 5 AND estimated_prep_time_minutes <= 480),
    CONSTRAINT chk_store_settings_min_amount CHECK (min_delivery_amount IS NULL OR min_delivery_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_store_settings_store ON store_settings(store_id);

-- Sembrar configuración inicial para tiendas existentes
INSERT INTO store_settings (store_id)
SELECT id FROM stores
ON CONFLICT (store_id) DO NOTHING;

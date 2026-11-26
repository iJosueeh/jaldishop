-- ============================================================================
-- V44: Sistema unificado de códigos promocionales y descuentos (promo_codes)
-- Migra cupones CODE existentes desde discounts, elimina tabla huérfana y
-- agrega trazabilidad financiera promo_funding_source en orders.
-- ============================================================================

CREATE TABLE promo_codes (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    funding_source VARCHAR(20) NOT NULL,
    store_id UUID NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    min_order_amount NUMERIC(12, 2) NULL,
    max_discount_amount NUMERIC(12, 2) NULL,
    usage_limit INTEGER NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ NULL,
    ends_at TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    show_in_banner BOOLEAN NOT NULL DEFAULT FALSE,
    banner_text VARCHAR(240) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_promo_codes PRIMARY KEY (id),
    CONSTRAINT fk_promo_codes_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT ck_promo_codes_scope CHECK (scope IN ('GLOBAL', 'STORE')),
    CONSTRAINT ck_promo_codes_funding_source CHECK (funding_source IN ('PLATFORM', 'MERCHANT')),
    CONSTRAINT ck_promo_codes_scope_store CHECK (
        (scope = 'GLOBAL' AND store_id IS NULL AND funding_source = 'PLATFORM')
        OR (scope = 'STORE' AND store_id IS NOT NULL AND funding_source = 'MERCHANT')
    ),
    CONSTRAINT ck_promo_codes_scope_banner CHECK (scope = 'GLOBAL' OR show_in_banner = FALSE),
    CONSTRAINT ck_promo_codes_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_promo_codes_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_promo_codes_percentage_max CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT ck_promo_codes_min_order CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT ck_promo_codes_max_discount CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT ck_promo_codes_usage_limit CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT ck_promo_codes_used_count CHECK (used_count >= 0),
    CONSTRAINT ck_promo_codes_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE UNIQUE INDEX uq_promo_codes_code_upper
    ON promo_codes (UPPER(code));

CREATE INDEX idx_promo_codes_store_active
    ON promo_codes (store_id, is_active)
    WHERE store_id IS NOT NULL;

CREATE INDEX idx_promo_codes_active_banner
    ON promo_codes (updated_at DESC)
    WHERE scope = 'GLOBAL' AND is_active = TRUE AND show_in_banner = TRUE;

-- Migrar cupones CODE existentes desde la tabla legacy discounts (sin acumular AUTOMATIC)
INSERT INTO promo_codes (
    id, code, scope, funding_source, store_id, discount_type, discount_value,
    min_order_amount, max_discount_amount, usage_limit, used_count,
    starts_at, ends_at, is_active, show_in_banner, banner_text, created_at, updated_at
)
SELECT
    d.id,
    UPPER(TRIM(d.code)),
    'STORE',
    'MERCHANT',
    d.store_id,
    d.type,
    d.value,
    d.minimum_purchase_amount,
    NULL,
    NULL,
    0,
    d.starts_at,
    d.ends_at,
    (d.status = 'ACTIVE'),
    FALSE,
    NULL,
    d.created_at,
    d.updated_at
FROM discounts d
WHERE d.modality = 'CODE'
  AND d.code IS NOT NULL
  AND LENGTH(TRIM(d.code)) BETWEEN 3 AND 40
ON CONFLICT DO NOTHING;

DROP TABLE IF EXISTS discounts;

-- Snapshot financiero del financiamiento del descuento en órdenes
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS promo_funding_source VARCHAR(20) NULL;

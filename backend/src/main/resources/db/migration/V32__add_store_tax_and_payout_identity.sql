-- ==============================================================================
-- JaldiShop Migration V32: Add Store Tax Identity (RUC/DNI) and Payout LifeCycle
-- Version: V32__add_store_tax_and_payout_identity.sql
-- Description:
--   1. Adds SUNAT tax identity (RUC 10, RUC 20, DNI) and regimes (RUS, MYPE, etc.) to stores.
--   2. Adds can_issue_invoices flag to control electronic invoice eligibility.
--   3. Creates merchant_payout_destinations table for BCP/Interbank CCI or Yape numbers.
--   4. Creates merchant_payout_batches table for stateful payout lifecycle.
-- ==============================================================================

-- 1. Ampliar tabla stores con identidad fiscal SUNAT
ALTER TABLE stores ADD COLUMN IF NOT EXISTS legal_business_name VARCHAR(200);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS tax_id_type VARCHAR(20) NOT NULL DEFAULT 'DNI';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS tax_id_number VARCHAR(11);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS tax_regime VARCHAR(20) NOT NULL DEFAULT 'RUS';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS can_issue_invoices BOOLEAN NOT NULL DEFAULT false;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stores_tax_id_type') THEN
        ALTER TABLE stores ADD CONSTRAINT chk_stores_tax_id_type 
        CHECK (tax_id_type IN ('DNI', 'RUC_10', 'RUC_20'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stores_tax_regime') THEN
        ALTER TABLE stores ADD CONSTRAINT chk_stores_tax_regime 
        CHECK (tax_regime IN ('NONE', 'RUS', 'RER', 'MYPE', 'GENERAL'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stores_ruc_format') THEN
        ALTER TABLE stores ADD CONSTRAINT chk_stores_ruc_format CHECK (
            (tax_id_type = 'DNI' AND length(tax_id_number) = 8) OR
            (tax_id_type = 'RUC_10' AND length(tax_id_number) = 11 AND tax_id_number LIKE '10%') OR
            (tax_id_type = 'RUC_20' AND length(tax_id_number) = 11 AND tax_id_number LIKE '20%') OR
            (tax_id_number IS NULL)
        );
    END IF;
END $$;

-- 2. Cuentas y Destinos de Cobro del Comercio para Liquidaciones (Payouts)
CREATE TABLE IF NOT EXISTS merchant_payout_destinations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    payout_channel VARCHAR(30) NOT NULL,
    bank_name VARCHAR(50),
    account_number VARCHAR(50),
    account_cci VARCHAR(20),
    phone_number VARCHAR(15),
    account_holder_name VARCHAR(150) NOT NULL,
    account_holder_doc_type VARCHAR(10) NOT NULL,
    account_holder_doc_number VARCHAR(11) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT true,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_payout_channel CHECK (payout_channel IN ('BANK_TRANSFER_CCI', 'YAPE_PHONE', 'PLIN_PHONE')),
    CONSTRAINT chk_payout_holder_doc CHECK (account_holder_doc_type IN ('DNI', 'RUC', 'CE'))
);

CREATE INDEX IF NOT EXISTS idx_payout_dest_store ON merchant_payout_destinations(store_id);

-- 3. Lotes de Liquidación con Máquina de Estados
CREATE TABLE IF NOT EXISTS merchant_payout_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    destination_id UUID NOT NULL REFERENCES merchant_payout_destinations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gross_amount NUMERIC(12, 2) NOT NULL,
    banking_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_disbursed_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    bank_operation_number VARCHAR(80),
    voucher_url TEXT,
    failure_reason TEXT,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    scheduled_for DATE NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_payout_batch_status CHECK (status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_batch_min_gross CHECK (gross_amount >= 50.00),
    CONSTRAINT chk_batch_net_disbursed CHECK (net_disbursed_amount = gross_amount - banking_fee)
);

CREATE INDEX IF NOT EXISTS idx_payout_batches_status ON merchant_payout_batches(status, scheduled_for)
WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_payout_batches_store ON merchant_payout_batches(store_id, created_at DESC);

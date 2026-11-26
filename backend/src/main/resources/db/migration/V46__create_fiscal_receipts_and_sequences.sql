-- ==============================================================================
-- JaldiShop Migration V46: SUNAT Fiscal Receipts & Atomic Sequences
-- Version: V46__create_fiscal_receipts_and_sequences.sql
-- Description:
--   1. Adds optional customer receipt request columns to payments (requested_receipt_type,
--      customer_doc_type, customer_doc_number, customer_legal_name).
--   2. Creates store_receipt_sequences for atomic series/correlative numbering per store.
--   3. Creates fiscal_receipts for immutable SUNAT-aligned receipts (01 Factura, 03 Boleta, 00 Nota de Venta).
-- ==============================================================================

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS requested_receipt_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS customer_doc_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS customer_doc_number VARCHAR(11),
    ADD COLUMN IF NOT EXISTS customer_legal_name VARCHAR(200);

CREATE TABLE IF NOT EXISTS store_receipt_sequences (
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    document_type_code VARCHAR(2) NOT NULL,
    series VARCHAR(4) NOT NULL,
    last_correlative BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (store_id, document_type_code),
    CONSTRAINT chk_receipt_seq_doc_code CHECK (document_type_code IN ('01', '03', '00')),
    CONSTRAINT chk_receipt_seq_correlative_nonneg CHECK (last_correlative >= 0)
);

CREATE TABLE IF NOT EXISTS fiscal_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
    payment_id UUID REFERENCES payments(id) ON DELETE RESTRICT,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL,

    -- Código Catálogo N° 01 SUNAT: 01 = Factura, 03 = Boleta de Venta, 00 = Nota de Venta / Ticket
    document_type_code VARCHAR(2) NOT NULL,
    series VARCHAR(4) NOT NULL,
    correlative BIGINT NOT NULL,
    full_number VARCHAR(20) NOT NULL,

    -- Snapshot Emisor (Tienda)
    emitter_tax_id_type VARCHAR(20) NOT NULL,
    emitter_tax_id_number VARCHAR(11),
    emitter_legal_name VARCHAR(200) NOT NULL,
    emitter_tax_regime VARCHAR(20) NOT NULL,

    -- Snapshot Receptor (Cliente)
    customer_doc_type VARCHAR(10) NOT NULL,
    customer_doc_number VARCHAR(11),
    customer_name VARCHAR(200) NOT NULL,

    -- Totales e IGV
    taxable_amount NUMERIC(12, 2) NOT NULL,
    igv_rate NUMERIC(5, 2) NOT NULL,
    igv_amount NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',

    -- Trazabilidad de Pago
    payment_provider VARCHAR(30),
    provider_payment_id VARCHAR(100),

    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_fiscal_receipts_store_number UNIQUE (store_id, document_type_code, series, correlative),
    CONSTRAINT chk_fiscal_receipts_doc_code CHECK (document_type_code IN ('01', '03', '00')),
    CONSTRAINT chk_fiscal_receipts_amounts CHECK (
        taxable_amount >= 0 AND igv_rate >= 0 AND igv_amount >= 0 AND total_amount >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_fiscal_receipts_store_issued
    ON fiscal_receipts(store_id, issued_at DESC);

CREATE INDEX IF NOT EXISTS idx_fiscal_receipts_user_issued
    ON fiscal_receipts(user_id, issued_at DESC);

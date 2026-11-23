-- ==============================================================================
-- JaldiShop Migration V31: Create Billing, Fee Policies and Settlement Ledger
-- Version: V31__create_billing_and_settlement_ledger.sql
-- Description:
--   1. Creates fee_policies table for configurable merchant take-rates and platform fees.
--   2. Creates order_payment_breakdowns table for immutable financial snapshot per order.
--   3. Creates merchant_payout_ledgers table (append-only ledger) for merchant balance & settlements.
--   4. Creates domain_outbox_events table for transactional outbox pattern (SUNAT / Webhooks).
--   5. Seeds default Peru restaurant fee policy (3.5% online, 2.0% cash/direct, S/ 0.90 service fee).
-- ==============================================================================

-- 1. Políticas de Tarifas Configurables
CREATE TABLE IF NOT EXISTS fee_policies (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    merchant_take_rate_pct NUMERIC(5, 4) NOT NULL DEFAULT 0.0350, -- 3.50%
    merchant_cash_take_rate_pct NUMERIC(5, 4) NOT NULL DEFAULT 0.0200, -- 2.00%
    customer_service_fee_fixed NUMERIC(10, 2) NOT NULL DEFAULT 0.90, -- S/ 0.90
    customer_service_fee_threshold_waive NUMERIC(10, 2) NOT NULL DEFAULT 60.00,
    igv_rate NUMERIC(5, 4) NOT NULL DEFAULT 0.1800, -- 18.00%
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed política inicial estándar para Perú
INSERT INTO fee_policies (id, name, merchant_take_rate_pct, merchant_cash_take_rate_pct, customer_service_fee_fixed, customer_service_fee_threshold_waive, igv_rate, is_active)
VALUES ('DEFAULT_PERU_COMMERCE', 'Política Estándar Comercios Locales Perú', 0.0350, 0.0200, 0.90, 60.00, 0.1800, true)
ON CONFLICT (id) DO NOTHING;

-- 2. Desglose Financiero Inmutable de la Orden
CREATE TABLE IF NOT EXISTS order_payment_breakdowns (
    order_id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    user_id UUID NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    
    -- Subtotales
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    
    -- Tarifa de Plataforma al Comprador (Service Fee)
    platform_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    platform_fee_base NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    platform_fee_igv NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    
    -- Comisión de Plataforma al Comercio (Gravada solo sobre subtotal_amount)
    merchant_commission_base NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    merchant_commission_igv NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    merchant_commission_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    
    -- Resultados Financieros Finales
    total_paid_by_customer NUMERIC(12, 2) NOT NULL,
    merchant_net_amount NUMERIC(12, 2) NOT NULL,
    gateway_fee_estimated NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    fee_policy_id VARCHAR(50) REFERENCES fee_policies(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys e Invariantes
    CONSTRAINT fk_order_breakdowns_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT chk_breakdown_positive_amounts CHECK (
        subtotal_amount >= 0 AND
        delivery_fee >= 0 AND
        platform_fee >= 0 AND
        merchant_commission_total >= 0 AND
        merchant_net_amount >= 0 AND
        total_paid_by_customer >= 0
    ),
    CONSTRAINT chk_total_paid_integrity CHECK (
        total_paid_by_customer = (subtotal_amount + delivery_fee + platform_fee)
    ),
    CONSTRAINT chk_merchant_net_integrity CHECK (
        merchant_net_amount = (subtotal_amount + delivery_fee - merchant_commission_total)
    ),
    CONSTRAINT chk_pickup_no_delivery_fee CHECK (
        (delivery_mode = 'PICKUP' AND delivery_fee = 0.00) OR
        (delivery_mode <> 'PICKUP')
    )
);

-- Índices en Foreign Keys y búsquedas por inquilino (Store)
CREATE INDEX IF NOT EXISTS idx_breakdowns_store_created ON order_payment_breakdowns(store_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_breakdowns_user_created ON order_payment_breakdowns(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_breakdowns_fee_policy ON order_payment_breakdowns(fee_policy_id);

-- 3. Libro Mayor de Liquidaciones del Comercio (Append-Only Ledger)
CREATE TABLE IF NOT EXISTS merchant_payout_ledgers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id UUID NOT NULL,
    order_id UUID REFERENCES orders(id) ON DELETE RESTRICT,
    entry_type VARCHAR(30) NOT NULL,
    debit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    balance_after NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    reference_note TEXT,
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_ledger_positive_amounts CHECK (debit_amount >= 0 AND credit_amount >= 0),
    CONSTRAINT chk_ledger_entry_types CHECK (entry_type IN (
        'SALE_CREDIT',          -- Abono por venta de productos
        'DELIVERY_CREDIT',      -- Abono íntegro de delivery propio
        'COMMISSION_DEBIT',     -- Débito por comisión JaldiShop
        'PAYOUT_WITHDRAWAL',    -- Transferencia liquidada a cuenta bancaria
        'ADJUSTMENT'            -- Ajuste manual auditado
    ))
);

CREATE INDEX IF NOT EXISTS idx_merchant_ledger_store_id ON merchant_payout_ledgers(store_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_merchant_ledger_order_id ON merchant_payout_ledgers(order_id);

-- 4. Transactional Outbox para eventos de Dominio, SUNAT y Analítica
CREATE TABLE IF NOT EXISTS domain_outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending_events ON domain_outbox_events(status, created_at) WHERE status = 'PENDING';

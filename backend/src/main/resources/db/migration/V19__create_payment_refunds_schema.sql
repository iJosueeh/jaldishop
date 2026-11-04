-- Version: V19__create_payment_refunds_schema.sql
-- Description: Auditoría persistente, idempotencia, control de leases y trazabilidad de reembolsos para Mercado Pago.
-- 1. Añadir columna de control de lease en payments.
-- 2. Crear tabla payment_refund_intents para garantizar una única intención financiera por Payment.
-- 3. Crear tabla payment_refund_execution_attempts para auditar cada llamada técnica (POST / GET).

-- ============================================================================
-- 1. PAYMENTS: CONTROL DE LEASE PARA CONCURRENCIA DE WORKERS
-- ============================================================================
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_locked_until TIMESTAMPTZ;

-- ============================================================================
-- 2. TABLA PAYMENT_REFUND_INTENTS (INTENCIÓN FINANCIERA ÚNICA POR PAGO)
-- ============================================================================
CREATE TABLE IF NOT EXISTS payment_refund_intents (
    id UUID NOT NULL,
    payment_id UUID NOT NULL,
    provider_payment_id VARCHAR(100) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    claim_token UUID,
    lease_expires_at TIMESTAMPTZ,
    provider_refund_id VARCHAR(100),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT pk_payment_refund_intents PRIMARY KEY (id),
    CONSTRAINT fk_payment_refund_intents_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
    CONSTRAINT uq_payment_refund_intents_payment_id UNIQUE (payment_id),
    CONSTRAINT uq_payment_refund_intents_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_refund_intents_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_refund_intents_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX IF NOT EXISTS idx_payment_refund_intents_payment_id
    ON payment_refund_intents (payment_id);

CREATE INDEX IF NOT EXISTS idx_payment_refund_intents_provider_payment
    ON payment_refund_intents (provider_payment_id);

CREATE INDEX IF NOT EXISTS idx_payment_refund_intents_lease
    ON payment_refund_intents (status, lease_expires_at);

-- ============================================================================
-- 3. TABLA PAYMENT_REFUND_EXECUTION_ATTEMPTS (AUDITORÍA TÉCNICA DE LLAMADAS HTTP)
-- ============================================================================
CREATE TABLE IF NOT EXISTS payment_refund_execution_attempts (
    id UUID NOT NULL,
    refund_intent_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    request_method VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    http_status INT,
    response_body TEXT,
    error_code VARCHAR(80),
    error_message TEXT,
    executed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payment_refund_execution_attempts PRIMARY KEY (id),
    CONSTRAINT fk_payment_refund_execution_refund_intent FOREIGN KEY (refund_intent_id) REFERENCES payment_refund_intents(id) ON DELETE CASCADE,
    CONSTRAINT ck_payment_refund_execution_method CHECK (request_method IN ('POST', 'GET')),
    CONSTRAINT ck_payment_refund_execution_status CHECK (status IN ('SUCCESSFUL', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX IF NOT EXISTS idx_payment_refund_execution_intent_id
    ON payment_refund_execution_attempts (refund_intent_id);

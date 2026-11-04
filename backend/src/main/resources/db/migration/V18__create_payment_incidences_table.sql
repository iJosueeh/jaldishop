-- Version: V18__create_payment_incidences_table.sql
-- Description: Auditoría, trazabilidad y control de reintentos para incidencias operativas, comerciales y técnicas de pagos.
-- 1. Permite registrar fallos de confirmación de órdenes y ejecución de reembolsos de forma desacoplada de payments.
-- 2. Garantiza unicidad de incidencias activas por pago y tipo de proceso.
-- 3. Proporciona soporte para backoff exponencial y escalamiento a revisión manual.

CREATE TABLE IF NOT EXISTS payment_incidences (
    id UUID NOT NULL,
    payment_id UUID NOT NULL,
    process_type VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL,
    code VARCHAR(80) NOT NULL,
    message TEXT NOT NULL,
    attempts_count INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT pk_payment_incidences PRIMARY KEY (id),
    CONSTRAINT fk_payment_incidences_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT ck_payment_incidences_process_type CHECK (process_type IN ('ORDER_CONFIRMATION', 'REFUND_EXECUTION')),
    CONSTRAINT ck_payment_incidences_category CHECK (category IN ('COMMERCIAL', 'TECHNICAL', 'OPERATIONAL')),
    CONSTRAINT ck_payment_incidences_status CHECK (status IN ('OPEN', 'RESOLVED', 'MANUAL_REVIEW_REQUIRED')),
    CONSTRAINT ck_payment_incidences_attempts CHECK (attempts_count >= 1 AND attempts_count <= max_attempts + 1)
);

CREATE INDEX IF NOT EXISTS idx_payment_incidences_payment_id
    ON payment_incidences (payment_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_incidences_active
    ON payment_incidences (payment_id, process_type)
    WHERE status IN ('OPEN', 'MANUAL_REVIEW_REQUIRED');

CREATE INDEX IF NOT EXISTS idx_payment_incidences_retry_lookup
    ON payment_incidences (payment_id, process_type, status, next_retry_at);

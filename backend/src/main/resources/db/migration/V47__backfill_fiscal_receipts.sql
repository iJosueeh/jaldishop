-- ==============================================================================
-- JaldiShop Migration V47: Backfill Fiscal Receipts for Existing Orders
-- Version: V47__backfill_fiscal_receipts.sql
-- Description:
--   1. Issues retroactive fiscal_receipts for existing non-cancelled orders that
--      do not yet have a receipt (preserving payment_provider = 'MERCADO_PAGO'
--      when payment_id IS NOT NULL).
--   2. Synchronizes store_receipt_sequences with the highest correlative per store.
-- ==============================================================================

WITH existing_orders AS (
    SELECT
        o.id AS order_id,
        o.payment_id,
        o.store_id,
        o.user_id,
        o.customer_name,
        o.total_amount,
        COALESCE(NULLIF(o.currency, ''), 'PEN') AS currency,
        o.confirmed_at,
        s.tax_id_type AS emitter_tax_id_type,
        s.tax_id_number AS emitter_tax_id_number,
        COALESCE(NULLIF(BTRIM(s.legal_business_name), ''), s.name) AS emitter_legal_name,
        s.tax_regime AS emitter_tax_regime,
        CASE
            WHEN s.tax_id_type IN ('RUC_10', 'RUC_20') AND NULLIF(BTRIM(s.tax_id_number), '') IS NOT NULL
                THEN '03'
            ELSE '00'
        END AS document_type_code,
        CASE
            WHEN s.tax_id_type IN ('RUC_10', 'RUC_20') AND NULLIF(BTRIM(s.tax_id_number), '') IS NOT NULL
                THEN 'B001'
            ELSE 'NV01'
        END AS series,
        CASE
            WHEN s.tax_regime IN ('RUS', 'NONE') THEN 0.00
            ELSE COALESCE(s.tax_rate, 18.00)
        END AS igv_rate,
        COALESCE(p.customer_doc_type, 'NONE') AS customer_doc_type,
        p.customer_doc_number,
        (
            SELECT pa.provider_payment_id
            FROM payment_attempts pa
            WHERE pa.payment_id = o.payment_id
              AND pa.status = 'APPROVED'
              AND pa.provider_payment_id IS NOT NULL
            ORDER BY pa.attempt_number DESC
            LIMIT 1
        ) AS provider_payment_id
    FROM orders o
    JOIN stores s ON s.id = o.store_id
    LEFT JOIN payments p ON p.id = o.payment_id
    WHERE o.status <> 'CANCELLED'
      AND NOT EXISTS (
          SELECT 1 FROM fiscal_receipts fr WHERE fr.order_id = o.id
      )
),
numbered AS (
    SELECT
        eo.*,
        COALESCE(seq.last_correlative, 0) + ROW_NUMBER() OVER (
            PARTITION BY eo.store_id, eo.document_type_code
            ORDER BY eo.confirmed_at ASC, eo.order_id ASC
        ) AS correlative
    FROM existing_orders eo
    LEFT JOIN store_receipt_sequences seq
      ON seq.store_id = eo.store_id
     AND seq.document_type_code = eo.document_type_code
)
INSERT INTO fiscal_receipts (
    id, order_id, payment_id, store_id, user_id,
    document_type_code, series, correlative, full_number,
    emitter_tax_id_type, emitter_tax_id_number, emitter_legal_name, emitter_tax_regime,
    customer_doc_type, customer_doc_number, customer_name,
    taxable_amount, igv_rate, igv_amount, total_amount, currency,
    payment_provider, provider_payment_id, issued_at
)
SELECT
    gen_random_uuid(),
    n.order_id,
    n.payment_id,
    n.store_id,
    n.user_id,
    n.document_type_code,
    n.series,
    n.correlative,
    n.series || '-' || LPAD(n.correlative::text, 8, '0'),
    n.emitter_tax_id_type,
    n.emitter_tax_id_number,
    n.emitter_legal_name,
    n.emitter_tax_regime,
    n.customer_doc_type,
    n.customer_doc_number,
    COALESCE(NULLIF(BTRIM(n.customer_name), ''), 'Cliente General'),
    CASE
        WHEN n.igv_rate > 0 THEN ROUND(n.total_amount / (1 + (n.igv_rate / 100.0)), 2)
        ELSE n.total_amount
    END,
    n.igv_rate,
    CASE
        WHEN n.igv_rate > 0 THEN n.total_amount - ROUND(n.total_amount / (1 + (n.igv_rate / 100.0)), 2)
        ELSE 0.00
    END,
    n.total_amount,
    n.currency,
    CASE WHEN n.payment_id IS NOT NULL THEN 'MERCADO_PAGO' ELSE 'DIRECT' END,
    n.provider_payment_id,
    n.confirmed_at
FROM numbered n
ON CONFLICT (order_id) DO NOTHING;

INSERT INTO store_receipt_sequences (store_id, document_type_code, series, last_correlative, updated_at)
SELECT
    fr.store_id,
    fr.document_type_code,
    MAX(fr.series),
    MAX(fr.correlative),
    CURRENT_TIMESTAMP
FROM fiscal_receipts fr
GROUP BY fr.store_id, fr.document_type_code
ON CONFLICT (store_id, document_type_code)
DO UPDATE SET
    last_correlative = GREATEST(store_receipt_sequences.last_correlative, EXCLUDED.last_correlative),
    updated_at = CURRENT_TIMESTAMP;

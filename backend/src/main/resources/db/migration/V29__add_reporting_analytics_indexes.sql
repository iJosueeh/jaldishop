-- ==============================================================================
-- JaldiShop Migration V29: Reporting & Analytics Indexes
-- Version: V29__add_reporting_analytics_indexes.sql
-- Description:
--   1. Global orders confirmation timestamp index for platform-wide admin reporting.
--   2. Payment approval index for financial inflow queries.
--   3. Partial index for completed refunds on payments.
--   4. Partial index on order_status_history for cancellation tracking.
--   Strictly DDL, safe and non-blocking.
-- ==============================================================================

-- 1. Index for global platform orders querying without store_id
CREATE INDEX IF NOT EXISTS idx_orders_confirmed_at
    ON orders (confirmed_at DESC);

-- 2. Index for payments gross inflow aggregation
CREATE INDEX IF NOT EXISTS idx_payments_status_approved
    ON payments (status, approved_at DESC);

-- 3. Partial index for settled refunds aggregation
CREATE INDEX IF NOT EXISTS idx_payments_refund_completed
    ON payments (refunded_at DESC)
    WHERE refund_status = 'COMPLETED';

-- 4. Partial index for cancellation transitions lookup
CREATE INDEX IF NOT EXISTS idx_order_status_history_cancelled
    ON order_status_history (changed_at DESC)
    WHERE status = 'CANCELLED';

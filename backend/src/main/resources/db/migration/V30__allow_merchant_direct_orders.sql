-- ==============================================================================
-- JaldiShop Migration V30: Allow Merchant Direct Orders (Manual / WhatsApp / POS)
-- Version: V30__allow_merchant_direct_orders.sql
-- Description:
--   1. Relaxes NOT NULL constraints on payment_id and capacity_reservation_id
--      in orders table to allow merchant direct/manual orders without web checkout.
--   2. Adds origin ('ONLINE', 'MANUAL', 'WHATSAPP'), payment_method and notes columns.
--   3. Adds index on (store_id, origin) for fast filtering.
-- ==============================================================================

-- 1. Relax NOT NULL constraints for manual/direct orders
ALTER TABLE orders ALTER COLUMN payment_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN capacity_reservation_id DROP NOT NULL;

-- 2. Add origin, payment_method and notes columns
ALTER TABLE orders ADD COLUMN IF NOT EXISTS origin VARCHAR(20) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS notes TEXT;

-- 3. Add index for origin filtering within store tenant
CREATE INDEX IF NOT EXISTS idx_orders_store_origin ON orders (store_id, origin);

-- Version: V17__expand_payment_persistence_and_attempts.sql
-- Description: Preparar la persistencia de payments y payment_attempts para Checkout Pro (Mercado Pago).
-- 1. Ampliar restricción de métodos de pago en payment_attempts para soportar billeteras, transferencias y pasarelas de Checkout Pro.
-- 2. Añadir identificadores de preferencia de pago (preference_id, init_point) en payment_attempts.
-- 3. Añadir columnas de despacho, snapshot de precios e ítems congelados en payments (sin default forzado).
-- 4. Backfill seguro y documentado desde orders exclusivamente donde exista evidencia real.

-- ============================================================================
-- 1. PAYMENT_ATTEMPTS: MÉTODOS DE PAGO Y PREFERENCIA MERCADO PAGO
-- ============================================================================

-- Modificar restricción de método de pago para admitir métodos soportados por Mercado Pago en Perú
ALTER TABLE payment_attempts DROP CONSTRAINT IF EXISTS ck_payment_attempts_payment_method;

ALTER TABLE payment_attempts ADD CONSTRAINT ck_payment_attempts_payment_method
    CHECK (payment_method IN ('CARD', 'ACCOUNT_MONEY', 'WALLET_PURCHASE', 'TICKET', 'BANK_TRANSFER', 'YAPE', 'OTHER'));

-- Identificadores de sesión de Checkout Pro generados por Mercado Pago
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS preference_id VARCHAR(100);
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS init_point TEXT;

CREATE INDEX IF NOT EXISTS idx_payment_attempts_preference_id
    ON payment_attempts (preference_id)
    WHERE preference_id IS NOT NULL;

-- ============================================================================
-- 2. PAYMENTS: DESPACHO, SNAPSHOT DE PRECIOS Y PRODUCTOS PARA CHECKOUT ASÍNCRONO
-- ============================================================================

-- Modo de entrega y datos de contacto / despacho (NULLABLE para conservar historial sin suposiciones)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS fulfillment_type VARCHAR(20);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(30);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS customer_email VARCHAR(254);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_address TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_reference TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_latitude NUMERIC(9,6);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_longitude NUMERIC(9,6);

-- Restricción de tipo de despacho congruente con el dominio (PICKUP o DELIVERY)
ALTER TABLE payments ADD CONSTRAINT ck_payments_fulfillment_type
    CHECK (fulfillment_type IS NULL OR fulfillment_type IN ('PICKUP', 'DELIVERY'));

ALTER TABLE payments ADD CONSTRAINT ck_payments_delivery_latitude
    CHECK (delivery_latitude IS NULL OR (delivery_latitude >= -90 AND delivery_latitude <= 90));

ALTER TABLE payments ADD CONSTRAINT ck_payments_delivery_longitude
    CHECK (delivery_longitude IS NULL OR (delivery_longitude >= -180 AND delivery_longitude <= 180));

-- Desglose de precios determinado por el backend (NULLABLE para registros históricos)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS products_subtotal_amount NUMERIC(12,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2) DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS discount_code VARCHAR(80);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_fee_amount NUMERIC(12,2) DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS included_tax_amount NUMERIC(12,2) DEFAULT 0;

ALTER TABLE payments ADD CONSTRAINT ck_payments_products_subtotal
    CHECK (products_subtotal_amount IS NULL OR products_subtotal_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT ck_payments_discount
    CHECK (discount_amount IS NULL OR discount_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT ck_payments_delivery_fee
    CHECK (delivery_fee_amount IS NULL OR delivery_fee_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT ck_payments_tax
    CHECK (included_tax_amount IS NULL OR included_tax_amount >= 0);

-- Snapshot de ítems congelados al momento de iniciar checkout (JSONB para no depender del carrito mutable)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS items_snapshot JSONB;

-- ============================================================================
-- 3. BACKFILL CONDICIONAL EXCLUSIVAMENTE DONDE EXISTA EVIDENCIA HISTÓRICA EN ORDERS
-- (No se asigna STORE_PICKUP ni valores por defecto arbitrarios a filas sin pedido)
-- ============================================================================

UPDATE payments p
SET fulfillment_type = o.delivery_mode,
    customer_name = o.customer_name,
    customer_phone = o.customer_phone,
    customer_email = o.customer_email,
    delivery_address = o.delivery_address,
    delivery_reference = o.delivery_reference,
    delivery_latitude = o.delivery_latitude,
    delivery_longitude = o.delivery_longitude,
    products_subtotal_amount = o.products_subtotal_amount,
    discount_amount = o.discount_amount,
    discount_code = o.discount_code,
    delivery_fee_amount = o.delivery_fee_amount,
    included_tax_amount = o.included_tax_amount
FROM orders o
WHERE o.payment_id = p.id;

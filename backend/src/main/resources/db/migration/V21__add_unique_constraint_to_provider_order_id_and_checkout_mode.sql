-- Version: V21__add_unique_constraint_to_provider_order_id_and_checkout_mode.sql
-- Description: Añadir columna checkout_mode en payment_attempts para persistir explícitamente
-- la modalidad (PREFERENCES u ORDERS), y reemplazar el índice simple de provider_order_id
-- por un índice UNIQUE parcial, garantizando cardinalidad 1:1 estricta por intento.

-- 1. Añadir columna checkout_mode con valor por defecto 'PREFERENCES' para preservar compatibilidad con intentos históricos
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS checkout_mode VARCHAR(20) NOT NULL DEFAULT 'PREFERENCES';

-- 2. Eliminar el índice no único anterior de V20 si existe
DROP INDEX IF EXISTS idx_payment_attempts_provider_order_id;

-- 3. Crear índice UNIQUE condicional parcial para provider_order_id
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_attempts_provider_order_id
    ON payment_attempts (provider_order_id)
    WHERE provider_order_id IS NOT NULL;

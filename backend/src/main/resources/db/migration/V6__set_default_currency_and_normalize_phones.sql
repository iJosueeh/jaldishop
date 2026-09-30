-- ====================================================================
-- JaldiShop - Set Default Currency & Normalize Contact Phones
-- Version: V6__set_default_currency_and_normalize_phones.sql
-- Description: Establece 'PEN' como moneda por defecto en tiendas y
--              normaliza los números celulares al prefijo internacional +51
-- ====================================================================

-- 1. Establecer 'PEN' por defecto en delivery_fee_currency de tiendas
ALTER TABLE stores ALTER COLUMN delivery_fee_currency SET DEFAULT 'PEN';

UPDATE stores
SET delivery_fee_currency = 'PEN'
WHERE delivery_fee_currency IS NULL OR TRIM(delivery_fee_currency) = '';

-- 2. Normalizar teléfonos de usuarios con prefijo +51 si tienen formato nacional de 9 dígitos
UPDATE users
SET phone = '+51' || TRIM(phone)
WHERE phone ~ '^9[0-9]{8}$';

-- 3. Normalizar teléfonos de contacto de tiendas con prefijo +51 si tienen formato nacional de 9 dígitos
UPDATE stores
SET contact_phone = '+51' || TRIM(contact_phone)
WHERE contact_phone ~ '^9[0-9]{8}$';

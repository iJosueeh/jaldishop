-- ==============================================================================
-- JaldiShop Migration V5: Remove tax_applies and standardize tax_rate for MVP
-- ==============================================================================

-- 1. Remove obsolete tax_applies column
ALTER TABLE stores DROP COLUMN IF EXISTS tax_applies;

-- 2. Standardize tax_rate for all existing stores to 18.00% (Peru MVP default)
UPDATE stores SET tax_rate = 18.00 WHERE tax_rate IS NULL;

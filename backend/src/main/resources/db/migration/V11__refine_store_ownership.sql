-- ==============================================================================
-- JaldiShop Migration V11: Refine Store Ownership & Retire Synchronizer Trigger
-- Version: V11__refine_store_ownership.sql
-- Description:
--   1. Rename stores.merchant_user_id to owner_user_id (clarifies ownership vs role).
--   2. Rename associated UNIQUE constraint and Foreign Key.
--   3. Retire V10 trigger trg_sync_merchant_user_role_store to move role assignment
--      orchestration explicitly to Spring Boot's CreateStoreService @Transactional.
-- ==============================================================================

-- 1. Rename column in stores
ALTER TABLE stores RENAME COLUMN merchant_user_id TO owner_user_id;

-- 2. Rename UNIQUE constraint
ALTER TABLE stores RENAME CONSTRAINT uq_stores_merchant_user_id TO uq_stores_owner_user_id;

-- 3. Rename Foreign Key
ALTER TABLE stores RENAME CONSTRAINT fk_stores_merchant_user_id TO fk_stores_owner_user_id;

-- 4. Retire V10 trigger & helper function (responsibility shifted to application layer)
DROP TRIGGER IF EXISTS trg_sync_merchant_user_role_store ON stores;
DROP FUNCTION IF EXISTS sync_merchant_user_role_store_id();

-- 5. Documentation comment
COMMENT ON COLUMN stores.owner_user_id IS 'ID del usuario propietario de la tienda (ownership estructural).';

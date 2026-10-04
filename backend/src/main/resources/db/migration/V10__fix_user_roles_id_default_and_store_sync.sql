-- ==============================================================================
-- JaldiShop Migration V10: Set Default for user_roles.id & Auto-sync store_id
-- Version: V10__fix_user_roles_id_default_and_store_sync.sql
-- Description:
--   1. Set DEFAULT gen_random_uuid() for user_roles.id so JPA join table inserts succeed.
--   2. Ensure all existing rows have a valid id.
--   3. Add trigger to automatically populate store_id for MERCHANT role on store creation.
-- ==============================================================================

-- 1. Ensure user_roles.id has a default value generated automatically
ALTER TABLE user_roles ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- 2. Backfill any existing row that might have id IS NULL (safety)
UPDATE user_roles SET id = gen_random_uuid() WHERE id IS NULL;

-- 3. Automatic synchronization/assignment of user_roles.store_id when a new store is created
CREATE OR REPLACE FUNCTION sync_merchant_user_role_store_id()
RETURNS TRIGGER AS $$
DECLARE
    v_merchant_role_id SMALLINT;
BEGIN
    SELECT id INTO v_merchant_role_id FROM roles WHERE name = 'MERCHANT' LIMIT 1;
    
    IF v_merchant_role_id IS NOT NULL THEN
        UPDATE user_roles
        SET store_id = NEW.id
        WHERE user_id = NEW.merchant_user_id
          AND role_id = v_merchant_role_id;

        IF NOT FOUND THEN
            INSERT INTO user_roles (id, user_id, role_id, store_id)
            VALUES (gen_random_uuid(), NEW.merchant_user_id, v_merchant_role_id, NEW.id);
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_merchant_user_role_store ON stores;
CREATE TRIGGER trg_sync_merchant_user_role_store
AFTER INSERT ON stores
FOR EACH ROW
EXECUTE FUNCTION sync_merchant_user_role_store_id();

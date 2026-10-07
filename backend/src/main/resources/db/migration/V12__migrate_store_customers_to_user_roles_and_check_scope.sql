-- ==============================================================================
-- JaldiShop Migration V12: Migrate Store Customers to User Roles & Check Role Scope
-- Version: V12__migrate_store_customers_to_user_roles_and_check_scope.sql
-- Description:
--   1. Migrate store_customers records into contextual user_roles (role_id = 1, CUSTOMER).
--   2. Preserve existing global CUSTOMER records (marketplace scope).
--   3. Validate that every relationship in store_customers has a contextual CUSTOMER role.
--   4. Validate that no MERCHANT (role_id=2) has store_id NULL and no ADMIN (role_id=3) has store_id NOT NULL.
--   5. Add CHECK constraint on user_roles ensuring:
--        - CUSTOMER (role_id=1) can be global (store_id NULL) or contextual (store_id NOT NULL).
--        - MERCHANT (role_id=2) must be contextual (store_id NOT NULL).
--        - ADMIN (role_id=3) must be global (store_id IS NULL).
--   6. Drop legacy index and table store_customers without CASCADE.
-- ==============================================================================

-- 1. Migrate all store_customers into contextual user_roles (role_id = 1, CUSTOMER)
INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), sc.user_id, 1, sc.store_id
FROM store_customers sc
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = sc.user_id
      AND ur.role_id = 1
      AND ur.store_id = sc.store_id
);

-- 2. Post-migration validation: verify that all store_customers records have been contextualized
DO $$
DECLARE
    v_unmigrated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_unmigrated_count
    FROM store_customers sc
    WHERE NOT EXISTS (
        SELECT 1 FROM user_roles ur
        WHERE ur.user_id = sc.user_id
          AND ur.role_id = 1
          AND ur.store_id = sc.store_id
    );

    IF v_unmigrated_count > 0 THEN
        RAISE EXCEPTION 'Fallo de migración: % registros de store_customers no tienen un CUSTOMER contextual en user_roles.', v_unmigrated_count;
    END IF;
END $$;

-- 3. Strict Pre-Validation before applying CHECK constraint:
-- CUSTOMER (role_id = 1) can have store_id NULL (global) or store_id NOT NULL (contextual) -> both valid.
-- MERCHANT (role_id = 2) must have store_id NOT NULL.
-- ADMIN    (role_id = 3) must have store_id IS NULL.
DO $$
DECLARE
    v_unresolved_merchants INTEGER;
    v_invalid_admins       INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_unresolved_merchants FROM user_roles WHERE role_id = 2 AND store_id IS NULL;
    SELECT COUNT(*) INTO v_invalid_admins       FROM user_roles WHERE role_id = 3 AND store_id IS NOT NULL;

    IF v_unresolved_merchants > 0 THEN
        RAISE EXCEPTION 'Inconsistencia en user_roles: % registros de MERCHANT (role_id=2) tienen store_id NULL.', v_unresolved_merchants;
    END IF;

    IF v_invalid_admins > 0 THEN
        RAISE EXCEPTION 'Inconsistencia en user_roles: % registros de ADMIN (role_id=3) tienen store_id no nulo.', v_invalid_admins;
    END IF;
END $$;

-- 4. Enforce role scope constraint at database level
ALTER TABLE user_roles
    ADD CONSTRAINT ck_user_roles_role_scope
    CHECK (
           role_id = 1
        OR (role_id = 2 AND store_id IS NOT NULL)
        OR (role_id = 3 AND store_id IS NULL)
    );

-- 5. Drop legacy objects cleanly without CASCADE
DROP INDEX IF EXISTS idx_store_customers_user_id;
DROP TABLE store_customers;

-- ==============================================================================
-- JaldiShop Migration V13: Strengthen Inventory Reservations Multitenancy
-- Version: V13__strengthen_inventory_reservations_multitenancy.sql
-- Description:
--   1. Validate cross-tenant consistency between capacity reservations and variants.
--   2. Add store_id to inventory_reservations and backfill from capacity_reservations.
--   3. Apply NOT NULL on inventory_reservations.store_id.
--   4. Add candidate key uq_capacity_reservations_store_id on capacity_reservations(store_id, id).
--   5. Replace simple FKs with composite multitenant FKs ensuring child.store_id = parent.store_id.
-- ==============================================================================

-- 1. Pre-Validation: Abort if cross-tenant reservations exist
DO $$
DECLARE
    v_cross_tenant_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_cross_tenant_count
    FROM inventory_reservations ir
    JOIN capacity_reservations cr ON ir.capacity_reservation_id = cr.id
    JOIN product_variants pv ON ir.variant_id = pv.id
    WHERE cr.store_id != pv.store_id;

    IF v_cross_tenant_count > 0 THEN
        RAISE EXCEPTION 'Inconsistencia cross-tenant detectada en % reservas de inventario (capacity_reservation.store_id != product_variant.store_id). Abortando.', v_cross_tenant_count;
    END IF;
END $$;

-- 2. Add store_id column initially nullable
ALTER TABLE inventory_reservations ADD COLUMN store_id UUID;

-- 3. Backfill store_id from capacity_reservations
UPDATE inventory_reservations ir
SET store_id = cr.store_id
FROM capacity_reservations cr
WHERE ir.capacity_reservation_id = cr.id;

-- 4. Validate that all rows were backfilled
DO $$
DECLARE
    v_null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_null_count
    FROM inventory_reservations
    WHERE store_id IS NULL;

    IF v_null_count > 0 THEN
        RAISE EXCEPTION 'Existen % registros en inventory_reservations sin store_id tras el backfill.', v_null_count;
    END IF;
END $$;

-- 5. Enforce NOT NULL on store_id
ALTER TABLE inventory_reservations ALTER COLUMN store_id SET NOT NULL;

-- 6. Add candidate key on capacity_reservations to support composite FK
ALTER TABLE capacity_reservations
    ADD CONSTRAINT uq_capacity_reservations_store_id UNIQUE (store_id, id);

-- 7. Replace simple FKs with composite multitenant FKs
ALTER TABLE inventory_reservations DROP CONSTRAINT fk_inventory_reservations_capacity_reservation;
ALTER TABLE inventory_reservations DROP CONSTRAINT fk_inventory_reservations_variant;

ALTER TABLE inventory_reservations
    ADD CONSTRAINT fk_inv_res_capacity_reservation_store
    FOREIGN KEY (store_id, capacity_reservation_id)
    REFERENCES capacity_reservations(store_id, id) ON DELETE CASCADE;

ALTER TABLE inventory_reservations
    ADD CONSTRAINT fk_inv_res_variant_store
    FOREIGN KEY (store_id, variant_id)
    REFERENCES product_variants(store_id, id) ON DELETE RESTRICT;

-- 8. Add tenant index
CREATE INDEX idx_inventory_reservations_store_id ON inventory_reservations (store_id);

-- 9. Documentation comment
COMMENT ON COLUMN inventory_reservations.store_id IS 'ID de la tienda (aislamiento multitenant verificado mediante FK compuestas).';

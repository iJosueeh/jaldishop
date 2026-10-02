-- ==============================================================================
-- JaldiShop Migration V7: Strengthen Relational Integrity, Multitenancy & Domains
-- Version: V7__strengthen_relational_integrity_and_multitenancy.sql
-- Description:
--   1. Store Categories (Rubros) and assignments (N:M).
--   2. Storefront attributes (logo, banner, social URLs) and coordinates constraint.
--   3. Contextual User Roles (User <-> Role <-> Store) with partial unique indexes.
--   4. Multitenant Category -> Product integrity (composite FK).
--   5. Multitenant Product -> ProductVariant integrity (store_id, composite FK, per-store SKU).
--   6. Multiple Product Images with single primary image constraint.
--   7. Multitenant Cart -> CartItem -> ProductVariant integrity (store_id, composite FKs).
--   8. Capacity reservations and Orders service time consistency constraints.
--   9. Inventory Reservations for checkout stock holds.
--  10. Order -> CapacityReservation (store+user) & Order -> Payment integrity.
--  11. Order -> OrderItem -> ProductVariant multitenant integrity.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. STORE CATEGORIES (Rubros Comerciales Generales)
-- ------------------------------------------------------------------------------
CREATE TABLE store_categories (
    id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_store_categories PRIMARY KEY (id),
    CONSTRAINT uq_store_categories_slug UNIQUE (slug),
    CONSTRAINT ck_store_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE store_category_assignments (
    store_id UUID NOT NULL,
    store_category_id UUID NOT NULL,
    CONSTRAINT pk_store_category_assignments PRIMARY KEY (store_id, store_category_id),
    CONSTRAINT fk_store_cat_assign_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_store_cat_assign_category FOREIGN KEY (store_category_id) REFERENCES store_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_store_cat_assign_category_id ON store_category_assignments (store_category_id);

INSERT INTO store_categories (id, name, slug, description, status, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111001', 'Restaurantes y Cafeterías', 'restaurantes-cafeterias', 'Comida preparada, cafés, repostería y bebidas', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111002', 'Moda y Calzado', 'moda-calzado', 'Prendas de vestir, zapatos y accesorios de moda', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111003', 'Supermercado y Bodega', 'supermercado-bodega', 'Abarrotes, alimentos frescos y productos de primera necesidad', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111004', 'Tecnología y Electrónica', 'tecnologia-electronica', 'Dispositivos electrónicos, computadoras y gadgets', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111005', 'Salud y Belleza', 'salud-belleza', 'Cosméticos, cuidado personal y bienestar', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111006', 'Hogar y Decoración', 'hogar-decoracion', 'Muebles, artículos para el hogar y decoración', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111007', 'Mascotas', 'mascotas', 'Alimentos y accesorios para mascotas', 'ACTIVE', NOW(), NOW()),
('11111111-1111-1111-1111-111111111008', 'Servicios y Otros', 'servicios-otros', 'Servicios profesionales y diversos', 'ACTIVE', NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 2. STOREFRONT & COORDINATES INTEGRITY IN STORES
-- ------------------------------------------------------------------------------
ALTER TABLE stores
    ADD COLUMN logo_url TEXT,
    ADD COLUMN banner_url TEXT,
    ADD COLUMN instagram_url VARCHAR(255),
    ADD COLUMN facebook_url VARCHAR(255),
    ADD COLUMN whatsapp_number VARCHAR(30);

ALTER TABLE stores
    ADD CONSTRAINT ck_stores_coordinates_null CHECK ((latitude IS NULL) = (longitude IS NULL));

-- ------------------------------------------------------------------------------
-- 3. CONTEXTUAL USER_ROLES (User <-> Role <-> Store)
-- ------------------------------------------------------------------------------
ALTER TABLE user_roles ADD COLUMN id UUID;
UPDATE user_roles SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE user_roles ALTER COLUMN id SET NOT NULL;

ALTER TABLE user_roles ADD COLUMN store_id UUID;

-- Backfill para MERCHANT (role_id = 2) desde stores.merchant_user_id
UPDATE user_roles ur
SET store_id = s.id
FROM stores s
WHERE ur.user_id = s.merchant_user_id AND ur.role_id = 2;

-- Reemplazar Primary Key por Surrogate ID y agregar FK hacia stores
ALTER TABLE user_roles DROP CONSTRAINT pk_user_roles;
ALTER TABLE user_roles ADD CONSTRAINT pk_user_roles PRIMARY KEY (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_idx_user_roles_global ON user_roles (user_id, role_id) WHERE store_id IS NULL;
CREATE UNIQUE INDEX uq_idx_user_roles_store ON user_roles (user_id, role_id, store_id) WHERE store_id IS NOT NULL;
CREATE INDEX idx_user_roles_store_id ON user_roles (store_id) WHERE store_id IS NOT NULL;

-- ------------------------------------------------------------------------------
-- 4. MULTITENANT CATEGORY -> PRODUCT INTEGRITY
-- ------------------------------------------------------------------------------
ALTER TABLE categories ADD CONSTRAINT uq_categories_store_id_id UNIQUE (store_id, id);

ALTER TABLE products DROP CONSTRAINT fk_products_category_id;
ALTER TABLE products
    ADD CONSTRAINT fk_products_category_store FOREIGN KEY (store_id, category_id) REFERENCES categories(store_id, id) ON DELETE RESTRICT;

-- ------------------------------------------------------------------------------
-- 5. MULTITENANT PRODUCT -> PRODUCT_VARIANTS & SKU POR TIENDA
-- ------------------------------------------------------------------------------
ALTER TABLE products ADD CONSTRAINT uq_products_store_id_id UNIQUE (store_id, id);

ALTER TABLE product_variants ADD COLUMN store_id UUID;

-- Backfill store_id en product_variants
UPDATE product_variants pv
SET store_id = p.store_id
FROM products p
WHERE pv.product_id = p.id;

ALTER TABLE product_variants ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE product_variants DROP CONSTRAINT fk_product_variants_product_id;
ALTER TABLE product_variants
    ADD CONSTRAINT fk_product_variants_product_store FOREIGN KEY (store_id, product_id) REFERENCES products(store_id, id) ON DELETE CASCADE;

DROP INDEX IF EXISTS uq_idx_product_variants_sku;
CREATE UNIQUE INDEX uq_idx_product_variants_store_sku ON product_variants (store_id, sku) WHERE sku IS NOT NULL;

ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_store_id_id UNIQUE (store_id, id);
CREATE INDEX idx_product_variants_store_id ON product_variants (store_id);

-- ------------------------------------------------------------------------------
-- 6. PRODUCT IMAGES (Múltiples imágenes con imagen principal única)
-- ------------------------------------------------------------------------------
CREATE TABLE product_images (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_images_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX uq_idx_product_images_primary ON product_images (product_id) WHERE is_primary = true;
CREATE INDEX idx_product_images_product_position ON product_images (product_id, position ASC);

-- Backfill desde products.image_url existente
INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at)
SELECT gen_random_uuid(), p.id, p.image_url, 0, true, NOW()
FROM products p
WHERE p.image_url IS NOT NULL AND TRIM(p.image_url) != ''
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 7. MULTITENANT CART -> CART_ITEMS -> PRODUCT_VARIANTS INTEGRITY
-- ------------------------------------------------------------------------------
ALTER TABLE carts ADD CONSTRAINT uq_carts_store_id_id UNIQUE (store_id, id);

ALTER TABLE cart_items ADD COLUMN store_id UUID;

-- Backfill store_id en cart_items
UPDATE cart_items ci
SET store_id = c.store_id
FROM carts c
WHERE ci.cart_id = c.id;

ALTER TABLE cart_items ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE cart_items DROP CONSTRAINT fk_cart_items_cart_id;
ALTER TABLE cart_items DROP CONSTRAINT fk_cart_items_variant_id;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_cart_store FOREIGN KEY (store_id, cart_id) REFERENCES carts(store_id, id) ON DELETE CASCADE;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_variant_store FOREIGN KEY (store_id, variant_id) REFERENCES product_variants(store_id, id) ON DELETE CASCADE;

CREATE INDEX idx_cart_items_store_id ON cart_items (store_id);

-- ------------------------------------------------------------------------------
-- 8. CAPACITY RESERVATIONS & ORDERS SERVICE TIME CONSISTENCY
-- ------------------------------------------------------------------------------
ALTER TABLE capacity_reservations
    ADD CONSTRAINT ck_capacity_reservations_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
    ADD CONSTRAINT ck_capacity_reservations_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time);

ALTER TABLE capacity_reservations ADD CONSTRAINT uq_capacity_reservations_store_user_id UNIQUE (store_id, user_id, id);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS ck_orders_service_time;
ALTER TABLE orders
    ADD CONSTRAINT ck_orders_service_time_null CHECK ((service_start_time IS NULL) = (service_end_time IS NULL)),
    ADD CONSTRAINT ck_orders_service_time_order CHECK (service_start_time IS NULL OR service_end_time IS NULL OR service_start_time < service_end_time);

-- ------------------------------------------------------------------------------
-- 9. INVENTORY RESERVATIONS (Holds temporales de stock para checkout)
-- ------------------------------------------------------------------------------
CREATE TABLE inventory_reservations (
    id UUID NOT NULL,
    capacity_reservation_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_inventory_reservations PRIMARY KEY (id),
    CONSTRAINT fk_inventory_reservations_capacity_reservation FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_reservations_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
    CONSTRAINT uq_inventory_reservations_reservation_variant UNIQUE (capacity_reservation_id, variant_id),
    CONSTRAINT ck_inventory_reservations_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservations_status CHECK (status IN ('ACTIVE', 'COMMITTED', 'EXPIRED', 'RELEASED'))
);

CREATE INDEX idx_inv_res_variant_status ON inventory_reservations (variant_id, status);
CREATE INDEX idx_inv_res_expires ON inventory_reservations (expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_inv_res_capacity_reservation ON inventory_reservations (capacity_reservation_id);

-- ------------------------------------------------------------------------------
-- 10. ORDER -> CAPACITY_RESERVATION & ORDER -> PAYMENT INTEGRITY
-- ------------------------------------------------------------------------------
ALTER TABLE payments ADD CONSTRAINT uq_payments_reservation_id UNIQUE (capacity_reservation_id, id);

ALTER TABLE orders ADD CONSTRAINT uq_orders_store_id_id UNIQUE (store_id, id);

ALTER TABLE orders DROP CONSTRAINT fk_orders_capacity_reservation_id;
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_capacity_reservation_store_user FOREIGN KEY (store_id, user_id, capacity_reservation_id) REFERENCES capacity_reservations(store_id, user_id, id) ON DELETE RESTRICT;

ALTER TABLE orders DROP CONSTRAINT fk_orders_payment_id;
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_payment_capacity_reservation FOREIGN KEY (capacity_reservation_id, payment_id) REFERENCES payments(capacity_reservation_id, id) ON DELETE RESTRICT;

-- ------------------------------------------------------------------------------
-- 11. MULTITENANT ORDER -> ORDER_ITEMS -> PRODUCT_VARIANTS INTEGRITY
-- ------------------------------------------------------------------------------
ALTER TABLE order_items ADD COLUMN store_id UUID;

-- Backfill store_id en order_items
UPDATE order_items oi
SET store_id = o.store_id
FROM orders o
WHERE oi.order_id = o.id;

ALTER TABLE order_items ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE order_items DROP CONSTRAINT fk_order_items_order_id;
ALTER TABLE order_items DROP CONSTRAINT fk_order_items_variant_id;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_order_store FOREIGN KEY (store_id, order_id) REFERENCES orders(store_id, id) ON DELETE CASCADE;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_variant_store FOREIGN KEY (store_id, variant_id) REFERENCES product_variants(store_id, id) ON DELETE RESTRICT;

CREATE INDEX idx_order_items_store_id ON order_items (store_id);

-- ============================================
-- JaldiShop - Initial Schema Migration
-- Version: V1__initial_schema.sql
-- Source: docs/04-diseno/modelo-er.md v1.6.1
-- Flyway: managed schema
-- ============================================

-- ============================================
-- SECTION 1: USERS
-- ============================================
CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_encoded VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_users_status ON users (status);

-- ============================================
-- SECTION 2: ROLES
-- ============================================
CREATE TABLE roles (
    id SMALLINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('CUSTOMER', 'MERCHANT', 'ADMIN'))
);

-- ============================================
-- SECTION 3: USER_ROLES
-- ============================================
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id SMALLINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- ============================================
-- SECTION 4: STORES
-- ============================================
CREATE TABLE stores (
    id UUID NOT NULL,
    merchant_user_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    contact_phone VARCHAR(30),
    address TEXT,
    address_reference TEXT,
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    pickup_enabled BOOLEAN NOT NULL,
    delivery_enabled BOOLEAN NOT NULL,
    delivery_fee_amount NUMERIC(12,2),
    delivery_fee_currency CHAR(3),
    tax_applies BOOLEAN NOT NULL,
    tax_rate NUMERIC(5,2),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT uq_stores_merchant_user_id UNIQUE (merchant_user_id),
    CONSTRAINT uq_stores_slug UNIQUE (slug),
    CONSTRAINT fk_stores_merchant_user_id FOREIGN KEY (merchant_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_stores_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_stores_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_stores_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_stores_delivery_fee CHECK (delivery_fee_amount IS NULL OR delivery_fee_amount >= 0),
    CONSTRAINT ck_stores_tax_rate CHECK (tax_rate IS NULL OR (tax_rate > 0 AND tax_rate <= 100))
);

-- ============================================
-- SECTION 5: CATEGORIES
-- ============================================
CREATE TABLE categories (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_categories_store_id ON categories (store_id);
CREATE UNIQUE INDEX uq_idx_categories_store_name ON categories (store_id, lower(trim(name))) WHERE status = 'ACTIVE';

-- ============================================
-- SECTION 6: PRODUCTS
-- ============================================
CREATE TABLE products (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    image_url TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT uq_products_store_slug UNIQUE (store_id, slug),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_store_status ON products (store_id, status);

-- ============================================
-- SECTION 7: PRODUCT_VARIANTS
-- ============================================
CREATE TABLE product_variants (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    presentation_name VARCHAR(120) NOT NULL,
    sku VARCHAR(100),
    price_amount NUMERIC(12,2) NOT NULL,
    price_currency CHAR(3) NOT NULL,
    tracks_inventory BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT fk_product_variants_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_variants_price CHECK (price_amount > 0),
    CONSTRAINT ck_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_product_variants_product_status ON product_variants (product_id, status);
CREATE UNIQUE INDEX uq_idx_product_variants_sku ON product_variants (sku) WHERE sku IS NOT NULL;

-- ============================================
-- SECTION 8: VARIANT_ATTRIBUTES
-- ============================================
CREATE TABLE variant_attributes (
    variant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    value TEXT NOT NULL,
    CONSTRAINT pk_variant_attributes PRIMARY KEY (variant_id, name),
    CONSTRAINT fk_variant_attributes_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);

-- ============================================
-- SECTION 9: INVENTORIES
-- ============================================
CREATE TABLE inventories (
    variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    low_stock_threshold INTEGER,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_inventories PRIMARY KEY (variant_id),
    CONSTRAINT fk_inventories_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    CONSTRAINT ck_inventories_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_inventories_low_stock CHECK (low_stock_threshold IS NULL OR low_stock_threshold >= 0)
);

-- ============================================
-- SECTION 10: CARTS
-- ============================================
CREATE TABLE carts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT fk_carts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_carts_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT uq_carts_user_store UNIQUE (user_id, store_id)
);

-- ============================================
-- SECTION 11: CART_ITEMS
-- ============================================
CREATE TABLE cart_items (
    cart_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    reference_price_amount NUMERIC(12,2) NOT NULL,
    reference_price_currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (cart_id, variant_id),
    CONSTRAINT fk_cart_items_cart_id FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_cart_items_reference_price CHECK (reference_price_amount >= 0)
);

-- ============================================
-- SECTION 12: DISCOUNTS
-- ============================================
CREATE TABLE discounts (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(30) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    value NUMERIC(12,2) NOT NULL,
    code VARCHAR(80),
    minimum_purchase_amount NUMERIC(12,2),
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_discounts PRIMARY KEY (id),
    CONSTRAINT fk_discounts_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,

    CONSTRAINT ck_discounts_type CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_discounts_modality CHECK (modality IN ('AUTOMATIC', 'CODE')),
    CONSTRAINT ck_discounts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_discounts_value CHECK (value > 0),
    CONSTRAINT ck_discounts_minimum_purchase CHECK (minimum_purchase_amount IS NULL OR minimum_purchase_amount >= 0),
    CONSTRAINT ck_discounts_percentage CHECK (type != 'PERCENTAGE' OR value <= 100),
    CONSTRAINT ck_discounts_code_modality CHECK ((modality = 'CODE' AND code IS NOT NULL) OR (modality = 'AUTOMATIC' AND code IS NULL)),
    CONSTRAINT ck_discounts_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX idx_discounts_store_modality_status ON discounts (store_id, modality, status);
CREATE UNIQUE INDEX uq_idx_discounts_store_code ON discounts (store_id, code) WHERE code IS NOT NULL;

-- ============================================
-- SECTION 13: CAPACITY_CONFIGURATIONS
-- ============================================
CREATE TABLE capacity_configurations (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME,
    end_time TIME,
    max_capacity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_capacity_configurations PRIMARY KEY (id),
    CONSTRAINT fk_capacity_configurations_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT ck_capacity_configurations_day CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT ck_capacity_configurations_max_capacity CHECK (max_capacity >= 0),
    CONSTRAINT ck_capacity_configurations_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_capacity_configurations_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
    CONSTRAINT ck_capacity_configurations_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time)
);

CREATE INDEX idx_capacity_configurations_store_day ON capacity_configurations (store_id, day_of_week, status, start_time, end_time);

-- ============================================
-- SECTION 14: CAPACITY_EXCEPTIONS
-- ============================================
CREATE TABLE capacity_exceptions (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    service_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    exception_capacity INTEGER NOT NULL,
    reason TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_capacity_exceptions PRIMARY KEY (id),
    CONSTRAINT fk_capacity_exceptions_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT ck_capacity_exceptions_capacity CHECK (exception_capacity >= 0),
    CONSTRAINT ck_capacity_exceptions_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_capacity_exceptions_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
    CONSTRAINT ck_capacity_exceptions_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time)
);

CREATE INDEX idx_capacity_exceptions_store_date ON capacity_exceptions (store_id, service_date, status);

-- ============================================
-- SECTION 15: CAPACITY_RESERVATIONS
-- ============================================
CREATE TABLE capacity_reservations (
    id UUID NOT NULL,
    store_id UUID NOT NULL,
    user_id UUID NOT NULL,
    service_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    payment_protection_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_capacity_reservations PRIMARY KEY (id),
    CONSTRAINT fk_capacity_reservations_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_capacity_reservations_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_capacity_reservations_status CHECK (status IN ('ACTIVE', 'PAYMENT_PROTECTED', 'COMMITTED', 'EXPIRED', 'RELEASED'))
);

CREATE INDEX idx_capacity_reservations_store_date_time ON capacity_reservations (store_id, service_date, start_time, end_time, status);
CREATE INDEX idx_capacity_reservations_expires ON capacity_reservations (expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_capacity_reservations_protection_expires ON capacity_reservations (payment_protection_expires_at) WHERE status = 'PAYMENT_PROTECTED';

-- ============================================
-- SECTION 16: PAYMENTS
-- ============================================
CREATE TABLE payments (
    id UUID NOT NULL,
    capacity_reservation_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    refund_status VARCHAR(30) NOT NULL,
    refund_amount NUMERIC(12,2),
    refund_reference VARCHAR(100),
    approved_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_capacity_reservation_id UNIQUE (capacity_reservation_id),
    CONSTRAINT fk_payments_capacity_reservation_id FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE RESTRICT,
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_currency CHECK (currency = 'PEN'),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'FAILED')),
    CONSTRAINT ck_payments_refund_status CHECK (refund_status IN ('NOT_REQUIRED', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_payments_refund_amount CHECK (refund_amount IS NULL OR refund_amount >= 0),
    CONSTRAINT ck_payments_refund_limit CHECK (refund_amount IS NULL OR refund_amount <= amount)
);

-- ============================================
-- SECTION 17: PAYMENT_ATTEMPTS
-- ============================================
CREATE TABLE payment_attempts (
    id UUID NOT NULL,
    payment_id UUID NOT NULL,
    attempt_number SMALLINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    provider_payment_id VARCHAR(100),
    provider_status VARCHAR(50),
    provider_status_detail VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payment_attempts PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempts_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT uq_payment_attempts_payment_number UNIQUE (payment_id, attempt_number),
    CONSTRAINT uq_payment_attempts_provider_idempotency UNIQUE (provider, idempotency_key),

    CONSTRAINT ck_payment_attempts_number CHECK (attempt_number > 0),
    CONSTRAINT ck_payment_attempts_provider CHECK (provider = 'MERCADO_PAGO'),
    CONSTRAINT ck_payment_attempts_payment_method CHECK (payment_method = 'CARD'),
    CONSTRAINT ck_payment_attempts_status CHECK (status IN ('CREATED', 'PROCESSING', 'APPROVED', 'REJECTED', 'ERROR'))
);

CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts (payment_id);
CREATE UNIQUE INDEX uq_idx_payment_attempts_provider_payment_id ON payment_attempts (provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL;

-- ============================================
-- SECTION 18: ORDERS
-- ============================================
CREATE TABLE orders (
    id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    capacity_reservation_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    service_date DATE NOT NULL,
    service_start_time TIME,
    service_end_time TIME,
    customer_name VARCHAR(200) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    customer_email VARCHAR(254) NOT NULL,
    delivery_address TEXT,
    delivery_reference TEXT,
    delivery_latitude NUMERIC(9,6),
    delivery_longitude NUMERIC(9,6),
    currency CHAR(3) NOT NULL,
    products_subtotal_amount NUMERIC(12,2) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL,
    discount_code VARCHAR(80),
    delivery_fee_amount NUMERIC(12,2) NOT NULL,
    included_tax_amount NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT uq_orders_payment_id UNIQUE (payment_id),
    CONSTRAINT uq_orders_capacity_reservation_id UNIQUE (capacity_reservation_id),
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_capacity_reservation_id FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE RESTRICT,
    CONSTRAINT ck_orders_status CHECK (status IN ('CONFIRMED', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_orders_delivery_mode CHECK (delivery_mode IN ('PICKUP', 'DELIVERY')),
    CONSTRAINT ck_orders_currency CHECK (currency = 'PEN'),
    CONSTRAINT ck_orders_products_subtotal CHECK (products_subtotal_amount >= 0),
    CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0),
    CONSTRAINT ck_orders_discount_limit CHECK (discount_amount <= products_subtotal_amount),
    CONSTRAINT ck_orders_delivery_fee CHECK (delivery_fee_amount >= 0),
    CONSTRAINT ck_orders_tax CHECK (included_tax_amount >= 0),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_delivery_latitude CHECK (delivery_latitude IS NULL OR delivery_latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_orders_delivery_longitude CHECK (delivery_longitude IS NULL OR delivery_longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_orders_delivery_address CHECK ((delivery_mode = 'DELIVERY' AND delivery_address IS NOT NULL) OR delivery_mode = 'PICKUP'),
    CONSTRAINT ck_orders_service_time CHECK (service_start_time IS NULL OR service_end_time IS NULL OR service_start_time < service_end_time)
);

CREATE INDEX idx_orders_store_confirmed ON orders (store_id, confirmed_at DESC);
CREATE INDEX idx_orders_user_confirmed ON orders (user_id, confirmed_at DESC);
CREATE INDEX idx_orders_store_status ON orders (store_id, status);
CREATE INDEX idx_orders_store_service_date ON orders (store_id, service_date);

-- ============================================
-- SECTION 19: ORDER_ITEMS
-- ============================================
CREATE TABLE order_items (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    variant_name VARCHAR(120) NOT NULL,
    attributes_snapshot JSONB,
    quantity INTEGER NOT NULL,
    unit_price_amount NUMERIC(12,2) NOT NULL,
    subtotal_amount NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price CHECK (unit_price_amount >= 0),
    CONSTRAINT ck_order_items_subtotal CHECK (subtotal_amount >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_variant_id ON order_items (variant_id);

-- ============================================
-- SECTION 20: ORDER_STATUS_HISTORY
-- ============================================
CREATE TABLE order_status_history (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    changed_by_user_id UUID,
    status VARCHAR(30) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_changed_by_user_id FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_order_status_history_order_changed ON order_status_history (order_id, changed_at);

-- ============================================
-- SECTION 21: FAVORITES
-- ============================================
CREATE TABLE favorites (
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_favorites_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ============================================
-- SECTION 22: REVIEWS
-- ============================================
CREATE TABLE reviews (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    rating INTEGER NOT NULL,
    comment TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_reviews_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT ck_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN'))
);

CREATE INDEX idx_reviews_product_status ON reviews (product_id, status);

-- ============================================
-- SECTION 23: NOTIFICATIONS
-- ============================================
CREATE TABLE notifications (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_type CHECK (type IN ('NEW_ORDER', 'ORDER_STATUS_CHANGED', 'LOW_STOCK', 'SYSTEM')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('UNREAD', 'READ')),
    CONSTRAINT ck_notifications_read_check CHECK ((status = 'UNREAD' AND read_at IS NULL) OR (status = 'READ' AND read_at IS NOT NULL))
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, created_at DESC) WHERE status = 'UNREAD';

-- ============================================
-- SECTION 24: SEQUENCE FOR ORDER_NUMBER
-- ============================================
CREATE SEQUENCE seq_order_number START WITH 1 INCREMENT BY 1;

-- ============================================
-- SECTION 25: TABLE DEFAULTS
-- ============================================
COMMENT ON TABLE users IS 'Usuarios de la plataforma';
COMMENT ON TABLE roles IS 'Catalogo de roles';
COMMENT ON TABLE user_roles IS 'Relacion usuario-rol';
COMMENT ON TABLE stores IS 'Tiendas de los comerciantes';
COMMENT ON TABLE categories IS 'Categorias de productos';
COMMENT ON TABLE products IS 'Productos dentro de una tienda';
COMMENT ON TABLE product_variants IS 'Variantes/presentaciones de productos';
COMMENT ON TABLE variant_attributes IS 'Atributos de cada variante';
COMMENT ON TABLE inventories IS 'Stock de variantes con control';
COMMENT ON TABLE carts IS 'Carritos de compra';
COMMENT ON TABLE cart_items IS 'Items dentro de un carrito';
COMMENT ON TABLE discounts IS 'Descuentos definidos por tienda';
COMMENT ON TABLE capacity_configurations IS 'Capacidad base recurrente';
COMMENT ON TABLE capacity_exceptions IS 'Excepciones de capacidad';
COMMENT ON TABLE capacity_reservations IS 'Reservas temporales de cupo';
COMMENT ON TABLE payments IS 'Procesos de pago';
COMMENT ON TABLE payment_attempts IS 'Intentos de pago';
COMMENT ON TABLE orders IS 'Pedidos confirmados';
COMMENT ON TABLE order_items IS 'Detalles de cada pedido';
COMMENT ON TABLE order_status_history IS 'Historial de estados';
COMMENT ON TABLE favorites IS 'Favoritos de usuarios';
COMMENT ON TABLE reviews IS 'Resenas de productos';
COMMENT ON TABLE notifications IS 'Notificaciones a usuarios';

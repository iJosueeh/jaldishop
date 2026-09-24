-- ============================================
-- JaldiShop - Multi-Tenant Store Customers
-- Version: V3__add_store_customers.sql
-- Description: Creación de la relación explícita de clientes por tienda (StoreCustomer)
-- ============================================

CREATE TABLE store_customers (
    store_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_store_customers PRIMARY KEY (store_id, user_id),
    CONSTRAINT fk_store_customers_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_store_customers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_store_customers_user_id ON store_customers(user_id);

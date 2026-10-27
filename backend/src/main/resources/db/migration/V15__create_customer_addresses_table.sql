-- ===================================================================
-- ARCHIVO: V15__create_customer_addresses_table.sql
-- DESCRIPCIÓN: Libreta de direcciones de clientes CUSTOMER (máx. 5 por usuario)
-- ===================================================================

CREATE TABLE customer_addresses (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    alias VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    ubigeo_code CHAR(6) NOT NULL,
    reference VARCHAR(300),
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_user_id FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_addresses_lat CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT ck_customer_addresses_lon CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    CONSTRAINT ck_customer_addresses_ubigeo CHECK (ubigeo_code ~ '^[0-9]{6}$')
);

CREATE INDEX idx_customer_addresses_user_created 
    ON customer_addresses (user_id, created_at DESC);

CREATE UNIQUE INDEX uq_customer_addresses_user_default 
    ON customer_addresses (user_id) 
    WHERE (is_default = TRUE);

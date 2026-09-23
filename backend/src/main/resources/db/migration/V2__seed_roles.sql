-- ============================================
-- JaldiShop - Seed Initial Roles & Demo Data
-- Version: V2__seed_roles.sql
-- Description: Inserción de roles base y dataset de demostración
-- Passwords en texto plano para demo: Password123!
-- ============================================

-- 1. ROLES DEL SISTEMA
INSERT INTO roles (id, name) VALUES
    (1, 'CUSTOMER'),
    (2, 'MERCHANT'),
    (3, 'ADMIN')
ON CONFLICT (id) DO NOTHING;

-- 2. USUARIOS DEMO DE EVALUACIÓN
-- Merchant Demo (Dueño de la tienda)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'merchant.demo@jaldishop.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- Password123!
    'Carlos',
    'García',
    '+51987654321',
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Cliente Demo
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'cliente.demo@jaldishop.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- Password123!
    'Valeria',
    'Ramos',
    '+51984552109',
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Admin Demo (Administrador de Plataforma)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3',
    'admin.demo@jaldishop.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- Password123!
    'Admin',
    'JaldiShop',
    '+51999999999',
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- 3. ASIGNACIÓN DE ROLES (user_roles)
INSERT INTO user_roles (user_id, role_id)
VALUES 
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 2), -- MERCHANT
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', 1), -- CUSTOMER
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9e3', 3)  -- ADMIN
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4. TIENDA DEMO (stores)
INSERT INTO stores (
    id, merchant_user_id, name, slug, description, contact_phone, 
    address, address_reference, latitude, longitude, pickup_enabled, 
    delivery_enabled, delivery_fee_amount, delivery_fee_currency, 
    tax_applies, tax_rate, status, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'Pastelería Dulce Deleite',
    'dulce-deleite',
    'Tortas artesanales y postres premium para celebraciones.',
    '+51987654321',
    'Av. La Marina 1234, San Miguel, Lima',
    'Frente a Plaza San Miguel',
    -12.076842,
    -77.086431,
    true,
    true,
    5.00,
    'PEN',
    true,
    18.00,
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (merchant_user_id) DO NOTHING;

-- 5. CONFIGURACIÓN DE CAPACIDAD BASE (capacity_configurations)
-- Sábados (day_of_week = 6): 14:00 - 18:00 (Máximo 8 pedidos)
INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9c1',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    6,
    '14:00:00',
    '18:00:00',
    8,
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

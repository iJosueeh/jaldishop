-- ====================================================================
-- JaldiShop — Seed Dataset Reproducible End-to-End (E2E)
-- Ubicación: db/seed/seed_e2e_demo_data.sql
-- Compatible con: Esquema migrado hasta V19
-- Idempotencia: Verificaciones condicionales y ON CONFLICT
-- Credenciales universales demo: Password123!
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. ROLES DEL SISTEMA
-- --------------------------------------------------------------------
INSERT INTO roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT (id) DO NOTHING;
INSERT INTO roles (id, name) VALUES (2, 'MERCHANT') ON CONFLICT (id) DO NOTHING;
INSERT INTO roles (id, name) VALUES (3, 'ADMIN') ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 2. USUARIOS DEMO (Hash BCrypt para 'Password123!')
-- --------------------------------------------------------------------
-- Super Admin
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3',
    'admin.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Admin', 'JaldiShop', '+51999999999', 'ACTIVE', NOW() - INTERVAL '60 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Admin Seguridad
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e4',
    'admin.seguridad@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Rodrigo', 'Paz', '+51988888888', 'ACTIVE', NOW() - INTERVAL '50 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Merchant 1: Carlos García (Pastelería Dulce Deleite)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'merchant.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Carlos', 'García', '+51987654321', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Merchant 2: Lucía Fernández (Cafetería Aroma & Grano)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1',
    'merchant.lucia@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Lucía', 'Fernández', '+51976543210', 'ACTIVE', NOW() - INTERVAL '40 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Customer 1: Valeria (cliente.demo)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'cliente.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Valeria', 'Ríos', '+51965432109', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Customer 2: Juan Morales
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    'cliente.juan@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Juan', 'Morales', '+51954321098', 'ACTIVE', NOW() - INTERVAL '20 days', NOW()
) ON CONFLICT (id) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- --------------------------------------------------------------------
-- 3. TIENDAS DEMO (stores)
-- --------------------------------------------------------------------
INSERT INTO stores (
    id, owner_user_id, name, slug, description, contact_phone,
    address, address_reference, latitude, longitude,
    pickup_enabled, delivery_enabled, delivery_fee_amount, delivery_fee_currency,
    tax_rate, status, created_at, updated_at, logo_url, banner_url,
    instagram_url, facebook_url, whatsapp_number
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'Pastelería Dulce Deleite',
    'dulce-deleite',
    'Pastelería fina y repostería artesanal con ingredientes de alta calidad.',
    '+51987654321',
    'Av. Larco 1234, Miraflores, Lima',
    'A media cuadra del Parque Salazar',
    -12.128765, -77.029876,
    TRUE, TRUE, 7.50, 'PEN',
    18.00, 'ACTIVE', NOW() - INTERVAL '45 days', NOW(),
    'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=300',
    'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=1200',
    'https://instagram.com/dulcedeleite_pe',
    'https://facebook.com/dulcedeleitepe',
    '+51987654321'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    pickup_enabled = EXCLUDED.pickup_enabled,
    delivery_enabled = EXCLUDED.delivery_enabled;

INSERT INTO stores (
    id, owner_user_id, name, slug, description, contact_phone,
    address, address_reference, latitude, longitude,
    pickup_enabled, delivery_enabled, delivery_fee_amount, delivery_fee_currency,
    tax_rate, status, created_at, updated_at, logo_url, banner_url,
    instagram_url, facebook_url, whatsapp_number
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1',
    'Cafetería Aroma & Grano',
    'aroma-grano',
    'Café de especialidad peruano y panes de masa madre horneados diariamente.',
    '+51976543210',
    'Calle Los Libertadores 567, San Isidro, Lima',
    'Frente al Bosque El Olivar',
    -12.098765, -77.034567,
    TRUE, TRUE, 6.00, 'PEN',
    18.00, 'ACTIVE', NOW() - INTERVAL '40 days', NOW(),
    'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=300',
    'https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=1200',
    'https://instagram.com/aromaygrano_cafe',
    'https://facebook.com/aromaygranocafe',
    '+51976543210'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

-- --------------------------------------------------------------------
-- 4. ASIGNACIÓN DE ROLES (user_roles conforme a V12)
-- --------------------------------------------------------------------
INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3', 3, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3' AND role_id = 3 AND store_id IS NULL
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 2, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1' AND role_id = 2 AND store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1', 2, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1' AND role_id = 2 AND store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6'
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2' AND role_id = 1 AND store_id IS NULL
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', 1, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2' AND role_id = 1 AND store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2', 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2' AND role_id = 1 AND store_id IS NULL
);

INSERT INTO user_roles (id, user_id, role_id, store_id)
SELECT gen_random_uuid(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2', 1, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2' AND role_id = 1 AND store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6'
);

-- --------------------------------------------------------------------
-- 5. LIBRETA DE DIRECCIONES (customer_addresses conforme a V15)
-- --------------------------------------------------------------------
INSERT INTO customer_addresses (
    id, user_id, alias, recipient_name, recipient_phone,
    street_address, department, province, district, ubigeo_code, reference,
    latitude, longitude, is_default, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfb01',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'Casa Miraflores', 'Valeria Ríos', '+51965432109',
    'Av. José Larco 345, Dpto 402', 'Lima', 'Lima', 'Miraflores', '150122', 'Cerca al Parque Kennedy',
    -12.121900, -77.029800, TRUE, NOW() - INTERVAL '25 days', NOW()
) ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    recipient_name = EXCLUDED.recipient_name,
    is_default = EXCLUDED.is_default;

INSERT INTO customer_addresses (
    id, user_id, alias, recipient_name, recipient_phone,
    street_address, department, province, district, ubigeo_code, reference,
    latitude, longitude, is_default, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfb02',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'Oficina San Isidro', 'Valeria Ríos', '+51965432109',
    'Av. Javier Prado Este 450, Piso 12', 'Lima', 'Lima', 'San Isidro', '150131', 'Torre Empresarial',
    -12.093100, -77.028900, FALSE, NOW() - INTERVAL '15 days', NOW()
) ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    recipient_name = EXCLUDED.recipient_name,
    is_default = EXCLUDED.is_default;

INSERT INTO customer_addresses (
    id, user_id, alias, recipient_name, recipient_phone,
    street_address, department, province, district, ubigeo_code, reference,
    latitude, longitude, is_default, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfb03',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    'Departamento Barranco', 'Juan Morales', '+51954321098',
    'Jr. Centenario 120', 'Lima', 'Lima', 'Barranco', '150104', 'A dos cuadras del Puente de los Suspiros',
    -12.148900, -77.021500, TRUE, NOW() - INTERVAL '18 days', NOW()
) ON CONFLICT (id) DO UPDATE SET is_default = TRUE;

-- --------------------------------------------------------------------
-- 6. CATEGORÍAS (categories)
-- --------------------------------------------------------------------
INSERT INTO categories (id, store_id, name, description, status, created_at, updated_at)
VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf101', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Tortas Artesanales', 'Tortas para toda ocasión', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf102', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Postres Individuales', 'Cheesecakes, mousses y cupcakes', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf103', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Packs Especiales', 'Cajas de regalo y degustación', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf104', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Café en Grano', 'Cafés de especialidad de origen peruano', 'ACTIVE', NOW() - INTERVAL '40 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf105', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Bebidas Calientes y Frías', 'Espressos, lattes y cold brew', 'ACTIVE', NOW() - INTERVAL '40 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf106', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Panadería Artesanal', 'Panes de masa madre recién horneados', 'ACTIVE', NOW() - INTERVAL '40 days', NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, status = EXCLUDED.status;

-- --------------------------------------------------------------------
-- 7. PRODUCTOS (products)
-- --------------------------------------------------------------------
INSERT INTO products (id, store_id, category_id, name, slug, description, image_url, status, created_at, updated_at)
VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf201', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf101',
     'Torta de Chocolate Artesanal', 'torta-chocolate-artesanal', 'Bizcocho húmedo de cacao cusqueño al 70% relleno de fudge casero.',
     'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600', 'ACTIVE', NOW() - INTERVAL '44 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf202', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf102',
     'Cheesecake de Frutos Rojos', 'cheesecake-frutos-rojos', 'Cremoso cheesecake estilo Nueva York horneado con coulis de frutos del bosque.',
     'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=600', 'ACTIVE', NOW() - INTERVAL '44 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf203', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf102',
     'Cupcake Red Velvet', 'cupcake-red-velvet', 'Bizcocho terciopelo rojo coronado con suave frosting de queso crema.',
     'https://images.unsplash.com/photo-1614707267537-b85aaf00c4b7?w=600', 'ACTIVE', NOW() - INTERVAL '43 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf204', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', '80acf0ba-5a79-4888-b00d-b5aef8fcf104',
     'Café Geisha Cuzco 250g', 'cafe-geisha-cuzco-250g', 'Café de especialidad con notas florales a jazmín y durazno.',
     'https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=600', 'ACTIVE', NOW() - INTERVAL '39 days', NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, status = EXCLUDED.status;

-- --------------------------------------------------------------------
-- 8. VARIANTES DE PRODUCTOS (product_variants)
-- --------------------------------------------------------------------
INSERT INTO product_variants (id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at, store_id)
VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf301', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', 'Mediana (8-10 porciones)', 'TORT-CHOC-MED', 45.00, 'PEN', TRUE, 'ACTIVE', NOW() - INTERVAL '44 days', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', 'Grande (16 porciones)', 'TORT-CHOC-GRA', 75.00, 'PEN', TRUE, 'ACTIVE', NOW() - INTERVAL '44 days', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', '80acf0ba-5a79-4888-b00d-b5aef8fcf202', 'Entero (8 porciones)', 'CHEESE-ENTERO', 60.00, 'PEN', TRUE, 'ACTIVE', NOW() - INTERVAL '44 days', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf304', '80acf0ba-5a79-4888-b00d-b5aef8fcf203', 'Caja x 6 unidades', 'CUPC-REDV-6PK', 30.00, 'PEN', TRUE, 'ACTIVE', NOW() - INTERVAL '43 days', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', '80acf0ba-5a79-4888-b00d-b5aef8fcf204', 'Grano Entero 250g', 'CAFE-GEISHA-ENT', 48.00, 'PEN', TRUE, 'ACTIVE', NOW() - INTERVAL '39 days', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6')
ON CONFLICT (id) DO UPDATE SET
    price_amount = EXCLUDED.price_amount,
    status = EXCLUDED.status;

-- --------------------------------------------------------------------
-- 9. INVENTARIOS (inventories)
-- --------------------------------------------------------------------
INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at)
VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf301', 25, 3, NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', 15, 2, NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', 12, 2, NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf304', 30, 5, NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', 40, 5, NOW())
ON CONFLICT (variant_id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    low_stock_threshold = EXCLUDED.low_stock_threshold;

-- --------------------------------------------------------------------
-- 10. CONFIGURACIÓN DE CAPACIDAD SEMANAL (capacity_configurations)
-- --------------------------------------------------------------------
INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9c3', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 2, '09:00:00', '13:00:00', 10, 'ACTIVE', NOW() - INTERVAL '45 days', NOW()
WHERE NOT EXISTS (SELECT 1 FROM capacity_configurations WHERE store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5' AND day_of_week = 2);

INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9c5', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 3, '09:00:00', '13:00:00', 10, 'ACTIVE', NOW() - INTERVAL '45 days', NOW()
WHERE NOT EXISTS (SELECT 1 FROM capacity_configurations WHERE store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5' AND day_of_week = 3);

INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9c7', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 4, '09:00:00', '13:00:00', 10, 'ACTIVE', NOW() - INTERVAL '45 days', NOW()
WHERE NOT EXISTS (SELECT 1 FROM capacity_configurations WHERE store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5' AND day_of_week = 4);

-- --------------------------------------------------------------------
-- 11. RESERVAS DE CAPACIDAD (capacity_reservations)
-- --------------------------------------------------------------------
-- Reserva 1: Pedido Histórico 1 (Valeria)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time,
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    CURRENT_DATE - INTERVAL '2 days', '09:00:00', '13:00:00',
    'COMMITTED', NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
) ON CONFLICT (id) DO UPDATE SET
    status = 'COMMITTED';

-- Reserva 2: Pedido 2 (Juan)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time,
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    CURRENT_DATE, '14:00:00', '19:00:00',
    'COMMITTED', NOW() + INTERVAL '2 hours', NULL, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'
) ON CONFLICT (id) DO UPDATE SET
    status = 'COMMITTED';

-- Reserva 3: Activa para Demo de Checkout (Mañana)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time,
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf604',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    CURRENT_DATE + INTERVAL '1 day', '09:00:00', '13:00:00',
    'ACTIVE', NOW() + INTERVAL '15 minutes', NULL, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET
    service_date = CURRENT_DATE + INTERVAL '1 day',
    expires_at = NOW() + INTERVAL '15 minutes';

-- --------------------------------------------------------------------
-- 12. PAGOS & INTENTOS DE PAGO (payments, payment_attempts conforme a V17)
-- --------------------------------------------------------------------
-- Pago 1: Entregado
INSERT INTO payments (
    id, capacity_reservation_id, amount, currency, status,
    refund_status, refund_amount, refund_reference, approved_at, refunded_at, created_at, updated_at,
    fulfillment_type, customer_name, customer_phone, customer_email, delivery_address, delivery_reference,
    products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, items_snapshot
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    52.50, 'PEN', 'APPROVED',
    'NOT_REQUIRED', NULL, NULL, NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days',
    'DELIVERY', 'Valeria Ríos', '+51965432109', 'cliente.demo@jaldishop.com', 'Av. José Larco 345, Dpto 402, Miraflores', 'Cerca al Parque Kennedy',
    45.00, 0.00, 7.50, 6.86,
    '[{"variantId":"80acf0ba-5a79-4888-b00d-b5aef8fcf301","productName":"Torta de Chocolate Artesanal","variantName":"Mediana","quantity":1,"unitPrice":45.00,"subtotal":45.00}]'::jsonb
) ON CONFLICT (id) DO UPDATE SET
    capacity_reservation_id = EXCLUDED.capacity_reservation_id,
    status = 'APPROVED';

INSERT INTO payment_attempts (
    id, payment_id, attempt_number, provider, payment_method,
    idempotency_key, provider_payment_id, provider_status, provider_status_detail,
    status, error_code, error_message, created_at, completed_at, updated_at,
    preference_id, init_point
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf711',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    1, 'MERCADO_PAGO', 'CARD',
    'idemp-pay-001', 'MP-PAY-100001', 'approved', 'accredited',
    'APPROVED', NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days',
    'PREF-E2E-001', 'https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=PREF-E2E-001'
) ON CONFLICT (id) DO NOTHING;

-- Pago 2: En preparación
INSERT INTO payments (
    id, capacity_reservation_id, amount, currency, status,
    refund_status, refund_amount, refund_reference, approved_at, refunded_at, created_at, updated_at,
    fulfillment_type, customer_name, customer_phone, customer_email, delivery_address, delivery_reference,
    products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, items_snapshot
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    60.00, 'PEN', 'APPROVED',
    'NOT_REQUIRED', NULL, NULL, NOW() - INTERVAL '1 hour', NULL, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour',
    'PICKUP', 'Juan Pérez', '+51912345678', 'cliente.juan@jaldishop.com', NULL, NULL,
    60.00, 0.00, 0.00, 9.15,
    '[{"variantId":"80acf0ba-5a79-4888-b00d-b5aef8fcf303","productName":"Cheesecake de Frutos Rojos","variantName":"Entero","quantity":1,"unitPrice":60.00,"subtotal":60.00}]'::jsonb
) ON CONFLICT (id) DO UPDATE SET
    capacity_reservation_id = EXCLUDED.capacity_reservation_id,
    status = 'APPROVED';

INSERT INTO payment_attempts (
    id, payment_id, attempt_number, provider, payment_method,
    idempotency_key, provider_payment_id, provider_status, provider_status_detail,
    status, error_code, error_message, created_at, completed_at, updated_at,
    preference_id, init_point
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf712',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    1, 'MERCADO_PAGO', 'WALLET_PURCHASE',
    'idemp-pay-002', 'MP-PAY-100002', 'approved', 'accredited',
    'APPROVED', NULL, NULL, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour',
    'PREF-E2E-002', 'https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=PREF-E2E-002'
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 13. PEDIDOS (orders, order_items, order_status_history)
-- --------------------------------------------------------------------
-- Pedido 1: Completado / Entregado
INSERT INTO orders (
    id, order_number, user_id, store_id, payment_id, capacity_reservation_id,
    status, delivery_mode, service_date, service_start_time, service_end_time,
    customer_name, customer_phone, customer_email, delivery_address, delivery_reference,
    delivery_latitude, delivery_longitude, products_subtotal_amount, discount_amount,
    delivery_fee_amount, included_tax_amount, total_amount, currency,
    confirmed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf801',
    'JAL-2026-000001',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    'COMPLETED', 'DELIVERY', CURRENT_DATE - INTERVAL '2 days', '09:00:00', '13:00:00',
    'Valeria Ríos', '+51965432109', 'cliente.demo@jaldishop.com', 'Av. José Larco 345, Dpto 402, Miraflores', 'Cerca al Parque Kennedy',
    -12.121900, -77.029800, 45.00, 0.00,
    7.50, 6.86, 52.50, 'PEN',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 hours'
) ON CONFLICT (id) DO UPDATE SET status = 'COMPLETED';

INSERT INTO order_items (
    id, order_id, variant_id, product_name, variant_name,
    attributes_snapshot, quantity, unit_price_amount, subtotal_amount, store_id
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf811',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf801',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf301',
    'Torta de Chocolate Artesanal', 'Mediana (8-10 porciones)',
    NULL, 1, 45.00, 45.00, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO order_status_history (
    id, order_id, changed_by_user_id, status, reason, changed_at
) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf821', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'CONFIRMED', 'Pago aprobado por Mercado Pago', NOW() - INTERVAL '2 days'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf822', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'IN_PREPARATION', 'Iniciando horneado', NOW() - INTERVAL '2 days' + INTERVAL '30 minutes'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf823', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'READY', 'Pedido listo para repartidor', NOW() - INTERVAL '2 days' + INTERVAL '2 hours'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf824', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'COMPLETED', 'Entregado a conformidad del cliente', NOW() - INTERVAL '2 days' + INTERVAL '3 hours')
ON CONFLICT (id) DO NOTHING;

-- Pedido 2: En preparación
INSERT INTO orders (
    id, order_number, user_id, store_id, payment_id, capacity_reservation_id,
    status, delivery_mode, service_date, service_start_time, service_end_time,
    customer_name, customer_phone, customer_email, delivery_address, delivery_reference,
    delivery_latitude, delivery_longitude, products_subtotal_amount, discount_amount,
    delivery_fee_amount, included_tax_amount, total_amount, currency,
    confirmed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf802',
    'JAL-2026-000002',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    'IN_PREPARATION', 'PICKUP', CURRENT_DATE, '14:00:00', '19:00:00',
    'Juan Pérez', '+51912345678', 'cliente.juan@jaldishop.com', NULL, NULL,
    NULL, NULL, 60.00, 0.00,
    0.00, 9.15, 60.00, 'PEN',
    NOW() - INTERVAL '1 hour', NOW() - INTERVAL '30 minutes'
) ON CONFLICT (id) DO UPDATE SET status = 'IN_PREPARATION';

INSERT INTO order_items (
    id, order_id, variant_id, product_name, variant_name,
    attributes_snapshot, quantity, unit_price_amount, subtotal_amount, store_id
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf812',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf802',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf303',
    'Cheesecake de Frutos Rojos', 'Entero (8 porciones)',
    NULL, 1, 60.00, 60.00, '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO order_status_history (
    id, order_id, changed_by_user_id, status, reason, changed_at
) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf825', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'CONFIRMED', 'Pago confirmado vía Mercado Pago Billetera', NOW() - INTERVAL '1 hour'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf826', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'IN_PREPARATION', 'Preparando pedido en cocina', NOW() - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 14. CARRITO ACTIVO DEMO (carts & cart_items)
-- --------------------------------------------------------------------
INSERT INTO carts (id, user_id, store_id, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf901',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    NOW() - INTERVAL '2 hours', NOW()
) ON CONFLICT (user_id, store_id) DO UPDATE SET updated_at = NOW();

INSERT INTO cart_items (cart_id, variant_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at, store_id)
SELECT c.id, '80acf0ba-5a79-4888-b00d-b5aef8fcf304', 2, 30.00, 'PEN', NOW() - INTERVAL '2 hours', NOW(), '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
FROM carts c
WHERE c.user_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2' AND c.store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
ON CONFLICT (cart_id, variant_id) DO UPDATE SET quantity = 2;

-- --------------------------------------------------------------------
-- 15. NOTIFICACIONES DEL SISTEMA (notifications conforme a V16)
-- --------------------------------------------------------------------
-- Notificación a Comerciante por Pedido 2 (UNREAD)
INSERT INTO notifications (
    id, user_id, type, title, message, status, created_at, read_at, deduplication_key
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfd01',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'NEW_ORDER', '¡Nuevo pedido recibido!',
    'Has recibido el pedido JAL-2026-000002 por S/ 60.00.',
    'UNREAD', NOW() - INTERVAL '1 hour', NULL, 'NEW_ORDER:80acf0ba-5a79-4888-b00d-b5aef8fcf802'
) ON CONFLICT (id) DO NOTHING;

-- Notificación a Cliente por Entrega de Pedido 1 (READ)
INSERT INTO notifications (
    id, user_id, type, title, message, status, created_at, read_at, deduplication_key
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfd02',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'ORDER_STATUS_CHANGED', 'Tu pedido ha sido entregado',
    'El pedido JAL-2026-000001 fue completado con éxito. ¡Gracias por tu compra!',
    'READ', NOW() - INTERVAL '2 days' + INTERVAL '3 hours', NOW() - INTERVAL '2 days' + INTERVAL '3 hours' + INTERVAL '10 minutes',
    'ORDER_STATUS_CHANGED:80acf0ba-5a79-4888-b00d-b5aef8fcf801:COMPLETED'
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 16. INCIDENCIAS DE PAGO Y AUDITORÍA (payment_incidences conforme a V18)
-- --------------------------------------------------------------------
INSERT INTO payment_incidences (
    id, payment_id, process_type, category, code, message,
    attempts_count, max_attempts, next_retry_at, status, created_at, updated_at, resolved_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcfe01',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    'ORDER_CONFIRMATION', 'TECHNICAL', 'TRANSIENT_NETWORK_RETRY',
    'Timeout de conexión transitorio en webhook inicial; resuelto por conciliación automática.',
    2, 5, NULL, 'RESOLVED',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '1 minute', NOW() - INTERVAL '2 days' + INTERVAL '1 minute'
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 17. FAVORITOS Y RESEÑAS
-- --------------------------------------------------------------------
INSERT INTO favorites (user_id, product_id, created_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf201',
    NOW() - INTERVAL '20 days'
) ON CONFLICT (user_id, product_id) DO NOTHING;

INSERT INTO reviews (id, user_id, product_id, rating, comment, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf951',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf201',
    5, 'Excelente torta de chocolate, súper húmeda y el fudge delicioso.',
    'PUBLISHED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
) ON CONFLICT (user_id, product_id) DO UPDATE SET
    rating = EXCLUDED.rating,
    comment = EXCLUDED.comment,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

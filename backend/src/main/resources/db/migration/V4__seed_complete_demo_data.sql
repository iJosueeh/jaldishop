-- ====================================================================
-- JaldiShop - Seed Dataset Completo del Ecosistema
-- Version: V4__seed_complete_demo_data.sql
-- Description: Inserción de datos demo exhaustivos para todas las entidades
--              (Usuarios, Tiendas, Catálogo, Capacidad, Pedidos, Pagos,
--               Carrito, Favoritos, Reseñas y Notificaciones).
-- Contraseña universal demo: Password123!
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. ROLES DEL SISTEMA (Idempotente)
-- --------------------------------------------------------------------
INSERT INTO roles (id, name) VALUES
    (1, 'CUSTOMER'),
    (2, 'MERCHANT'),
    (3, 'ADMIN')
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 2. USUARIOS DEMO (Admins, Merchants, Customers)
-- Hash BCrypt para 'Password123!': $2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS
-- --------------------------------------------------------------------
-- Super Admin 1 (Principal)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3',
    'admin.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Admin', 'JaldiShop', '+51999999999', 'ACTIVE', NOW() - INTERVAL '60 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Admin 2 (Operaciones y Seguridad)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e4',
    'admin.seguridad@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Rodrigo', 'Paz', '+51988888888', 'ACTIVE', NOW() - INTERVAL '50 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Merchant 1: Carlos García (Pastelería Dulce Deleite)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
    'merchant.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Carlos', 'García', '+51987654321', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Merchant 2: Lucía Fernández (Cafetería Aroma & Grano)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1',
    'merchant.lucia@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Lucía', 'Fernández', '+51977777777', 'ACTIVE', NOW() - INTERVAL '40 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Cliente 1: Valeria Ramos (VIP / Frecuente)
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    'cliente.demo@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Valeria', 'Ramos', '+51984552109', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Cliente 2: Juan Pérez
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    'cliente.juan@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Juan', 'Pérez', '+51912345678', 'ACTIVE', NOW() - INTERVAL '20 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Cliente 3: Sofía Mendoza
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f3',
    'cliente.sofia@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Sofía', 'Mendoza', '+51923456789', 'ACTIVE', NOW() - INTERVAL '10 days', NOW()
) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';

-- Usuario de Prueba Suspendido
INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f4',
    'usuario.suspendido@jaldishop.com',
    '$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS',
    'Diego', 'Salazar', '+51934567890', 'SUSPENDED', NOW() - INTERVAL '15 days', NOW()
) ON CONFLICT (email) DO UPDATE SET status = 'SUSPENDED';

-- --------------------------------------------------------------------
-- 3. ASIGNACIÓN DE ROLES (user_roles)
-- --------------------------------------------------------------------
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 3 FROM users u WHERE u.email IN ('admin.demo@jaldishop.com', 'admin.seguridad@jaldishop.com')
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 2 FROM users u WHERE u.email IN ('merchant.demo@jaldishop.com', 'merchant.lucia@jaldishop.com')
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 1 FROM users u WHERE u.email IN ('cliente.demo@jaldishop.com', 'cliente.juan@jaldishop.com', 'cliente.sofia@jaldishop.com', 'usuario.suspendido@jaldishop.com')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- --------------------------------------------------------------------
-- 4. TIENDAS DEMO (stores)
-- --------------------------------------------------------------------
-- Tienda 1: Pastelería Dulce Deleite (Carlos García)
INSERT INTO stores (
    id, merchant_user_id, name, slug, description, contact_phone, 
    address, address_reference, latitude, longitude, pickup_enabled, 
    delivery_enabled, delivery_fee_amount, delivery_fee_currency, 
    tax_applies, tax_rate, status, created_at, updated_at
) 
SELECT 
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    u.id,
    'Pastelería Dulce Deleite',
    'dulce-deleite',
    'Tortas artesanales, pasteles personalizados y bocaditos finos para todo tipo de celebración.',
    '+51987654321',
    'Av. La Marina 1234, San Miguel, Lima',
    'Frente a Plaza San Miguel',
    -12.076842, -77.086431,
    true, true, 5.00, 'PEN',
    true, 18.00, 'ACTIVE',
    NOW() - INTERVAL '40 days', NOW()
FROM users u WHERE u.email = 'merchant.demo@jaldishop.com'
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';

-- Tienda 2: Café & Grano Especialidad (Lucía Fernández)
INSERT INTO stores (
    id, merchant_user_id, name, slug, description, contact_phone, 
    address, address_reference, latitude, longitude, pickup_enabled, 
    delivery_enabled, delivery_fee_amount, delivery_fee_currency, 
    tax_applies, tax_rate, status, created_at, updated_at
) 
SELECT 
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6',
    u.id,
    'Café & Grano Especialidad',
    'cafe-grano',
    'Cafés de origen peruano (Cusco, Cajamarca, Villa Rica), bebidas de autor y panadería gourmet.',
    '+51977777777',
    'Calle Alcanfores 456, Miraflores, Lima',
    'A media cuadra de Av. Benavides',
    -12.122841, -77.028912,
    true, true, 4.00, 'PEN',
    true, 18.00, 'ACTIVE',
    NOW() - INTERVAL '35 days', NOW()
FROM users u WHERE u.email = 'merchant.lucia@jaldishop.com'
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';

-- --------------------------------------------------------------------
-- 5. CARTERA DE CLIENTES POR TIENDA (store_customers)
-- --------------------------------------------------------------------
INSERT INTO store_customers (store_id, user_id, created_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', u.id, NOW() - INTERVAL '30 days'
FROM users u WHERE u.email = 'cliente.demo@jaldishop.com'
ON CONFLICT (store_id, user_id) DO NOTHING;

INSERT INTO store_customers (store_id, user_id, created_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', u.id, NOW() - INTERVAL '20 days'
FROM users u WHERE u.email = 'cliente.juan@jaldishop.com'
ON CONFLICT (store_id, user_id) DO NOTHING;

INSERT INTO store_customers (store_id, user_id, created_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', u.id, NOW() - INTERVAL '10 days'
FROM users u WHERE u.email = 'cliente.sofia@jaldishop.com'
ON CONFLICT (store_id, user_id) DO NOTHING;

INSERT INTO store_customers (store_id, user_id, created_at)
SELECT '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', u.id, NOW() - INTERVAL '5 days'
FROM users u WHERE u.email = 'cliente.demo@jaldishop.com'
ON CONFLICT (store_id, user_id) DO NOTHING;

-- --------------------------------------------------------------------
-- 6. CATEGORÍAS (categories)
-- --------------------------------------------------------------------
-- Categorías Pastelería Dulce Deleite
INSERT INTO categories (id, store_id, name, description, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf101', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Tortas & Pasteles', 'Tortas artesanales enteras y porciones individuales frescas.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf102', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Postres Individuales', 'Cheesecakes, mousses y tartaletas individuales.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf103', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Bocaditos & Salados', 'Alfajores, empanadas y bocaditos para compartir.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- Categorías Café & Grano
INSERT INTO categories (id, store_id, name, description, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf104', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Cafés Calientes', 'Espressos, cappuccinos y lattes preparados al momento.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf105', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Bebidas Frías & Frappés', 'Cold brew, frappuccinos y tés helados refrescantes.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf106', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Panadería & Sandwiches', 'Croissants recién horneados, tostadas y sandwiches gourmet.', 'ACTIVE', NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 7. PRODUCTOS (products)
-- --------------------------------------------------------------------
-- Productos Pastelería Dulce Deleite
INSERT INTO products (id, store_id, category_id, name, slug, description, image_url, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf201', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf101', 'Torta de Chocolate Húmeda', 'torta-de-chocolate-humeda', 'Bizcocho húmedo de puro cacao con doble relleno de fudge casero y cobertura de chocolate bitter.', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf202', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf102', 'Cheesecake de Fresa', 'cheesecake-de-fresa', 'Cremoso cheesecake estilo Nueva York horneado con base de galleta crocante y mermelada artesanal de fresas.', 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf203', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf102', 'Pie de Limón Clásico', 'pie-de-limon-clasico', 'Masa sablée crujiente rellena con crema de limón peruano y coronada con merengue italiano flameado.', 'https://images.unsplash.com/photo-1519915028121-7d3463d20b13?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf204', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', '80acf0ba-5a79-4888-b00d-b5aef8fcf103', 'Alfajores Artesanales de Maicena', 'alfajores-artesanales-de-maicena', 'Suaves alfajores de maicena rellenos con abundante manjar blanco de olla y espolvoreados con azúcar impalpable.', 'https://images.unsplash.com/photo-1587314168485-3236d6710814?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '25 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- Productos Café & Grano
INSERT INTO products (id, store_id, category_id, name, slug, description, image_url, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf205', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', '80acf0ba-5a79-4888-b00d-b5aef8fcf104', 'Cappuccino Italiano Clásico', 'cappuccino-italiano-clasico', 'Doble shot de espresso de especialidad con leche vaporizada y sedosa espuma de leche.', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf206', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', '80acf0ba-5a79-4888-b00d-b5aef8fcf105', 'Frappuccino Moka Crunch', 'frappuccino-moka-crunch', 'Café espresso frappé con chocolate premium, crema batida y virutas de cacao.', 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf207', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', '80acf0ba-5a79-4888-b00d-b5aef8fcf106', 'Croissant Artesanal de Jamón y Queso', 'croissant-artesanal-de-jamon-y-queso', 'Croissant de mantequilla hojaldrado artesanalmente con queso gouda fundido y jamón inglés premium.', 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&auto=format&fit=crop', 'ACTIVE', NOW() - INTERVAL '25 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 8. VARIANTES DE PRODUCTOS (product_variants)
-- --------------------------------------------------------------------
INSERT INTO product_variants (id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at) VALUES
    -- Torta de Chocolate: Entera vs Porción
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf301', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', 'Torta Entera (8-10 porciones)', 'TORTA-CHOC-ENT', 55.00, 'PEN', false, 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', 'Porción Individual', 'TORTA-CHOC-IND', 12.50, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    -- Cheesecake de Fresa: Individual
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', '80acf0ba-5a79-4888-b00d-b5aef8fcf202', 'Porción Individual', 'CHEESE-FRESA-01', 14.00, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    -- Pie de Limón: Individual
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf304', '80acf0ba-5a79-4888-b00d-b5aef8fcf203', 'Porción Individual', 'PIE-LIMON-01', 11.00, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '28 days', NOW()),
    -- Alfajores: Caja x6 vs Caja x12
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', '80acf0ba-5a79-4888-b00d-b5aef8fcf204', 'Caja x6 Unidades', 'ALFAJ-BOX-06', 20.00, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf306', '80acf0ba-5a79-4888-b00d-b5aef8fcf204', 'Caja x12 Unidades', 'ALFAJ-BOX-12', 36.00, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    -- Cappuccino: 12oz vs 16oz
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf307', '80acf0ba-5a79-4888-b00d-b5aef8fcf205', 'Mediano 12 oz', 'CAPPU-12OZ', 9.50, 'PEN', false, 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf308', '80acf0ba-5a79-4888-b00d-b5aef8fcf205', 'Grande 16 oz', 'CAPPU-16OZ', 12.00, 'PEN', false, 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    -- Frappuccino Moka
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf309', '80acf0ba-5a79-4888-b00d-b5aef8fcf206', 'Grande 16 oz', 'FRAPP-MOKA-16', 15.00, 'PEN', false, 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    -- Croissant Mixto
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf310', '80acf0ba-5a79-4888-b00d-b5aef8fcf207', 'Unidad Estándar', 'CROISS-MIXTO', 10.00, 'PEN', true, 'ACTIVE', NOW() - INTERVAL '25 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 9. ATRIBUTOS DE VARIANTES (variant_attributes)
-- --------------------------------------------------------------------
INSERT INTO variant_attributes (variant_id, name, value) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf301', 'Tamaño', 'Entera (8-10 porciones)'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', 'Tamaño', 'Porción Individual'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', 'Porción', 'Individual'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', 'Presentación', 'Caja x6 Unidades'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf306', 'Presentación', 'Caja x12 Unidades'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf307', 'Tamaño', '12 oz'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf308', 'Tamaño', '16 oz'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf309', 'Tamaño', '16 oz'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf310', 'Relleno', 'Jamón Inglés & Queso Gouda')
ON CONFLICT (variant_id, name) DO NOTHING;

-- --------------------------------------------------------------------
-- 10. INVENTARIOS (inventories)
-- --------------------------------------------------------------------
INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', 15, 5, NOW()), -- Porción Torta Choc (Stock normal)
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', 2, 5, NOW()),  -- Cheesecake (Stock bajo -> Alerta)
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf304', 8, 4, NOW()),  -- Pie de Limón
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', 25, 6, NOW()), -- Alfajores x6
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf306', 10, 4, NOW()), -- Alfajores x12
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf310', 18, 5, NOW())  -- Croissants
ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------
-- 11. DESCUENTOS & CUPONES (discounts)
-- --------------------------------------------------------------------
INSERT INTO discounts (
    id, store_id, name, type, modality, value, code, 
    minimum_purchase_amount, starts_at, ends_at, status, created_at, updated_at
) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf401', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Cupón de Bienvenida 10%', 'PERCENTAGE', 'CODE', 10.00, 'BIENVENIDA10', 30.00, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf402', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 'Descuento Dulce Festivo', 'FIXED_AMOUNT', 'AUTOMATIC', 5.00, NULL, 40.00, NOW() - INTERVAL '15 days', NOW() + INTERVAL '60 days', 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf403', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 'Amantes del Café 15%', 'PERCENTAGE', 'CODE', 15.00, 'CAFE15', 25.00, NOW() - INTERVAL '20 days', NOW() + INTERVAL '90 days', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 12. CONFIGURACIONES DE CAPACIDAD SEMANAL (capacity_configurations)
-- --------------------------------------------------------------------
INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) VALUES
    -- Pastelería Dulce Deleite (0=Domingo..6=Sábado)
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9c1', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 6, '14:00:00', '18:00:00', 8, 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf501', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 1, '09:00:00', '13:00:00', 12, 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf502', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 1, '14:00:00', '19:00:00', 15, 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf503', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', 5, '10:00:00', '20:00:00', 20, 'ACTIVE', NOW(), NOW()),
    -- Café & Grano
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf504', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 1, '08:00:00', '20:00:00', 25, 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf505', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6', 6, '08:00:00', '22:00:00', 30, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 13. EXCEPCIONES DE CAPACIDAD (capacity_exceptions)
-- --------------------------------------------------------------------
INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, reason, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf551', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', CURRENT_DATE + INTERVAL '10 days', NULL, NULL, 0, 'Cierre extraordinario por inventario general y mantenimiento.', 'ACTIVE', NOW(), NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf552', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', CURRENT_DATE + INTERVAL '15 days', '09:00:00', '21:00:00', 35, 'Campaña especial de fin de semana con alta demanda.', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 14. RESERVAS DE CAPACIDAD (capacity_reservations)
-- --------------------------------------------------------------------
-- Reserva 1: Comprometida en Pedido 1 (Valeria Ramos)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time, 
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    CURRENT_DATE - INTERVAL '2 days',
    '14:00:00', '18:00:00',
    'COMMITTED',
    NOW() - INTERVAL '2 days' + INTERVAL '10 minutes',
    NOW() - INTERVAL '2 days' + INTERVAL '20 minutes',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
) ON CONFLICT (id) DO NOTHING;

-- Reserva 2: Comprometida en Pedido 2 (Juan Pérez)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time, 
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    CURRENT_DATE,
    '14:00:00', '18:00:00',
    'COMMITTED',
    NOW() + INTERVAL '10 minutes',
    NOW() + INTERVAL '20 minutes',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour'
) ON CONFLICT (id) DO NOTHING;

-- Reserva 3: Comprometida en Pedido 3 (Sofía Mendoza)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time, 
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf603',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f3',
    CURRENT_DATE,
    '08:00:00', '20:00:00',
    'COMMITTED',
    NOW() + INTERVAL '10 minutes',
    NOW() + INTERVAL '20 minutes',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '30 minutes'
) ON CONFLICT (id) DO NOTHING;

-- Reserva 4: Activa (Hold de 10 minutos para Carrito en proceso)
INSERT INTO capacity_reservations (
    id, store_id, user_id, service_date, start_time, end_time, 
    status, expires_at, payment_protection_expires_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf604',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    CURRENT_DATE + INTERVAL '1 day',
    '14:00:00', '18:00:00',
    'ACTIVE',
    NOW() + INTERVAL '10 minutes',
    NULL,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 15. PAGOS & INTENTOS DE PAGO (payments, payment_attempts)
-- --------------------------------------------------------------------
-- Pago 1 (Pedido 1 - Completado)
INSERT INTO payments (
    id, capacity_reservation_id, amount, currency, status, 
    refund_status, refund_amount, refund_reference, approved_at, refunded_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    54.50, 'PEN', 'APPROVED',
    'NOT_REQUIRED', NULL, NULL, NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO payment_attempts (
    id, payment_id, attempt_number, provider, payment_method, 
    idempotency_key, provider_payment_id, provider_status, provider_status_detail, 
    status, error_code, error_message, created_at, completed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf711',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    1, 'MERCADO_PAGO', 'CARD',
    'mp-idempotency-key-001', 'MP-PAY-987654321', 'approved', 'accredited',
    'APPROVED', NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
) ON CONFLICT (id) DO NOTHING;

-- Pago 2 (Pedido 2 - En Preparación)
INSERT INTO payments (
    id, capacity_reservation_id, amount, currency, status, 
    refund_status, refund_amount, refund_reference, approved_at, refunded_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    48.00, 'PEN', 'APPROVED',
    'NOT_REQUIRED', NULL, NULL, NOW() - INTERVAL '1 hour', NULL, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO payment_attempts (
    id, payment_id, attempt_number, provider, payment_method, 
    idempotency_key, provider_payment_id, provider_status, provider_status_detail, 
    status, error_code, error_message, created_at, completed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf712',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    1, 'MERCADO_PAGO', 'CARD',
    'mp-idempotency-key-002', 'MP-PAY-987654322', 'approved', 'accredited',
    'APPROVED', NULL, NULL, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'
) ON CONFLICT (id) DO NOTHING;

-- Pago 3 (Pedido 3 - Listo para Entrega)
INSERT INTO payments (
    id, capacity_reservation_id, amount, currency, status, 
    refund_status, refund_amount, refund_reference, approved_at, refunded_at, created_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf703',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf603',
    29.00, 'PEN', 'APPROVED',
    'NOT_REQUIRED', NULL, NULL, NOW() - INTERVAL '30 minutes', NULL, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO payment_attempts (
    id, payment_id, attempt_number, provider, payment_method, 
    idempotency_key, provider_payment_id, provider_status, provider_status_detail, 
    status, error_code, error_message, created_at, completed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf713',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf703',
    1, 'MERCADO_PAGO', 'CARD',
    'mp-idempotency-key-003', 'MP-PAY-987654323', 'approved', 'accredited',
    'APPROVED', NULL, NULL, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 16. PEDIDOS CONFIRMADOS (orders)
-- --------------------------------------------------------------------
-- Pedido 1: COMPLETED (Valeria Ramos · Delivery Pastelería)
INSERT INTO orders (
    id, order_number, user_id, store_id, payment_id, capacity_reservation_id, 
    status, delivery_mode, service_date, service_start_time, service_end_time, 
    customer_name, customer_phone, customer_email, delivery_address, delivery_reference, 
    delivery_latitude, delivery_longitude, currency, products_subtotal_amount, 
    discount_amount, discount_code, delivery_fee_amount, included_tax_amount, total_amount, 
    confirmed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf801',
    'JALDI-1001',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf701',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf601',
    'COMPLETED',
    'DELIVERY',
    CURRENT_DATE - INTERVAL '2 days',
    '14:00:00', '18:00:00',
    'Valeria Ramos',
    '+51984552109',
    'cliente.demo@jaldishop.com',
    'Av. Javier Prado Este 2501, San Borja, Lima',
    'Dpto 502, frente al parque',
    -12.087123, -77.004561,
    'PEN',
    55.00,
    5.50,
    'BIENVENIDA10',
    5.00,
    8.31,
    54.50,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days' + INTERVAL '3 hours'
) ON CONFLICT (id) DO NOTHING;

-- Pedido 2: IN_PREPARATION (Juan Pérez · Delivery Pastelería)
INSERT INTO orders (
    id, order_number, user_id, store_id, payment_id, capacity_reservation_id, 
    status, delivery_mode, service_date, service_start_time, service_end_time, 
    customer_name, customer_phone, customer_email, delivery_address, delivery_reference, 
    delivery_latitude, delivery_longitude, currency, products_subtotal_amount, 
    discount_amount, discount_code, delivery_fee_amount, included_tax_amount, total_amount, 
    confirmed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf802',
    'JALDI-1002',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf702',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf602',
    'IN_PREPARATION',
    'DELIVERY',
    CURRENT_DATE,
    '14:00:00', '18:00:00',
    'Juan Pérez',
    '+51912345678',
    'cliente.juan@jaldishop.com',
    'Av. Brasil 1850, Jesús María, Lima',
    'Edificio Los Cedros, timbre 301',
    -12.072341, -77.049812,
    'PEN',
    48.00,
    5.00,
    NULL,
    5.00,
    7.32,
    48.00,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '45 minutes'
) ON CONFLICT (id) DO NOTHING;

-- Pedido 3: READY (Sofía Mendoza · Pickup Café & Grano)
INSERT INTO orders (
    id, order_number, user_id, store_id, payment_id, capacity_reservation_id, 
    status, delivery_mode, service_date, service_start_time, service_end_time, 
    customer_name, customer_phone, customer_email, delivery_address, delivery_reference, 
    delivery_latitude, delivery_longitude, currency, products_subtotal_amount, 
    discount_amount, discount_code, delivery_fee_amount, included_tax_amount, total_amount, 
    confirmed_at, updated_at
) VALUES (
    '80acf0ba-5a79-4888-b00d-b5aef8fcf803',
    'JALDI-1003',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9f3',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf9e6',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf703',
    '80acf0ba-5a79-4888-b00d-b5aef8fcf603',
    'READY',
    'PICKUP',
    CURRENT_DATE,
    '08:00:00', '20:00:00',
    'Sofía Mendoza',
    '+51923456789',
    'cliente.sofia@jaldishop.com',
    NULL,
    NULL,
    NULL,
    NULL,
    'PEN',
    29.00,
    0.00,
    NULL,
    0.00,
    4.42,
    29.00,
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '10 minutes'
) ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 17. DETALLES DE PEDIDOS (order_items)
-- --------------------------------------------------------------------
-- Ítems Pedido 1
INSERT INTO order_items (id, order_id, variant_id, product_name, variant_name, attributes_snapshot, quantity, unit_price_amount, subtotal_amount) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf811', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf301', 'Torta de Chocolate Húmeda', 'Torta Entera (8-10 porciones)', '{"Tamaño": "Entera (8-10 porciones)"}'::jsonb, 1, 55.00, 55.00)
ON CONFLICT (id) DO NOTHING;

-- Ítems Pedido 2
INSERT INTO order_items (id, order_id, variant_id, product_name, variant_name, attributes_snapshot, quantity, unit_price_amount, subtotal_amount) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf812', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf303', 'Cheesecake de Fresa', 'Porción Individual', '{"Porción": "Individual"}'::jsonb, 2, 14.00, 28.00),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf813', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf305', 'Alfajores Artesanales de Maicena', 'Caja x6 Unidades', '{"Presentación": "Caja x6 Unidades"}'::jsonb, 1, 20.00, 20.00)
ON CONFLICT (id) DO NOTHING;

-- Ítems Pedido 3
INSERT INTO order_items (id, order_id, variant_id, product_name, variant_name, attributes_snapshot, quantity, unit_price_amount, subtotal_amount) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf814', '80acf0ba-5a79-4888-b00d-b5aef8fcf803', '80acf0ba-5a79-4888-b00d-b5aef8fcf307', 'Cappuccino Italiano Clásico', 'Mediano 12 oz', '{"Tamaño": "12 oz"}'::jsonb, 1, 9.50, 9.50),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf815', '80acf0ba-5a79-4888-b00d-b5aef8fcf803', '80acf0ba-5a79-4888-b00d-b5aef8fcf308', 'Cappuccino Italiano Clásico', 'Grande 16 oz', '{"Tamaño": "16 oz"}'::jsonb, 1, 12.00, 12.00),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf816', '80acf0ba-5a79-4888-b00d-b5aef8fcf803', '80acf0ba-5a79-4888-b00d-b5aef8fcf310', 'Croissant Artesanal de Jamón y Queso', 'Unidad Estándar', '{"Relleno": "Jamón Inglés & Queso Gouda"}'::jsonb, 1, 10.00, 10.00)
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 18. HISTORIAL DE ESTADOS (order_status_history)
-- --------------------------------------------------------------------
INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at) VALUES
    -- Trazabilidad Pedido 1
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf851', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'CONFIRMED', 'Pago acreditado y pedido confirmado automáticamente.', NOW() - INTERVAL '2 days'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf852', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'IN_PREPARATION', 'Pastelería inició la elaboración de la torta.', NOW() - INTERVAL '2 days' + INTERVAL '30 minutes'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf853', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'OUT_FOR_DELIVERY', 'Repartidor salió rumbo a la dirección de entrega.', NOW() - INTERVAL '2 days' + INTERVAL '2 hours'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf854', '80acf0ba-5a79-4888-b00d-b5aef8fcf801', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'COMPLETED', 'Pedido entregado conforme al cliente.', NOW() - INTERVAL '2 days' + INTERVAL '3 hours'),

    -- Trazabilidad Pedido 2
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf855', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'CONFIRMED', 'Pago acreditado y pedido recibido.', NOW() - INTERVAL '1 hour'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf856', '80acf0ba-5a79-4888-b00d-b5aef8fcf802', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'IN_PREPARATION', 'Empacando porciones de cheesecake y alfajores.', NOW() - INTERVAL '45 minutes'),

    -- Trazabilidad Pedido 3
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf857', '80acf0ba-5a79-4888-b00d-b5aef8fcf803', '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1', 'CONFIRMED', 'Pago recibido para recojo en tienda.', NOW() - INTERVAL '30 minutes'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf858', '80acf0ba-5a79-4888-b00d-b5aef8fcf803', '80acf0ba-5a79-4888-b00d-b5aef8fcf9f1', 'READY', 'Bebidas y croissant listos en barra para entrega.', NOW() - INTERVAL '10 minutes')
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 19. CARRITO ACTIVO DEMO (carts & cart_items)
-- --------------------------------------------------------------------
INSERT INTO carts (id, user_id, store_id, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf901', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5', NOW() - INTERVAL '15 minutes', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO cart_items (cart_id, variant_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf901', '80acf0ba-5a79-4888-b00d-b5aef8fcf303', 1, 14.00, 'PEN', NOW() - INTERVAL '15 minutes', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf901', '80acf0ba-5a79-4888-b00d-b5aef8fcf305', 1, 20.00, 'PEN', NOW() - INTERVAL '10 minutes', NOW())
ON CONFLICT (cart_id, variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, reference_price_amount = EXCLUDED.reference_price_amount, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------
-- 20. FAVORITOS (favorites)
-- --------------------------------------------------------------------
INSERT INTO favorites (user_id, product_id, created_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', NOW() - INTERVAL '15 days'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', '80acf0ba-5a79-4888-b00d-b5aef8fcf202', NOW() - INTERVAL '10 days'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9f2', '80acf0ba-5a79-4888-b00d-b5aef8fcf204', NOW() - INTERVAL '5 days'),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf9f3', '80acf0ba-5a79-4888-b00d-b5aef8fcf205', NOW() - INTERVAL '2 days')
ON CONFLICT (user_id, product_id) DO NOTHING;

-- --------------------------------------------------------------------
-- 21. RESEÑAS & CALIFICACIONES (reviews)
-- --------------------------------------------------------------------
INSERT INTO reviews (id, user_id, product_id, rating, comment, status, created_at, updated_at) VALUES
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf951', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', '80acf0ba-5a79-4888-b00d-b5aef8fcf201', 5, '¡La mejor torta de chocolate de Lima! Súper húmeda y el fudge es espectacular.', 'PUBLISHED', NOW() - INTERVAL '1 day', NOW()),
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf952', '80acf0ba-5a79-4888-b00d-b5aef8fcf9f2', '80acf0ba-5a79-4888-b00d-b5aef8fcf204', 4, 'Muy ricos alfajores, la masa se deshace en la boca. Recomendados.', 'PUBLISHED', NOW() - INTERVAL '2 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- --------------------------------------------------------------------
-- 22. NOTIFICACIONES DEL SISTEMA (notifications)
-- --------------------------------------------------------------------
INSERT INTO notifications (id, user_id, type, title, message, status, created_at, read_at) VALUES
    -- Notificación Merchant: Nuevo Pedido
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf981', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'NEW_ORDER', '¡Nuevo pedido recibido! #JALDI-1002', 'El cliente Juan Pérez ha realizado un pedido por S/ 48.00.', 'UNREAD', NOW() - INTERVAL '1 hour', NULL),
    -- Notificación Merchant: Alerta de Stock Bajo
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf982', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1', 'LOW_STOCK', 'Alerta de Inventario: Stock Bajo', 'El producto Cheesecake de Fresa cuenta solo con 2 unidades disponibles.', 'UNREAD', NOW() - INTERVAL '2 hours', NULL),
    -- Notificación Cliente: Pedido en camino
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf983', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2', 'ORDER_STATUS_CHANGED', 'Tu pedido #JALDI-1001 fue entregado', '¡Esperamos que disfrutes tu Torta de Chocolate! No olvides dejar tu reseña.', 'READ', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 hours'),
    -- Notificación Admin: Mensaje del Sistema
    ('80acf0ba-5a79-4888-b00d-b5aef8fcf984', '80acf0ba-5a79-4888-b00d-b5aef8fcf9e3', 'SYSTEM', 'Respaldo del Sistema Exitoso', 'El respaldo automatizado de la base de datos se ejecutó correctamente a las 03:00 UTC.', 'READ', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;

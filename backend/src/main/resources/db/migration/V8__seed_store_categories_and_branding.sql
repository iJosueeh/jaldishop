-- ==============================================================================
-- JaldiShop Migration V8: Seed Store Categories, Storefront Branding & Assignments
-- Version: V8__seed_store_categories_and_branding.sql
-- Description:
--   1. Seed general store categories (Rubros Comerciales).
--   2. Assign default categories to existing demo stores.
--   3. Populate branding (logo, banner, social URLs) for demo stores.
--   4. Ensure product_images backfill for existing products.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. SEED GENERAL STORE CATEGORIES (Rubros Comerciales Generales)
-- ------------------------------------------------------------------------------
INSERT INTO store_categories (id, name, slug, description, status, created_at, updated_at) VALUES
(gen_random_uuid(), 'Restaurantes y Cafeterías', 'restaurantes-cafeterias', 'Comida preparada, cafés, repostería y bebidas', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Moda y Calzado', 'moda-calzado', 'Prendas de vestir, zapatos y accesorios de moda', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Supermercado y Bodega', 'supermercado-bodega', 'Abarrotes, alimentos frescos y productos de primera necesidad', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Tecnología y Electrónica', 'tecnologia-electronica', 'Dispositivos electrónicos, computadoras y gadgets', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Salud y Belleza', 'salud-belleza', 'Cosméticos, cuidado personal y bienestar', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Hogar y Decoración', 'hogar-decoracion', 'Muebles, artículos para el hogar y decoración', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Mascotas', 'mascotas', 'Alimentos y accesorios para mascotas', 'ACTIVE', NOW(), NOW()),
(gen_random_uuid(), 'Servicios y Otros', 'servicios-otros', 'Servicios profesionales y giros diversos', 'ACTIVE', NOW(), NOW())
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = NOW();

-- ------------------------------------------------------------------------------
-- 2. ASIGNACIÓN DE CATEGORÍAS A TIENDAS DEMO (store_category_assignments)
-- ------------------------------------------------------------------------------
INSERT INTO store_category_assignments (store_id, store_category_id)
SELECT s.id, sc.id
FROM stores s
CROSS JOIN store_categories sc
WHERE s.slug IN ('dulce-deleite', 'cafe-grano')
  AND sc.slug = 'restaurantes-cafeterias'
ON CONFLICT (store_id, store_category_id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 3. BRANDING Y REDES SOCIALES PARA TIENDAS DEMO (stores)
-- ------------------------------------------------------------------------------
UPDATE stores
SET logo_url = 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&auto=format&fit=crop&q=80',
    banner_url = 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=1200&auto=format&fit=crop&q=80',
    whatsapp_number = COALESCE(whatsapp_number, '+51987654321'),
    instagram_url = COALESCE(instagram_url, 'https://instagram.com/dulcedeleite.pe'),
    facebook_url = COALESCE(facebook_url, 'https://facebook.com/dulcedeleite.oficial')
WHERE slug = 'dulce-deleite' AND (logo_url IS NULL OR logo_url = '');

UPDATE stores
SET logo_url = 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=500&auto=format&fit=crop&q=80',
    banner_url = 'https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=1200&auto=format&fit=crop&q=80',
    whatsapp_number = COALESCE(whatsapp_number, '+51977777777'),
    instagram_url = COALESCE(instagram_url, 'https://instagram.com/cafeygrano.pe'),
    facebook_url = COALESCE(facebook_url, 'https://facebook.com/cafeygrano.oficial')
WHERE slug = 'cafe-grano' AND (logo_url IS NULL OR logo_url = '');

-- ------------------------------------------------------------------------------
-- 4. BACKFILL PRODUCT_IMAGES PARA PRODUCTOS EXISTENTES
-- ------------------------------------------------------------------------------
INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at)
SELECT gen_random_uuid(), p.id, p.image_url, 0, true, NOW()
FROM products p
WHERE p.image_url IS NOT NULL 
  AND TRIM(p.image_url) != ''
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi WHERE pi.product_id = p.id
  );

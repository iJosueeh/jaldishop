-- ============================================
-- JaldiShop - Seed Initial Roles
-- Version: V2__seed_roles.sql
-- Description: Insert initial roles (CUSTOMER, MERCHANT, ADMIN)
-- ============================================

INSERT INTO roles (id, name) VALUES
    (1, 'CUSTOMER'),
    (2, 'MERCHANT'),
    (3, 'ADMIN')
ON CONFLICT (id) DO NOTHING;
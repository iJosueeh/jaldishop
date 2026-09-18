-- ============================================
-- JaldiShop - Seed: Roles iniciales
-- Version: V2__seed_roles.sql
-- Source: docs/04-diseno/modelo-er.md v1.6.1
-- Flyway: managed seed
-- ============================================

INSERT INTO roles (id, name) VALUES
    (1, 'CUSTOMER'),
    (2, 'MERCHANT'),
    (3, 'ADMIN');

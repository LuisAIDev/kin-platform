-- ============================================================
-- V18: ajuste del catálogo de categorías (auditoría de selección)
--  1. Amplía GASTRONOMIA a "Gastronomía y Alimentos" para cubrir
--     food & beverage sin duplicar una categoría.
--  2. Añade SERVICIOS y OTRO (fallback genérico) para que una
--     clasificación errónea no fuerce el contexto del LLM.
-- Idempotente: UPDATE condicional + INSERT ON CONFLICT DO NOTHING.
-- NO modifica UUIDs existentes ni elimina categorías ni FKs.
-- ============================================================

UPDATE categories
SET name = 'Gastronomía y Alimentos'
WHERE code = 'GASTRONOMIA' AND name = 'Gastronomía';

INSERT INTO categories
    (id, code, name, description, display_order, icon, color, active, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111112', 'SERVICIOS', 'Servicios',
     NULL, 18, NULL, '#64748b', TRUE, NOW(), NOW()),
    ('11111111-1111-1111-1111-111111111113', 'OTRO', 'Otro / Sin clasificar',
     NULL, 19, NULL, '#94a3b8', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

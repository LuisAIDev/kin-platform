-- ============================================================
-- V86: consolidar los planes legacy de EMPRESAS con los canónicos.
--
-- Contexto: bases antiguas tienen planes sin `code` (p. ej. "GRATIS" y
-- "STANDARD"). V85 actualiza por `code`, así que no pudo tocarlos; el API
-- seguía devolviéndolos como tarjetas activas duplicadas y sin `price_cop`.
--
-- Esta migración NO modifica V85 (su checksum ya está aplicado en producción).
-- En su lugar:
--   1. Repunta suscripciones y `users.current_plan_id` al plan canónico
--      equivalente cuando ya existe.
--   2. Desactiva los planes legacy sin `code` de EMPRESAS. Si no existe el
--      canónico, DataInitializer adopta el legacy al arrancar (le asigna el
--      `code` y los precios aprobados) y lo reactiva.
--   3. Reafirma los precios aprobados en los planes canónicos (idempotente).
-- ============================================================

-- 1. Repuntar suscripciones de un plan legacy al canónico equivalente.
WITH legacy_map AS (
    SELECT l.id AS legacy_id,
           c.id AS canonical_id
      FROM pricing_plans l
      JOIN pricing_plans c
        ON c.vertical = l.vertical
       AND c.code = CASE
                        WHEN l.name IN ('GRATIS', 'FREE', 'Básico Gratis') THEN 'FREE'
                        WHEN l.name = 'STANDARD' THEN 'STANDARD'
                        WHEN l.name IN ('PREMIUM', 'Premium Pro') THEN 'PREMIUM'
                    END
     WHERE l.vertical = 'EMPRESAS'
       AND l.code IS NULL
)
UPDATE user_subscriptions us
   SET plan_id = lm.canonical_id
  FROM legacy_map lm
 WHERE us.plan_id = lm.legacy_id;

-- 2. Repuntar el plan actual del usuario.
WITH legacy_map AS (
    SELECT l.id AS legacy_id,
           c.id AS canonical_id
      FROM pricing_plans l
      JOIN pricing_plans c
        ON c.vertical = l.vertical
       AND c.code = CASE
                        WHEN l.name IN ('GRATIS', 'FREE', 'Básico Gratis') THEN 'FREE'
                        WHEN l.name = 'STANDARD' THEN 'STANDARD'
                        WHEN l.name IN ('PREMIUM', 'Premium Pro') THEN 'PREMIUM'
                    END
     WHERE l.vertical = 'EMPRESAS'
       AND l.code IS NULL
)
UPDATE users u
   SET current_plan_id = lm.canonical_id
  FROM legacy_map lm
 WHERE u.current_plan_id = lm.legacy_id;

-- 3. Desactivar los planes legacy de EMPRESAS (ya no se muestran en el catálogo).
UPDATE pricing_plans
   SET is_active = FALSE,
       updated_at = NOW()
 WHERE vertical = 'EMPRESAS'
   AND code IS NULL;

-- 4. Reafirmar precios aprobados en los planes canónicos (USD y COP).
UPDATE pricing_plans
   SET price = 0.00,
       price_cop = 0.00,
       updated_at = NOW()
 WHERE vertical = 'EMPRESAS'
   AND code = 'FREE';

UPDATE pricing_plans
   SET price = 15.00,
       price_cop = 60000.00,
       updated_at = NOW()
 WHERE vertical = 'EMPRESAS'
   AND code = 'STANDARD';

UPDATE pricing_plans
   SET price = 22.00,
       price_cop = 88000.00,
       updated_at = NOW()
 WHERE vertical = 'EMPRESAS'
   AND code = 'PREMIUM';

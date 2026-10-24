-- ============================================================
-- V85: Nueva estrategia de precios para la vertical EMPRESAS.
--
-- Precios aprobados por negocio:
--   FREE      USD 0.00   COP 0.00
--   STANDARD  USD 15.00  COP 60.000
--   PREMIUM   USD 22.00  COP 88.000
--
-- `price` es el monto en USD que cobra Stripe; `price_cop` es el monto
-- en COP que cobra Wompi. No se tocan planes de SALUD_PERSONAL ni
-- SALUD_PROFESIONAL.
-- ============================================================

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

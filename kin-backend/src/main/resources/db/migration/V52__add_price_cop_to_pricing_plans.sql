-- V52: Precio en COP para la pasarela Wompi.
-- Los planes de salud guardan su precio en USD (columna `price`) porque Stripe
-- cobra en dólares. Wompi solo acepta COP, por lo que se añade una columna
-- independiente `price_cop`. Es NULLABLE a propósito: no se inventa ningún
-- valor. Mientras un plan no tenga `price_cop`, el checkout con Wompi falla con
-- un error explícito en lugar de cobrar un monto incorrecto.
--
-- Acción del humano: fijar el valor por plan antes de habilitar Wompi, p. ej.:
--   UPDATE pricing_plans SET price_cop = 8000   WHERE code = 'PERSONAL_START' AND vertical = 'SALUD_PERSONAL';
--   UPDATE pricing_plans SET price_cop = 37000  WHERE code = 'PERSONAL_PLUS'  AND vertical = 'SALUD_PERSONAL';
--   UPDATE pricing_plans SET price_cop = 140000 WHERE code = 'PROFESSIONAL'   AND vertical = 'SALUD_PROFESIONAL';

ALTER TABLE pricing_plans
    ADD COLUMN IF NOT EXISTS price_cop NUMERIC(12, 2);

COMMENT ON COLUMN pricing_plans.price_cop IS
    'Precio en COP para Wompi (PSE/Nequi). NULL = no disponible para Wompi. Independiente de price (USD, Stripe).';

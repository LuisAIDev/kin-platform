-- V51: Añadir preferencia de pasarela de pago (STRIPE / WOMPI)
-- Permite a usuarios colombianos usar Wompi (PSE, Nequi, tarjetas locales)
-- mientras el resto sigue usando Stripe (USD, internacional)

ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_payment_gateway VARCHAR(20) DEFAULT 'STRIPE';

CREATE INDEX IF NOT EXISTS idx_users_preferred_gateway 
    ON users(preferred_payment_gateway);

ALTER TABLE users ADD CONSTRAINT chk_preferred_payment_gateway 
    CHECK (preferred_payment_gateway IN ('STRIPE', 'WOMPI'));

-- Actualizar usuarios colombianos existentes a WOMPI por defecto
-- (opcional: se puede hacer via batch posterior)
-- UPDATE users SET preferred_payment_gateway = 'WOMPI' 
-- WHERE country = 'CO' AND preferred_payment_gateway = 'STRIPE';
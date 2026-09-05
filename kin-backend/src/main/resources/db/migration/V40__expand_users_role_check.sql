-- ============================================================
-- V40: expande users_role_check para incluir los roles de salud
-- (PATIENT y PHYSICIAN).
--
-- Contexto: el CHECK previo (creado en un schema anterior, fuera
-- de Flyway) solo permitia FREE/PREMIUM/FACILITADOR/ADMIN. Desde
-- que el enum UserRole incluye PATIENT y PHYSICIAN, el registro de
-- pacientes y medicos fallaba con DataIntegrityViolationException
-- (SQLState 23514, check constraint "users_role_check") -> HTTP 409.
--
-- Idempotente: DROP IF EXISTS + ADD CONSTRAINT permite re-ejecutarla
-- en bases donde el fix ya se haya aplicado a mano.
-- ============================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('FREE', 'PREMIUM', 'FACILITADOR', 'PATIENT', 'PHYSICIAN', 'ADMIN'));

-- ============================================================
-- V62: Roles institucionales IPS_* (Sprint 2 - Fase 2.1)
-- ============================================================
-- Constraint real: users_role_check (creado en V40), NO chk_users_role.
-- Se mantiene FACILITADOR (existe en UserRole) para no romper filas previas.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('FREE', 'PREMIUM', 'FACILITADOR', 'PATIENT', 'PHYSICIAN', 'ADMIN',
                    'IPS_ADMIN', 'IPS_MEDICO', 'IPS_ENFERMERA', 'IPS_FACTURADOR', 'IPS_AUDITOR'));

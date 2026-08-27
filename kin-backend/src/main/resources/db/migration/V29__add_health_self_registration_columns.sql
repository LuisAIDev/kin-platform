-- ============================================================
-- V29: auto-registro de salud (pacientes y médicos)
-- Columnas adicionales en users para el flujo de registro de la
-- vertical Salud (ADR-032) y la verificación de identidad médica.
-- PostgreSQL (todos los entornos; Flyway V1..V29).
-- ============================================================

-- Datos del paciente (fecha de nacimiento, sexo, teléfono).
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sex VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

-- Datos del médico (cédula profesional, especialidad, país).
ALTER TABLE users ADD COLUMN IF NOT EXISTS license_number VARCHAR(60);
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialty VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(60);

-- Estado de verificación del médico: PENDING (auto-registro, pendiente de
-- aprobación por ADMIN), APPROVED o REJECTED. NULL = no sujeto a revisión
-- (médicos provisionados por el piloto/administrador, equivalen a APROBADO).
ALTER TABLE users ADD COLUMN IF NOT EXISTS physician_verification_status VARCHAR(20);

-- Consentimiento explícito para el tratamiento de datos de salud (requisito
-- legal para el auto-registro de la vertical Salud).
ALTER TABLE users ADD COLUMN IF NOT EXISTS health_data_consent BOOLEAN NOT NULL DEFAULT FALSE;

-- Índice para el panel de administración (médicos pendientes de revisión).
CREATE INDEX IF NOT EXISTS idx_users_physician_verification
    ON users (role, physician_verification_status);

-- ============================================================
-- V30: evolución de physician_patient_assignments — ciclo de vida
-- de la relación médico-paciente (invitación/aceptación/gestión).
-- Añade estado, quién invitó, fechas del ciclo de vida y motivo de fin.
-- Las filas existentes (creadas por ADMIN/piloto hasta ahora) quedan ACTIVE.
-- PostgreSQL (todos los entornos; Flyway V1..V30).
-- ============================================================

ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS invited_by UUID;
ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS invited_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ;
ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS ended_at TIMESTAMPTZ;
ALTER TABLE physician_patient_assignments ADD COLUMN IF NOT EXISTS ended_reason TEXT;

-- FK de quién invitó (referencia a users; si el usuario se elimina, la
-- referencia queda NULL, no se borra la relación).
ALTER TABLE physician_patient_assignments DROP CONSTRAINT IF EXISTS fk_ppa_invited_by;
ALTER TABLE physician_patient_assignments ADD CONSTRAINT fk_ppa_invited_by
    FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE SET NULL;

-- CHECK del estado del ciclo de vida.
ALTER TABLE physician_patient_assignments DROP CONSTRAINT IF EXISTS chk_ppa_status;
ALTER TABLE physician_patient_assignments ADD CONSTRAINT chk_ppa_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'ENDED'));

-- Índices para búsquedas por estado.
CREATE INDEX IF NOT EXISTS idx_ppa_physician_status ON physician_patient_assignments (physician_id, status);
CREATE INDEX IF NOT EXISTS idx_ppa_patient_status ON physician_patient_assignments (patient_id, status);

-- ============================================================
-- Backfill: las relaciones creadas por ADMIN/piloto (sin invited_by,
-- filas previas a esta migración) quedan ACTIVAS para no bloquear
-- el piloto ni la funcionalidad existente.
-- ============================================================
UPDATE physician_patient_assignments
   SET status = 'ACTIVE',
       invited_at = COALESCE(invited_at, NOW()),
       accepted_at = COALESCE(accepted_at, NOW())
 WHERE status = 'PENDING'
   AND invited_by IS NULL;

-- ============================================================
-- V32: agenda y disponibilidad de médicos (ADR-034)
-- Disponibilidad semanal recurrente + ampliación de appointments
-- (reprogramación, cancelación, slot y recordatorio).
-- PostgreSQL (todos los entornos; Flyway V1..V32).
-- ============================================================

CREATE TABLE IF NOT EXISTS physician_availabilities (
    id                    UUID PRIMARY KEY,
    physician_id          UUID NOT NULL,
    day_of_week           VARCHAR(12) NOT NULL,
    start_time            TIME NOT NULL,
    end_time              TIME NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pa_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_pa_day CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_pa_duration CHECK (slot_duration_minutes > 0 AND slot_duration_minutes <= 240),
    CONSTRAINT chk_pa_hours CHECK (start_time < end_time)
);

-- Ampliación de appointments (ADR-034): reprogramación, cancelación, slot, duración y recordatorio.
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS rescheduled_from UUID;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(300);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS availability_slot_id UUID;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 30;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- Estado REPROGRAMADA para el ciclo de vida de la cita.
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_appointments_status;
ALTER TABLE appointments ADD CONSTRAINT chk_appointments_status
    CHECK (status IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA', 'REPROGRAMADA'));

CREATE INDEX IF NOT EXISTS idx_appointments_physician_scheduled ON appointments (physician_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_scheduled ON appointments (patient_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_pa_physician_day ON physician_availabilities (physician_id, day_of_week);

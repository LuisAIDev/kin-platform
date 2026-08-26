-- ============================================================
-- V25: health physician (Portal para Médicos, ADR-031)
-- Asignación médico-paciente y alertas clínicas.
-- PostgreSQL (todos los entornos; Flyway V1..V25).
-- ============================================================

CREATE TABLE IF NOT EXISTS physician_patient_assignments (
    physician_id UUID NOT NULL,
    patient_id   UUID NOT NULL,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (physician_id, patient_id),
    CONSTRAINT fk_ppa_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ppa_patient   FOREIGN KEY (patient_id)   REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS clinical_alerts (
    id             UUID PRIMARY KEY,
    patient_id     UUID NOT NULL,
    physician_id   UUID NOT NULL,
    type           VARCHAR(30) NOT NULL,
    severity       VARCHAR(20) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    CONSTRAINT fk_ca_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_patient   FOREIGN KEY (patient_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ca_severity CHECK (severity IN ('ALTA', 'MEDIA', 'BAJA')),
    CONSTRAINT chk_ca_status   CHECK (status IN ('PENDING', 'ACKNOWLEDGED'))
);

CREATE INDEX IF NOT EXISTS idx_ca_physician_status ON clinical_alerts (physician_id, status);
CREATE INDEX IF NOT EXISTS idx_ca_patient ON clinical_alerts (patient_id);

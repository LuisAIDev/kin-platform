-- ============================================================
-- V39: enlace temporal de compartición de un informe de triaje
-- con un médico externo (fuera de KIN).
--
-- El paciente genera un token UUID (impredecible) que da acceso
-- de SOLO LECTURA al contenido de UN triaje concreto durante 24h.
-- El enlace es reutilizable dentro de esa ventana y puede revocarse
-- manualmente antes de que expire.
--
-- Sin seeds: todas las columnas NOT NULL se llenan desde la
-- aplicación (triage_id/patient_id/created_by vienen del triaje del
-- paciente autenticado; token/expires_at los genera el servidor).
-- revoked_at es nullable por diseño (solo se llena al revocar).
-- ============================================================

CREATE TABLE IF NOT EXISTS triage_share_links (
    id          UUID PRIMARY KEY,
    triage_id   UUID NOT NULL,
    patient_id  UUID NOT NULL,
    token       VARCHAR(36) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ,
    created_by  UUID NOT NULL,

    CONSTRAINT fk_share_link_triage
        FOREIGN KEY (triage_id) REFERENCES triage_consultations (id) ON DELETE CASCADE,
    CONSTRAINT fk_share_link_patient
        FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_share_link_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_share_link_token ON triage_share_links (token);
CREATE INDEX IF NOT EXISTS idx_share_link_triage ON triage_share_links (triage_id);
CREATE INDEX IF NOT EXISTS idx_share_link_expires ON triage_share_links (expires_at);

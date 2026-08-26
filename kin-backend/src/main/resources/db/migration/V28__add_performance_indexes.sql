-- ============================================================
-- V28: índices de rendimiento (fase de producción)
-- Tablas de salud de alto volumen: messages, appointments,
-- triage_consultations y catálogo.
-- PostgreSQL (todos los entornos; Flyway V1..V28).
-- ============================================================

-- Telemedicina: búsqueda de conversaciones por receptor no leído
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id, created_at);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages (receiver_id, created_at);

-- Telemedicina: listas de citas por estado (médico filtra pendientes)
CREATE INDEX IF NOT EXISTS idx_appointments_physician_status ON appointments (physician_id, status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_status ON appointments (patient_id, status, scheduled_at);

-- Dashboard: historial del paciente (fecha descendente) y conteos
CREATE INDEX IF NOT EXISTS idx_triage_consultations_user_created
    ON triage_consultations (user_id, created_at DESC);

-- Diagnóstico diferencial: alertas activas por médico
CREATE INDEX IF NOT EXISTS idx_alerts_physician_created ON clinical_alerts (physician_id, created_at DESC);

-- Catálogo: resolución de síntomas/condiciones por nombre (NLP e importación)
CREATE INDEX IF NOT EXISTS idx_symptoms_name ON symptoms (name);
CREATE INDEX IF NOT EXISTS idx_conditions_name ON conditions (name);

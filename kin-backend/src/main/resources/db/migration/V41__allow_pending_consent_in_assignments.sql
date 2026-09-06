-- ============================================================
-- V41: ampliar chk_ppa_status para permitir PENDING_CONSENT
-- (invitación a una cuenta existente sin capacidad de paciente).
-- El one-click consent (ADR-039/040) introdujo el estado
-- PENDING_CONSENT en el enum RelationshipStatus, pero la CHECK
-- de V30 solo permitía PENDING/ACTIVE/SUSPENDED/ENDED, por lo que
-- invitar a un usuario sin health_data_consent fallaba con
-- DataIntegrityViolation (409 genérico) en producción.
-- PostgreSQL (todos los entornos; Flyway V1..V41).
-- ============================================================

ALTER TABLE physician_patient_assignments DROP CONSTRAINT IF EXISTS chk_ppa_status;
ALTER TABLE physician_patient_assignments ADD CONSTRAINT chk_ppa_status
    CHECK (status IN ('PENDING', 'PENDING_CONSENT', 'ACTIVE', 'SUSPENDED', 'ENDED'));

-- V76: alinear tipos de columnas HCE con las entidades JPA (Hibernate schema-validation).
--
-- V75 declaró estas columnas como SMALLINT, pero las entidades JPA del Bloque 1
-- (paquete com.kinplatform.kin.health.hce.entity) las mapean como Integer, que
-- Hibernate espera como INTEGER (int4). En producción `spring.jpa.hibernate.ddl-auto=validate`
-- aborta el arranque con: "wrong column type encountered ... found [int2], but expecting [integer]".
--
-- Este ALTER es SEGURO y LOSSLESS:
--   * El ensanchado SMALLINT -> INTEGER no trunca ni pierde datos (todo valor int2 cabe en int4).
--   * PostgreSQL lo resuelve como cambio de metadata de la columna.
--   * Los CHECK de negocio (jemplos: severity 1..10, stratum 1..6, glasgow 3..15) se conservan y
--     se revalidan automáticamente.
--   * Forward-only: no toca V75 ni migraciones previas.
-- Idempotente a nivel de intención: si la columna ya es integer, ALTER ... TYPE INTEGER no altera datos.

ALTER TABLE patient_identification  ALTER COLUMN stratum TYPE INTEGER;
ALTER TABLE anamnesis               ALTER COLUMN severity_self_reported TYPE INTEGER;

ALTER TABLE physical_exam
    ALTER COLUMN bp_systolic      TYPE INTEGER,
    ALTER COLUMN bp_diastolic     TYPE INTEGER,
    ALTER COLUMN heart_rate       TYPE INTEGER,
    ALTER COLUMN respiratory_rate TYPE INTEGER,
    ALTER COLUMN spo2             TYPE INTEGER,
    ALTER COLUMN glasgow_score    TYPE INTEGER,
    ALTER COLUMN pain_scale       TYPE INTEGER;

ALTER TABLE medical_orders          ALTER COLUMN duration_days TYPE INTEGER;

ALTER TABLE obstetric_history
    ALTER COLUMN gravida                       TYPE INTEGER,
    ALTER COLUMN para                          TYPE INTEGER,
    ALTER COLUMN abortions                     TYPE INTEGER,
    ALTER COLUMN ectopic_pregnancies           TYPE INTEGER,
    ALTER COLUMN stillbirths                   TYPE INTEGER,
    ALTER COLUMN living_children               TYPE INTEGER,
    ALTER COLUMN gestational_weeks             TYPE INTEGER,
    ALTER COLUMN prenatal_controls             TYPE INTEGER,
    ALTER COLUMN breastfeeding_duration_months TYPE INTEGER;

ALTER TABLE surgical_history        ALTER COLUMN asa_classification TYPE INTEGER;

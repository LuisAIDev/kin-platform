-- ============================================================
-- V75: HCE Core - Resolución 839/1995 compliance
-- Crea 14 tablas nuevas + 1 función + 14 triggers + 1 vista para la
-- Historia Clínica Electrónica completa (núcleo Fase 2.5a).
--
-- IMPORTANTE (revisión PO — Opción C):
--   * NO aplicar hasta aprobación explícita.
--   * NO toca patient_profiles ni patient_evolutions (siguen como
--     tablas normales, los adapters JPA actuales no cambian).
--   * NO crea vistas de compatibilidad con esos nombres.
--   * hce_complete_view lee de la tabla existente patient_evolutions.
-- ============================================================

-- ============================================================
-- 0. FUNCTION: update_updated_at_column (FIX ERROR 7)
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- ============================================================
-- 1. encounters — Tabla central de encuentros clínicos (FIX ERROR 2)
-- ============================================================
CREATE TABLE encounters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL,
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    encounter_type VARCHAR(30) NOT NULL CHECK (encounter_type IN (
        'OUTPATIENT', 'INPATIENT', 'EMERGENCY', 'TELEMEDICINE', 'HOME_CARE', 'DAY_SURGERY'
    )),
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN (
        'SCHEDULED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
    )),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    chief_complaint TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_encounters_patient ON encounters(patient_id);
CREATE INDEX idx_encounters_physician ON encounters(physician_id);
CREATE INDEX idx_encounters_org ON encounters(organization_id);
CREATE INDEX idx_encounters_appointment ON encounters(appointment_id);
CREATE INDEX idx_encounters_status ON encounters(status);
CREATE INDEX idx_encounters_started ON encounters(started_at);

CREATE TRIGGER trg_encounters_updated BEFORE UPDATE ON encounters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 2. patient_identification — Datos identificación paciente (FIX ERROR 5)
-- ============================================================
CREATE TABLE patient_identification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type VARCHAR(10) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    document_expedition_date DATE,
    document_expedition_place VARCHAR(100),
    rh_factor VARCHAR(5),
    eps_code VARCHAR(20),
    eps_name VARCHAR(200),
    regimen VARCHAR(20),
    guardian_name VARCHAR(200),
    guardian_document_type VARCHAR(10),
    guardian_document_number VARCHAR(20),
    guardian_phone VARCHAR(30),
    guardian_relationship VARCHAR(50),
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(30),
    emergency_contact_relationship VARCHAR(50),
    address VARCHAR(500),
    city_code VARCHAR(10),
    department_code VARCHAR(10),
    zone VARCHAR(20),
    stratum SMALLINT CHECK (stratum BETWEEN 1 AND 6),
    email_institutional VARCHAR(255),
    phone_secondary VARCHAR(30),
    ethnicity VARCHAR(50),
    displacement_victim BOOLEAN DEFAULT FALSE,
    disability_certificate VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_patient_identification_user_doc UNIQUE (user_id, document_number)
);

CREATE INDEX idx_patient_id_user ON patient_identification(user_id);
CREATE INDEX idx_patient_id_doc ON patient_identification(document_type, document_number);

CREATE TRIGGER trg_patient_identification_updated BEFORE UPDATE ON patient_identification
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 3. anamnesis — Historia de la enfermedad actual (HEA) (FIX ERROR 3)
-- ============================================================
CREATE TABLE anamnesis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    onset_datetime TIMESTAMPTZ,
    evolution_description TEXT,
    aggravating_factors TEXT,
    alleviating_factors TEXT,
    associated_symptoms TEXT,
    severity_self_reported SMALLINT CHECK (severity_self_reported BETWEEN 1 AND 10),
    systems_review JSONB DEFAULT '{}',
    previous_episodes INTEGER DEFAULT 0,
    previous_treatments TEXT,
    functional_impact TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_anamnesis_encounter ON anamnesis(encounter_id);
CREATE INDEX idx_anamnesis_patient ON anamnesis(patient_id);
CREATE INDEX idx_anamnesis_physician ON anamnesis(physician_id);

CREATE TRIGGER trg_anamnesis_updated BEFORE UPDATE ON anamnesis
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 4. patient_history — Antecedentes normalizados
-- ============================================================
CREATE TABLE patient_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    history_type VARCHAR(30) NOT NULL CHECK (history_type IN (
        'ALLERGY', 'SURGERY', 'MEDICATION', 'VACCINE', 'FAMILY', 'TOXICOLOGICAL', 'GYNECO_OBSTETRIC'
    )),
    description TEXT NOT NULL,
    onset_date DATE,
    resolution_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RESOLVED', 'CHRONIC', 'IN_REMISSION')),
    severity VARCHAR(20),
    notes TEXT,
    recorded_by UUID REFERENCES users(id),
    recorded_at TIMESTAMPTZ DEFAULT NOW(),
    details JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_patient_history_patient_type ON patient_history(patient_id, history_type);
CREATE INDEX idx_patient_history_status ON patient_history(status);

CREATE TRIGGER trg_patient_history_updated BEFORE UPDATE ON patient_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 5. physical_exam — Examen físico + signos vitales tipados (FIX ERROR 3)
-- ============================================================
CREATE TABLE physical_exam (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    evolution_id UUID REFERENCES patient_evolutions(id) ON DELETE SET NULL,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    bp_systolic SMALLINT,
    bp_diastolic SMALLINT,
    heart_rate SMALLINT,
    respiratory_rate SMALLINT,
    temperature NUMERIC(4,1),
    spo2 SMALLINT,
    weight_kg NUMERIC(5,2),
    height_cm NUMERIC(5,2),
    bmi NUMERIC(4,2) GENERATED ALWAYS AS (
        CASE WHEN height_cm > 0 THEN round(weight_kg / pow(height_cm/100, 2)::numeric, 2) END
    ) STORED,
    glasgow_score SMALLINT CHECK (glasgow_score BETWEEN 3 AND 15),
    pain_scale SMALLINT CHECK (pain_scale BETWEEN 0 AND 10),
    pain_scale_type VARCHAR(20) DEFAULT 'EVA',
    general_appearance VARCHAR(100),
    head_neck TEXT,
    cardiovascular TEXT,
    respiratory TEXT,
    abdominal TEXT,
    neurological TEXT,
    musculoskeletal TEXT,
    skin TEXT,
    genitourinary TEXT,
    psychiatric TEXT,
    validated_scales JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_physical_exam_encounter ON physical_exam(encounter_id);
CREATE INDEX idx_physical_exam_evolution ON physical_exam(evolution_id);
CREATE INDEX idx_physical_exam_patient ON physical_exam(patient_id);

CREATE TRIGGER trg_physical_exam_updated BEFORE UPDATE ON physical_exam
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 6. diagnoses — Diagnósticos clínicos (FIX ERROR 3)
-- ============================================================
CREATE TABLE diagnoses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    cie10_code VARCHAR(10) NOT NULL,
    cie10_description VARCHAR(500),
    diagnosis_type VARCHAR(20) NOT NULL CHECK (diagnosis_type IN (
        'PRINCIPAL', 'SECUNDARIO', 'COMORBILIDAD', 'COMPLICACION', 'INGRESO', 'EGRESO'
    )),
    certainty VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (certainty IN (
        'CONFIRMED', 'PRESUMPTIVE', 'RULED_OUT', 'WORKING'
    )),
    classification VARCHAR(20) DEFAULT 'CONSULTA' CHECK (classification IN (
        'CONSULTA', 'INGRESO', 'EGRESO', 'INTERCONSULTA', 'URGENCIA'
    )),
    supported_by TEXT,
    onset_date DATE,
    resolution_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RESOLVED', 'CHRONIC', 'IN_REMISSION')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_diagnoses_encounter ON diagnoses(encounter_id);
CREATE INDEX idx_diagnoses_patient ON diagnoses(patient_id);
CREATE INDEX idx_diagnoses_cie10 ON diagnoses(cie10_code);
CREATE INDEX idx_diagnoses_type_status ON diagnoses(diagnosis_type, status);

CREATE TRIGGER trg_diagnoses_updated BEFORE UPDATE ON diagnoses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 7. treatment_plans — Plan de tratamiento / conducta (FIX ERROR 3)
-- ============================================================
CREATE TABLE treatment_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    conduct VARCHAR(30) NOT NULL CHECK (conduct IN (
        'OBSERVATION', 'OUTPATIENT_TREATMENT', 'REFERRAL', 'HOSPITALIZATION', 'SURGERY', 'PALLIATIVE', 'REHABILITATION'
    )),
    therapeutic_goals TEXT[],
    followup_plan TEXT,
    reevaluation_criteria TEXT,
    prognosis VARCHAR(20) CHECK (prognosis IN ('EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'GUARDED', 'UNKNOWN')),
    estimated_duration INTERVAL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_treatment_plans_encounter ON treatment_plans(encounter_id);
CREATE INDEX idx_treatment_plans_patient ON treatment_plans(patient_id);

CREATE TRIGGER trg_treatment_plans_updated BEFORE UPDATE ON treatment_plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 8. medical_orders — Órdenes médicas (FIX ERROR 3)
-- ============================================================
CREATE TABLE medical_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    treatment_plan_id UUID REFERENCES treatment_plans(id) ON DELETE CASCADE,
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physician_id UUID NOT NULL REFERENCES users(id),
    order_type VARCHAR(30) NOT NULL CHECK (order_type IN (
        'MEDICATION', 'PROCEDURE', 'LAB_EXAM', 'IMAGING', 'DIET', 'NURSING_CARE', 'OTHER'
    )),
    drug_name VARCHAR(200),
    dose VARCHAR(100),
    dose_unit VARCHAR(50),
    route VARCHAR(50),
    frequency VARCHAR(100),
    duration_days SMALLINT,
    cups_code VARCHAR(20),
    cups_description VARCHAR(500),
    body_site VARCHAR(100),
    priority VARCHAR(20) DEFAULT 'ROUTINE' CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE', 'SCHEDULED')),
    status VARCHAR(30) DEFAULT 'ORDERED' CHECK (status IN (
        'ORDERED', 'IN_PROGRESS', 'EXECUTED', 'PARTIALLY_EXECUTED', 'SUSPENDED', 'CANCELLED', 'ON_HOLD'
    )),
    instructions TEXT,
    ordered_at TIMESTAMPTZ DEFAULT NOW(),
    executed_at TIMESTAMPTZ,
    executed_by UUID REFERENCES users(id),
    execution_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_medical_orders_plan ON medical_orders(treatment_plan_id);
CREATE INDEX idx_medical_orders_encounter ON medical_orders(encounter_id);
CREATE INDEX idx_medical_orders_patient ON medical_orders(patient_id);
CREATE INDEX idx_medical_orders_type_status ON medical_orders(order_type, status);
CREATE INDEX idx_medical_orders_physician ON medical_orders(physician_id);

CREATE TRIGGER trg_medical_orders_updated BEFORE UPDATE ON medical_orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 9. informed_consents — Consentimientos informados
-- ============================================================
CREATE TABLE informed_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    procedure_name VARCHAR(200) NOT NULL,
    procedure_cups_code VARCHAR(20),
    consent_type VARCHAR(30) NOT NULL CHECK (consent_type IN (
        'SURGICAL', 'INVASIVE', 'ANESTHESIA', 'TRANSFUSION', 'RESEARCH', 'TELEMEDICINE', 'OTHER'
    )),
    document_version VARCHAR(50) NOT NULL,
    document_storage_key VARCHAR(500),
    patient_signature_hash VARCHAR(500),
    witness1_name VARCHAR(200),
    witness1_document VARCHAR(50),
    witness1_signature_hash VARCHAR(500),
    witness2_name VARCHAR(200),
    witness2_document VARCHAR(50),
    witness2_signature_hash VARCHAR(500),
    physician_id UUID NOT NULL REFERENCES users(id),
    physician_signature_hash VARCHAR(500),
    signed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revocation_reason TEXT,
    status VARCHAR(20) DEFAULT 'VALID' CHECK (status IN ('VALID', 'EXPIRED', 'REVOKED', 'PENDING')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_informed_consents_patient ON informed_consents(patient_id);
CREATE INDEX idx_informed_consents_status ON informed_consents(status);
CREATE INDEX idx_informed_consents_type ON informed_consents(consent_type);

CREATE TRIGGER trg_informed_consents_updated BEFORE UPDATE ON informed_consents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 10. referrals — Referencias y contrarreferencias
-- ============================================================
CREATE TABLE referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referring_physician_id UUID NOT NULL REFERENCES users(id),
    referring_service VARCHAR(100),
    referred_to_service VARCHAR(100) NOT NULL,
    referred_to_institution VARCHAR(200),
    referred_to_physician_id UUID REFERENCES users(id),
    referral_type VARCHAR(30) NOT NULL CHECK (referral_type IN (
        'INTERCONSULTATION', 'COUNTERRREFERRAL', 'EMERGENCY', 'SECOND_OPINION', 'TRANSFER'
    )),
    priority VARCHAR(20) DEFAULT 'ROUTINE' CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE', 'SCHEDULED')),
    reason TEXT NOT NULL,
    clinical_summary TEXT,
    status VARCHAR(30) DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'ACCEPTED', 'REJECTED', 'MODIFIED', 'COMPLETED', 'CANCELLED'
    )),
    counterreferral_summary TEXT,
    counterreferral_recommendations TEXT,
    counterreferral_at TIMESTAMPTZ,
    counterreferral_by UUID REFERENCES users(id),
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_referrals_patient ON referrals(patient_id);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referrals_type ON referrals(referral_type);
CREATE INDEX idx_referrals_physician ON referrals(referring_physician_id);

CREATE TRIGGER trg_referrals_updated BEFORE UPDATE ON referrals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 11. discharge_summaries — Resúmenes de alta / Epicrisis
-- ============================================================
CREATE TABLE discharge_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    admission_id UUID,
    attending_physician_id UUID NOT NULL REFERENCES users(id),
    admission_date TIMESTAMPTZ NOT NULL,
    discharge_date TIMESTAMPTZ NOT NULL,
    length_of_stay INTERVAL GENERATED ALWAYS AS (discharge_date - admission_date) STORED,
    admission_diagnosis_cie10 VARCHAR(10),
    discharge_diagnosis_cie10 VARCHAR(10) NOT NULL,
    secondary_diagnoses_cie10 VARCHAR(10)[],
    clinical_summary TEXT NOT NULL,
    procedures_performed TEXT[],
    complications TEXT[],
    discharge_condition VARCHAR(30) CHECK (discharge_condition IN (
        'STABLE', 'IMPROVED', 'UNCHANGED', 'WORSENED', 'DECEASED', 'TRANSFERRED'
    )),
    discharge_disposition VARCHAR(50),
    discharge_medications JSONB DEFAULT '[]',
    followup_appointments JSONB DEFAULT '[]',
    alarm_signs TEXT[],
    general_recommendations TEXT,
    physician_signature_hash VARCHAR(500),
    signed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_discharge_summaries_patient ON discharge_summaries(patient_id);
CREATE INDEX idx_discharge_summaries_admission ON discharge_summaries(admission_id);
CREATE INDEX idx_discharge_summaries_dates ON discharge_summaries(admission_date, discharge_date);

CREATE TRIGGER trg_discharge_summaries_updated BEFORE UPDATE ON discharge_summaries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 12. clinical_attachments — Anexos (lab, imágenes, patología) (FIX ERROR 1 + 3)
-- ============================================================
CREATE TABLE clinical_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encounter_id UUID REFERENCES encounters(id) ON DELETE SET NULL,
    evolution_id UUID REFERENCES patient_evolutions(id) ON DELETE SET NULL,
    order_id UUID REFERENCES medical_orders(id) ON DELETE SET NULL,
    attachment_type VARCHAR(30) NOT NULL CHECK (attachment_type IN (
        'LAB_RESULT', 'IMAGING', 'PATHOLOGY', 'ENDOSCOPY', 'ELECTROCARDIOGRAM', 'OTHER'
    )),
    loinc_code VARCHAR(20),
    loinc_display VARCHAR(200),
    dicom_study_uid VARCHAR(100),
    dicom_series_uid VARCHAR(100),
    dicom_modality VARCHAR(20),
    pathology_code VARCHAR(50),
    result_value NUMERIC(15,4),
    result_unit VARCHAR(50),
    result_text TEXT,
    reference_range_low NUMERIC(15,4),
    reference_range_high NUMERIC(15,4),
    reference_range_text VARCHAR(200),
    abnormal_flag VARCHAR(10) CHECK (abnormal_flag IN ('NORMAL', 'HIGH', 'LOW', 'CRITICAL', 'ABNORMAL')),
    interpretation TEXT,
    document_id UUID REFERENCES clinical_documents(id) ON DELETE SET NULL,
    storage_key VARCHAR(500),
    mime_type VARCHAR(100),
    performed_at TIMESTAMPTZ NOT NULL,
    reported_at TIMESTAMPTZ,
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_clinical_attachments_patient ON clinical_attachments(patient_id);
CREATE INDEX idx_clinical_attachments_encounter ON clinical_attachments(encounter_id);
CREATE INDEX idx_clinical_attachments_type ON clinical_attachments(attachment_type);
CREATE INDEX idx_clinical_attachments_loinc ON clinical_attachments(loinc_code);
CREATE INDEX idx_clinical_attachments_dicom ON clinical_attachments(dicom_study_uid);
CREATE INDEX idx_clinical_attachments_performed ON clinical_attachments(performed_at);
-- FIX ERROR 1: GIN index en columna TEXT usando pg_trgm (no jsonb_path_ops)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_clinical_attachments_result_gin ON clinical_attachments USING GIN (result_text gin_trgm_ops);

CREATE TRIGGER trg_clinical_attachments_updated BEFORE UPDATE ON clinical_attachments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 13. obstetric_history — Historia obstétrica/perinatal
-- ============================================================
CREATE TABLE obstetric_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gravida SMALLINT DEFAULT 0,
    para SMALLINT DEFAULT 0,
    abortions SMALLINT DEFAULT 0,
    ectopic_pregnancies SMALLINT DEFAULT 0,
    stillbirths SMALLINT DEFAULT 0,
    living_children SMALLINT DEFAULT 0,
    current_pregnancy BOOLEAN DEFAULT FALSE,
    lmp DATE,
    estimated_edd DATE,
    gestational_weeks SMALLINT,
    prenatal_controls SMALLINT DEFAULT 0,
    previous_deliveries JSONB DEFAULT '[]',
    breastfeeding_status VARCHAR(30),
    breastfeeding_duration_months SMALLINT,
    obstetric_complications JSONB DEFAULT '[]',
    recorded_by UUID REFERENCES users(id),
    recorded_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_obstetric_history_patient ON obstetric_history(patient_id);

CREATE TRIGGER trg_obstetric_history_updated BEFORE UPDATE ON obstetric_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 14. surgical_history — Historia quirúrgica/anestésica
-- ============================================================
CREATE TABLE surgical_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    surgery_date DATE NOT NULL,
    procedure_cups_code VARCHAR(20) NOT NULL,
    procedure_cups_description VARCHAR(500),
    diagnosis_cie10 VARCHAR(10),
    diagnosis_description VARCHAR(500),
    surgery_type VARCHAR(30) CHECK (surgery_type IN ('ELECTIVE', 'URGENT', 'EMERGENCY', 'AMBULATORY')),
    anesthesia_type VARCHAR(30) CHECK (anesthesia_type IN (
        'GENERAL', 'REGIONAL_EPIDURAL', 'REGIONAL_SPINAL', 'REGIONAL_PLEXUS', 'LOCAL', 'SEDATION', 'NONE'
    )),
    anesthesiologist_id UUID REFERENCES users(id),
    asa_classification SMALLINT CHECK (asa_classification BETWEEN 1 AND 6),
    duration_minutes INTEGER,
    estimated_blood_loss_ml INTEGER,
    complications JSONB DEFAULT '[]',
    surgeon_id UUID REFERENCES users(id),
    assistant_surgeon_id UUID REFERENCES users(id),
    institution VARCHAR(200),
    notes TEXT,
    recorded_by UUID REFERENCES users(id),
    recorded_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_surgical_history_patient ON surgical_history(patient_id);
CREATE INDEX idx_surgical_history_date ON surgical_history(surgery_date);
CREATE INDEX idx_surgical_history_cups ON surgical_history(procedure_cups_code);

CREATE TRIGGER trg_surgical_history_updated BEFORE UPDATE ON surgical_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- NOTA MIGRACIÓN FUTURA (V76+):
-- Las tablas patient_profiles y patient_evolutions serán migradas a patient_history
-- y physical_exam en un release posterior, una vez que el código backend y frontend
-- estén listos para leer/escribir en las nuevas tablas.
-- La vista hce_complete_view leerá ambas fuentes mientras convivan.
-- ============================================================

-- ============================================================
-- 15. VISTA: hce_complete_view (FIX ERROR 6 - usa started_at)
-- ============================================================
CREATE OR REPLACE VIEW hce_complete_view AS
SELECT
    u.id AS patient_id,
    u.email,
    u.full_name,
    pi.document_type,
    pi.document_number,
    pi.rh_factor,
    pi.eps_name,
    e.id AS last_encounter_id,
    e.started_at AS last_encounter_date,
    e.chief_complaint AS last_motivo_consulta,
    d.cie10_code AS principal_diagnosis_cie10,
    d.cie10_description AS principal_diagnosis_desc,
    pe.recorded_at AS last_evolution_date,
    pe.vitals AS last_vitals,
    (SELECT count(*) FROM clinical_documents cd WHERE cd.patient_id = u.id AND cd.status = 'PENDING') AS pending_documents,
    (SELECT count(*) FROM medical_orders mo WHERE mo.patient_id = u.id AND mo.status IN ('ORDERED', 'IN_PROGRESS')) AS active_orders
FROM users u
LEFT JOIN patient_identification pi ON pi.user_id = u.id
LEFT JOIN encounters e ON e.patient_id = u.id
    AND e.started_at = (SELECT max(started_at) FROM encounters WHERE patient_id = u.id)
LEFT JOIN diagnoses d ON d.encounter_id = e.id AND d.diagnosis_type = 'PRINCIPAL' AND d.status = 'ACTIVE'
LEFT JOIN patient_evolutions pe ON pe.patient_id = u.id
    AND pe.recorded_at = (SELECT max(recorded_at) FROM patient_evolutions WHERE patient_id = u.id)
WHERE u.role = 'PATIENT' OR u.health_data_consent = TRUE;

-- ============================================================
-- NOTA FINAL PARA REVISIÓN DEL PO (NO EJECUTAR TODAVÍA)
-- ============================================================
-- 1. Opción C aplicada: V75 NO renombra ni crea vistas sobre
--    patient_profiles / patient_evolutions. Ambas siguen siendo
--    tablas normales y los adapters JPA actuales no se rompen.
-- 2. Migración futura (V76+): mover patient_profiles a patient_history
--    y patient_evolutions a physical_exam cuando backend y frontend
--    estén listos.
-- 3. gen_random_uuid() requiere PostgreSQL >= 13 (Neon 18: OK).
-- 4. pg_trgm debe estar permitido en la instancia (Neon: OK).
-- ============================================================

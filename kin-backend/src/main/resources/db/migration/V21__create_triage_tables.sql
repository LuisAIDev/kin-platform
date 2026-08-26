-- ============================================================
-- V21: health triage (Módulo de Triaje Digital, ADR-028)
-- Catálogo de síntomas, condiciones y relaciones con peso,
-- más el historial de consultas por paciente.
-- PostgreSQL (todos los entornos; Flyway V1..V21).
-- ============================================================

CREATE TABLE IF NOT EXISTS symptoms (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    icd_code    VARCHAR(20),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conditions (
    id             UUID PRIMARY KEY,
    name           VARCHAR(160) NOT NULL UNIQUE,
    description    VARCHAR(255),
    icd_code       VARCHAR(20),
    severity       VARCHAR(20) NOT NULL,
    urgency        VARCHAR(20) NOT NULL,
    recommendation VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_conditions_severity CHECK (severity IN ('LEVE', 'MODERADO', 'GRAVE')),
    CONSTRAINT chk_conditions_urgency  CHECK (urgency  IN ('BAJA', 'MEDIA', 'ALTA'))
);

CREATE TABLE IF NOT EXISTS symptom_condition_relations (
    symptom_id    UUID NOT NULL,
    condition_id  UUID NOT NULL,
    weight        NUMERIC(4,3) NOT NULL CHECK (weight > 0 AND weight <= 1),
    required      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (symptom_id, condition_id),
    CONSTRAINT fk_scr_symptom   FOREIGN KEY (symptom_id)   REFERENCES symptoms (id)   ON DELETE CASCADE,
    CONSTRAINT fk_scr_condition FOREIGN KEY (condition_id) REFERENCES conditions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS triage_consultations (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    symptoms   JSONB NOT NULL,
    results    JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_triage_consultations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_triage_consultations_user_id ON triage_consultations (user_id);
CREATE INDEX IF NOT EXISTS idx_scr_condition ON symptom_condition_relations (condition_id);

-- ============================================================
-- Seed inicial: 12 condiciones comunes + 20 síntomas asociados.
-- UUIDs deterministas (prefijo 22… para condiciones, 21… para síntomas).
-- ============================================================

INSERT INTO symptoms (id, name, description, icd_code) VALUES
    ('22220000-0000-0000-0000-000000000001', 'fiebre',            'Temperatura corporal elevada (≥ 38 °C)',          'R50.9'),
    ('22220000-0000-0000-0000-000000000002', 'tos',               'Tos seca o productiva',                            NULL),
    ('22220000-0000-0000-0000-000000000003', 'dolor de garganta', 'Dolor o irritación en la garganta',                'R07.0'),
    ('22220000-0000-0000-0000-000000000004', 'congestión nasal',  'Nariz congestionada o tapada',                     'R09.81'),
    ('22220000-0000-0000-0000-000000000005', 'estornudos',        'Estornudos frecuentes',                            NULL),
    ('22220000-0000-0000-0000-000000000006', 'dolor de cabeza',   'Cefalea o dolor de cabeza',                        'R51'),
    ('22220000-0000-0000-0000-000000000007', 'náuseas',           'Sensación de malestar estomacal con ganas de vomitar', 'R11.0'),
    ('22220000-0000-0000-0000-000000000008', 'vómitos',           'Expulsión del contenido del estómago',             'R11.10'),
    ('22220000-0000-0000-0000-000000000009', 'diarrea',           'Deposiciones líquidas o frecuentes',               'R19.7'),
    ('22220000-0000-0000-0000-000000000010', 'dolor abdominal',   'Dolor en la zona del abdomen',                     'R10.4'),
    ('22220000-0000-0000-0000-000000000011', 'ardor al orinar',   'Dolor o ardor al orinar',                          'R30.0'),
    ('22220000-0000-0000-0000-000000000012', 'micción frecuente', 'Necesidad de orinar con frecuencia inusual',       NULL),
    ('22220000-0000-0000-0000-000000000013', 'picazón en los ojos','Comezón ocular',                                  'H57.1'),
    ('22220000-0000-0000-0000-000000000014', 'lagrimeo',          'Producción excesiva de lágrimas',                  'H04.2'),
    ('22220000-0000-0000-0000-000000000015', 'dificultad para respirar', 'Sensación de falta de aire',               'R06.0'),
    ('22220000-0000-0000-0000-000000000016', 'sibilancias',       'Silbido al respirar',                              'R06.2'),
    ('22220000-0000-0000-0000-000000000017', 'presión en el pecho','Opresión o malestar torácico',                    'R07.89'),
    ('22220000-0000-0000-0000-000000000018', 'fatiga',            'Cansancio o debilidad general',                    'R53'),
    ('22220000-0000-0000-0000-000000000019', 'dolor muscular',    'Dolores musculares generalizados',                 'M79.1'),
    ('22220000-0000-0000-0000-000000000020', 'escalofríos',       'Sensación de frío con temblores',                  'R68.83')
ON CONFLICT (id) DO NOTHING;

INSERT INTO conditions (id, name, description, icd_code, severity, urgency, recommendation) VALUES
    ('22220000-0000-0000-0000-000000010001', 'Resfriado común',     'Infección viral leve de vías respiratorias altas', 'J00',  'LEVE',      'BAJA',  'Reposo e hidratación. Consultar si la fiebre supera los 3 días.'),
    ('22220000-0000-0000-0000-000000010002', 'Gripe',               'Infección viral respiratoria con fiebre y dolores', 'J11',  'MODERADO',  'MEDIA', 'Consulta médica en 24-48 h. Reposo e hidratación abundante.'),
    ('22220000-0000-0000-0000-000000010003', 'Alergia estacional',  'Reacción alérgica a alérgenos ambientales',        'J30',  'LEVE',      'BAJA',  'Antihistamínicos de venta libre. Consultar si no mejora en 72 h.'),
    ('22220000-0000-0000-0000-000000010004', 'Migraña',             'Cefalea recurrente con náuseas y sensibilidad a la luz', 'G43', 'MODERADO', 'MEDIA', 'Consulta médica. Evitar desencadenantes y descansar en sitio oscuro.'),
    ('22220000-0000-0000-0000-000000010005', 'Infección urinaria',  'Infección bacteriana del tracto urinario',         'N39.0', 'MODERADO', 'MEDIA', 'Consulta médica en 24 h para antibiótico. Beber abundante agua.'),
    ('22220000-0000-0000-0000-000000010006', 'Gastroenteritis',     'Inflamación del estómago e intestino',              'K52.9', 'MODERADO', 'MEDIA', 'Hidratación oral frecuente. Consulta urgente si hay signos de deshidratación.'),
    ('22220000-0000-0000-0000-000000010007', 'Amigdalitis',         'Inflamación de las amígdalas',                      'J03.9', 'MODERADO', 'MEDIA', 'Consulta médica en 24-48 h para evaluar tratamiento.'),
    ('22220000-0000-0000-0000-000000010008', 'Bronquitis',          'Inflamación de los bronquios con tos persistente',  'J20',   'MODERADO', 'MEDIA', 'Consulta médica. Reposo e hidratación.'),
    ('22220000-0000-0000-0000-000000010009', 'Conjuntivitis',       'Inflamación de la conjuntiva ocular',               'H10',   'LEVE',     'BAJA',  'Higiene ocular y consulta si hay secreción purulenta.'),
    ('22220000-0000-0000-0000-000000010010', 'Asma',                'Enfermedad respiratoria crónica con sibilancias',   'J45',   'GRAVE',    'ALTA',  'Consulta médica en 24 h. Si hay dificultad respiratoria grave, ir a urgencias.'),
    ('22220000-0000-0000-0000-000000010011', 'Neumonía',            'Infección pulmonar con fiebre alta y dificultad respiratoria', 'J18', 'GRAVE', 'ALTA', 'Acudir a urgencias. No automedicarse.'),
    ('22220000-0000-0000-0000-000000010012', 'Intoxicación alimentaria', 'Enfermedad por consumo de alimentos contaminados', 'A05', 'MODERADO', 'ALTA', 'Hidratación oral. Acudir a urgencias si hay deshidratación o fiebre alta.')
ON CONFLICT (id) DO NOTHING;

-- Relaciones síntoma ↔ condición con peso (relevancia) y bandera de obligatorio.
-- Convención: como mucho UN síntoma required por condición (el más patognomónico);
-- si un required está ausente, la condición no es candidata.
INSERT INTO symptom_condition_relations (symptom_id, condition_id, weight, required) VALUES
    -- Resfriado común
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010001', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010001', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010001', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000005', '22220000-0000-0000-0000-000000010001', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010001', 0.40, FALSE),
    -- Gripe
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010002', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010002', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010002', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010002', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000019', '22220000-0000-0000-0000-000000010002', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000020', '22220000-0000-0000-0000-000000010002', 0.60, FALSE),
    -- Alergia estacional
    ('22220000-0000-0000-0000-000000000005', '22220000-0000-0000-0000-000000010003', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000013', '22220000-0000-0000-0000-000000010003', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000014', '22220000-0000-0000-0000-000000010003', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010003', 0.60, FALSE),
    -- Migraña
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010004', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010004', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010004', 0.40, FALSE),
    -- Infección urinaria
    ('22220000-0000-0000-0000-000000000011', '22220000-0000-0000-0000-000000010005', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010005', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010005', 0.30, FALSE),
    -- Gastroenteritis
    ('22220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010006', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010006', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010006', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010006', 0.80, FALSE),
    -- Amigdalitis
    ('22220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010007', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010007', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010007', 0.30, FALSE),
    -- Bronquitis
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010008', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010008', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010008', 0.50, FALSE),
    -- Conjuntivitis
    ('22220000-0000-0000-0000-000000000013', '22220000-0000-0000-0000-000000010009', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000014', '22220000-0000-0000-0000-000000010009', 0.80, FALSE),
    -- Asma
    ('22220000-0000-0000-0000-000000000016', '22220000-0000-0000-0000-000000010010', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010010', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010010', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000017', '22220000-0000-0000-0000-000000010010', 0.60, FALSE),
    -- Neumonía
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010011', 0.90, FALSE),
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010011', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010011', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000017', '22220000-0000-0000-0000-000000010011', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000020', '22220000-0000-0000-0000-000000010011', 0.50, FALSE),
    -- Intoxicación alimentaria
    ('22220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010012', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010012', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010012', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010012', 0.40, FALSE)
ON CONFLICT (symptom_id, condition_id) DO NOTHING;

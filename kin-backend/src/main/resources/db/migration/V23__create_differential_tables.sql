-- ============================================================
-- V23: health differential (Diagnóstico Diferencial, ADR-029)
-- Factores de riesgo y pruebas complementarias por condición.
-- PostgreSQL (todos los entornos; Flyway V1..V23).
-- ============================================================

CREATE TABLE IF NOT EXISTS risk_factors (
    id           UUID PRIMARY KEY,
    condition_id UUID NOT NULL,
    factor       VARCHAR(120) NOT NULL,
    weight       NUMERIC(4,3) NOT NULL CHECK (weight > 0 AND weight <= 1),
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rf_condition FOREIGN KEY (condition_id) REFERENCES conditions (id) ON DELETE CASCADE,
    CONSTRAINT uk_rf_condition_factor UNIQUE (condition_id, factor)
);

CREATE TABLE IF NOT EXISTS recommended_tests (
    id           UUID PRIMARY KEY,
    condition_id UUID NOT NULL,
    test         VARCHAR(120) NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rt_condition FOREIGN KEY (condition_id) REFERENCES conditions (id) ON DELETE CASCADE,
    CONSTRAINT uk_rt_condition_test UNIQUE (condition_id, test)
);

CREATE INDEX IF NOT EXISTS idx_risk_factors_condition ON risk_factors (condition_id);
CREATE INDEX IF NOT EXISTS idx_recommended_tests_condition ON recommended_tests (condition_id);

-- ============================================================
-- Seed inicial: factores de riesgo y pruebas por condición.
-- UUIDs deterministas (prefijo 24… para risk_factors, 25… para tests).
-- ============================================================

INSERT INTO risk_factors (id, condition_id, factor, weight, description) VALUES
    -- Gripe
    ('24220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010002', 'edad avanzada',        0.30, 'Adultos mayores de 65 años'),
    ('24220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010002', 'embarazo',             0.25, 'Mujeres embarazadas'),
    ('24220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010002', 'inmunodepresión',      0.30, 'Pacientes inmunodeprimidos'),
    -- Asma
    ('24220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010010', 'fumador',              0.40, 'Tabaquismo activo'),
    ('24220000-0000-0000-0000-000000000005', '22220000-0000-0000-0000-000000010010', 'antecedente familiar', 0.20, 'Historia familiar de asma'),
    -- Neumonía
    ('24220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010011', 'edad avanzada',        0.30, 'Adultos mayores de 65 años'),
    ('24220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010011', 'fumador',              0.35, 'Tabaquismo activo'),
    ('24220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010011', 'inmunodepresión',      0.25, 'Pacientes inmunodeprimidos'),
    -- Infección urinaria
    ('24220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010005', 'embarazo',             0.25, 'Mujeres embarazadas'),
    -- Diabetes tipo 2
    ('24220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010041', 'obesidad',             0.35, 'Obesidad (IMC ≥ 30)'),
    ('24220000-0000-0000-0000-000000000011', '22220000-0000-0000-0000-000000010041', 'antecedente familiar', 0.25, 'Historia familiar de diabetes'),
    ('24220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010041', 'sedentarismo',         0.20, 'Vida sedentaria'),
    -- Hipertensión arterial
    ('24220000-0000-0000-0000-000000000013', '22220000-0000-0000-0000-000000010032', 'obesidad',             0.30, 'Obesidad (IMC ≥ 30)'),
    ('24220000-0000-0000-0000-000000000014', '22220000-0000-0000-0000-000000010032', 'antecedente familiar', 0.25, 'Historia familiar de hipertensión'),
    -- Angina de pecho
    ('24220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010033', 'fumador',              0.40, 'Tabaquismo activo'),
    ('24220000-0000-0000-0000-000000000016', '22220000-0000-0000-0000-000000010033', 'edad avanzada',        0.30, 'Adultos mayores de 65 años'),
    ('24220000-0000-0000-0000-000000000017', '22220000-0000-0000-0000-000000010033', 'hipertensión',         0.25, 'Hipertensión arterial previa'),
    -- Gota
    ('24220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010029', 'obesidad',             0.20, 'Obesidad (IMC ≥ 30)'),
    ('24220000-0000-0000-0000-000000000019', '22220000-0000-0000-0000-000000010029', 'alcohol',              0.30, 'Consumo excesivo de alcohol'),
    -- Osteoartritis / Artrosis
    ('24220000-0000-0000-0000-000000000020', '22220000-0000-0000-0000-000000010027', 'edad avanzada',        0.35, 'Adultos mayores de 50 años'),
    ('24220000-0000-0000-0000-000000000021', '22220000-0000-0000-0000-000000010027', 'obesidad',             0.30, 'Obesidad (IMC ≥ 30)'),
    -- Artritis reumatoide
    ('24220000-0000-0000-0000-000000000022', '22220000-0000-0000-0000-000000010028', 'sexo femenino',        0.25, 'Mayor prevalencia en mujeres'),
    ('24220000-0000-0000-0000-000000000023', '22220000-0000-0000-0000-000000010028', 'fumador',              0.25, 'Tabaquismo activo'),
    -- Bronquitis
    ('24220000-0000-0000-0000-000000000024', '22220000-0000-0000-0000-000000010008', 'fumador',              0.40, 'Tabaquismo activo'),
    -- Sinusitis
    ('24220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010013', 'alergia',              0.20, 'Antecedentes de alergia')
ON CONFLICT (condition_id, factor) DO NOTHING;

INSERT INTO recommended_tests (id, condition_id, test, description) VALUES
    -- Gripe
    ('25220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010002', 'Prueba rápida de antígeno', 'Detección rápida del virus de la influenza'),
    ('25220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010002', 'PCR respiratoria',        'Reacción en cadena de la polimerasa para influenza'),
    -- Asma
    ('25220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010010', 'Espirometría',            'Evaluación de la función pulmonar'),
    ('25220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010010', 'Pico flujo espiratorio',  'Medición del flujo máximo espiratorio'),
    -- Neumonía
    ('25220000-0000-0000-0000-000000000005', '22220000-0000-0000-0000-000000010011', 'Radiografía de tórax',    'Imagen del tórax para detectar infiltrados'),
    ('25220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010011', 'Hemograma completo',      'Recuento de glóbulos blancos'),
    ('25220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010011', 'Cultivo de esputo',       'Identificación del agente etiológico'),
    -- Infección urinaria
    ('25220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010005', 'Urocultivo',              'Cultivo de orina y antibiograma'),
    ('25220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010005', 'Examen de orina',         'Análisis básico de orina'),
    -- Diabetes tipo 2
    ('25220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010041', 'Glucosa en ayunas',       'Medición de glucosa en sangre en ayunas'),
    ('25220000-0000-0000-0000-000000000011', '22220000-0000-0000-0000-000000010041', 'Hemoglobina glicosilada', 'Promedio de glucosa de 3 meses (HbA1c)'),
    -- Hipertensión arterial
    ('25220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010032', 'Medición de presión arterial', 'Toma repetida de presión arterial'),
    ('25220000-0000-0000-0000-000000000013', '22220000-0000-0000-0000-000000010032', 'Perfil lipídico',         'Colesterol y triglicéridos'),
    -- Angina de pecho
    ('25220000-0000-0000-0000-000000000014', '22220000-0000-0000-0000-000000010033', 'Electrocardiograma',      'Registro de la actividad eléctrica del corazón'),
    ('25220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010033', 'Prueba de esfuerzo',      'ECG durante ejercicio controlado'),
    ('25220000-0000-0000-0000-000000000016', '22220000-0000-0000-0000-000000010033', 'Troponina',               'Marcador de daño miocárdico'),
    -- Gota
    ('25220000-0000-0000-0000-000000000017', '22220000-0000-0000-0000-000000010029', 'Ácido úrico sérico',      'Medición de ácido úrico en sangre'),
    -- Artrosis
    ('25220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010027', 'Radiografía articular',   'Imagen de la articulación afectada'),
    -- Artritis reumatoide
    ('25220000-0000-0000-0000-000000000019', '22220000-0000-0000-0000-000000010028', 'Factor reumatoide',       'Anticuerpo asociado a artritis reumatoide'),
    ('25220000-0000-0000-0000-000000000020', '22220000-0000-0000-0000-000000010028', 'Anti-CCP',                'Anticuerpo anti-proteína citrulinada'),
    -- Bronquitis
    ('25220000-0000-0000-0000-000000000021', '22220000-0000-0000-0000-000000010008', 'Espirometría',            'Evaluación de la función pulmonar'),
    -- Sinusitis
    ('25220000-0000-0000-0000-000000000022', '22220000-0000-0000-0000-000000010013', 'Radiografía de senos',    'Imagen de los senos paranasales')
ON CONFLICT (condition_id, test) DO NOTHING;

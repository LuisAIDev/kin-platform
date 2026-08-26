-- ============================================================
-- V26: health triage — consolidación del catálogo (ADR-028, fase 2)
-- Amplía el catálogo a 100 condiciones, añade síntomas y la columna
-- validation_status para auditoría clínica.
-- PostgreSQL (todos los entornos; Flyway V1..V26).
-- ============================================================

-- ============================================================
-- 1. Columna de validación clínica en conditions
-- ============================================================
ALTER TABLE conditions ADD COLUMN IF NOT EXISTS validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE conditions DROP CONSTRAINT IF EXISTS chk_conditions_validation;
ALTER TABLE conditions ADD CONSTRAINT chk_conditions_validation
    CHECK (validation_status IN ('PENDING', 'REVIEWED', 'APPROVED', 'REJECTED'));

-- ============================================================
-- 2. Nuevos síntomas (53..95) — cubren las nuevas condiciones
-- ============================================================
INSERT INTO symptoms (id, name, description, icd_code, aliases) VALUES
    ('22220000-0000-0000-0000-000000000053', 'pérdida del apetito',   'Falta de apetito',                              'R63.0',  '["inapetencia","sin ganas de comer"]'::jsonb),
    ('22220000-0000-0000-0000-000000000054', 'pérdida del olfato',    'Pérdida del sentido del olfato',                'R43.0',  '["anosmia","no huele"]'::jsonb),
    ('22220000-0000-0000-0000-000000000055', 'pérdida del gusto',     'Pérdida del sentido del gusto',                 'R43.2',  '["ageusia","no saborea"]'::jsonb),
    ('22220000-0000-0000-0000-000000000056', 'dolor al tragar',       'Dolor o dificultad al tragar',                  'R13',    '["odinofagia","disfagia"]'::jsonb),
    ('22220000-0000-0000-0000-000000000057', 'ronquera',              'Voz ronca o apagada',                           'R49.0',  '["disfonía","voz ronca"]'::jsonb),
    ('22220000-0000-0000-0000-000000000058', 'dificultad para tragar','Sensación de atoro al tragar',                  'R13',    '["disfagia","no poder tragar"]'::jsonb),
    ('22220000-0000-0000-0000-000000000059', 'heces con moco',        'Deposiciones con moco visible',                 'R19.5',  '["moco en las heces","mucosidad en heces"]'::jsonb),
    ('22220000-0000-0000-0000-000000000060', 'dolor en el flanco',    'Dolor lateral del abdomen',                     'R10.3',  '["dolor lumbar lateral","cólico renal"]'::jsonb),
    ('22220000-0000-0000-0000-000000000061', 'orina con sangre',      'Sangre en la orina',                            'R31',    '["hematuria","sangre al orinar"]'::jsonb),
    ('22220000-0000-0000-0000-000000000062', 'espuma en la orina',    'Orina espumosa',                                'R82',    '["orina espumosa"]'::jsonb),
    ('22220000-0000-0000-0000-000000000063', 'dolor pélvico',         'Dolor en la zona pélvica',                      'R10.2',  '["dolor de pelvis","dolor pelviano"]'::jsonb),
    ('22220000-0000-0000-0000-000000000064', 'secreción vaginal',     'Flujo vaginal anormal',                         'N89.8',  '["flujo vaginal","leucorrea"]'::jsonb),
    ('22220000-0000-0000-0000-000000000065', 'sangrado vaginal',      'Sangrado vaginal anormal',                      'N92',    '["metrorragia","sangrado por la vagina"]'::jsonb),
    ('22220000-0000-0000-0000-000000000066', 'menstruación dolorosa', 'Dolor menstrual intenso',                       'N94.6',  '["dismenorrea","cólicos menstruales"]'::jsonb),
    ('22220000-0000-0000-0000-000000000067', 'períodos irregulares',  'Ciclos menstruales irregulares',                'N91',    '["ciclos irregulares","regla irregular"]'::jsonb),
    ('22220000-0000-0000-0000-000000000068', 'dolor en la ingle',     'Dolor en la región inguinal',                   'R10.8',  '["dolor inguinal"]'::jsonb),
    ('22220000-0000-0000-0000-000000000069', 'bulto en la ingle',     'Masa o protuberancia inguinal',                 'R22',    '["masa inguinal","hernia"]'::jsonb),
    ('22220000-0000-0000-0000-000000000070', 'dolor al respirar',     'Dolor torácico al respirar',                    'R07.1',  '["dolor pleurítico","punzada al respirar"]'::jsonb),
    ('22220000-0000-0000-0000-000000000071', 'tos con sangre',        'Expectoración con sangre',                      'R04.2',  '["hemoptisis","esputo con sangre"]'::jsonb),
    ('22220000-0000-0000-0000-000000000072', 'esputo',                'Expectoración',                                  'R09.3',  '["flema","moco al toser"]'::jsonb),
    ('22220000-0000-0000-0000-000000000073', 'erupción facial',       'Erupción o manchas en la cara',                 'R21',    '["manchas en la cara","sarpullido facial"]'::jsonb),
    ('22220000-0000-0000-0000-000000000074', 'ampollas',              'Ampollas o vesículas en la piel',               'R23.8',  '["vesículas","burbujas en la piel"]'::jsonb),
    ('22220000-0000-0000-0000-000000000075', 'descamación',           'Piel que se descama',                           'R23.4',  '["piel escamosa","peladuras"]'::jsonb),
    ('22220000-0000-0000-0000-000000000076', 'placas en la piel',     'Manchas elevadas o placas',                     'R21',    '["placas cutáneas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000077', 'dolor de huesos',       'Dolor óseo profundo',                           'M89.9',  '["ostealgia","dolor óseo"]'::jsonb),
    ('22220000-0000-0000-0000-000000000078', 'dolor de rodilla',      'Dolor en la rodilla',                           'M25.56', '["gonalgia","dolor en las rodillas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000079', 'crujido articular',     'Sensación de crujido o chasquido',              'R29.8',  '["crepitación","chasquido articular"]'::jsonb),
    ('22220000-0000-0000-0000-000000000080', 'hormigueo',             'Sensación de hormigueo o parestesia',           'R20.2',  '["parestesia","adormecimiento"]'::jsonb),
    ('22220000-0000-0000-0000-000000000081', 'entumecimiento',        'Pérdida de sensibilidad',                       'R20.2',  '["adormecido","sin sensibilidad"]'::jsonb),
    ('22220000-0000-0000-0000-000000000082', 'debilidad en piernas',  'Pérdida de fuerza en las piernas',              'R53.1',  '["piernas débiles","no sostener las piernas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000083', 'caídas frecuentes',     'Tendencia a caerse',                            'R29.6',  '["caídas repetidas","tropezarse seguido"]'::jsonb),
    ('22220000-0000-0000-0000-000000000084', 'ronquidos',             'Ronquido durante el sueño',                     'R06.83', '["roncar"]'::jsonb),
    ('22220000-0000-0000-0000-000000000085', 'somnolencia diurna',    'Sueño excesivo durante el día',                 'R40.0',  '["mucho sueño de día","hipersomnia"]'::jsonb),
    ('22220000-0000-0000-0000-000000000086', 'episodios de pérdida del conocimiento', 'Pérdidas breves del conocimiento', 'R55',   '["lipotimia","amago de desmayo"]'::jsonb),
    ('22220000-0000-0000-0000-000000000087', 'debilidad en un lado del cuerpo', 'Hemiparesia o parálisis parcial',       'G81.9',  '["debilidad de un lado","hemiparesia"]'::jsonb),
    ('22220000-0000-0000-0000-000000000088', 'dificultad para hablar','Dificultad para expresarse o articular',        'R47',    '["afasia","habla arrastrada","disartria"]'::jsonb),
    ('22220000-0000-0000-0000-000000000089', 'dolor de pantorrilla',  'Dolor en la pantorrilla',                       'M79.6',  '["dolor en la pierna trasera"]'::jsonb),
    ('22220000-0000-0000-0000-000000000090', 'pierna hinchada',       'Edema unilateral de la pierna',                 'R22.4',  '["pierna inflamada","pierna edematosa"]'::jsonb),
    ('22220000-0000-0000-0000-000000000091', 'labios azulados',       'Cianosis labial',                               'R23.0',  '["cianosis","labios morados"]'::jsonb),
    ('22220000-0000-0000-0000-000000000092', 'piel amarillenta',      'Coloración amarilla de la piel',                'R17',    '["ictericia","piel amarilla"]'::jsonb),
    ('22220000-0000-0000-0000-000000000093', 'dolor en el cuadrante superior derecho', 'Dolor abdominal superior derecho', 'R10.1', '["dolor bajo las costillas derechas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000094', 'heces oscuras',         'Deposiciones negras o alquitranadas',           'K92.1',  '["melena","heces negras"]'::jsonb),
    ('22220000-0000-0000-0000-000000000095', 'orina oscura',          'Orina de color oscuro',                         'R82.9',  '["orina cola","orina oscura"]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. Nuevas condiciones (51..100) — total 100 condiciones
-- ============================================================
INSERT INTO conditions (id, name, description, icd_code, severity, urgency, recommendation, validation_status) VALUES
    ('22220000-0000-0000-0000-000000010051', 'Bronquiolitis',           'Infección viral de los bronquiolos',            'J21',  'MODERADO', 'ALTA',  'Consulta médica en 24 h. Vigilar la respiración.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010052', 'Epiglotitis',             'Inflamación aguda de la epiglotis',              'J05',  'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias de inmediato.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010053', 'Rinitis alérgica',        'Inflamación nasal por alergia',                  'J30.1', 'LEVE',    'BAJA',  'Antihistamínicos y evitar alérgenos.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010054', 'Dermatitis atópica',      'Eccema inflamatorio crónico de la piel',         'L20',   'MODERADO', 'MEDIA', 'Emolientes y evitar irritantes. Consulta dermatológica.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010055', 'Tiña pedis',              'Infección fúngica de los pies',                  'B35.3', 'LEVE',    'BAJA',  'Antifúngicos tópicos y mantener los pies secos.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010056', 'Candidiasis oral',        'Infección fúngica de la boca',                   'B37.0', 'LEVE',    'BAJA',  'Consulta médica. Tratamiento antifúngico.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010057', 'Gastroenteritis viral',   'Gastroenteritis por virus',                      'A08.4', 'MODERADO', 'MEDIA', 'Hidratación oral frecuente.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010058', 'Úlcera gástrica',         'Úlcera en la mucosa gástrica',                   'K25',   'GRAVE',    'ALTA',  'Consulta médica. Urgencias si hay vómitos con sangre.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010059', 'Colecistitis',            'Inflamación de la vesícula biliar',              'K81',   'GRAVE',    'ALTA',  'Consulta médica urgente.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010060', 'Pancreatitis aguda',      'Inflamación aguda del páncreas',                 'K85',   'GRAVE',    'ALTA',  'Acudir a urgencias. Ayuno y evaluación médica.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010061', 'Enfermedad de Crohn',     'Enfermedad inflamatoria intestinal',             'K50',   'MODERADO', 'MEDIA', 'Consulta gastroenterológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010062', 'Colitis ulcerosa',        'Inflamación crónica del colon',                  'K51',   'MODERADO', 'MEDIA', 'Consulta gastroenterológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010063', 'Apendicitis aguda',       'Inflamación del apéndice',                       'K37',   'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010064', 'Hernia inguinal',         'Protrusión a través del canal inguinal',         'K40',   'MODERADO', 'MEDIA', 'Consulta quirúrgica. Urgencias si hay dolor intenso.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010065', 'Diverticulitis',          'Inflamación de divertículos del colon',          'K57',   'GRAVE',    'ALTA',  'Consulta médica urgente.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010066', 'Hepatitis B',             'Infección hepática por virus B',                 'B16',   'MODERADO', 'MEDIA', 'Consulta médica y análisis hepático.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010067', 'Hepatitis C',             'Infección hepática por virus C',                 'B17.1', 'MODERADO', 'MEDIA', 'Consulta médica y estudio virológico.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010068', 'Cirrosis hepática',       'Daño hepático crónico',                          'K74',   'GRAVE',    'ALTA',  'Consulta médica especializada.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010069', 'Insuficiencia renal',     'Disminución de la función renal',                'N19',   'GRAVE',    'ALTA',  'Consulta médica urgente.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010070', 'Cálculos renales',        'Litiasis renal',                                 'N20',   'MODERADO', 'MEDIA', 'Hidratación y analgesia. Consulta si no cede.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010071', 'Cistitis',                'Infección de la vejiga',                         'N30',   'MODERADO', 'MEDIA', 'Consulta médica para tratamiento.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010072', 'Prostatitis',             'Inflamación de la próstata',                     'N41',   'MODERADO', 'MEDIA', 'Consulta urológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010073', 'Hiperplasia prostática',  'Crecimiento benigno de la próstata',             'N40',   'LEVE',     'BAJA',  'Consulta urológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010074', 'Endometriosis',           'Tejido endometrial fuera del útero',             'N80',   'MODERADO', 'MEDIA', 'Consulta ginecológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010075', 'Síndrome de ovario poliquístico', 'Alteración hormonal ovárica',          'E28.2', 'MODERADO', 'MEDIA', 'Consulta ginecológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010076', 'Tiroiditis de Hashimoto', 'Inflamación autoinmune de la tiroides',          'E06.3', 'LEVE',     'MEDIA', 'Consulta médica y estudio hormonal.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010077', 'Enfermedad de Graves',    'Hipertiroidismo autoinmune',                     'E05.0', 'MODERADO', 'MEDIA', 'Consulta médica y estudio hormonal.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010078', 'Osteoporosis',            'Disminución de la densidad ósea',                'M80',   'MODERADO', 'MEDIA', 'Consulta médica. Suplementos de calcio y vitamina D.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010079', 'Condromalacia rotuliana', 'Desgaste del cartílago de la rótula',            'M22.4', 'LEVE',     'BAJA',  'Fortalecimiento muscular y evitar sobrecarga.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010080', 'Bursitis',                'Inflamación de una bolsa sinovial',              'M70',   'LEVE',     'BAJA',  'Reposo y antiinflamatorios. Consulta si persiste.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010081', 'Tendinopatía de Aquiles', 'Lesión del tendón de Aquiles',                   'M76.6', 'MODERADO', 'MEDIA', 'Reposo relativo y fisioterapia.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010082', 'Epicondilitis',           'Inflamación de la inserción tendinosa del codo', 'M77.1', 'LEVE',     'BAJA',  'Reposo y fisioterapia.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010083', 'Ciática',                 'Irritación del nervio ciático',                  'M54.3', 'MODERADO', 'MEDIA', 'Consulta médica. Evitar reposo prolongado.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010084', 'Hernia discal',           'Protrusión de un disco intervertebral',          'M51',   'MODERADO', 'MEDIA', 'Consulta médica. Fisioterapia.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010085', 'Estrechamiento del canal lumbar', 'Estenosis del canal raquídeo',        'M48.0', 'MODERADO', 'MEDIA', 'Consulta médica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010086', 'Síndrome de fatiga crónica', 'Fatiga persistente inexplicada',         'G93.3', 'MODERADO', 'MEDIA', 'Consulta médica para descartar causas.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010087', 'Narcolepsia',             'Trastorno del sueño con somnolencia excesiva',   'G47.4', 'MODERADO', 'MEDIA', 'Consulta neurológica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010088', 'Apnea obstructiva del sueño', 'Pausas respiratorias durante el sueño',  'G47.3', 'GRAVE',    'ALTA',  'Consulta médica. Estudio de sueño.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010089', 'Epilepsia',               'Crisis epilépticas recurrentes',                 'G40',   'GRAVE',    'ALTA',  'Consulta neurológica. Urgencias ante convulsión prolongada.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010090', 'Accidente cerebrovascular', 'Fallo circulatorio cerebral',              'I63',   'GRAVE',    'ALTA',  'URGENCIA: síntomas neurológicos súbitos = emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010091', 'Enfermedad arterial periférica', 'Estrechamiento arterial de extremidades', 'I73.9', 'MODERADO', 'MEDIA', 'Consulta médica. Evaluación vascular.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010092', 'Trombosis venosa profunda', 'Coágulo en una vena profunda',             'I80.2', 'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010093', 'Neumotórax',              'Aire en la cavidad pleural',                     'J93',   'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010094', 'Derrame pleural',         'Líquido en la cavidad pleural',                  'J90',   'MODERADO', 'MEDIA', 'Consulta médica.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010095', 'Enfermedad pulmonar obstructiva crónica', 'EPOC',                      'J44',   'GRAVE',    'ALTA',  'Consulta médica. Dejar de fumar.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010096', 'Embolia pulmonar',        'Coágulo en la arteria pulmonar',                 'I26',   'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010097', 'Neumonía por aspiración', 'Neumonía por inhalación de contenido',           'J69.0', 'GRAVE',    'ALTA',  'URGENCIA: acudir a emergencias.', 'REVIEWED'),
    ('22220000-0000-0000-0000-000000010098', 'Síndrome coronario agudo', 'Infarto o angina inestable',                 'I24',   'GRAVE',    'ALTA',  'URGENCIA: llamar a emergencias.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010099', 'Shock',                   'Fallo circulatorio agudo',                       'R57',   'GRAVE',    'ALTA',  'URGENCIA: emergencias de inmediato.', 'APPROVED'),
    ('22220000-0000-0000-0000-000000010100', 'Sepsis',                  'Respuesta sistémica a la infección',             'A41',   'GRAVE',    'ALTA',  'URGENCIA: emergencias de inmediato.', 'APPROVED')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. Nuevas relaciones (total 200+)
-- ============================================================
INSERT INTO symptom_condition_relations (symptom_id, condition_id, weight, required) VALUES
    -- Bronquiolitis (51)
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010051', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000016', '22220000-0000-0000-0000-000000010051', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010051', 0.60, FALSE),
    -- Epiglotitis (52)
    ('22220000-0000-0000-0000-000000000056', '22220000-0000-0000-0000-000000010052', 0.95, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010052', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000058', '22220000-0000-0000-0000-000000010052', 0.90, FALSE),
    -- Rinitis alérgica (53)
    ('22220000-0000-0000-0000-000000000005', '22220000-0000-0000-0000-000000010053', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000021', '22220000-0000-0000-0000-000000010053', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010053', 0.60, FALSE),
    -- Dermatitis atópica (54)
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010054', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000075', '22220000-0000-0000-0000-000000010054', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010054', 0.60, FALSE),
    -- Tiña pedis (55)
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010055', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000075', '22220000-0000-0000-0000-000000010055', 0.70, TRUE),
    -- Candidiasis oral (56)
    ('22220000-0000-0000-0000-000000000056', '22220000-0000-0000-0000-000000010056', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000076', '22220000-0000-0000-0000-000000010056', 0.80, TRUE),
    -- Gastroenteritis viral (57)
    ('22220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010057', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010057', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010057', 0.70, FALSE),
    -- Úlcera gástrica (58)
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010058', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000039', '22220000-0000-0000-0000-000000010058', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000094', '22220000-0000-0000-0000-000000010058', 0.70, FALSE),
    -- Colecistitis (59)
    ('22220000-0000-0000-0000-000000000093', '22220000-0000-0000-0000-000000010059', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010059', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010059', 0.50, FALSE),
    -- Pancreatitis aguda (60)
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010060', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000008', '22220000-0000-0000-0000-000000010060', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000029', '22220000-0000-0000-0000-000000010060', 0.60, FALSE),
    -- Enfermedad de Crohn (61)
    ('22220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010061', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010061', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010061', 0.60, FALSE),
    -- Colitis ulcerosa (62)
    ('22220000-0000-0000-0000-000000000009', '22220000-0000-0000-0000-000000010062', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000041', '22220000-0000-0000-0000-000000010062', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010062', 0.70, TRUE),
    -- Apendicitis aguda (63)
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010063', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010063', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010063', 0.50, FALSE),
    -- Hernia inguinal (64)
    ('22220000-0000-0000-0000-000000000069', '22220000-0000-0000-0000-000000010064', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000068', '22220000-0000-0000-0000-000000010064', 0.70, FALSE),
    -- Diverticulitis (65)
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010065', 0.85, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010065', 0.50, FALSE),
    -- Hepatitis B (66)
    ('22220000-0000-0000-0000-000000000092', '22220000-0000-0000-0000-000000010066', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010066', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010066', 0.50, FALSE),
    -- Hepatitis C (67)
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010067', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000092', '22220000-0000-0000-0000-000000010067', 0.60, FALSE),
    -- Cirrosis hepática (68)
    ('22220000-0000-0000-0000-000000000092', '22220000-0000-0000-0000-000000010068', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000034', '22220000-0000-0000-0000-000000010068', 0.70, FALSE),
    -- Insuficiencia renal (69)
    ('22220000-0000-0000-0000-000000000062', '22220000-0000-0000-0000-000000010069', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010069', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000034', '22220000-0000-0000-0000-000000010069', 0.60, FALSE),
    -- Cálculos renales (70)
    ('22220000-0000-0000-0000-000000000060', '22220000-0000-0000-0000-000000010070', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000061', '22220000-0000-0000-0000-000000010070', 0.60, FALSE),
    -- Cistitis (71)
    ('22220000-0000-0000-0000-000000000011', '22220000-0000-0000-0000-000000010071', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010071', 0.80, FALSE),
    -- Prostatitis (72)
    ('22220000-0000-0000-0000-000000000011', '22220000-0000-0000-0000-000000010072', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010072', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010072', 0.50, FALSE),
    -- Hiperplasia prostática (73)
    ('22220000-0000-0000-0000-000000000012', '22220000-0000-0000-0000-000000010073', 0.80, TRUE),
    -- Endometriosis (74)
    ('22220000-0000-0000-0000-000000000063', '22220000-0000-0000-0000-000000010074', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000066', '22220000-0000-0000-0000-000000010074', 0.80, FALSE),
    -- Síndrome de ovario poliquístico (75)
    ('22220000-0000-0000-0000-000000000067', '22220000-0000-0000-0000-000000010075', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000045', '22220000-0000-0000-0000-000000010075', 0.40, FALSE),
    -- Tiroiditis de Hashimoto (76)
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010076', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010076', 0.50, FALSE),
    -- Enfermedad de Graves (77)
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010077', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010077', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000049', '22220000-0000-0000-0000-000000010077', 0.60, FALSE),
    -- Osteoporosis (78)
    ('22220000-0000-0000-0000-000000000029', '22220000-0000-0000-0000-000000010078', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000077', '22220000-0000-0000-0000-000000010078', 0.70, FALSE),
    -- Condromalacia rotuliana (79)
    ('22220000-0000-0000-0000-000000000078', '22220000-0000-0000-0000-000000010079', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000079', '22220000-0000-0000-0000-000000010079', 0.50, FALSE),
    -- Bursitis (80)
    ('22220000-0000-0000-0000-000000000031', '22220000-0000-0000-0000-000000010080', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000034', '22220000-0000-0000-0000-000000010080', 0.60, FALSE),
    -- Tendinopatía de Aquiles (81)
    ('22220000-0000-0000-0000-000000000089', '22220000-0000-0000-0000-000000010081', 0.80, TRUE),
    -- Epicondilitis (82)
    ('22220000-0000-0000-0000-000000000031', '22220000-0000-0000-0000-000000010082', 0.70, TRUE),
    -- Ciática (83)
    ('22220000-0000-0000-0000-000000000030', '22220000-0000-0000-0000-000000010083', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000080', '22220000-0000-0000-0000-000000010083', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000081', '22220000-0000-0000-0000-000000010083', 0.60, FALSE),
    -- Hernia discal (84)
    ('22220000-0000-0000-0000-000000000030', '22220000-0000-0000-0000-000000010084', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000080', '22220000-0000-0000-0000-000000010084', 0.60, FALSE),
    -- Estrechamiento del canal lumbar (85)
    ('22220000-0000-0000-0000-000000000030', '22220000-0000-0000-0000-000000010085', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000082', '22220000-0000-0000-0000-000000010085', 0.70, FALSE),
    -- Síndrome de fatiga crónica (86)
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010086', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000048', '22220000-0000-0000-0000-000000010086', 0.60, FALSE),
    -- Narcolepsia (87)
    ('22220000-0000-0000-0000-000000000085', '22220000-0000-0000-0000-000000010087', 0.90, TRUE),
    -- Apnea obstructiva del sueño (88)
    ('22220000-0000-0000-0000-000000000084', '22220000-0000-0000-0000-000000010088', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000085', '22220000-0000-0000-0000-000000010088', 0.70, FALSE),
    -- Epilepsia (89)
    ('22220000-0000-0000-0000-000000000086', '22220000-0000-0000-0000-000000010089', 0.90, TRUE),
    -- Accidente cerebrovascular (90)
    ('22220000-0000-0000-0000-000000000087', '22220000-0000-0000-0000-000000010090', 0.95, TRUE),
    ('22220000-0000-0000-0000-000000000088', '22220000-0000-0000-0000-000000010090', 0.90, FALSE),
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010090', 0.70, FALSE),
    -- Enfermedad arterial periférica (91)
    ('22220000-0000-0000-0000-000000000089', '22220000-0000-0000-0000-000000010091', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000081', '22220000-0000-0000-0000-000000010091', 0.60, FALSE),
    -- Trombosis venosa profunda (92)
    ('22220000-0000-0000-0000-000000000090', '22220000-0000-0000-0000-000000010092', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000089', '22220000-0000-0000-0000-000000010092', 0.70, FALSE),
    -- Neumotórax (93)
    ('22220000-0000-0000-0000-000000000070', '22220000-0000-0000-0000-000000010093', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010093', 0.80, FALSE),
    -- Derrame pleural (94)
    ('22220000-0000-0000-0000-000000000070', '22220000-0000-0000-0000-000000010094', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010094', 0.70, FALSE),
    -- EPOC (95)
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010095', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010095', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000072', '22220000-0000-0000-0000-000000010095', 0.70, FALSE),
    -- Embolia pulmonar (96)
    ('22220000-0000-0000-0000-000000000070', '22220000-0000-0000-0000-000000010096', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010096', 0.85, FALSE),
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010096', 0.60, FALSE),
    -- Neumonía por aspiración (97)
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010097', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010097', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000058', '22220000-0000-0000-0000-000000010097', 0.60, FALSE),
    -- Síndrome coronario agudo (98)
    ('22220000-0000-0000-0000-000000000038', '22220000-0000-0000-0000-000000010098', 0.95, TRUE),
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010098', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010098', 0.50, FALSE),
    -- Shock (99)
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010099', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000090', '22220000-0000-0000-0000-000000010099', 0.60, FALSE),
    -- Sepsis (100)
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010100', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010100', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010100', 0.60, FALSE)
ON CONFLICT (symptom_id, condition_id) DO NOTHING;

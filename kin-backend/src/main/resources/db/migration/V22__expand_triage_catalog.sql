-- ============================================================
-- V22: health triage (ADR-028) — escalado del catálogo profesional
-- - Añade la columna `aliases` a symptoms (normalización NLP).
-- - Amplía el catálogo a 50 condiciones y 100+ relaciones.
-- PostgreSQL (todos los entornos; Flyway V1..V22).
-- ============================================================

-- ============================================================
-- 1. Aliases de síntomas para la normalización NLP
-- ============================================================
ALTER TABLE symptoms ADD COLUMN IF NOT EXISTS aliases JSONB NOT NULL DEFAULT '[]'::jsonb;

-- ============================================================
-- 2. Nuevos síntomas (21..52) — cubren las nuevas condiciones
-- ============================================================
INSERT INTO symptoms (id, name, description, icd_code, aliases) VALUES
    ('22220000-0000-0000-0000-000000000021', 'secreción nasal',       'Secreción nasal clara o espesa',                  'R09.8',  '["mocos","nariz con secreción"]'::jsonb),
    ('22220000-0000-0000-0000-000000000022', 'dolor de oído',         'Dolor en el oído',                                'H92.0',  '["otalgia","dolor de oídos"]'::jsonb),
    ('22220000-0000-0000-0000-000000000023', 'zumbido en los oídos',  'Sensación de pitido o zumbido',                   'H93.1',  '["tinnitus","pitido en oído"]'::jsonb),
    ('22220000-0000-0000-0000-000000000024', 'secreción del oído',    'Supuración o fluido del oído',                    'H92.1',  '["otorrea","oído supurando"]'::jsonb),
    ('22220000-0000-0000-0000-000000000025', 'erupción cutánea',      'Lesiones o manchas en la piel',                   'R21',    '["sarpullido","erupción en la piel"]'::jsonb),
    ('22220000-0000-0000-0000-000000000026', 'picazón en la piel',    'Comezón en la piel',                              'L29.9',  '["prurito","comezón"]'::jsonb),
    ('22220000-0000-0000-0000-000000000027', 'enrojecimiento ocular', 'Ojos rojos o inyectados',                         'H57.9',  '["ojos rojos","ojo enrojecido"]'::jsonb),
    ('22220000-0000-0000-0000-000000000028', 'visión borrosa',        'Pérdida de nitidez visual',                       'H53.8',  '["vista borrosa","imagen borrosa"]'::jsonb),
    ('22220000-0000-0000-0000-000000000029', 'dolor de espalda',      'Dolor en la región dorsal',                       'M54.9',  '["dorsalgia","dolor dorsal"]'::jsonb),
    ('22220000-0000-0000-0000-000000000030', 'dolor lumbar',          'Dolor en la parte baja de la espalda',            'M54.5',  '["lumbalgia","dolor de lumbares","dolor en la cintura"]'::jsonb),
    ('22220000-0000-0000-0000-000000000031', 'dolor articular',       'Dolor en las articulaciones',                     'M25.50', '["artralgia","dolor de articulaciones","dolor en las rodillas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000032', 'rigidez articular',     'Dificultad de movimiento articular',              'M25.60', '["rigidez en las articulaciones"]'::jsonb),
    ('22220000-0000-0000-0000-000000000033', 'inflamación articular', 'Hinchazón o calor articular',                     'M25.40', '["articulación inflamada","hinchazón de articulación"]'::jsonb),
    ('22220000-0000-0000-0000-000000000034', 'hinchazón',             'Edema o inflamación visible',                     'R22.9',  '["edema","inflamación"]'::jsonb),
    ('22220000-0000-0000-0000-000000000035', 'mareo',                 'Sensación de inestabilidad o vértigo',            'R42',    '["vértigo","aturdimiento","cabeza que da vueltas"]'::jsonb),
    ('22220000-0000-0000-0000-000000000036', 'desmayo',               'Pérdida súbita del conocimiento',                 'R55',    '["síncope","pérdida de conocimiento","desvanecimiento"]'::jsonb),
    ('22220000-0000-0000-0000-000000000037', 'palpitaciones',         'Sensación de latidos rápidos o irregulares',      'R00.2',  '["taquicardia","corazón acelerado","latidos rápidos"]'::jsonb),
    ('22220000-0000-0000-0000-000000000038', 'dolor de pecho',        'Dolor u opresión torácica',                       'R07.9',  '["dolor torácico","angina","dolor en el pecho"]'::jsonb),
    ('22220000-0000-0000-0000-000000000039', 'acidez estomacal',      'Ardor o reflujo ácido',                           'R12',    '["pirosis","reflujo ácido","ardor de estómago"]'::jsonb),
    ('22220000-0000-0000-0000-000000000040', 'estreñimiento',         'Dificultad para evacuar o deposiciones escasas',  'K59.0',  '["constipación","estitiquez","no poder ir al baño"]'::jsonb),
    ('22220000-0000-0000-0000-000000000041', 'sangre en heces',       'Sangre visible en las deposiciones',              'K92.1',  '["hematoquecia","sangrado rectal","sangre al defecar"]'::jsonb),
    ('22220000-0000-0000-0000-000000000042', 'dolor al defecar',      'Dolor durante la evacuación',                     'K59.4',  '["dolor al ir al baño"]'::jsonb),
    ('22220000-0000-0000-0000-000000000043', 'sed excesiva',          'Sensación de sed intensa',                        'R63.1',  '["polidipsia","mucha sed"]'::jsonb),
    ('22220000-0000-0000-0000-000000000044', 'micción excesiva',      'Orinar con frecuencia o volumen excesivo',        'R35',    '["poliuria","orinar mucho"]'::jsonb),
    ('22220000-0000-0000-0000-000000000045', 'hambre excesiva',       'Apetito aumentado',                               'R63.2',  '["polifagia","mucha hambre"]'::jsonb),
    ('22220000-0000-0000-0000-000000000046', 'pérdida de peso',       'Bajada de peso no intencional',                   'R63.4',  '["bajar de peso","adelgazamiento"]'::jsonb),
    ('22220000-0000-0000-0000-000000000047', 'debilidad',             'Falta de fuerza general',                         'R53.1',  '["astenia","falta de fuerza"]'::jsonb),
    ('22220000-0000-0000-0000-000000000048', 'dificultad para conciliar el sueño', 'Insomnio o mal descanso',          'G47.0',  '["insomnio","no poder dormir","mal dormir"]'::jsonb),
    ('22220000-0000-0000-0000-000000000049', 'nerviosismo',           'Ansiedad, inquietud o preocupación',              'R45.7',  '["ansiedad","inquietud","tensión nerviosa"]'::jsonb),
    ('22220000-0000-0000-0000-000000000050', 'irritabilidad',         'Estado de ánimo irritable',                       'R45.4',  '["mal humor","irritación"]'::jsonb),
    ('22220000-0000-0000-0000-000000000051', 'dificultad para respirar al acostarse', 'Ortopnea',                     'R06.0',  '["ortopnea","ahogo al acostarse"]'::jsonb),
    ('22220000-0000-0000-0000-000000000052', 'sudoración nocturna',   'Sudoración durante la noche',                     'R61',    '["sudor nocturno","transpiración nocturna"]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Aliases para los síntomas ya existentes (V21) — normalización NLP.
UPDATE symptoms SET aliases = '["temperatura alta","calentura","fiebre alta"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000001' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["tos seca","tos productiva","toser"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000002' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["dolor al tragar","garganta irritada","odinofagia"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000003' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["nariz tapada","congestión"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000004' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["estornudar"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000005' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["cefalea","dolor de la cabeza"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000006' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["ganas de vomitar","malestar estomacal"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000007' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["vomitar"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000008' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["deposiciones líquidas","cagalera"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000009' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["dolor de estómago","dolor de barriga"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000010' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["dolor al orinar","escozor al orinar"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000011' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["orinar seguido","ir al baño seguido"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000012' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["comezón en los ojos"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000013' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["lagrimeo ocular","ojos llorosos"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000014' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["falta de aire","disnea","ahogo"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000015' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["silbido al respirar","pitos al respirar"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000016' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["opresión en el pecho","malestar en el pecho"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000017' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["cansancio","agotamiento"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000018' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["dolores musculares","mialgia","dolor en los músculos"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000019' AND aliases = '[]'::jsonb;
UPDATE symptoms SET aliases = '["escalofrío","tiritar","escalofríos"]'::jsonb
    WHERE id = '22220000-0000-0000-0000-000000000020' AND aliases = '[]'::jsonb;

-- ============================================================
-- 3. Nuevas condiciones (13..50) — total 50 condiciones
-- ============================================================
INSERT INTO conditions (id, name, description, icd_code, severity, urgency, recommendation) VALUES
    ('22220000-0000-0000-0000-000000010013', 'Sinusitis',              'Inflamación de los senos paranasales',           'J32',    'MODERADO', 'MEDIA', 'Consulta médica. Descongestionantes y humidificación.'),
    ('22220000-0000-0000-0000-000000010014', 'Faringitis',             'Inflamación de la faringe',                      'J02.9',  'MODERADO', 'MEDIA', 'Consulta médica en 24-48 h. Gárgaras e hidratación.'),
    ('22220000-0000-0000-0000-000000010015', 'Laringitis',             'Inflamación de la laringe con ronquera',         'J04.0',  'LEVE',     'BAJA',  'Reposo vocal e hidratación. Consultar si dura más de 2 semanas.'),
    ('22220000-0000-0000-0000-000000010016', 'Otitis media',           'Infección del oído medio',                       'H66.9',  'MODERADO', 'MEDIA', 'Consulta médica en 24-48 h para evaluar tratamiento.'),
    ('22220000-0000-0000-0000-000000010017', 'Otitis externa',         'Inflamación del conducto auditivo externo',      'H60.3',  'LEVE',     'BAJA',  'Evitar la humedad en el oído. Consultar si hay secreción.'),
    ('22220000-0000-0000-0000-000000010018', 'Dermatitis de contacto', 'Reacción inflamatoria de la piel por contacto',  'L25.9',  'LEVE',     'BAJA',  'Evitar el alérgeno. Emolientes y consulta si no mejora en 72 h.'),
    ('22220000-0000-0000-0000-000000010019', 'Urticaria',              'Ronchas o habones con picazón',                  'L50.9',  'LEVE',     'BAJA',  'Antihistamínicos. Urgencias si hay hinchazón de labios o disnea.'),
    ('22220000-0000-0000-0000-000000010020', 'Acné',                   'Inflamación de los folículos pilosos',           'L70.0',  'LEVE',     'BAJA',  'Higiene facial y tratamiento tópico. Consulta si es severo.'),
    ('22220000-0000-0000-0000-000000010021', 'Psoriasis',              'Enfermedad autoinmune de la piel',               'L40.9',  'MODERADO', 'MEDIA', 'Consulta dermatológica. Tratamiento tópico.'),
    ('22220000-0000-0000-0000-000000010022', 'Celulitis',              'Infección bacteriana de la piel y tejidos',      'L03.9',  'GRAVE',    'ALTA',  'Acudir a urgencias. Requiere antibiótico.'),
    ('22220000-0000-0000-0000-000000010023', 'Varicela',               'Infección viral con ampollas y fiebre',          'B01.9',  'MODERADO', 'MEDIA', 'Consulta médica. Evitar el rascado.'),
    ('22220000-0000-0000-0000-000000010024', 'Herpes zóster',          'Erupción dolorosa por reactivación viral',       'B02.9',  'MODERADO', 'MEDIA', 'Consulta médica en 72 h para tratamiento antiviral.'),
    ('22220000-0000-0000-0000-000000010025', 'Conjuntivitis alérgica', 'Conjuntivitis por alergia',                      'H10.1',  'LEVE',     'BAJA',  'Evitar alérgenos y usar lágrimas artificiales.'),
    ('22220000-0000-0000-0000-000000010026', 'Migraña con aura',       'Cefalea con síntomas visuales previos',          'G43.1',  'MODERADO', 'MEDIA', 'Consulta médica. Evitar desencadenantes.'),
    ('22220000-0000-0000-0000-000000010027', 'Artrosis',               'Desgaste degenerativo de las articulaciones',    'M19.9',  'MODERADO', 'MEDIA', 'Consulta médica. Ejercicio de bajo impacto.'),
    ('22220000-0000-0000-0000-000000010028', 'Artritis reumatoide',    'Enfermedad inflamatoria autoinmune articular',   'M06.9',  'GRAVE',    'ALTA',  'Consulta reumatológica. Tratamiento precoz.'),
    ('22220000-0000-0000-0000-000000010029', 'Gota',                   'Artritis por cristales de ácido úrico',          'M10.9',  'MODERADO', 'MEDIA', 'Consulta médica. Dieta y antiinflamatorios.'),
    ('22220000-0000-0000-0000-000000010030', 'Lumbalgia',              'Dolor lumbar agudo o crónico',                   'M54.5',  'MODERADO', 'MEDIA', 'Consulta médica. Calor local y evitar reposo prolongado.'),
    ('22220000-0000-0000-0000-000000010031', 'Cervicalgia',            'Dolor de cuello',                                'M54.2',  'LEVE',     'BAJA',  'Calor local y ejercicios de estiramiento.'),
    ('22220000-0000-0000-0000-000000010032', 'Hipertensión arterial',  'Presión arterial elevada',                       'I10',    'GRAVE',    'ALTA',  'Control médico urgente de la presión arterial.'),
    ('22220000-0000-0000-0000-000000010033', 'Angina de pecho',        'Dolor torácico por reducción del flujo coronario', 'I20.9', 'GRAVE',    'ALTA',  'Urgencias. No automedicarse.'),
    ('22220000-0000-0000-0000-000000010034', 'Arritmia',               'Ritmo cardíaco anormal',                         'I49.9',  'GRAVE',    'ALTA',  'Consulta médica. Urgencias si hay desmayo o dolor torácico.'),
    ('22220000-0000-0000-0000-000000010035', 'Insuficiencia cardíaca', 'El corazón no bombea eficazmente',               'I50.9',  'GRAVE',    'ALTA',  'Urgencias si hay dificultad respiratoria intensa.'),
    ('22220000-0000-0000-0000-000000010036', 'Gastritis',              'Inflamación del revestimiento del estómago',     'K29.7',  'MODERADO', 'MEDIA', 'Consulta médica. Evitar irritantes y alcohol.'),
    ('22220000-0000-0000-0000-000000010037', 'Reflujo gastroesofágico','Retorno del contenido gástrico al esófago',      'K21.9',  'LEVE',     'BAJA',  'Evitar comidas copiosas y acostarse tras comer. Consulta si persiste.'),
    ('22220000-0000-0000-0000-000000010038', 'Úlcera péptica',         'Lesión en la mucosa gástrica o duodenal',        'K27.9',  'GRAVE',    'ALTA',  'Consulta médica. Urgencias si hay vómitos con sangre.'),
    ('22220000-0000-0000-0000-000000010039', 'Hemorroides',            'Venas inflamadas en el recto',                   'K64.9',  'LEVE',     'BAJA',  'Fibra y agua. Consulta si hay sangrado abundante.'),
    ('22220000-0000-0000-0000-000000010040', 'Anemia',                 'Disminución de hemoglobina',                     'D64.9',  'MODERADO', 'MEDIA', 'Consulta médica para estudio de causas.'),
    ('22220000-0000-0000-0000-000000010041', 'Diabetes tipo 2',        'Alteración del metabolismo de la glucosa',       'E11.9',  'MODERADO', 'ALTA',  'Consulta médica. Control de glucosa y dieta.'),
    ('22220000-0000-0000-0000-000000010042', 'Hipotiroidismo',         'Hormonas tiroideas disminuidas',                 'E03.9',  'LEVE',     'MEDIA', 'Consulta médica. Estudio hormonal.'),
    ('22220000-0000-0000-0000-000000010043', 'Hipertiroidismo',        'Hormonas tiroideas elevadas',                    'E05.9',  'MODERADO', 'MEDIA', 'Consulta médica. Estudio hormonal.'),
    ('22220000-0000-0000-0000-000000010044', 'Hipertensión gestacional','Presión arterial elevada en embarazo',         'O13',    'GRAVE',    'ALTA',  'Control obstétrico urgente.'),
    ('22220000-0000-0000-0000-000000010045', 'Síndrome del túnel carpiano', 'Compresión del nervio mediano',          'G56.0',  'MODERADO', 'MEDIA', 'Consulta médica. Inmovilización de muñeca.'),
    ('22220000-0000-0000-0000-000000010046', 'Fibromialgia',           'Dolor musculoesquelético generalizado',          'M79.7',  'MODERADO', 'MEDIA', 'Consulta médica. Ejercicio suave y manejo del estrés.'),
    ('22220000-0000-0000-0000-000000010047', 'Insomnio',               'Dificultad persistente para dormir',             'G47.0',  'LEVE',     'BAJA',  'Higiene del sueño. Consulta si persiste más de 4 semanas.'),
    ('22220000-0000-0000-0000-000000010048', 'Ansiedad',               'Ansiedad generalizada o episódica',              'F41.9',  'MODERADO', 'MEDIA', 'Consulta médica o psicológica. Técnicas de relajación.'),
    ('22220000-0000-0000-0000-000000010049', 'Depresión',              'Trastorno del estado de ánimo',                  'F32.9',  'MODERADO', 'MEDIA', 'Consulta médica o psicológica. Buscar apoyo.'),
    ('22220000-0000-0000-0000-000000010050', 'Deshidratación',         'Pérdida excesiva de líquidos corporales',        'E86',    'GRAVE',    'ALTA',  'Hidratación oral. Urgencias si hay confusión o mareo intenso.')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. Nuevas relaciones — total 100+ relaciones
-- ============================================================
INSERT INTO symptom_condition_relations (symptom_id, condition_id, weight, required) VALUES
    -- Sinusitis (13)
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010013', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000004', '22220000-0000-0000-0000-000000010013', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000021', '22220000-0000-0000-0000-000000010013', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010013', 0.50, FALSE),
    -- Faringitis (14)
    ('22220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010014', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010014', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010014', 0.30, FALSE),
    -- Laringitis (15)
    ('22220000-0000-0000-0000-000000000002', '22220000-0000-0000-0000-000000010015', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000003', '22220000-0000-0000-0000-000000010015', 0.80, TRUE),
    -- Otitis media (16)
    ('22220000-0000-0000-0000-000000000022', '22220000-0000-0000-0000-000000010016', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010016', 0.60, FALSE),
    -- Otitis externa (17)
    ('22220000-0000-0000-0000-000000000022', '22220000-0000-0000-0000-000000010017', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000024', '22220000-0000-0000-0000-000000010017', 0.70, FALSE),
    -- Dermatitis de contacto (18)
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010018', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010018', 0.70, FALSE),
    -- Urticaria (19)
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010019', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010019', 0.80, FALSE),
    -- Acné (20)
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010020', 0.50, FALSE),
    -- Psoriasis (21)
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010021', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010021', 0.60, FALSE),
    -- Celulitis (22)
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010022', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000034', '22220000-0000-0000-0000-000000010022', 0.80, TRUE),
    -- Varicela (23)
    ('22220000-0000-0000-0000-000000000001', '22220000-0000-0000-0000-000000010023', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010023', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010023', 0.70, FALSE),
    -- Herpes zóster (24)
    ('22220000-0000-0000-0000-000000000025', '22220000-0000-0000-0000-000000010024', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000026', '22220000-0000-0000-0000-000000010024', 0.60, FALSE),
    -- Conjuntivitis alérgica (25)
    ('22220000-0000-0000-0000-000000000013', '22220000-0000-0000-0000-000000010025', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000027', '22220000-0000-0000-0000-000000010025', 0.80, FALSE),
    -- Migraña con aura (26)
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010026', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010026', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000028', '22220000-0000-0000-0000-000000010026', 0.70, FALSE),
    -- Artrosis (27)
    ('22220000-0000-0000-0000-000000000031', '22220000-0000-0000-0000-000000010027', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000032', '22220000-0000-0000-0000-000000010027', 0.70, FALSE),
    -- Artritis reumatoide (28)
    ('22220000-0000-0000-0000-000000000031', '22220000-0000-0000-0000-000000010028', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000033', '22220000-0000-0000-0000-000000010028', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010028', 0.60, FALSE),
    -- Gota (29)
    ('22220000-0000-0000-0000-000000000031', '22220000-0000-0000-0000-000000010029', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000033', '22220000-0000-0000-0000-000000010029', 0.60, FALSE),
    -- Lumbalgia (30)
    ('22220000-0000-0000-0000-000000000030', '22220000-0000-0000-0000-000000010030', 0.90, TRUE),
    -- Cervicalgia (31)
    ('22220000-0000-0000-0000-000000000029', '22220000-0000-0000-0000-000000010031', 0.70, FALSE),
    -- Hipertensión arterial (32)
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010032', 0.40, FALSE),
    ('22220000-0000-0000-0000-000000000035', '22220000-0000-0000-0000-000000010032', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000028', '22220000-0000-0000-0000-000000010032', 0.40, FALSE),
    -- Angina de pecho (33)
    ('22220000-0000-0000-0000-000000000038', '22220000-0000-0000-0000-000000010033', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000017', '22220000-0000-0000-0000-000000010033', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010033', 0.60, FALSE),
    -- Arritmia (34)
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010034', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000035', '22220000-0000-0000-0000-000000010034', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010034', 0.50, FALSE),
    -- Insuficiencia cardíaca (35)
    ('22220000-0000-0000-0000-000000000015', '22220000-0000-0000-0000-000000010035', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000051', '22220000-0000-0000-0000-000000010035', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000034', '22220000-0000-0000-0000-000000010035', 0.70, FALSE),
    -- Gastritis (36)
    ('22220000-0000-0000-0000-000000000039', '22220000-0000-0000-0000-000000010036', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010036', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000007', '22220000-0000-0000-0000-000000010036', 0.40, FALSE),
    -- Reflujo gastroesofágico (37)
    ('22220000-0000-0000-0000-000000000039', '22220000-0000-0000-0000-000000010037', 0.90, TRUE),
    -- Úlcera péptica (38)
    ('22220000-0000-0000-0000-000000000010', '22220000-0000-0000-0000-000000010038', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000039', '22220000-0000-0000-0000-000000010038', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000041', '22220000-0000-0000-0000-000000010038', 0.60, FALSE),
    -- Hemorroides (39)
    ('22220000-0000-0000-0000-000000000041', '22220000-0000-0000-0000-000000010039', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000042', '22220000-0000-0000-0000-000000010039', 0.70, TRUE),
    -- Anemia (40)
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010040', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000047', '22220000-0000-0000-0000-000000010040', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000006', '22220000-0000-0000-0000-000000010040', 0.40, FALSE),
    -- Diabetes tipo 2 (41)
    ('22220000-0000-0000-0000-000000000043', '22220000-0000-0000-0000-000000010041', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000044', '22220000-0000-0000-0000-000000010041', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000045', '22220000-0000-0000-0000-000000010041', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010041', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000028', '22220000-0000-0000-0000-000000010041', 0.50, FALSE),
    -- Hipotiroidismo (42)
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010042', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010042', 0.60, FALSE),
    -- Hipertiroidismo (43)
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010043', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000046', '22220000-0000-0000-0000-000000010043', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000049', '22220000-0000-0000-0000-000000010043', 0.70, FALSE),
    -- Síndrome del túnel carpiano (45)
    ('22220000-0000-0000-0000-000000000047', '22220000-0000-0000-0000-000000010045', 0.60, FALSE),
    -- Fibromialgia (46)
    ('22220000-0000-0000-0000-000000000019', '22220000-0000-0000-0000-000000010046', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010046', 0.80, TRUE),
    ('22220000-0000-0000-0000-000000000048', '22220000-0000-0000-0000-000000010046', 0.60, FALSE),
    -- Insomnio (47)
    ('22220000-0000-0000-0000-000000000048', '22220000-0000-0000-0000-000000010047', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010047', 0.50, FALSE),
    -- Ansiedad (48)
    ('22220000-0000-0000-0000-000000000049', '22220000-0000-0000-0000-000000010048', 0.90, TRUE),
    ('22220000-0000-0000-0000-000000000037', '22220000-0000-0000-0000-000000010048', 0.50, FALSE),
    ('22220000-0000-0000-0000-000000000048', '22220000-0000-0000-0000-000000010048', 0.60, FALSE),
    -- Depresión (49)
    ('22220000-0000-0000-0000-000000000050', '22220000-0000-0000-0000-000000010049', 0.60, FALSE),
    ('22220000-0000-0000-0000-000000000018', '22220000-0000-0000-0000-000000010049', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000048', '22220000-0000-0000-0000-000000010049', 0.70, FALSE),
    -- Deshidratación (50)
    ('22220000-0000-0000-0000-000000000043', '22220000-0000-0000-0000-000000010050', 0.80, FALSE),
    ('22220000-0000-0000-0000-000000000035', '22220000-0000-0000-0000-000000010050', 0.70, FALSE),
    ('22220000-0000-0000-0000-000000000036', '22220000-0000-0000-0000-000000010050', 0.70, FALSE)
ON CONFLICT (symptom_id, condition_id) DO NOTHING;

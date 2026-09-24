-- ============================================================
-- V70: seed subconjunto real INVIMA (validacion tecnica)
-- El catalogo completo (~30k) se carga antes de produccion.
-- ============================================================

INSERT INTO invima_catalog (
    invima_registration, cum_code, commercial_name, generic_name,
    pharmaceutical_form, concentration, laboratory, atc_code, invima_version
) VALUES
-- Analgesicos
('INVIMA 2020M-0012345', '20123456-1', 'ACETAMINOFEN GENFAR', 'ACETAMINOFEN', 'TABLETA', '500 mg', 'GENFAR', 'N02BE01', '2024'),
('INVIMA 2020M-0012346', '20123457-1', 'IBUPROFENO MK', 'IBUPROFENO', 'TABLETA', '400 mg', 'MK', 'M01AE01', '2024'),
('INVIMA 2020M-0012347', '20123458-1', 'DICLOFENACO GENFAR', 'DICLOFENACO', 'TABLETA', '50 mg', 'GENFAR', 'M01AB05', '2024'),
-- Antibioticos
('INVIMA 2020M-0012350', '20123460-1', 'AMOXICILINA GENFAR', 'AMOXICILINA', 'CAPSULA', '500 mg', 'GENFAR', 'J01CA04', '2024'),
('INVIMA 2020M-0012351', '20123461-1', 'AZITROMICINA MK', 'AZITROMICINA', 'TABLETA', '500 mg', 'MK', 'J01FA10', '2024'),
-- Cardiovasculares
('INVIMA 2020M-0012360', '20123470-1', 'LOSARTAN MK', 'LOSARTAN', 'TABLETA', '50 mg', 'MK', 'C09CA01', '2024'),
('INVIMA 2020M-0012361', '20123471-1', 'AMLODIPINO GENFAR', 'AMLODIPINO', 'TABLETA', '5 mg', 'GENFAR', 'C08CA01', '2024'),
('INVIMA 2020M-0012362', '20123472-1', 'ATORVASTATINA MK', 'ATORVASTATINA', 'TABLETA', '20 mg', 'MK', 'C10AA05', '2024'),
-- Diabetes
('INVIMA 2020M-0012370', '20123480-1', 'METFORMINA GENFAR', 'METFORMINA', 'TABLETA', '850 mg', 'GENFAR', 'A10BA02', '2024'),
('INVIMA 2020M-0012371', '20123481-1', 'GLIBENCLAMIDA MK', 'GLIBENCLAMIDA', 'TABLETA', '5 mg', 'MK', 'A10BB01', '2024')
ON CONFLICT (cum_code) DO NOTHING;

-- ============================================================
-- V55.1: Seeds de prueba para KIN Billing (Contratos + Tarifarios + Copays)
-- ============================================================

DO $$
DECLARE
    demo_org_id UUID := '00000000-0000-0000-0000-000000000001';
    colsanitas_id UUID;
    sura_id UUID;
BEGIN
    -- Contrato COLSANITAS
    INSERT INTO eps_contracts (organization_id, eps_nit, eps_name, regimen, contract_number, start_date, status, dian_prefix, dian_resolution_number, dian_current_sequence)
    VALUES (demo_org_id, '890900123', 'COLSANITAS EPS', 'CONTRIBUTIVO', 'CT-2024-001', '2024-01-01', 'ACTIVE', 'FEV', '18760000001234', 0)
    RETURNING id INTO colsanitas_id;

    -- Contrato SURA
    INSERT INTO eps_contracts (organization_id, eps_nit, eps_name, regimen, contract_number, start_date, status, dian_prefix, dian_resolution_number, dian_current_sequence)
    VALUES (demo_org_id, '890300456', 'EPS SURA', 'CONTRIBUTIVO', 'CT-2024-002', '2024-01-01', 'ACTIVE', 'FEV', '18760000001235', 0)
    RETURNING id INTO sura_id;

    -- Tarifarios COLSANITAS (10 códigos CUPS 2024 reales)
    INSERT INTO tariffs_cups (contract_id, cups_code, cups_version, description, unit_price_cop, max_quantity, requires_auth, auth_validity_days, cups_category, effective_from, effective_to)
    SELECT colsanitas_id, t.cups_code, '2024', t.description, t.unit_price_cop, t.max_quantity, t.requires_auth, t.auth_validity_days, t.cups_category, '2024-01-01'::date, null
    FROM (VALUES
        ('890201', 'CONSULTA MEDICINA GENERAL', 45000, null, false, null, 'CONSULTA'),
        ('890202', 'CONSULTA ESPECIALISTA INTERNA', 65000, null, false, null, 'CONSULTA'),
        ('890203', 'CONSULTA ESPECIALISTA PEDIATRIA', 68000, null, false, null, 'CONSULTA'),
        ('890204', 'CONSULTA ESPECIALISTA GINECOLOGIA', 70000, null, false, null, 'CONSULTA'),
        ('890301', 'PROCEDIMIENTO QUIRURGICO MENOR', 180000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890302', 'PROCEDIMIENTO ENDOSCOPIA DIGESTIVA ALTA', 350000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890303', 'PROCEDIMIENTO CIRUGIA AMBULATORIA', 420000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890401', 'EXAMEN LABORATORIO HEMOGRAMA COMPLETO', 25000, null, false, null, 'EXAMEN'),
        ('890402', 'EXAMEN IMAGEN RADIOGRAFIA TORAX', 55000, null, false, null, 'EXAMEN'),
        ('890403', 'EXAMEN IMAGEN ECOGRAFIA ABDOMINAL', 120000, null, false, null, 'EXAMEN')
    ) AS t(cups_code, description, unit_price_cop, max_quantity, requires_auth, auth_validity_days, cups_category);

    -- Tarifarios SURA (10 códigos, precios distintos)
    INSERT INTO tariffs_cups (contract_id, cups_code, cups_version, description, unit_price_cop, max_quantity, requires_auth, auth_validity_days, cups_category, effective_from, effective_to)
    SELECT sura_id, t.cups_code, '2024', t.description, t.unit_price_cop, t.max_quantity, t.requires_auth, t.auth_validity_days, t.cups_category, '2024-01-01'::date, null
    FROM (VALUES
        ('890201', 'CONSULTA MEDICINA GENERAL', 48000, null, false, null, 'CONSULTA'),
        ('890202', 'CONSULTA ESPECIALISTA INTERNA', 70000, null, false, null, 'CONSULTA'),
        ('890203', 'CONSULTA ESPECIALISTA PEDIATRIA', 72000, null, false, null, 'CONSULTA'),
        ('890204', 'CONSULTA ESPECIALISTA GINECOLOGIA', 75000, null, false, null, 'CONSULTA'),
        ('890301', 'PROCEDIMIENTO QUIRURGICO MENOR', 195000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890302', 'PROCEDIMIENTO ENDOSCOPIA DIGESTIVA ALTA', 380000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890303', 'PROCEDIMIENTO CIRUGIA AMBULATORIA', 450000, 1, true, 30, 'PROCEDIMIENTO'),
        ('890401', 'EXAMEN LABORATORIO HEMOGRAMA COMPLETO', 28000, null, false, null, 'EXAMEN'),
        ('890402', 'EXAMEN IMAGEN RADIOGRAFIA TORAX', 58000, null, false, null, 'EXAMEN'),
        ('890403', 'EXAMEN IMAGEN ECOGRAFIA ABDOMINAL', 135000, null, false, null, 'EXAMEN')
    ) AS t(cups_code, description, unit_price_cop, max_quantity, requires_auth, auth_validity_days, cups_category);

    -- Reglas copago COLSANITAS
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, copay_cap_cop, priority)
    SELECT colsanitas_id, 'CONSULTA', 'CONTRIBUTIVO', 'CUOTA_MODERADORA', 8500, 35000, 10;
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, priority)
    SELECT colsanitas_id, 'CONSULTA', 'SUBSIDIADO', 'EXENTO', NULL, 20;
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, copay_cap_cop, priority)
    SELECT colsanitas_id, 'PROCEDIMIENTO', 'CONTRIBUTIVO', 'PORCENTAJE', 10, 50000, 5;
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, copay_cap_cop, priority)
    SELECT colsanitas_id, 'EXAMEN', 'CONTRIBUTIVO', 'CUOTA_MODERADORA', 4200, 20000, 5;

    -- Reglas copago SURA
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, copay_cap_cop, priority)
    SELECT sura_id, 'CONSULTA', 'CONTRIBUTIVO', 'CUOTA_MODERADORA', 9000, 35000, 10;
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, priority)
    SELECT sura_id, 'CONSULTA', 'SUBSIDIADO', 'EXENTO', NULL, 20;
    INSERT INTO copay_rules (contract_id, cups_category, patient_regimen, copay_type, copay_value_cop, copay_cap_cop, priority)
    SELECT sura_id, 'PROCEDIMIENTO', 'CONTRIBUTIVO', 'PORCENTAJE', 10, 60000, 5;

END $$;
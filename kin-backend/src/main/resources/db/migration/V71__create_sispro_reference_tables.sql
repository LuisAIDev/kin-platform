-- SISPRO Reference Tables for RIPS Validation per Resolución 2275 Anexo Técnico 1

-- Modalidades de pago (tabla modalidadPago)
CREATE TABLE sispro_modalidad_pago (
    codigo VARCHAR(20) PRIMARY KEY,
    descripcion VARCHAR(500) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE sispro_modalidad_pago IS 'Tabla de referencia SISPRO: Modalidades de pago (inciso 3.2 Anexo Técnico 1 Resolución 2275)';
COMMENT ON COLUMN sispro_modalidad_pago.codigo IS 'Código de modalidad (ej: 01, 02, 03, 04, 05)';
COMMENT ON COLUMN sispro_modalidad_pago.descripcion IS 'Descripción de la modalidad';
COMMENT ON COLUMN sispro_modalidad_pago.activo IS 'Si la modalidad está vigente';

-- Coberturas/Planes de beneficios (tabla coberturaPlan)
CREATE TABLE sispro_cobertura_plan (
    codigo VARCHAR(20) PRIMARY KEY,
    descripcion VARCHAR(500) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE sispro_cobertura_plan IS 'Tabla de referencia SISPRO: Coberturas/Planes de beneficios (inciso 3.3 Anexo Técnico 1 Resolución 2275)';
COMMENT ON COLUMN sispro_cobertura_plan.codigo IS 'Código de cobertura (ej: 01-15)';
COMMENT ON COLUMN sispro_cobertura_plan.descripcion IS 'Descripción de la cobertura';
COMMENT ON COLUMN sispro_cobertura_plan.activo IS 'Si la cobertura está vigente';

-- Conceptos de recaudo (tabla conceptoRecaudo)
CREATE TABLE sispro_concepto_recaudo (
    codigo VARCHAR(20) PRIMARY KEY,
    descripcion VARCHAR(500) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE sispro_concepto_recaudo IS 'Tabla de referencia SISPRO: Conceptos de recaudo (Anexo Técnico 1 Resolución 2275)';
COMMENT ON COLUMN sispro_concepto_recaudo.codigo IS 'Código de concepto (ej: 01, 02, 03, 04)';
COMMENT ON COLUMN sispro_concepto_recaudo.descripcion IS 'Descripción del concepto';
COMMENT ON COLUMN sispro_concepto_recaudo.activo IS 'Si el concepto está vigente';

-- Tipos de identificación (tabla TipoIdPISIS)
CREATE TABLE sispro_tipo_id (
    codigo VARCHAR(10) PRIMARY KEY,
    descripcion VARCHAR(500) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE sispro_tipo_id IS 'Tabla de referencia SISPRO: Tipos de identificación (TipoIdPISIS Anexo Técnico 1 Resolución 2275)';
COMMENT ON COLUMN sispro_tipo_id.codigo IS 'Código de tipo identificación (ej: CC, CE, TI, PA, NU)';
COMMENT ON COLUMN sispro_tipo_id.descripcion IS 'Descripción del tipo';
COMMENT ON COLUMN sispro_tipo_id.activo IS 'Si el tipo está vigente';

-- Seed inicial - Modalidades de pago (inciso 3.2 Anexo Técnico 1)
INSERT INTO sispro_modalidad_pago (codigo, descripcion) VALUES
('01', 'Pago individual por caso/Conjunto integral de atenciones'),
('02', 'Pago global prospectivo'),
('03', 'Pago por capitación'),
('04', 'Pago por evento'),
('05', 'Otra modalidad específica')
ON CONFLICT (codigo) DO NOTHING;

-- Seed inicial - Coberturas/Planes de beneficios (inciso 3.3 Anexo Técnico 1)
INSERT INTO sispro_cobertura_plan (codigo, descripcion) VALUES
('01', 'Plan de beneficios en salud financiado con UPC'),
('02', 'Presupuesto máximo'),
('03', 'Prima EPS, no asegurados SOAT'),
('04', 'Cobertura póliza SOAT'),
('05', 'Cobertura ARL'),
('06', 'Cobertura ADRES'),
('07', 'Cobertura salud pública'),
('08', 'Cobertura entidad territorial, recursos de oferta'),
('09', 'Urgencias población migrante'),
('10', 'Plan complementario en salud'),
('11', 'Plan medicina prepagada'),
('12', 'Pólizas en salud'),
('13', 'Cobertura Régimen Especial o Excepción'),
('14', 'Cobertura Fondo Nacional de Salud de las Personas Privadas de la Libertad'),
('15', 'Particular')
ON CONFLICT (codigo) DO NOTHING;

-- Seed inicial - Conceptos de recaudo
INSERT INTO sispro_concepto_recaudo (codigo, descripcion) VALUES
('01', 'Copago'),
('02', 'Cuota moderadora'),
('03', 'Pagos compartidos en planes voluntarios de salud'),
('04', 'Anticipos')
ON CONFLICT (codigo) DO NOTHING;

-- Seed inicial - Tipos de identificación (TipoIdPISIS - valores comunes)
INSERT INTO sispro_tipo_id (codigo, descripcion) VALUES
('RC', 'Registro Civil'),
('TI', 'Tarjeta de Identidad'),
('CC', 'Cédula de Ciudadanía'),
('CE', 'Cédula de Extranjería'),
('PA', 'Pasaporte'),
('MS', 'Menor sin identificación'),
('AS', 'Adulto sin identificación'),
('CD', 'Carné Diplomático'),
('NU', 'Número Único de Identificación Personal')
ON CONFLICT (codigo) DO NOTHING;

-- Índices para consultas rápidas
CREATE INDEX idx_sispro_modalidad_pago_activo ON sispro_modalidad_pago(activo);
CREATE INDEX idx_sispro_cobertura_plan_activo ON sispro_cobertura_plan(activo);
CREATE INDEX idx_sispro_concepto_recaudo_activo ON sispro_concepto_recaudo(activo);
CREATE INDEX idx_sispro_tipo_id_activo ON sispro_tipo_id(activo);
-- V82: Privacy policy versioning (Ley 1581 Art. 13)

CREATE TABLE privacy_policy_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    content_md TEXT NOT NULL,
    effective_date DATE NOT NULL,
    published_at TIMESTAMPTZ DEFAULT NOW(),
    published_by UUID REFERENCES users(id),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Solo puede haber 1 activa a la vez
CREATE UNIQUE INDEX idx_privacy_policy_active ON privacy_policy_versions(active) WHERE active = TRUE;

-- Seed inicial: versión 1.0
INSERT INTO privacy_policy_versions (version, title, content_md, effective_date, active)
VALUES (
    '1.0',
    'Política de Privacidad KIN Salud',
    '# Política de Privacidad KIN Salud\n\n## 1. Responsable del Tratamiento\nKIN Salud...\n\n## 2. Finalidad\nGestión clínica...\n\n## 3. Derechos del Titular\nAcceso, rectificación, eliminación...\n\n## 4. Contacto\nprivacidad@kin-platform.com',
    CURRENT_DATE,
    TRUE
);

COMMENT ON TABLE privacy_policy_versions IS 'Versiones de la política de privacidad (Ley 1581 Art. 13)';
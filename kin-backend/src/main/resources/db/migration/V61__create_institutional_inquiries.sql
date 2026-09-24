-- ============================================================
-- V61: Solicitudes de acceso Beta para clínicas/hospitales (IPS)
-- Captura de leads del programa Beta Cerrada. Sin multi-tenant: es un
-- formulario público (landing) antes de crear organización.
-- ============================================================

CREATE TABLE IF NOT EXISTS institutional_inquiries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ips_name        VARCHAR(200) NOT NULL,
    nit             VARCHAR(20) NOT NULL,
    city            VARCHAR(100),
    contact_name    VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    beds            INTEGER,
    comments        TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','CONTACTED','QUALIFIED','REJECTED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_institutional_inquiries_status ON institutional_inquiries (status);
CREATE INDEX idx_institutional_inquiries_created ON institutional_inquiries (created_at);
CREATE INDEX idx_institutional_inquiries_email ON institutional_inquiries (email);

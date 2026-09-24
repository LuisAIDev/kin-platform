-- ============================================================
-- V68: Catalogo INVIMA (Sprint 2 - Fase 2.2)
-- Full-text (spanish) + trigram para autocomplete.
-- ============================================================

CREATE TABLE IF NOT EXISTS invima_catalog (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invima_registration VARCHAR(50) NOT NULL,
    cum_code            VARCHAR(50),
    ium_code            VARCHAR(50),
    commercial_name     VARCHAR(500) NOT NULL,
    generic_name        VARCHAR(500),
    pharmaceutical_form VARCHAR(200),
    concentration       VARCHAR(200),
    laboratory          VARCHAR(300),
    atc_code            VARCHAR(20),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    invima_version      VARCHAR(10) NOT NULL DEFAULT '2024',
    search_vector       TSVECTOR,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invima_cum UNIQUE (cum_code)
);

CREATE INDEX IF NOT EXISTS idx_invima_registration ON invima_catalog (invima_registration);
CREATE INDEX IF NOT EXISTS idx_invima_ium ON invima_catalog (ium_code);
CREATE INDEX IF NOT EXISTS idx_invima_generic_trgm ON invima_catalog USING gin (generic_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_invima_search ON invima_catalog USING gin (search_vector);

CREATE OR REPLACE FUNCTION invima_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'pg_catalog.spanish',
        coalesce(NEW.commercial_name, '') || ' ' || coalesce(NEW.generic_name, ''));
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_invima_search ON invima_catalog;
CREATE TRIGGER trg_invima_search
    BEFORE INSERT OR UPDATE ON invima_catalog
    FOR EACH ROW EXECUTE FUNCTION invima_search_vector_update();

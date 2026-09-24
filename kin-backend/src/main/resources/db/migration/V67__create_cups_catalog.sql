-- ============================================================
-- V67: Catalogo nacional CUPS (Sprint 2 - Fase 2.2)
-- Full-text (spanish) + trigram para autocomplete.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS cups_catalog (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cups_code           VARCHAR(20) NOT NULL UNIQUE,
    cups_description    VARCHAR(500) NOT NULL,
    cups_category       VARCHAR(50) NOT NULL,
    cups_group          VARCHAR(100),
    cups_version        VARCHAR(10) NOT NULL DEFAULT '2024',
    reference_price_cop NUMERIC(14,2),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    search_vector       TSVECTOR,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cups_code ON cups_catalog (cups_code);
CREATE INDEX IF NOT EXISTS idx_cups_category ON cups_catalog (cups_category);
CREATE INDEX IF NOT EXISTS idx_cups_description_trgm ON cups_catalog USING gin (cups_description gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_cups_search ON cups_catalog USING gin (search_vector);

CREATE OR REPLACE FUNCTION cups_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'pg_catalog.spanish',
        coalesce(NEW.cups_code, '') || ' ' || coalesce(NEW.cups_description, ''));
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cups_search ON cups_catalog;
CREATE TRIGGER trg_cups_search
    BEFORE INSERT OR UPDATE ON cups_catalog
    FOR EACH ROW EXECUTE FUNCTION cups_search_vector_update();

-- ============================================================
-- KIN Platform - Database Initialization Script
-- Knowledge, Innovation & Navigation
-- PostgreSQL 16
--
-- NOTA (C3/C5 - Opción B): este script ya NO forma parte de la ruta de
-- despliegue. Queda ÚNICAMENTE como referencia histórica del esquema.
-- El esquema de producción lo crea Flyway desde cero (migraciones
-- V1..V10 en kin-backend/src/main/resources/db/migration, incluidas las
-- tablas enterprise de V7 y el plan/suscripción de V9/V10) sobre una base
-- PostgreSQL vacía; ningún docker-compose monta este archivo en
-- /docker-entrypoint-initdb.d.
-- M3H (Fase 10): sincronizado con V7 (enterprise_project/enterprise_document).
-- Brecha de esquema (V9/V10): current_plan_id en users y user_subscriptions.
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUMS
-- ============================================================

-- Enums are handled as VARCHAR at the application level for portability
-- (see JPA @Enumerated(EnumType.STRING) in entity classes)

-- ============================================================
-- TABLE: users
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(180) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'FREE',
    avatar_url      VARCHAR(512),
    credits         INTEGER NOT NULL DEFAULT 10
                        CHECK (credits >= 0),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    current_plan_id UUID,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_is_active ON users (is_active);
CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_current_plan_id ON users (current_plan_id);

-- ============================================================
-- TABLE: projects
-- ============================================================

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    category_id     UUID,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    viability_score NUMERIC(5, 2)
                        CHECK (viability_score >= 0 AND viability_score <= 100),
    ai_summary      TEXT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_projects_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_projects_user_id ON projects (user_id);
CREATE INDEX idx_projects_category_id ON projects (category_id);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_user_status ON projects (user_id, status);
CREATE INDEX idx_projects_created_at ON projects (created_at DESC);

-- ============================================================
-- TABLE: categories (catálogo SaaS-ready, reemplaza el enum ProjectCategory)
-- ============================================================

CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(60) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(255),
    display_order   INTEGER NOT NULL,
    icon            VARCHAR(40),
    color           VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- El FK lo aporta la migración Flyway V6 (ALTER TABLE projects ADD CONSTRAINT ...).
-- El seed inicial de 17 categorías también lo aporta la migración V6.

-- ============================================================
-- TABLE: chat_messages
-- ============================================================

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    metadata        JSONB,
    tokens_used     INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_chat_messages_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_chat_messages_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_project_id ON chat_messages (project_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages (user_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (project_id, created_at ASC);
CREATE INDEX idx_chat_messages_role ON chat_messages (role);
CREATE INDEX idx_chat_messages_metadata ON chat_messages USING GIN (metadata);

-- ============================================================
-- TABLE: viability_scores
-- ============================================================

CREATE TABLE viability_scores (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id          UUID NOT NULL UNIQUE,
    overall_score       NUMERIC(5, 2) NOT NULL
                            CHECK (overall_score >= 0 AND overall_score <= 100),
    market_feasibility  NUMERIC(5, 2)
                            CHECK (market_feasibility >= 0 AND market_feasibility <= 100),
    technical_feasibility NUMERIC(5, 2)
                            CHECK (technical_feasibility >= 0 AND technical_feasibility <= 100),
    financial_viability NUMERIC(5, 2)
                            CHECK (financial_viability >= 0 AND financial_viability <= 100),
    team_capability     NUMERIC(5, 2)
                            CHECK (team_capability >= 0 AND team_capability <= 100),
    risk_level          VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    ai_insights         JSONB,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_viability_scores_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_viability_scores_project_id ON viability_scores (project_id);
CREATE INDEX idx_viability_scores_overall ON viability_scores (overall_score DESC);
CREATE INDEX idx_viability_scores_risk ON viability_scores (risk_level);
CREATE INDEX idx_viability_scores_ai_insights ON viability_scores USING GIN (ai_insights);

-- ============================================================
-- TABLE: project_context
-- Estado durable del ProjectContext de cada proyecto (1:1).
-- Gestionado por el adaptador JPA de ContextRepository (Fase 5.2.1).
-- ============================================================

CREATE TABLE project_context (
    project_id      UUID PRIMARY KEY,
    context_data    TEXT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_context_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_project_context_updated_at ON project_context (updated_at);

-- ============================================================
-- TABLE: interview_state
-- Estado durable de la entrevista estratégica de cada proyecto (1:1).
-- Gestionado por el adaptador JPA de InterviewRepository (Fase 7, E5/ADR-015).
-- ============================================================

CREATE TABLE interview_state (
    project_id      UUID PRIMARY KEY,
    state_data      TEXT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interview_state_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_interview_state_updated_at ON interview_state (updated_at);

-- ============================================================
-- TABLE: enterprise_project / enterprise_document
-- Bounded Context Enterprise (Fase 10, M2G/ADR-018, migración Flyway V7).
-- Generación/versionado/persistencia de documentos de negocio.
-- ============================================================

CREATE TABLE enterprise_project (
    project_id           UUID NOT NULL,
    version              INTEGER NOT NULL,
    status               VARCHAR(32) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    completed_at         TIMESTAMPTZ,
    failed_reason        TEXT,

    -- EnterpriseScore (@Embedded opcional, M3D)
    score_market         DOUBLE PRECISION,
    score_innovation     DOUBLE PRECISION,
    score_viability      DOUBLE PRECISION,
    score_financial      DOUBLE PRECISION,
    score_risk           DOUBLE PRECISION,
    score_scalability    DOUBLE PRECISION,
    score_team           DOUBLE PRECISION,
    score_sustainability DOUBLE PRECISION,
    score_overall        INTEGER,
    score_confidence     DOUBLE PRECISION,
    score_grade          VARCHAR(16),

    PRIMARY KEY (project_id, version),

    CONSTRAINT fk_enterprise_project_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE TABLE enterprise_document (
    id             UUID PRIMARY KEY,
    project_id     UUID NOT NULL,
    version        INTEGER NOT NULL,
    type           VARCHAR(32) NOT NULL,
    content        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    generated_by   VARCHAR(128) NOT NULL,
    engine_version VARCHAR(32) NOT NULL,
    input_hash     VARCHAR(128) NOT NULL,
    metadata_json  TEXT,
    checksum       VARCHAR(128),
    size           BIGINT NOT NULL,
    mime_type      VARCHAR(128),
    render_format  VARCHAR(32),

    CONSTRAINT fk_enterprise_document_project
        FOREIGN KEY (project_id, version)
        REFERENCES enterprise_project (project_id, version)
        ON DELETE CASCADE,

    CONSTRAINT uq_enterprise_document_type
        UNIQUE (project_id, version, type)
);

CREATE INDEX idx_enterprise_document_project
    ON enterprise_document (project_id, version);

-- ============================================================
-- FUNCTION: auto-update updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

-- ============================================================
-- TABLE: pricing_plans
-- ============================================================

CREATE TABLE pricing_plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    price           NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_period  VARCHAR(20) NOT NULL DEFAULT 'monthly',
    features        JSONB NOT NULL,
    is_popular      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pricing_plans_display_order ON pricing_plans (display_order);

-- FK users -> pricing_plans (V9). La columna current_plan_id vive en CREATE
-- TABLE users; la FK se declara aquí porque pricing_plans se crea después.
ALTER TABLE users ADD CONSTRAINT fk_users_current_plan
    FOREIGN KEY (current_plan_id)
    REFERENCES pricing_plans (id)
    ON DELETE SET NULL;

-- ============================================================
-- TABLE: user_subscriptions (V10 — sistema de suscripciones por plan)
-- ============================================================

CREATE TABLE user_subscriptions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    plan_id         UUID NOT NULL,
    start_date      TIMESTAMPTZ NOT NULL,
    end_date        TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'TRIAL')),
    messages_used   INTEGER NOT NULL DEFAULT 0,
    last_reset_date TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_subscriptions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_subscriptions_plan
        FOREIGN KEY (plan_id)
        REFERENCES pricing_plans (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_user_subscriptions_user_id ON user_subscriptions (user_id);
CREATE INDEX idx_user_subscriptions_user_status ON user_subscriptions (user_id, status);
CREATE INDEX idx_user_subscriptions_plan_id ON user_subscriptions (plan_id);

-- ============================================================
-- SEED: pricing plans
-- ============================================================

INSERT INTO pricing_plans (id, name, price, currency, billing_period, features, is_popular, display_order)
VALUES (
    uuid_generate_v4(),
    'Básico Gratis',
    0.00,
    'USD',
    'monthly',
    '["Hasta 3 proyectos", "Asistente de IA básico", "Scoring de viabilidad", "Exportación a PDF"]',
    FALSE,
    1
);

INSERT INTO pricing_plans (id, name, price, currency, billing_period, features, is_popular, display_order)
VALUES (
    uuid_generate_v4(),
    'Premium Pro',
    19.00,
    'USD',
    'monthly',
    '["Proyectos ilimitados", "IA avanzada con contexto extendido", "Scoring detallado con métricas", "Exportación PDF premium", "Soporte prioritario 24/7"]',
    TRUE,
    2
);

-- ============================================================
-- SEED: admin user (password: Admin123!)
-- BCrypt hash generated for: Admin123!
-- ============================================================

INSERT INTO users (id, email, password_hash, full_name, role, credits)
VALUES (
    uuid_generate_v4(),
    'admin@kinplatform.com',
    '$2a$12$LJ3m4ys3Lg3YOCwLg3YOCeX9kDOH5F5q5Z5q5Z5q5Z5q5Z5q5Z5qO',
    'Admin KIN',
    'ADMIN',
    999999
);

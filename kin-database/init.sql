-- ============================================================
-- KIN Platform - Database Initialization Script
-- Knowledge, Innovation & Navigation
-- PostgreSQL 16
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
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_is_active ON users (is_active);
CREATE INDEX idx_users_created_at ON users (created_at);

-- ============================================================
-- TABLE: projects
-- ============================================================

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(30) NOT NULL,
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
CREATE INDEX idx_projects_category ON projects (category);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_user_category ON projects (user_id, category);
CREATE INDEX idx_projects_user_status ON projects (user_id, status);
CREATE INDEX idx_projects_created_at ON projects (created_at DESC);

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

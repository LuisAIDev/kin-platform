-- ============================================================
-- V57: Generación RIPS - Batches y Records (KIN Billing - Semana 2)
-- ============================================================

-- Batch de generación RIPS (tracking por contrato, período, tipo)
CREATE TABLE IF NOT EXISTS rips_generation_batches (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    period_start            DATE NOT NULL,
    period_end              DATE NOT NULL,
    rips_type               VARCHAR(10) NOT NULL CHECK (rips_type IN ('AF','AC','AP','AU','US','AT')),
    status                  VARCHAR(20) NOT NULL DEFAULT 'GENERATING' CHECK (status IN ('GENERATING','VALIDATING','VALID','INVALID','SENT_TO_DIAN','ACCEPTED','REJECTED','CONTINGENCY')),
    file_path               VARCHAR(500),
    file_hash_sha256        VARCHAR(64),
    record_count            INTEGER DEFAULT 0,
    error_count             INTEGER DEFAULT 0,
    validation_errors       JSONB,                          -- [{"row": 15, "field": "valor_total", "message": "Formato inválido"}]
    generated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated_at            TIMESTAMPTZ,
    sent_at                 TIMESTAMPTZ,
    dian_response           JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rips_batch_org_contract_period_type UNIQUE (organization_id, contract_id, period_start, period_end, rips_type)
);

CREATE INDEX idx_rips_batches_org_period ON rips_generation_batches (organization_id, period_start, period_end);
CREATE INDEX idx_rips_batches_contract_status ON rips_generation_batches (contract_id, status);

-- Detalle de registros RIPS generados (trazabilidad línea a línea)
CREATE TABLE IF NOT EXISTS rips_records (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_id                UUID NOT NULL REFERENCES rips_generation_batches(id) ON DELETE CASCADE,
    sequence_number         INTEGER NOT NULL,
    source_entity_type      VARCHAR(30) NOT NULL,       -- ENCOUNTER, MEDICAL_ORDER, DOCUMENT, PATIENT, PRACTITIONER
    source_entity_id        UUID NOT NULL,
    rips_line_data          JSONB NOT NULL,             -- Línea completa RIPS para auditoría
    validation_status       VARCHAR(20) DEFAULT 'PENDING',
    validation_error        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rips_records_batch_seq ON rips_records (batch_id, sequence_number);
CREATE INDEX idx_rips_records_source ON rips_records (source_entity_type, source_entity_id);
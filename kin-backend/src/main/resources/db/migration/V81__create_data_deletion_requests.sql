-- V81: Data deletion requests for GDPR/Ley 1581 Right of Erasure
-- Art. 8 Ley 1581 de 2012 - Derecho de suprimir
-- IMPORTANTE: HCE NO se elimina (retención 20 años Res 839/1995)

CREATE TABLE data_deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'FULL'
        CHECK (scope IN ('FULL', 'PARTIAL')),
    data_categories JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXECUTED', 'PARTIALLY_EXECUTED')),
    legal_hold BOOLEAN DEFAULT FALSE,
    legal_hold_reason TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    executed_at TIMESTAMPTZ,
    execution_notes TEXT,
    anonymized_count INTEGER DEFAULT 0,
    deleted_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_deletion_user ON data_deletion_requests(user_id);
CREATE INDEX idx_deletion_status ON data_deletion_requests(status);

COMMENT ON TABLE data_deletion_requests IS 'Solicitudes de eliminación (Ley 1581 Art. 8). HCE protegida por Res 839/1995.';
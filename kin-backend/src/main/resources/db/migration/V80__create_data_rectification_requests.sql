-- V80: Data rectification requests for GDPR/Ley 1581 Right of Rectification
-- Art. 8 Ley 1581 de 2012 - Derecho de actualizar y rectificar

CREATE TABLE data_rectification_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    field_path VARCHAR(200) NOT NULL,
    old_value TEXT,
    new_value TEXT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXECUTED')),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    executed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_rectification_user ON data_rectification_requests(user_id);
CREATE INDEX idx_rectification_status ON data_rectification_requests(status);

COMMENT ON TABLE data_rectification_requests IS 'Solicitudes de rectificación (Ley 1581 Art. 8)';
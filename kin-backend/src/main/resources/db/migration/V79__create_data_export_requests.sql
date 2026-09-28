-- V79: Data export requests for GDPR/Ley 1581 Right of Access
-- Art. 8 Ley 1581 de 2012 - Derecho de conocer, actualizar y rectificar

CREATE TABLE data_export_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'EXPIRED')),
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    download_count INTEGER DEFAULT 0,
    requested_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_export_requests_user ON data_export_requests(user_id);
CREATE INDEX idx_export_requests_status ON data_export_requests(status);
CREATE INDEX idx_export_requests_expires ON data_export_requests(expires_at);

COMMENT ON TABLE data_export_requests IS 'Solicitudes de exportación de datos (Ley 1581 Art. 8 - Derecho de Acceso)';
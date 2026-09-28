-- V77: Create user_consents table for Ley 1581 consent management
CREATE TABLE user_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    accepted BOOLEAN NOT NULL DEFAULT FALSE,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, consent_type, version)
);

CREATE INDEX idx_user_consents_user_id ON user_consents(user_id);
CREATE INDEX idx_user_consents_type ON user_consents(consent_type);
CREATE INDEX idx_user_consents_accepted ON user_consents(accepted);

-- Insert initial consent types
INSERT INTO user_consents (user_id, consent_type, version, accepted, accepted_at, created_at)
SELECT u.id, 'TERMS_OF_SERVICE', '1.0', FALSE, NULL, NOW()
FROM users u
WHERE u.is_active = TRUE
ON CONFLICT (user_id, consent_type, version) DO NOTHING;

INSERT INTO user_consents (user_id, consent_type, version, accepted, accepted_at, created_at)
SELECT u.id, 'PRIVACY_POLICY', '1.0', FALSE, NULL, NOW()
FROM users u
WHERE u.is_active = TRUE
ON CONFLICT (user_id, consent_type, version) DO NOTHING;

INSERT INTO user_consents (user_id, consent_type, version, accepted, accepted_at, created_at)
SELECT u.id, 'HEALTH_DATA', '1.0', u.health_data_consent, 
       CASE WHEN u.health_data_consent THEN u.updated_at ELSE NULL END, NOW()
FROM users u
WHERE u.is_active = TRUE
ON CONFLICT (user_id, consent_type, version) DO NOTHING;

INSERT INTO user_consents (user_id, consent_type, version, accepted, accepted_at, created_at)
SELECT u.id, 'MARKETING', '1.0', FALSE, NULL, NOW()
FROM users u
WHERE u.is_active = TRUE
ON CONFLICT (user_id, consent_type, version) DO NOTHING;

INSERT INTO user_consents (user_id, consent_type, version, accepted, accepted_at, created_at)
SELECT u.id, 'DATA_SHARING', '1.0', FALSE, NULL, NOW()
FROM users u
WHERE u.is_active = TRUE
ON CONFLICT (user_id, consent_type, version) DO NOTHING;
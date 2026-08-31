CREATE TABLE IF NOT EXISTS automation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    trigger_event VARCHAR(50) NOT NULL,
    conditions JSONB NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_params JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rule_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL,
    event_id UUID NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    executed BOOLEAN NOT NULL DEFAULT false,
    error TEXT,
    details JSONB
);

CREATE INDEX IF NOT EXISTS idx_automation_rules_physician ON automation_rules (created_by);
CREATE INDEX IF NOT EXISTS idx_automation_rules_event ON automation_rules (trigger_event, enabled);
CREATE INDEX IF NOT EXISTS idx_exec_log_rule ON rule_execution_logs (rule_id);
CREATE INDEX IF NOT EXISTS idx_exec_log_event ON rule_execution_logs (event_id);
CREATE INDEX IF NOT EXISTS idx_exec_log_executed ON rule_execution_logs (executed, triggered_at DESC);
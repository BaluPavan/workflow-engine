CREATE TABLE workflow_definitions (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE workflow_step_configs (
    id UUID PRIMARY KEY,
    workflow_def_id UUID NOT NULL REFERENCES workflow_definitions(id),
    step_name VARCHAR(255) NOT NULL,
    step_order INTEGER NOT NULL,
    max_retries INTEGER NOT NULL DEFAULT 3,
    is_critical BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_seconds INTEGER NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workflow_step_order UNIQUE (workflow_def_id, step_order)
);

CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY,
    workflow_def_id UUID NOT NULL REFERENCES workflow_definitions(id),
    current_step_id UUID REFERENCES workflow_step_configs(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    context JSONB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);

CREATE TABLE workflow_step_instances (
    id UUID PRIMARY KEY,
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
    workflow_step_config_id UUID NOT NULL REFERENCES workflow_step_configs(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    failure_reason TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_step_instances_instance ON workflow_step_instances(workflow_instance_id);

CREATE TABLE dlq_task_results (
    id UUID PRIMARY KEY,
    workflow_instance_id UUID,
    step_instance_id UUID,
    step_name VARCHAR(255),
    raw_payload TEXT NOT NULL,
    error_reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNRESOLVED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX idx_dlq_status ON dlq_task_results (status);

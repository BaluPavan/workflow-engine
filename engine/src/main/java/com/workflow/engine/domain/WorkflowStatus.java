package com.workflow.engine.domain;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    RETRYING,
    FAILED,
    COMPLETED,
    PAUSED
}

package com.workflow.engine.domain;

public enum StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING,
    EXHAUSTED,
    SKIPPED
}

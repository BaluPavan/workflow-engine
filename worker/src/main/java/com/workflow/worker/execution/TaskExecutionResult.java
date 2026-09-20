package com.workflow.worker.execution;

/** Result returned by a worker-side step handler before it is published to Kafka. */
public record TaskExecutionResult(boolean success, String failureReason) {

    public static TaskExecutionResult succeeded() {
        return new TaskExecutionResult(true, null);
    }

    public static TaskExecutionResult failed(String failureReason) {
        return new TaskExecutionResult(false, failureReason);
    }
}

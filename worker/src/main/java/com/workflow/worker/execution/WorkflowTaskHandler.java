package com.workflow.worker.execution;

import com.workflow.worker.kafka.dto.TaskAssignedMessage;

/**
 * Extension point for real step integrations. Production handlers can support
 * a named step and call a payment, inventory, or shipping service.
 */
public interface WorkflowTaskHandler {
    boolean supports(String stepName);

    TaskExecutionResult execute(TaskAssignedMessage task) throws InterruptedException;
}

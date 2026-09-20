package com.workflow.worker.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.worker.config.WorkerProperties;
import com.workflow.worker.kafka.dto.TaskAssignedMessage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedTaskHandlerTest {
    private final SimulatedTaskHandler handler = new SimulatedTaskHandler(new ObjectMapper(), properties());

    @Test
    void succeedsWhenNoFailureIsRequested() throws InterruptedException {
        TaskExecutionResult result = handler.execute(task("validate_order", 0, "{}"));

        assertTrue(result.success());
    }

    @Test
    void failsOnlyTheFirstRequestedAttempt() throws InterruptedException {
        TaskExecutionResult firstAttempt = handler.execute(task("charge_payment", 0,
                "{\"simulateFailure\":\"charge_payment\"}"));
        TaskExecutionResult retry = handler.execute(task("charge_payment", 1,
                "{\"simulateFailure\":\"charge_payment\"}"));

        assertFalse(firstAttempt.success());
        assertTrue(retry.success());
    }

    private static WorkerProperties properties() {
        WorkerProperties properties = new WorkerProperties();
        properties.setProcessingDelayMs(0);
        return properties;
    }

    private static TaskAssignedMessage task(String stepName, int retryCount, String context) {
        return TaskAssignedMessage.builder()
                .workflowInstanceId(UUID.randomUUID())
                .stepInstanceId(UUID.randomUUID())
                .stepName(stepName)
                .retryCount(retryCount)
                .context(context)
                .build();
    }
}

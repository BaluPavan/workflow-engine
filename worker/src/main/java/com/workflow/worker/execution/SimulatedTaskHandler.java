package com.workflow.worker.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.worker.config.WorkerProperties;
import com.workflow.worker.kafka.dto.TaskAssignedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Default local handler used by the demo. Replace or supplement it with real integrations. */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class SimulatedTaskHandler implements WorkflowTaskHandler {
    private final ObjectMapper objectMapper;
    private final WorkerProperties workerProperties;

    @Override
    public boolean supports(String stepName) {
        return true;
    }

    @Override
    public TaskExecutionResult execute(TaskAssignedMessage task) throws InterruptedException {
        Thread.sleep(workerProperties.getProcessingDelayMs());
        if (shouldSimulateFailure(task)) {
            return TaskExecutionResult.failed("Simulated failure for step " + task.getStepName());
        }
        return TaskExecutionResult.succeeded();
    }

    private boolean shouldSimulateFailure(TaskAssignedMessage task) {
        try {
            JsonNode context = objectMapper.readTree(task.getContext());
            JsonNode simulateFailure = context.get("simulateFailure");
            return simulateFailure != null
                    && !simulateFailure.isNull()
                    && task.getStepName().equals(simulateFailure.asText())
                    && task.getRetryCount() == 0;
        } catch (Exception exception) {
            log.warn("Unable to parse workflow context, treating step as successful", exception);
            return false;
        }
    }
}

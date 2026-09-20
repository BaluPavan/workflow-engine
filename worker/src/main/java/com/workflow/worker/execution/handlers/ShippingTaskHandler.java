package com.workflow.worker.execution.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.worker.config.WorkerProperties;
import com.workflow.worker.execution.TaskExecutionResult;
import com.workflow.worker.execution.WorkflowTaskHandler;
import com.workflow.worker.kafka.dto.TaskAssignedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ShippingTaskHandler implements WorkflowTaskHandler {

    private final ObjectMapper objectMapper;
    private final WorkerProperties workerProperties;

    @Override
    public boolean supports(String stepName) {
        return "ship_order".equalsIgnoreCase(stepName);
    }

    @Override
    public TaskExecutionResult execute(TaskAssignedMessage task) throws InterruptedException {
        log.info("Dispatching Shipping Order for workflow {}", task.getWorkflowInstanceId());
        Thread.sleep(workerProperties.getProcessingDelayMs());

        try {
            JsonNode context = objectMapper.readTree(task.getContext());
            JsonNode simulateFailure = context.get("simulateFailure");
            if (simulateFailure != null && "ship_order".equals(simulateFailure.asText()) && task.getRetryCount() == 0) {
                return TaskExecutionResult.failed("Carrier integration error");
            }
        } catch (Exception ex) {
            log.warn("Error parsing context in ShippingTaskHandler", ex);
        }

        return TaskExecutionResult.succeeded();
    }
}

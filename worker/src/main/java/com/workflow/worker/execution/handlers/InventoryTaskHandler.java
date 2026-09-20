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
public class InventoryTaskHandler implements WorkflowTaskHandler {

    private final ObjectMapper objectMapper;
    private final WorkerProperties workerProperties;

    @Override
    public boolean supports(String stepName) {
        return "reserve_inventory".equalsIgnoreCase(stepName);
    }

    @Override
    public TaskExecutionResult execute(TaskAssignedMessage task) throws InterruptedException {
        log.info("Reserving Warehouse Inventory for workflow {}", task.getWorkflowInstanceId());
        Thread.sleep(workerProperties.getProcessingDelayMs());

        try {
            JsonNode context = objectMapper.readTree(task.getContext());
            JsonNode simulateFailure = context.get("simulateFailure");
            if (simulateFailure != null && "reserve_inventory".equals(simulateFailure.asText()) && task.getRetryCount() == 0) {
                return TaskExecutionResult.failed("Warehouse stock reservation unavailable");
            }
        } catch (Exception ex) {
            log.warn("Error parsing context in InventoryTaskHandler", ex);
        }

        return TaskExecutionResult.succeeded();
    }
}

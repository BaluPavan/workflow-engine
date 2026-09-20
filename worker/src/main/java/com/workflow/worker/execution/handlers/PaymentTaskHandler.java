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
public class PaymentTaskHandler implements WorkflowTaskHandler {

    private final ObjectMapper objectMapper;
    private final WorkerProperties workerProperties;

    @Override
    public boolean supports(String stepName) {
        return "charge_payment".equalsIgnoreCase(stepName);
    }

    @Override
    public TaskExecutionResult execute(TaskAssignedMessage task) throws InterruptedException {
        log.info("Processing Payment Gateway charge for workflow {}", task.getWorkflowInstanceId());
        Thread.sleep(workerProperties.getProcessingDelayMs());

        try {
            JsonNode context = objectMapper.readTree(task.getContext());
            JsonNode simulateFailure = context.get("simulateFailure");
            if (simulateFailure != null && "charge_payment".equals(simulateFailure.asText()) && task.getRetryCount() == 0) {
                return TaskExecutionResult.failed("Payment gateway timeout on attempt 1");
            }
        } catch (Exception ex) {
            log.warn("Error parsing context in PaymentTaskHandler", ex);
        }

        return TaskExecutionResult.succeeded();
    }
}

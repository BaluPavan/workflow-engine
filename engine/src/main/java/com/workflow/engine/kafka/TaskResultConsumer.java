package com.workflow.engine.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.kafka.dto.TaskResultMessage;
import com.workflow.engine.service.DlqService;
import com.workflow.engine.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultConsumer {

    private final ObjectMapper objectMapper;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final DlqService dlqService;

    @KafkaListener(topics = "${workflow.kafka.topic.task-result}", groupId = "workflow-engine")
    public void consumeTaskResult(String payload) {
        TaskResultMessage message = null;
        try {
            message = objectMapper.readValue(payload, TaskResultMessage.class);
            MDC.put("workflowInstanceId", message.getWorkflowInstanceId().toString());
            MDC.put("stepInstanceId", message.getStepInstanceId().toString());
            MDC.put("stepName", message.getStepName());

            log.info("Received task result success={} for step {}", message.isSuccess(), message.getStepName());
            workflowOrchestrator.handleTaskResult(message);
        } catch (Exception e) {
            log.error("Failed to process task result message, routing to DLQ", e);
            UUID workflowId = message != null ? message.getWorkflowInstanceId() : null;
            UUID stepId = message != null ? message.getStepInstanceId() : null;
            String stepName = message != null ? message.getStepName() : "unknown";
            dlqService.saveToDlq(payload, e.getMessage(), workflowId, stepId, stepName);
        } finally {
            MDC.clear();
        }
    }
}

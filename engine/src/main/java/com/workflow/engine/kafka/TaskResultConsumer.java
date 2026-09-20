package com.workflow.engine.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.kafka.dto.TaskResultMessage;
import com.workflow.engine.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultConsumer {

    private final ObjectMapper objectMapper;
    private final WorkflowOrchestrator workflowOrchestrator;

    @KafkaListener(topics = "${workflow.kafka.topic.task-result}", groupId = "workflow-engine")
    public void consumeTaskResult(String payload) {
        try {
            TaskResultMessage message = objectMapper.readValue(payload, TaskResultMessage.class);
            MDC.put("workflowInstanceId", message.getWorkflowInstanceId().toString());
            MDC.put("stepInstanceId", message.getStepInstanceId().toString());
            MDC.put("stepName", message.getStepName());

            log.info("Received task result success={} for step {}", message.isSuccess(), message.getStepName());
            workflowOrchestrator.handleTaskResult(message);
        } catch (Exception e) {
            log.error("Failed to process task result message", e);
            throw new IllegalStateException("Failed to process task result message", e);
        } finally {
            MDC.clear();
        }
    }
}

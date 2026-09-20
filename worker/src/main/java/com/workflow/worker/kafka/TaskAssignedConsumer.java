package com.workflow.worker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.worker.execution.TaskExecutionResult;
import com.workflow.worker.execution.WorkflowTaskHandler;
import com.workflow.worker.kafka.dto.TaskAssignedMessage;
import com.workflow.worker.kafka.dto.TaskResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssignedConsumer {

    private final ObjectMapper objectMapper;
    private final TaskResultPublisher taskResultPublisher;
    private final List<WorkflowTaskHandler> taskHandlers;

    @KafkaListener(topics = "${workflow.kafka.topic.task-assigned}", groupId = "workflow-worker")
    public void consumeTaskAssigned(String payload) throws InterruptedException {
        TaskAssignedMessage message = parseMessage(payload);
        log.info("Processing step {} for workflow {}", message.getStepName(), message.getWorkflowInstanceId());

        TaskExecutionResult execution = taskHandlers.stream()
                .filter(handler -> handler.supports(message.getStepName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler registered for step " + message.getStepName()))
                .execute(message);

        TaskResultMessage result = TaskResultMessage.builder()
                .idempotencyKey(message.getIdempotencyKey())
                .workflowInstanceId(message.getWorkflowInstanceId())
                .stepInstanceId(message.getStepInstanceId())
                .stepName(message.getStepName())
                .retryCount(message.getRetryCount())
                .success(execution.success())
                .failureReason(execution.failureReason())
                .build();

        taskResultPublisher.publishTaskResult(result);
    }

    private TaskAssignedMessage parseMessage(String payload) {
        try {
            return objectMapper.readValue(payload, TaskAssignedMessage.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse task assigned message", e);
        }
    }

}

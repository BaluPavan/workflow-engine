package com.workflow.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.config.WorkflowProperties;
import com.workflow.engine.domain.OutboxEvent;
import com.workflow.engine.kafka.dto.TaskAssignedMessage;
import com.workflow.engine.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Persists work to be delivered in the same transaction as workflow state. */
@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowProperties workflowProperties;

    public void enqueueTaskAssigned(TaskAssignedMessage message) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setTopic(workflowProperties.getKafka().getTopic().getTaskAssigned());
            event.setPartitionKey(message.getWorkflowInstanceId().toString());
            event.setPayload(objectMapper.writeValueAsString(message));
            outboxEventRepository.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize task assignment for outbox", exception);
        }
    }
}

package com.workflow.worker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.worker.config.WorkerProperties;
import com.workflow.worker.kafka.dto.TaskResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final WorkerProperties workerProperties;

    public void publishTaskResult(TaskResultMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(workerProperties.getKafka().getTopic().getTaskResult(),
                    message.getStepInstanceId().toString(), payload);
            log.info("Published task result success={} for step {}", message.isSuccess(), message.getStepName());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish task result", e);
        }
    }
}

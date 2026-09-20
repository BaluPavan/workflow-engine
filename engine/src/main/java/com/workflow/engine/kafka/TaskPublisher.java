package com.workflow.engine.kafka;

import com.workflow.engine.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(OutboxEvent event) {
        // Mark an event as delivered only after the broker acknowledges the write.
        kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload()).join();
        log.info("Published outbox event {} to {}", event.getId(), event.getTopic());
    }
}

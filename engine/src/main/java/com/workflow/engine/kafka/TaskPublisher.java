package com.workflow.engine.kafka;

import com.workflow.engine.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> publishAsync(OutboxEvent event) {
        return kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish outbox event {} to topic {}", event.getId(), event.getTopic(), ex);
                    } else {
                        log.info("Successfully published outbox event {} to topic {} partition {}",
                                event.getId(), event.getTopic(), result.getRecordMetadata().partition());
                    }
                });
    }

    public void publish(OutboxEvent event) {
        publishAsync(event).join();
    }
}

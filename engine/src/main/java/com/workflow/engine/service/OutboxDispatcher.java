package com.workflow.engine.service;

import com.workflow.engine.domain.OutboxEvent;
import com.workflow.engine.kafka.TaskPublisher;
import com.workflow.engine.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/** Delivers committed outbox records. Unpublished records are safely retried. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "workflow.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private final OutboxEventRepository outboxEventRepository;
    private final TaskPublisher taskPublisher;

    @Scheduled(fixedDelayString = "${workflow.outbox.fixed-delay-ms:1000}")
    public void dispatchPendingEvents() {
        outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()
                .forEach(this::dispatchAsync);
    }

    public void dispatchAsync(OutboxEvent event) {
        if (event == null || event.getPublishedAt() != null) {
            return;
        }
        taskPublisher.publishAsync(event)
                .thenAccept(result -> markPublished(event.getId()))
                .exceptionally(ex -> {
                    log.warn("Outbox event {} delivery deferred: {}", event.getId(), ex.getMessage());
                    return null;
                });
    }

    public void dispatch(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getPublishedAt() != null) {
            return;
        }
        try {
            taskPublisher.publish(event);
            markPublished(eventId);
        } catch (RuntimeException exception) {
            log.warn("Outbox event {} could not be delivered and will be retried", eventId, exception);
        }
    }

    private void markPublished(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            if (event.getPublishedAt() == null) {
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            }
        });
    }
}

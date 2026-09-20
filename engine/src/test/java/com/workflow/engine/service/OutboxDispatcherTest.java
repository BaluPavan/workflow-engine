package com.workflow.engine.service;

import com.workflow.engine.domain.OutboxEvent;
import com.workflow.engine.kafka.TaskPublisher;
import com.workflow.engine.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {
    private final OutboxEventRepository events = mock(OutboxEventRepository.class);
    private final TaskPublisher publisher = mock(TaskPublisher.class);
    private final OutboxDispatcher dispatcher = new OutboxDispatcher(events, publisher);

    @Test
    void marksEventPublishedOnlyAfterKafkaDeliverySucceeds() {
        OutboxEvent event = event();
        when(events.findById(event.getId())).thenReturn(Optional.of(event));

        dispatcher.dispatch(event.getId());

        assertNotNull(event.getPublishedAt());
        verify(publisher).publish(event);
        verify(events).save(event);
    }

    @Test
    void leavesEventPendingWhenKafkaDeliveryFails() {
        OutboxEvent event = event();
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("Kafka unavailable")).when(publisher).publish(event);

        dispatcher.dispatch(event.getId());

        assertNull(event.getPublishedAt());
        verify(events, never()).save(event);
    }

    private static OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTopic("workflow.task.assigned");
        event.setPartitionKey("workflow-1");
        event.setPayload("{}");
        return event;
    }
}

package com.workflow.engine.service;

import com.workflow.engine.config.WorkflowProperties;
import com.workflow.engine.domain.StepStatus;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.domain.WorkflowInstance;
import com.workflow.engine.domain.WorkflowStatus;
import com.workflow.engine.domain.WorkflowStepConfig;
import com.workflow.engine.domain.WorkflowStepInstance;
import com.workflow.engine.kafka.dto.TaskResultMessage;
import com.workflow.engine.repository.WorkflowDefinitionRepository;
import com.workflow.engine.repository.WorkflowInstanceRepository;
import com.workflow.engine.repository.WorkflowStepConfigRepository;
import com.workflow.engine.repository.WorkflowStepInstanceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowOrchestratorTest {
    private final WorkflowDefinitionRepository definitions = mock(WorkflowDefinitionRepository.class);
    private final WorkflowStepConfigRepository configs = mock(WorkflowStepConfigRepository.class);
    private final WorkflowInstanceRepository instances = mock(WorkflowInstanceRepository.class);
    private final WorkflowStepInstanceRepository steps = mock(WorkflowStepInstanceRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(
            definitions, configs, instances, steps, outbox, new WorkflowProperties(), null);

    @Test
    void completesWorkflowAfterLastSuccessfulStep() {
        Fixture fixture = fixture(true, 0, 0);
        when(steps.findById(fixture.step.getId())).thenReturn(Optional.of(fixture.step));
        when(configs.findByWorkflowDefinitionOrderByStepOrderAsc(fixture.definition)).thenReturn(List.of(fixture.config));

        orchestrator.handleTaskResult(result(fixture, true));

        assertEquals(StepStatus.COMPLETED, fixture.step.getStatus());
        assertEquals(WorkflowStatus.COMPLETED, fixture.instance.getStatus());
        assertNotNull(fixture.step.getCompletedAt());
        verify(instances).save(fixture.instance);
    }

    @Test
    void skipsNonCriticalStepAndCompletesWhenItIsLast() {
        Fixture fixture = fixture(false, 0, 0);
        when(steps.findById(fixture.step.getId())).thenReturn(Optional.of(fixture.step));
        when(configs.findByWorkflowDefinitionOrderByStepOrderAsc(fixture.definition)).thenReturn(List.of(fixture.config));

        orchestrator.handleTaskResult(result(fixture, false));

        assertEquals(StepStatus.SKIPPED, fixture.step.getStatus());
        assertEquals(WorkflowStatus.COMPLETED, fixture.instance.getStatus());
    }

    @Test
    void schedulesRetryForCriticalFailureWithinRetryBudget() {
        Fixture fixture = fixture(true, 2, 0);
        when(steps.findById(fixture.step.getId())).thenReturn(Optional.of(fixture.step));

        orchestrator.handleTaskResult(result(fixture, false));

        assertEquals(StepStatus.RETRYING, fixture.step.getStatus());
        assertEquals(1, fixture.step.getRetryCount());
        assertEquals(WorkflowStatus.RETRYING, fixture.instance.getStatus());
        assertNotNull(fixture.step.getNextRetryAt());
    }

    @Test
    void ignoresDuplicateResultForFinishedStep() {
        Fixture fixture = fixture(true, 0, 0);
        fixture.step.setStatus(StepStatus.COMPLETED);
        when(steps.findById(fixture.step.getId())).thenReturn(Optional.of(fixture.step));

        orchestrator.handleTaskResult(result(fixture, true));

        verify(steps, never()).save(fixture.step);
        verify(instances, never()).save(fixture.instance);
    }

    private static TaskResultMessage result(Fixture fixture, boolean success) {
        return TaskResultMessage.builder()
                .idempotencyKey("attempt")
                .workflowInstanceId(fixture.instance.getId())
                .stepInstanceId(fixture.step.getId())
                .stepName(fixture.config.getStepName())
                .success(success)
                .failureReason("provider rejected request")
                .build();
    }

    private static Fixture fixture(boolean critical, int maxRetries, int retryCount) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(UUID.randomUUID());
        WorkflowStepConfig config = new WorkflowStepConfig();
        config.setId(UUID.randomUUID());
        config.setWorkflowDefinition(definition);
        config.setStepName("charge_payment");
        config.setStepOrder(1);
        config.setCritical(critical);
        config.setMaxRetries(maxRetries);
        config.setTimeoutSeconds(30);
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(UUID.randomUUID());
        instance.setWorkflowDefinition(definition);
        instance.setCurrentStep(config);
        instance.setStatus(WorkflowStatus.RUNNING);
        WorkflowStepInstance step = new WorkflowStepInstance();
        step.setId(UUID.randomUUID());
        step.setWorkflowInstance(instance);
        step.setWorkflowStepConfig(config);
        step.setStatus(StepStatus.RUNNING);
        step.setRetryCount(retryCount);
        return new Fixture(definition, config, instance, step);
    }

    private record Fixture(WorkflowDefinition definition, WorkflowStepConfig config,
                           WorkflowInstance instance, WorkflowStepInstance step) { }
}

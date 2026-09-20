package com.workflow.engine.service;

import com.workflow.engine.api.dto.RegisterWorkflowRequest;
import com.workflow.engine.api.dto.StepRequest;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.repository.WorkflowDefinitionRepository;
import com.workflow.engine.repository.WorkflowStepConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowServiceTest {
    private final WorkflowDefinitionRepository definitions = mock(WorkflowDefinitionRepository.class);
    private final WorkflowStepConfigRepository steps = mock(WorkflowStepConfigRepository.class);
    private final WorkflowService service = new WorkflowService(definitions, steps);

    @Test
    void rejectsExistingWorkflowName() {
        RegisterWorkflowRequest request = request(step("validate", 1));
        when(definitions.findByName("checkout")).thenReturn(Optional.of(new WorkflowDefinition()));

        assertThrows(IllegalArgumentException.class, () -> service.registerWorkFlowDefinition(request));

        verify(definitions, never()).save(any());
    }

    @Test
    void rejectsDuplicateStepOrderBeforePersistingDefinition() {
        RegisterWorkflowRequest request = request(step("validate", 1), step("charge", 1));
        when(definitions.findByName("checkout")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.registerWorkFlowDefinition(request));

        verify(definitions, never()).save(any());
    }

    @Test
    void persistsValidWorkflowAndEveryStep() {
        RegisterWorkflowRequest request = request(step("validate", 1), step("charge", 2));
        when(definitions.findByName("checkout")).thenReturn(Optional.empty());

        service.registerWorkFlowDefinition(request);

        verify(definitions).save(any(WorkflowDefinition.class));
        verify(steps, org.mockito.Mockito.times(2)).save(any());
    }

    private static RegisterWorkflowRequest request(StepRequest... steps) {
        RegisterWorkflowRequest request = new RegisterWorkflowRequest();
        request.setName("checkout");
        request.setSteps(List.of(steps));
        return request;
    }

    private static StepRequest step(String name, int order) {
        StepRequest step = new StepRequest();
        step.setStepName(name);
        step.setStepOrder(order);
        step.setTimeoutSeconds(30);
        return step;
    }
}

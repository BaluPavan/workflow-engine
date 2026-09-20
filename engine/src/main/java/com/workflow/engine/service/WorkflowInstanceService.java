package com.workflow.engine.service;

import com.workflow.engine.api.dto.StepInstanceSummary;
import com.workflow.engine.api.dto.WorkflowInstanceResponse;
import com.workflow.engine.api.dto.WorkflowInstanceSummary;
import com.workflow.engine.domain.WorkflowInstance;
import com.workflow.engine.domain.WorkflowStepInstance;
import com.workflow.engine.repository.WorkflowInstanceRepository;
import com.workflow.engine.repository.WorkflowStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowInstanceService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;
    private final WorkflowOrchestrator workflowOrchestrator;

    @Transactional
    public WorkflowInstance startInstance(String workflowName, String context) {
        return workflowOrchestrator.startWorkflow(workflowName, context);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstance(UUID instanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));

        List<WorkflowStepInstance> stepInstances = workflowStepInstanceRepository.findByWorkflowInstance(instance);
        stepInstances.sort(Comparator.comparingInt(step -> step.getWorkflowStepConfig().getStepOrder()));

        List<StepInstanceSummary> steps = stepInstances.stream()
                .map(step -> StepInstanceSummary.builder()
                        .id(step.getId())
                        .stepName(step.getWorkflowStepConfig().getStepName())
                        .stepOrder(step.getWorkflowStepConfig().getStepOrder())
                        .status(step.getStatus())
                        .retryCount(step.getRetryCount())
                        .failureReason(step.getFailureReason())
                        .startedAt(step.getStartedAt())
                        .completedAt(step.getCompletedAt())
                        .build())
                .toList();

        String currentStepName = instance.getCurrentStep() != null
                ? instance.getCurrentStep().getStepName()
                : null;

        return WorkflowInstanceResponse.builder()
                .id(instance.getId())
                .workflowName(instance.getWorkflowDefinition().getName())
                .status(instance.getStatus())
                .currentStepName(currentStepName)
                .context(instance.getContext())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .failedAt(instance.getFailedAt())
                .steps(steps)
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkflowInstanceSummary> listInstances() {
        return workflowInstanceRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(instance -> WorkflowInstanceSummary.builder()
                        .id(instance.getId())
                        .workflowName(instance.getWorkflowDefinition().getName())
                        .status(instance.getStatus())
                        .currentStepName(instance.getCurrentStep() != null
                                ? instance.getCurrentStep().getStepName()
                                : null)
                        .startedAt(instance.getStartedAt())
                        .completedAt(instance.getCompletedAt())
                        .createdAt(instance.getCreatedAt())
                        .build())
                .toList();
    }
}

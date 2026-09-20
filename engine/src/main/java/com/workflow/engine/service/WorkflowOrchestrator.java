package com.workflow.engine.service;

import com.workflow.engine.api.dto.StepInstanceSummary;
import com.workflow.engine.api.dto.WorkflowInstanceResponse;
import com.workflow.engine.config.WorkflowProperties;
import com.workflow.engine.domain.*;
import com.workflow.engine.kafka.dto.TaskAssignedMessage;
import com.workflow.engine.kafka.dto.TaskResultMessage;
import com.workflow.engine.repository.WorkflowDefinitionRepository;
import com.workflow.engine.repository.WorkflowInstanceRepository;
import com.workflow.engine.repository.WorkflowStepConfigRepository;
import com.workflow.engine.repository.WorkflowStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepConfigRepository workflowStepConfigRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;
    private final OutboxService outboxService;
    private final WorkflowProperties workflowProperties;

    @Transactional
    public WorkflowInstance startWorkflow(String workflowName, String context) {
        WorkflowDefinition definition = workflowDefinitionRepository.findByName(workflowName)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowName));

        List<WorkflowStepConfig> stepConfigs = workflowStepConfigRepository
                .findByWorkflowDefinitionOrderByStepOrderAsc(definition);
        if (stepConfigs.isEmpty()) {
            throw new IllegalArgumentException("Workflow has no steps: " + workflowName);
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowDefinition(definition);
        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setContext(context != null ? context : "{}");
        instance.setStartedAt(LocalDateTime.now());
        instance.setCurrentStep(stepConfigs.get(0));
        workflowInstanceRepository.save(instance);

        for (WorkflowStepConfig stepConfig : stepConfigs) {
            WorkflowStepInstance stepInstance = new WorkflowStepInstance();
            stepInstance.setWorkflowInstance(instance);
            stepInstance.setWorkflowStepConfig(stepConfig);
            stepInstance.setStatus(StepStatus.PENDING);
            workflowStepInstanceRepository.save(stepInstance);
        }

        dispatchStep(instance, stepConfigs.get(0));
        return instance;
    }

    @Transactional
    public void handleTaskResult(TaskResultMessage message) {
        WorkflowStepInstance stepInstance = workflowStepInstanceRepository.findById(message.getStepInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Step instance not found: " + message.getStepInstanceId()));

        if (stepInstance.getStatus() != StepStatus.RUNNING) {
            log.warn("Ignoring duplicate result for step {} in status {}", stepInstance.getId(), stepInstance.getStatus());
            return;
        }

        WorkflowInstance instance = stepInstance.getWorkflowInstance();
        WorkflowStepConfig stepConfig = stepInstance.getWorkflowStepConfig();

        if (message.isSuccess()) {
            completeStep(stepInstance);
            advanceWorkflow(instance, stepConfig);
            return;
        }

        handleStepFailure(instance, stepInstance, stepConfig, message.getFailureReason());
    }

    @Transactional
    public void processDueRetries() {
        List<WorkflowStepInstance> dueSteps = workflowStepInstanceRepository
                .findStepsDueForRetry(LocalDateTime.now());
        for (WorkflowStepInstance stepInstance : dueSteps) {
            if (stepInstance.getStatus() != StepStatus.RETRYING) {
                continue;
            }
            WorkflowInstance instance = stepInstance.getWorkflowInstance();
            if (instance.getStatus() != WorkflowStatus.RUNNING && instance.getStatus() != WorkflowStatus.RETRYING) {
                continue;
            }
            dispatchExistingStep(instance, stepInstance);
        }
    }

    @Transactional
    public void processTimeouts() {
        List<WorkflowStepInstance> runningSteps = workflowStepInstanceRepository.findByStatus(StepStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();

        for (WorkflowStepInstance stepInstance : runningSteps) {
            WorkflowStepConfig stepConfig = stepInstance.getWorkflowStepConfig();
            if (stepInstance.getStartedAt() == null) {
                continue;
            }
            LocalDateTime deadline = stepInstance.getStartedAt().plusSeconds(stepConfig.getTimeoutSeconds());
            if (now.isAfter(deadline)) {
                handleStepFailure(
                        stepInstance.getWorkflowInstance(),
                        stepInstance,
                        stepConfig,
                        "Step timed out after " + stepConfig.getTimeoutSeconds() + " seconds"
                );
            }
        }
    }

    private void completeStep(WorkflowStepInstance stepInstance) {
        stepInstance.setStatus(StepStatus.COMPLETED);
        stepInstance.setCompletedAt(LocalDateTime.now());
        workflowStepInstanceRepository.save(stepInstance);
    }

    private void advanceWorkflow(WorkflowInstance instance, WorkflowStepConfig completedStepConfig) {
        List<WorkflowStepConfig> stepConfigs = workflowStepConfigRepository
                .findByWorkflowDefinitionOrderByStepOrderAsc(instance.getWorkflowDefinition());

        WorkflowStepConfig nextStepConfig = stepConfigs.stream()
                .filter(config -> config.getStepOrder() > completedStepConfig.getStepOrder())
                .min(Comparator.comparingInt(WorkflowStepConfig::getStepOrder))
                .orElse(null);

        if (nextStepConfig == null) {
            instance.setStatus(WorkflowStatus.COMPLETED);
            instance.setCompletedAt(LocalDateTime.now());
            instance.setCurrentStep(null);
            workflowInstanceRepository.save(instance);
            log.info("Workflow instance {} completed", instance.getId());
            return;
        }

        instance.setCurrentStep(nextStepConfig);
        instance.setStatus(WorkflowStatus.RUNNING);
        workflowInstanceRepository.save(instance);
        dispatchStep(instance, nextStepConfig);
    }

    private void handleStepFailure(WorkflowInstance instance,
                                   WorkflowStepInstance stepInstance,
                                   WorkflowStepConfig stepConfig,
                                   String failureReason) {
        if (!stepConfig.isCritical()) {
            stepInstance.setStatus(StepStatus.SKIPPED);
            stepInstance.setFailureReason(failureReason);
            stepInstance.setCompletedAt(LocalDateTime.now());
            workflowStepInstanceRepository.save(stepInstance);
            log.info("Non-critical step {} skipped: {}", stepConfig.getStepName(), failureReason);
            advanceWorkflow(instance, stepConfig);
            return;
        }

        if (stepInstance.getRetryCount() < stepConfig.getMaxRetries()) {
            stepInstance.setStatus(StepStatus.RETRYING);
            stepInstance.setRetryCount(stepInstance.getRetryCount() + 1);
            stepInstance.setFailureReason(failureReason);
            stepInstance.setNextRetryAt(LocalDateTime.now().plusSeconds(workflowProperties.getRetryDelaySeconds()));
            stepInstance.setStartedAt(null);
            instance.setStatus(WorkflowStatus.RETRYING);
            workflowStepInstanceRepository.save(stepInstance);
            workflowInstanceRepository.save(instance);
            log.info("Step {} scheduled for retry {}/{}", stepConfig.getStepName(),
                    stepInstance.getRetryCount(), stepConfig.getMaxRetries());
            return;
        }

        stepInstance.setStatus(StepStatus.EXHAUSTED);
        stepInstance.setFailureReason(failureReason);
        stepInstance.setCompletedAt(LocalDateTime.now());
        workflowStepInstanceRepository.save(stepInstance);

        instance.setStatus(WorkflowStatus.FAILED);
        instance.setFailedAt(LocalDateTime.now());
        workflowInstanceRepository.save(instance);
        log.warn("Critical step {} exhausted retries for workflow {}", stepConfig.getStepName(), instance.getId());
    }

    private void dispatchStep(WorkflowInstance instance, WorkflowStepConfig stepConfig) {
        WorkflowStepInstance stepInstance = workflowStepInstanceRepository
                .findByWorkflowInstanceAndWorkflowStepConfig(instance, stepConfig)
                .orElseThrow(() -> new IllegalStateException("Step instance missing for " + stepConfig.getStepName()));
        dispatchExistingStep(instance, stepInstance);
    }

    private void dispatchExistingStep(WorkflowInstance instance, WorkflowStepInstance stepInstance) {
        WorkflowStepConfig stepConfig = stepInstance.getWorkflowStepConfig();

        stepInstance.setStatus(StepStatus.RUNNING);
        stepInstance.setStartedAt(LocalDateTime.now());
        stepInstance.setNextRetryAt(null);
        workflowStepInstanceRepository.save(stepInstance);

        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setCurrentStep(stepConfig);
        workflowInstanceRepository.save(instance);

        TaskAssignedMessage message = TaskAssignedMessage.builder()
                .idempotencyKey(buildIdempotencyKey(stepInstance))
                .workflowInstanceId(instance.getId())
                .stepInstanceId(stepInstance.getId())
                .stepName(stepConfig.getStepName())
                .retryCount(stepInstance.getRetryCount())
                .context(instance.getContext())
                .build();

        MDC.put("workflowInstanceId", instance.getId().toString());
        MDC.put("stepInstanceId", stepInstance.getId().toString());
        MDC.put("stepName", stepConfig.getStepName());
        try {
            outboxService.enqueueTaskAssigned(message);
        } finally {
            MDC.clear();
        }
    }

    private String buildIdempotencyKey(WorkflowStepInstance stepInstance) {
        return stepInstance.getId() + ":" + stepInstance.getRetryCount();
    }
}

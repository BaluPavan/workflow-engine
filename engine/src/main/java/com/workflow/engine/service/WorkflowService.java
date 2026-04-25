package com.workflow.engine.service;

import com.workflow.engine.api.dto.RegisterWorkflowRequest;
import com.workflow.engine.api.dto.StepRequest;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.domain.WorkflowStepConfig;
import com.workflow.engine.repository.WorkflowDefinitionRepository;
import com.workflow.engine.repository.WorkflowStepConfigRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowStepConfigRepository workflowStepConfigRepository;

    public Optional<WorkflowDefinition> getWorkflowDefinitionByName(String name) {
        return workflowDefinitionRepository.findByName(name);
    }

    @Transactional
    public WorkflowDefinition registerWorkFlowDefinition(RegisterWorkflowRequest request) {

        if(getWorkflowDefinitionByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Workflow already exists: " + request.getName());
        }

        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setName(request.getName());
        workflowDefinition.setDescription(request.getDescription());
        List<StepRequest> stepRequests = request.getSteps();

        workflowDefinitionRepository.save(workflowDefinition);

        request.getSteps().forEach(stepRequest -> {
            WorkflowStepConfig workflowStepConfig = getWorkflowStepConfig(stepRequest, workflowDefinition);
            workflowStepConfigRepository.save(workflowStepConfig);
        });

        return workflowDefinition;
    }

    private static @NonNull WorkflowStepConfig getWorkflowStepConfig(StepRequest stepRequest, WorkflowDefinition workflowDefinition) {
        WorkflowStepConfig workflowStepConfig = new WorkflowStepConfig();
        workflowStepConfig.setWorkflowDefinition(workflowDefinition);
        workflowStepConfig.setStepName(stepRequest.getStepName());
        workflowStepConfig.setStepOrder(stepRequest.getStepOrder());
        workflowStepConfig.setCritical(stepRequest.isCritical());
        workflowStepConfig.setMaxRetries(stepRequest.getMaxRetries());
        workflowStepConfig.setTimeoutSeconds(stepRequest.getTimeoutSeconds());
        return workflowStepConfig;
    }
}

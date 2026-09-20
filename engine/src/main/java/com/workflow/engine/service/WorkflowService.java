package com.workflow.engine.service;

import com.workflow.engine.api.dto.RegisterWorkflowRequest;
import com.workflow.engine.api.dto.StepRequest;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.domain.WorkflowStepConfig;
import com.workflow.engine.repository.WorkflowDefinitionRepository;
import com.workflow.engine.repository.WorkflowStepConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;

@Service
@AllArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowStepConfigRepository workflowStepConfigRepository;

    public Optional<WorkflowDefinition> getWorkflowDefinitionByName(String name) {
        return workflowDefinitionRepository.findByName(name);
    }

    public List<WorkflowDefinition> listWorkflowDefinitions() {
        return workflowDefinitionRepository.findAll();
    }

    @Transactional
    public WorkflowDefinition registerWorkFlowDefinition(RegisterWorkflowRequest request) {

        if(getWorkflowDefinitionByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Workflow already exists: " + request.getName());
        }

        validateStepConfiguration(request.getSteps());

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

    private static void validateStepConfiguration(List<StepRequest> steps) {
        HashSet<Integer> orders = new HashSet<>();
        HashSet<String> names = new HashSet<>();
        for (StepRequest step : steps) {
            if (!orders.add(step.getStepOrder())) {
                throw new IllegalArgumentException("Workflow step orders must be unique");
            }
            if (!names.add(step.getStepName())) {
                throw new IllegalArgumentException("Workflow step names must be unique");
            }
        }
    }

    private static WorkflowStepConfig getWorkflowStepConfig(StepRequest stepRequest, WorkflowDefinition workflowDefinition) {
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

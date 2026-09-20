package com.workflow.engine.repository;

import com.workflow.engine.domain.WorkflowStepConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowStepConfigRepository extends JpaRepository<WorkflowStepConfig, UUID> {

    List<WorkflowStepConfig> findByWorkflowDefinitionOrderByStepOrderAsc(
            com.workflow.engine.domain.WorkflowDefinition workflowDefinition);
}

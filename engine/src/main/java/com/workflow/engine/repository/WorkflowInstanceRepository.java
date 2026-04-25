package com.workflow.engine.repository;

import com.workflow.engine.domain.WorkflowInstance;
import com.workflow.engine.domain.WorkflowStatus;
import com.workflow.engine.domain.WorkflowStepConfig;
import com.workflow.engine.domain.WorkflowStepInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    public List<WorkflowInstance> findByStatus(WorkflowStatus workflowStatus);



}

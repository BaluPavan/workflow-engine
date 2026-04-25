package com.workflow.engine.repository;

import com.workflow.engine.domain.StepStatus;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.domain.WorkflowInstance;
import com.workflow.engine.domain.WorkflowStepInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WorkflowStepInstanceRepository extends JpaRepository<WorkflowStepInstance, UUID> {

    @Query("SELECT s FROM WorkflowStepInstance s WHERE s.status = 'RETRYING' AND s.nextRetryAt <= :now")
    List<WorkflowStepInstance> findStepsDueForRetry(@Param("now") LocalDateTime now);


    List<WorkflowStepInstance> findByWorkflowInstance(WorkflowInstance workflowInstance);

}

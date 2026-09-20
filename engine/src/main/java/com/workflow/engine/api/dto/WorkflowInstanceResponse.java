package com.workflow.engine.api.dto;

import com.workflow.engine.domain.WorkflowStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkflowInstanceResponse {
    private UUID id;
    private String workflowName;
    private WorkflowStatus status;
    private String currentStepName;
    private String context;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private List<StepInstanceSummary> steps;
}

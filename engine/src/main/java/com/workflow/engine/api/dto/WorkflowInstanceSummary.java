package com.workflow.engine.api.dto;

import com.workflow.engine.domain.WorkflowStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkflowInstanceSummary {
    private UUID id;
    private String workflowName;
    private WorkflowStatus status;
    private String currentStepName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

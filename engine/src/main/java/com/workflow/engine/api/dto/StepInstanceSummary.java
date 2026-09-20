package com.workflow.engine.api.dto;

import com.workflow.engine.domain.StepStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StepInstanceSummary {
    private UUID id;
    private String stepName;
    private int stepOrder;
    private StepStatus status;
    private int retryCount;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}

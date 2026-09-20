package com.workflow.engine.kafka.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultMessage {
    @NotNull
    private String idempotencyKey;

    @NotNull
    private UUID workflowInstanceId;

    @NotNull
    private UUID stepInstanceId;

    @NotNull
    private String stepName;

    private boolean success;

    private String failureReason;

    private int retryCount;
}

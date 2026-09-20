package com.workflow.worker.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignedMessage {
    private String idempotencyKey;
    private UUID workflowInstanceId;
    private UUID stepInstanceId;
    private String stepName;
    private int retryCount;
    private String context;
}

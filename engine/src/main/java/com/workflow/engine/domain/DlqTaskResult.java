package com.workflow.engine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dlq_task_results")
@Getter
@Setter
public class DlqTaskResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    @Column(name = "step_instance_id")
    private UUID stepInstanceId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "error_reason", nullable = false, columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "status", nullable = false)
    private String status = "UNRESOLVED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}

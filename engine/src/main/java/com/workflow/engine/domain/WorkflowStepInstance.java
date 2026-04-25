package com.workflow.engine.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow_step_instances")
public class WorkflowStepInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @ManyToOne
    @JoinColumn(name = "workflow_step_config_id", nullable = false)
    private WorkflowStepConfig workflowStepConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    private LocalDateTime nextRetryAt;

    private String failureReason;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}

package com.workflow.engine.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "workflow_def_id" , nullable = false)
    private WorkflowDefinition workflowDefId;

    @ManyToOne
    @JoinColumn(name = "current_step_id")
    private WorkflowStepConfig currentStep;

    @Enumerated(EnumType.STRING)
    private WorkflowStatus status = WorkflowStatus.PENDING;

    @Column(columnDefinition = "jsonb")
    private String context;


    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}

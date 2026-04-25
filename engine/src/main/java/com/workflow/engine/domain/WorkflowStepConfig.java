package com.workflow.engine.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow_step_configs")
public class WorkflowStepConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "workflow_def_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    @Column(nullable = false)
    private String stepName;

    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private int maxRetries = 3;

    @Column(name = "is_critical", nullable = false)
    private boolean critical = true;

    @Column(nullable = false)
    private int timeoutSeconds = 30;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}

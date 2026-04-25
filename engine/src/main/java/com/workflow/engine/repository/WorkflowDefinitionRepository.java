package com.workflow.engine.repository;

import com.workflow.engine.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    public Optional<WorkflowDefinition> findByName(String name);
}

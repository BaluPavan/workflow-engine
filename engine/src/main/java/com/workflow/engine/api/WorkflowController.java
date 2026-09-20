package com.workflow.engine.api;

import com.workflow.engine.api.dto.RegisterWorkflowRequest;
import com.workflow.engine.api.dto.StartWorkflowInstanceRequest;
import com.workflow.engine.api.dto.WorkflowInstanceResponse;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.domain.WorkflowInstance;
import com.workflow.engine.service.WorkflowInstanceService;
import com.workflow.engine.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@AllArgsConstructor
public class WorkflowController {
    private final WorkflowService workflowService;
    private final WorkflowInstanceService workflowInstanceService;

    @GetMapping
    public ResponseEntity<List<WorkflowDefinition>> listWorkflows() {
        return ResponseEntity.ok(workflowService.listWorkflowDefinitions());
    }

    @PostMapping
    public ResponseEntity<WorkflowDefinition> registerWorkflowDefinition(@Valid @RequestBody RegisterWorkflowRequest request) {
        WorkflowDefinition workflowDefinition = workflowService.registerWorkFlowDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowDefinition);
    }

    @PostMapping("/{name}/instances")
    public ResponseEntity<WorkflowInstanceResponse> startWorkflowInstance(
            @PathVariable String name,
            @RequestBody(required = false) StartWorkflowInstanceRequest request) {
        String context = request != null ? request.getContext() : "{}";
        WorkflowInstance instance = workflowInstanceService.startInstance(name, context);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowInstanceService.getInstance(instance.getId()));
    }
}

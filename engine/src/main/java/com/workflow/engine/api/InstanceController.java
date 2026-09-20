package com.workflow.engine.api;

import com.workflow.engine.api.dto.StartWorkflowInstanceRequest;
import com.workflow.engine.api.dto.WorkflowInstanceResponse;
import com.workflow.engine.api.dto.WorkflowInstanceSummary;
import com.workflow.engine.service.WorkflowInstanceService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instances")
@AllArgsConstructor
public class InstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    @GetMapping
    public ResponseEntity<List<WorkflowInstanceSummary>> listInstances() {
        return ResponseEntity.ok(workflowInstanceService.listInstances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowInstanceResponse> getInstance(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowInstanceService.getInstance(id));
    }
}

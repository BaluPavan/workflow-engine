package com.workflow.engine.api;

import com.workflow.engine.api.dto.RegisterWorkflowRequest;
import com.workflow.engine.domain.WorkflowDefinition;
import com.workflow.engine.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
@AllArgsConstructor
public class WorkflowController {
    private final WorkflowService workflowService;


    @PostMapping
    public ResponseEntity<WorkflowDefinition> registerWorkflowDefinition(@Valid @RequestBody RegisterWorkflowRequest request){
        WorkflowDefinition workflowDefinition = workflowService.registerWorkFlowDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowDefinition);
    }

}

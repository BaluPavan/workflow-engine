package com.workflow.engine.api.dto;

import lombok.Data;

@Data
public class StartWorkflowInstanceRequest {
    private String context = "{}";
}

package com.workflow.engine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RegisterWorkflowRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private List<StepRequest> steps;
}

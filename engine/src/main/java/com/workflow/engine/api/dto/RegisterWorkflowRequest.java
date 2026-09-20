package com.workflow.engine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class RegisterWorkflowRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @NotEmpty
    @Valid
    private List<StepRequest> steps;
}

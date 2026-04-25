package com.workflow.engine.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StepRequest {

    @NotBlank
    private String stepName;

    @Min(0)
    private int maxRetries = 3;

    private boolean critical = true;

    @Min(1)
    private int stepOrder;

    @Min(1)
    private int timeoutSeconds;

}

package com.eternamente.assessment.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateAssessmentRequest(
    @NotNull @Min(0) Integer age,
    @NotNull Map<String, Object> metrics
) {
}


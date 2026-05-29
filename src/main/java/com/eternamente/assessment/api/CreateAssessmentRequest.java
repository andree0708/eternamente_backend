package com.eternamente.assessment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateAssessmentRequest(
    @JsonProperty("age") @NotNull @Min(0) Integer age,
    @JsonProperty("metrics") @NotNull Map<String, Object> metrics
) {
}


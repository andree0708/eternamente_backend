package com.eternamente.assessment.api;

import java.util.UUID;

public record AssessmentAnalysisResponse(
    UUID assessmentId,
    String model,
    String analysis
) {
}

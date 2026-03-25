package com.eternamente.assessment.ml;

public record MlPrediction(
    String modelVersion,
    double riskScore,
    boolean predictedDcl
) {
}


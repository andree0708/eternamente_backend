package com.eternamente.assessment.ml;

public record MlPrediction(
    String modelVersion,
    double riskScore,
    boolean predictedDcl,
    AlertLevel alertLevel,
    String domainsFlagged
) {
  public MlPrediction(String modelVersion, double riskScore, boolean predictedDcl) {
    this(modelVersion, riskScore, predictedDcl, predictedDcl ? AlertLevel.ALERT : AlertLevel.NORMAL, "");
  }
}

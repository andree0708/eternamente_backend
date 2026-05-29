package com.eternamente.assessment.ml;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CognitiveAnalyzer {

  public record AnalysisResult(
      float anomalyScore,
      float statisticalRisk,
      float combinedRiskScore,
      AlertLevel alertLevel,
      List<String> flaggedDomains
  ) {}

  public AnalysisResult analyze(
      FeatureVector vector,
      float anomalyScore,
      float statisticalRisk
  ) {
    List<String> flagged = identifyFlaggedDomains(vector);
    float combined = clamp01((anomalyScore + statisticalRisk) / 2f);
    float declineSd = Math.max(0f, -vector.raw(13));
    AlertLevel level = determineAlertLevel(anomalyScore, declineSd, flagged.size());
    return new AnalysisResult(anomalyScore, statisticalRisk, combined, level, flagged);
  }

  private List<String> identifyFlaggedDomains(FeatureVector vector) {
    List<String> flagged = new ArrayList<>();
    float[] n = vector.normalized();
    if (n[4] < 0.35f || n[0] > 0.70f || n[9] < 0.35f) {
      flagged.add("MEMORY");
    }
    if (n[5] < 0.35f || n[1] > 0.70f || n[10] < 0.35f) {
      flagged.add("ATTENTION");
    }
    if (n[6] < 0.35f || n[2] > 0.70f) {
      flagged.add("EXECUTIVE");
    }
    if (n[7] < 0.35f || n[3] > 0.70f) {
      flagged.add("PROCESSING_SPEED");
    }
    if (n[8] < 0.35f) {
      flagged.add("ORIENTATION");
    }
    return flagged;
  }

  private AlertLevel determineAlertLevel(float anomalyScore, float declineSd, int flaggedCount) {
    if (anomalyScore > 0.70f || (declineSd > 1.5f && flaggedCount >= 2)) {
      return AlertLevel.ALERT;
    }
    if (anomalyScore >= 0.40f || declineSd >= 1.0f) {
      return AlertLevel.WATCH;
    }
    return AlertLevel.NORMAL;
  }

  private static float clamp01(float x) {
    return Math.max(0f, Math.min(1f, x));
  }
}

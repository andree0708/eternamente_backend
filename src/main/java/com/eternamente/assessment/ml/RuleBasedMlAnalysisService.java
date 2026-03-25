package com.eternamente.assessment.ml;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RuleBasedMlAnalysisService implements MlAnalysisService {

  @Override
  public MlPrediction analyze(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", readDouble(metrics, "accuracyMean", Double.NaN));
    double reactionTimeMs = readDouble(metrics, "reactionTimeMs", readDouble(metrics, "reactionTimeMeanMs", Double.NaN));

    // Normalizamos:
    // - accuracy: si viene en 0..100 lo pasamos a 0..1.
    if (!Double.isNaN(accuracy) && accuracy > 1d) {
      accuracy = accuracy / 100d;
    }
    // accuracyRisk: entre 0..1 (1 significa mayor riesgo)
    double accuracyRisk = Double.isNaN(accuracy) ? 0.5d : clamp01(1d - accuracy);

    // reactionTimeRisk: más tiempo => más riesgo (con rango aproximado 300..2000 ms)
    double reactionTimeRisk;
    if (Double.isNaN(reactionTimeMs)) {
      reactionTimeRisk = 0.5d;
    } else {
      reactionTimeRisk = clamp01((reactionTimeMs - 300d) / (2000d - 300d));
    }

    double riskScore = 0.6d * accuracyRisk + 0.4d * reactionTimeRisk;
    boolean predictedDcl = riskScore >= 0.6d;

    return new MlPrediction("rule-v0", riskScore, predictedDcl);
  }

  private static double readDouble(Map<String, Object> metrics, String key, double defaultValue) {
    if (metrics == null) {
      return defaultValue;
    }
    Object v = metrics.get(key);
    if (v == null) {
      return defaultValue;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (Exception ignored) {
      return defaultValue;
    }
  }

  private static double clamp01(double x) {
    if (x < 0d) return 0d;
    if (x > 1d) return 1d;
    return x;
  }
}


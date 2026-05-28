package com.eternamente.assessment.ml;

import org.springframework.stereotype.Component;

/**
 * Fallback estadístico cuando no hay modelo TFLite (pesos del doc migración §4).
 */
@Component
public class StatisticalRiskEstimator {

  public float estimate(FeatureVector vector) {
    float[] n = vector.normalized();
    float rtRisk = average(
        invert(n[0]), invert(n[1]), invert(n[2]), invert(n[3])
    );
    float accRisk = average(
        invert(n[4]), invert(n[5]), invert(n[6]), invert(n[7]), invert(n[8])
    );
    float trendRisk = average(
        invertTrend(n[9]), invertTrend(n[10])
    );
    float completionRisk = invert(n[11]);
    float variabilityRisk = clamp01(n[12]);
    float baselineRisk = clamp01(-n[13] + 0.5f);

    float risk = 0.15f * rtRisk
        + 0.35f * accRisk
        + 0.20f * trendRisk
        + 0.10f * completionRisk
        + 0.10f * variabilityRisk
        + 0.10f * baselineRisk;
    return clamp01(risk);
  }

  private static float invert(float normalized) {
    return clamp01(1f - normalized);
  }

  private static float invertTrend(float normalized) {
    return clamp01(1f - normalized);
  }

  private static float average(float... values) {
    float sum = 0;
    int count = 0;
    for (float v : values) {
      if (!Float.isNaN(v)) {
        sum += v;
        count++;
      }
    }
    return count == 0 ? 0.5f : sum / count;
  }

  private static float clamp01(float x) {
    return Math.max(0f, Math.min(1f, x));
  }
}

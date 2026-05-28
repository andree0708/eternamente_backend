package com.eternamente.assessment.ml;

import org.springframework.stereotype.Component;

/**
 * Normalización min-max [0,1] — bounds según FeatureNormalizer Android / doc migración.
 */
@Component
public class FeatureNormalizer {

  private static final float[] MIN = {
      200f, 200f, 200f, 200f,
      0f, 0f, 0f, 0f, 0f,
      -10f, -10f,
      0f,
      0f,
      -3f
  };

  private static final float[] MAX = {
      5000f, 5000f, 5000f, 5000f,
      100f, 100f, 100f, 100f, 100f,
      10f, 10f,
      1f,
      1.5f,
      3f
  };

  public FeatureVector normalize(float[] raw) {
    float[] norm = new float[FeatureVector.SIZE];
    for (int i = 0; i < FeatureVector.SIZE; i++) {
      float value = i < raw.length ? raw[i] : 0.5f;
      norm[i] = normalizeIndex(i, value);
    }
    return new FeatureVector(raw, norm);
  }

  public float normalizeIndex(int index, float raw) {
    float lo = MIN[index];
    float hi = MAX[index];
    if (hi == lo) {
      return 0.5f;
    }
    return Math.max(0f, Math.min(1f, (raw - lo) / (hi - lo)));
  }
}

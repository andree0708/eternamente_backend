package com.eternamente.assessment.ml;

/**
 * Vector de 14 características (orden fijo, ver MIGRACION_SPRING_BOOT_REACT.md §4).
 */
public final class FeatureVector {

  public static final int SIZE = 14;

  private final float[] raw;
  private final float[] normalized;

  public FeatureVector(float[] raw, float[] normalized) {
    this.raw = raw;
    this.normalized = normalized;
  }

  public float[] raw() {
    return raw;
  }

  public float[] normalized() {
    return normalized;
  }

  public float raw(int index) {
    return raw[index];
  }

  public float normalized(int index) {
    return normalized[index];
  }
}

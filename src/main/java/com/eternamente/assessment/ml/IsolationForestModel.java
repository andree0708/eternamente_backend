package com.eternamente.assessment.ml;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolation Forest simplificado (100 árboles, submuestra 64) por usuario.
 */
@Component
public class IsolationForestModel {

  private static final int TREES = 100;
  private static final int SUBSAMPLE = 64;
  private static final int MIN_SAMPLES = 10;
  private static final int MAX_HISTORY = 200;

  private final ConcurrentHashMap<java.util.UUID, List<float[]>> historyByUser = new ConcurrentHashMap<>();

  public float anomalyScore(java.util.UUID userId, float[] normalizedFeatures) {
    List<float[]> history = historyByUser.computeIfAbsent(userId, id -> new ArrayList<>());
    synchronized (history) {
      history.add(normalizedFeatures.clone());
      while (history.size() > MAX_HISTORY) {
        history.removeFirst();
      }
      if (history.size() < MIN_SAMPLES) {
        return 0.5f;
      }
      return score(history, normalizedFeatures);
    }
  }

  private float score(List<float[]> data, float[] point) {
    Random random = new Random(42);
    double total = 0;
    for (int t = 0; t < TREES; t++) {
      List<float[]> sample = subsample(data, SUBSAMPLE, random);
      total += pathLength(point, sample, 0, (int) (Math.log(SUBSAMPLE) / Math.log(2)) + 1, random);
    }
    double avgPath = total / TREES;
    double c = averagePathLength(SUBSAMPLE);
    return (float) Math.pow(2, -avgPath / c);
  }

  private static List<float[]> subsample(List<float[]> data, int size, Random random) {
    List<float[]> copy = new ArrayList<>(data);
    List<float[]> sample = new ArrayList<>(size);
    for (int i = 0; i < size && !copy.isEmpty(); i++) {
      int idx = random.nextInt(copy.size());
      sample.add(copy.remove(idx));
    }
    return sample;
  }

  private double pathLength(float[] point, List<float[]> data, int depth, int maxDepth, Random random) {
    if (depth >= maxDepth || data.size() <= 1) {
      return depth + averagePathLength(data.size());
    }
    int feature = random.nextInt(FeatureVector.SIZE);
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    for (float[] row : data) {
      min = Math.min(min, row[feature]);
      max = Math.max(max, row[feature]);
    }
    if (min >= max) {
      return depth + averagePathLength(data.size());
    }
    float split = min + random.nextFloat() * (max - min);
    List<float[]> left = new ArrayList<>();
    List<float[]> right = new ArrayList<>();
    for (float[] row : data) {
      if (row[feature] < split) {
        left.add(row);
      } else {
        right.add(row);
      }
    }
    if (point[feature] < split) {
      return pathLength(point, left.isEmpty() ? data : left, depth + 1, maxDepth, random);
    }
    return pathLength(point, right.isEmpty() ? data : right, depth + 1, maxDepth, random);
  }

  private static double averagePathLength(int n) {
    if (n <= 1) {
      return 0;
    }
    return 2 * (Math.log(n - 1) + 0.5772156649) - (2 * (n - 1) / (double) n);
  }
}

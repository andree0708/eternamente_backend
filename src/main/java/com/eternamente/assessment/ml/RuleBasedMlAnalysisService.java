package com.eternamente.assessment.ml;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RuleBasedMlAnalysisService implements MlAnalysisService {

  @Override
  public MlPrediction analyze(Map<String, Object> metrics) {
    // Preferimos las claves que ya maneja el "rule-based model".
    // Si el frontend manda otras (como en tu juego), hacemos fallback con métricas equivalentes.
    double accuracy = readDouble(metrics, "accuracy", readDouble(metrics, "accuracyMean", Double.NaN));
    double reactionTimeMs = readDouble(metrics, "reactionTimeMs", readDouble(metrics, "reactionTimeMeanMs", Double.NaN));

    // Fallback desde métricas del juego:
    // - accuracy: estimacion basada en matchedPairs/totalPairs
    // - reactionTimeMs: usamos averageRevealMs como "tiempo de reaccion"
    if (Double.isNaN(accuracy)) {
      double matchedPairs = readDouble(metrics, "matchedPairs", Double.NaN);
      double totalPairs = readDouble(metrics, "totalPairs", Double.NaN);
      if (!Double.isNaN(matchedPairs) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
        accuracy = matchedPairs / totalPairs; // 0..1
      }
    }
    if (Double.isNaN(reactionTimeMs)) {
      double avgRevealMs = readDouble(metrics, "averageRevealMs", Double.NaN);
      if (!Double.isNaN(avgRevealMs)) {
        reactionTimeMs = avgRevealMs;
      }
    }
    double moves = readDouble(metrics, "moves", Double.NaN);
    double mismatches = readDouble(metrics, "mismatches", Double.NaN);
    double totalPairs = readDouble(metrics, "totalPairs", Double.NaN);
    double durationSeconds = readDouble(metrics, "durationSeconds", Double.NaN);

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

    // errorRateRisk: mas errores por movimiento => mas riesgo
    double errorRateRisk = 0.5d;
    if (!Double.isNaN(moves) && moves > 0d && !Double.isNaN(mismatches) && mismatches >= 0d) {
      errorRateRisk = clamp01(mismatches / moves);
    }

    // efficiencyRisk: movimientos por par esperado (ideal ~=1). Si sube mucho, sube riesgo.
    double efficiencyRisk = 0.5d;
    if (!Double.isNaN(moves) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
      double movesPerPair = moves / totalPairs;
      efficiencyRisk = clamp01((movesPerPair - 1d) / 3d); // 1..4 -> 0..1
    }

    // paceRisk: duracion total por par (segundos) para capturar lentitud sostenida
    double paceRisk = 0.5d;
    if (!Double.isNaN(durationSeconds) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
      double secPerPair = durationSeconds / totalPairs;
      paceRisk = clamp01((secPerPair - 4d) / 20d); // 4..24 seg/par -> 0..1
    }

    // Mezcla final mas sensible a variaciones del juego
    double riskScore =
        0.35d * accuracyRisk +
        0.20d * reactionTimeRisk +
        0.20d * errorRateRisk +
        0.15d * efficiencyRisk +
        0.10d * paceRisk;
    riskScore = clamp01(riskScore);
    boolean predictedDcl = riskScore >= 0.6d;

    return new MlPrediction("rule-v2-memory", riskScore, predictedDcl);
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


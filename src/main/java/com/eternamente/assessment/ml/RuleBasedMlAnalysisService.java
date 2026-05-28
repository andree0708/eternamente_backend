package com.eternamente.assessment.ml;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RuleBasedMlAnalysisService implements MlAnalysisService {

  @Override
  public MlPrediction analyze(Map<String, Object> metrics) {
    Object rawType = metrics.get("gameType");
    String gameType = rawType != null && !String.valueOf(rawType).isBlank()
        ? String.valueOf(rawType)
        : "memory";
    
    return switch (gameType) {
      case "stroop" -> analyzeStroop(metrics);
      case "whackamole" -> analyzeWhackamole(metrics);
      case "navigation" -> analyzeNavigation(metrics);
      case "digitspan" -> analyzeDigitSpan(metrics);
      case "corsi" -> analyzeCorsi(metrics);
      case "orientation" -> analyzeOrientation(metrics);
      case "arithmetic" -> analyzeArithmetic(metrics);
      default -> analyzeMemory(metrics);
    };
  }

  private MlPrediction analyzeStroop(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", 0.0);
    double avgReactionTime = readDouble(metrics, "averageReactionTimeMs", Double.NaN);
    int correct = (int) readDouble(metrics, "correct", 0);
    int errors = (int) readDouble(metrics, "errors", 0);
    int totalRounds = (int) readDouble(metrics, "totalRounds", 20);
    
    double accuracyRisk = clamp01(1.0 - accuracy);
    
    double reactionTimeRisk;
    if (Double.isNaN(avgReactionTime)) {
      reactionTimeRisk = 0.5;
    } else {
      reactionTimeRisk = clamp01((avgReactionTime - 500) / 2000);
    }
    
    double errorRate = totalRounds > 0 ? (double) errors / totalRounds : 0;
    double errorRisk = clamp01(errorRate * 2);
    
    double riskScore = 0.40 * accuracyRisk + 0.35 * reactionTimeRisk + 0.25 * errorRisk;
    riskScore = clamp01(riskScore);
    boolean predictedDcl = riskScore >= 0.6;
    
    return new MlPrediction("rule-v1-stroop", riskScore, predictedDcl);
  }

  private MlPrediction analyzeWhackamole(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", 0.0);
    double avgReactionTime = readDouble(metrics, "averageReactionTimeMs", Double.NaN);
    int correct = (int) readDouble(metrics, "correct", 0);
    int errors = (int) readDouble(metrics, "errors", 0);
    
    double accuracyRisk = clamp01(1.0 - accuracy);
    
    double reactionTimeRisk;
    if (Double.isNaN(avgReactionTime)) {
      reactionTimeRisk = 0.5;
    } else {
      reactionTimeRisk = clamp01((avgReactionTime - 200) / 800);
    }
    
    double riskScore = 0.50 * accuracyRisk + 0.50 * reactionTimeRisk;
    riskScore = clamp01(riskScore);
    boolean predictedDcl = riskScore >= 0.6;
    
    return new MlPrediction("rule-v1-whackamole", riskScore, predictedDcl);
  }

  private MlPrediction analyzeNavigation(Map<String, Object> metrics) {
    int achievedLevel = (int) readDouble(metrics, "achievedLevel", 0);
    int maxLevel = (int) readDouble(metrics, "maxLevel", 5);
    int totalMoves = (int) readDouble(metrics, "totalMoves", 0);
    int errors = (int) readDouble(metrics, "errors", 0);
    
    double levelProgress = maxLevel > 0 ? (double) achievedLevel / maxLevel : 0;
    double levelRisk = clamp01(1.0 - levelProgress);
    
    double errorRate = totalMoves > 0 ? (double) errors / totalMoves : 0;
    double errorRisk = clamp01(errorRate * 2);
    
    double riskScore = 0.60 * levelRisk + 0.40 * errorRisk;
    riskScore = clamp01(riskScore);
    boolean predictedDcl = riskScore >= 0.6;
    
    return new MlPrediction("rule-v1-navigation", riskScore, predictedDcl);
  }

  private MlPrediction analyzeMemory(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", readDouble(metrics, "accuracyMean", Double.NaN));
    double reactionTimeMs = readDouble(metrics, "reactionTimeMs", readDouble(metrics, "reactionTimeMeanMs", Double.NaN));

    if (Double.isNaN(accuracy)) {
      double matchedPairs = readDouble(metrics, "matchedPairs", Double.NaN);
      double totalPairs = readDouble(metrics, "totalPairs", Double.NaN);
      if (!Double.isNaN(matchedPairs) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
        accuracy = matchedPairs / totalPairs;
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

    if (!Double.isNaN(accuracy) && accuracy > 1d) {
      accuracy = accuracy / 100d;
    }
    double accuracyRisk = Double.isNaN(accuracy) ? 0.5d : clamp01(1d - accuracy);

    double reactionTimeRisk;
    if (Double.isNaN(reactionTimeMs)) {
      reactionTimeRisk = 0.5d;
    } else {
      reactionTimeRisk = clamp01((reactionTimeMs - 300d) / (2000d - 300d));
    }

    double errorRateRisk = 0.5d;
    if (!Double.isNaN(moves) && moves > 0d && !Double.isNaN(mismatches) && mismatches >= 0d) {
      errorRateRisk = clamp01(mismatches / moves);
    }

    double efficiencyRisk = 0.5d;
    if (!Double.isNaN(moves) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
      double movesPerPair = moves / totalPairs;
      efficiencyRisk = clamp01((movesPerPair - 1d) / 3d);
    }

    double paceRisk = 0.5d;
    if (!Double.isNaN(durationSeconds) && !Double.isNaN(totalPairs) && totalPairs > 0d) {
      double secPerPair = durationSeconds / totalPairs;
      paceRisk = clamp01((secPerPair - 4d) / 20d);
    }

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

  private MlPrediction analyzeDigitSpan(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", 0.0);
    int achievedLevel = (int) readDouble(metrics, "achievedLevel", 0);
    int maxLevel = (int) readDouble(metrics, "maxLevel", 5);
    double levelProgress = maxLevel > 0 ? (double) achievedLevel / maxLevel : 0;
    double riskScore = clamp01(0.55 * (1.0 - accuracy) + 0.45 * (1.0 - levelProgress));
    return new MlPrediction("rule-v1-digitspan", riskScore, riskScore >= 0.6);
  }

  private MlPrediction analyzeCorsi(Map<String, Object> metrics) {
    return analyzeDigitSpan(metrics);
  }

  private MlPrediction analyzeOrientation(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", 0.0);
    double riskScore = clamp01(1.0 - accuracy);
    return new MlPrediction("rule-v1-orientation", riskScore, riskScore >= 0.6);
  }

  private MlPrediction analyzeArithmetic(Map<String, Object> metrics) {
    double accuracy = readDouble(metrics, "accuracy", 0.0);
    double avgReactionTime = readDouble(metrics, "averageReactionTimeMs", Double.NaN);
    double accuracyRisk = clamp01(1.0 - accuracy);
    double reactionTimeRisk = Double.isNaN(avgReactionTime)
        ? 0.5
        : clamp01((avgReactionTime - 1500) / 6000);
    double riskScore = clamp01(0.65 * accuracyRisk + 0.35 * reactionTimeRisk);
    return new MlPrediction("rule-v1-arithmetic", riskScore, riskScore >= 0.6);
  }

  private static double clamp01(double x) {
    if (x < 0d) return 0d;
    if (x > 1d) return 1d;
    return x;
  }
}


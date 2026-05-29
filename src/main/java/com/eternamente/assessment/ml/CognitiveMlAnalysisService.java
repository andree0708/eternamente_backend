package com.eternamente.assessment.ml;

import com.eternamente.assessment.AssessmentSession;
import com.eternamente.assessment.AssessmentSessionRepository;
import com.eternamente.assessment.UserMlFeatureSnapshot;
import com.eternamente.assessment.UserMlFeatureSnapshotRepository;
import com.eternamente.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline cognitivo (doc migración §4): FeatureExtractor → Normalizer → Isolation Forest
 * → estimador estadístico (fallback TFLite) → CognitiveAnalyzer.
 * Con &lt; 3 sesiones en 4 semanas usa reglas por juego.
 */
@Service
public class CognitiveMlAnalysisService implements MlAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(CognitiveMlAnalysisService.class);

  private final RuleBasedMlAnalysisService ruleBased;
  private final AssessmentSessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final UserMlFeatureSnapshotRepository snapshotRepository;
  private final SessionMetricsReader metricsReader;
  private final FeatureExtractor featureExtractor;
  private final FeatureNormalizer featureNormalizer;
  private final IsolationForestModel isolationForest;
  private final StatisticalRiskEstimator statisticalRiskEstimator;
  private final CognitiveAnalyzer cognitiveAnalyzer;

  public CognitiveMlAnalysisService(
      RuleBasedMlAnalysisService ruleBased,
      AssessmentSessionRepository sessionRepository,
      UserRepository userRepository,
      UserMlFeatureSnapshotRepository snapshotRepository,
      SessionMetricsReader metricsReader,
      FeatureExtractor featureExtractor,
      FeatureNormalizer featureNormalizer,
      IsolationForestModel isolationForest,
      StatisticalRiskEstimator statisticalRiskEstimator,
      CognitiveAnalyzer cognitiveAnalyzer
  ) {
    this.ruleBased = ruleBased;
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
    this.snapshotRepository = snapshotRepository;
    this.metricsReader = metricsReader;
    this.featureExtractor = featureExtractor;
    this.featureNormalizer = featureNormalizer;
    this.isolationForest = isolationForest;
    this.statisticalRiskEstimator = statisticalRiskEstimator;
    this.cognitiveAnalyzer = cognitiveAnalyzer;
  }

  @Override
  public MlPrediction analyze(UUID userId, Map<String, Object> metrics) {
    try {
      return analyzeCognitive(userId, metrics);
    } catch (Exception ex) {
      log.error("Pipeline cognitivo falló para userId={}, usando reglas: {}", userId, ex.getMessage(), ex);
      return ruleBased.analyze(userId, metrics);
    }
  }

  private MlPrediction analyzeCognitive(UUID userId, Map<String, Object> metrics) {
    List<FeatureExtractor.SessionPoint> points = loadHistory(userId);
    String gameType = resolveGameType(metrics);
    points.add(new FeatureExtractor.SessionPoint(Instant.now(), gameType, metrics));

    if (!featureExtractor.hasMinimumData(points)) {
      log.debug("ML cognitivo: usuario {} con pocas sesiones, usando reglas por juego", userId);
      return ruleBased.analyze(userId, metrics);
    }

    float[] raw = sanitizeRaw(featureExtractor.extractRaw(points));
    FeatureVector vector = featureNormalizer.normalize(raw);
    if (!isolationForest.hasHistory(userId)) {
      warmupForest(userId);
    }
    float anomaly = isolationForest.anomalyScore(userId, vector.normalized());
    float statRisk = statisticalRiskEstimator.estimate(vector);
    CognitiveAnalyzer.AnalysisResult result = cognitiveAnalyzer.analyze(vector, anomaly, statRisk);

    persistSnapshot(userId, vector.normalized());

    return new MlPrediction(
        "cognitive-v1-if-stat",
        result.combinedRiskScore(),
        result.alertLevel() == AlertLevel.ALERT,
        result.alertLevel(),
        String.join(",", result.flaggedDomains())
    );
  }

  private List<FeatureExtractor.SessionPoint> loadHistory(UUID userId) {
    Instant after = Instant.now().minus(4 * 7L, ChronoUnit.DAYS);
    List<AssessmentSession> sessions = sessionRepository.findRecentByUserId(userId, after);
    List<FeatureExtractor.SessionPoint> points = new ArrayList<>();
    for (AssessmentSession s : sessions) {
      String type = s.getGameType() != null ? s.getGameType() : "memory";
      points.add(new FeatureExtractor.SessionPoint(
          s.getCreatedAt(),
          type,
          metricsReader.parse(s.getMetrics())
      ));
    }
    return points;
  }

  private void warmupForest(UUID userId) {
    try {
      List<UserMlFeatureSnapshot> snapshots = snapshotRepository
          .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200));
      if (snapshots.isEmpty()) return;
      List<float[]> vectors = new ArrayList<>();
      for (UserMlFeatureSnapshot snap : snapshots) {
        float[] arr = new float[FeatureVector.SIZE];
        List<Double> list = snap.getFeatures();
        for (int i = 0; i < FeatureVector.SIZE && i < list.size(); i++) {
          arr[i] = list.get(i).floatValue();
        }
        vectors.add(arr);
      }
      isolationForest.warmup(userId, vectors);
      log.info("Isolation Forest warmeado para {} con {} snapshots", userId, vectors.size());
    } catch (Exception ex) {
      log.warn("No se pudo warmear Isolation Forest para {}: {}", userId, ex.getMessage());
    }
  }

  private void persistSnapshot(UUID userId, float[] normalized) {
    try {
      userRepository.findById(userId).ifPresent(user -> {
        UserMlFeatureSnapshot snap = new UserMlFeatureSnapshot();
        snap.setUser(user);
        List<Double> list = new ArrayList<>(normalized.length);
        for (float v : normalized) {
          list.add((double) v);
        }
        snap.setFeatures(list);
        snapshotRepository.save(snap);
      });
    } catch (Exception ex) {
      log.warn("No se pudo guardar snapshot ML: {}", ex.getMessage());
    }
  }

  private static float[] sanitizeRaw(float[] raw) {
    float[] out = new float[FeatureVector.SIZE];
    for (int i = 0; i < FeatureVector.SIZE; i++) {
      float v = i < raw.length ? raw[i] : 0.5f;
      out[i] = Float.isNaN(v) || Float.isInfinite(v) ? 0.5f : v;
    }
    return out;
  }

  private static String resolveGameType(Map<String, Object> metrics) {
    Object gameType = metrics.get("gameType");
    return gameType != null ? String.valueOf(gameType) : "memory";
  }
}

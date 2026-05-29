package com.eternamente.assessment.service;

import com.eternamente.assessment.AssessmentSession;
import com.eternamente.assessment.AssessmentSessionRepository;
import com.eternamente.assessment.UserCognitiveSummary;
import com.eternamente.assessment.UserCognitiveSummaryRepository;
import com.eternamente.assessment.api.AnalyticsResponse;
import com.eternamente.assessment.api.AssessmentAnalysisResponse;
import com.eternamente.assessment.api.AssessmentResponse;
import com.eternamente.assessment.api.CognitiveSummaryResponse;
import com.eternamente.assessment.api.CreateAssessmentRequest;
import com.eternamente.assessment.ml.AlertLevel;
import com.eternamente.assessment.ml.GroqCognitiveAnalysisService;
import com.eternamente.assessment.ml.MlAnalysisService;
import com.eternamente.assessment.ml.MlPrediction;
import com.eternamente.assessment.ml.RuleBasedMlAnalysisService;
import com.eternamente.user.User;
import com.eternamente.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

  private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

  private final AssessmentSessionRepository repository;
  private final UserRepository userRepository;
  private final UserCognitiveSummaryRepository summaryRepository;
  private final MlAnalysisService mlAnalysisService;
  private final RuleBasedMlAnalysisService ruleBasedMlAnalysisService;
  private final GroqCognitiveAnalysisService groqCognitiveAnalysisService;
  private final ObjectMapper objectMapper;

  public AssessmentService(
      AssessmentSessionRepository repository,
      UserRepository userRepository,
      UserCognitiveSummaryRepository summaryRepository,
      MlAnalysisService mlAnalysisService,
      RuleBasedMlAnalysisService ruleBasedMlAnalysisService,
      GroqCognitiveAnalysisService groqCognitiveAnalysisService,
      ObjectMapper objectMapper
  ) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.summaryRepository = summaryRepository;
    this.mlAnalysisService = mlAnalysisService;
    this.ruleBasedMlAnalysisService = ruleBasedMlAnalysisService;
    this.groqCognitiveAnalysisService = groqCognitiveAnalysisService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AssessmentResponse create(CreateAssessmentRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

    Map<String, Object> metrics = sanitizeMetrics(request.metrics());
    MlPrediction prediction = resolvePrediction(userId, metrics);
    String gameType = resolveGameType(metrics);

    AssessmentSession session = new AssessmentSession();
    session.setUser(user);
    session.setUserExternalId(user.getEmail() != null ? user.getEmail() : userId.toString());
    session.setAge(request.age());
    session.setMetrics(metrics);
    session.setGameType(gameType);
    session.setModelVersion(prediction.modelVersion());
    session.setRiskScore(prediction.riskScore());
    session.setPredictedDcl(prediction.predictedDcl());
    AlertLevel alert = prediction.alertLevel() != null ? prediction.alertLevel() : AlertLevel.NORMAL;
    session.setAlertLevel(alert.name());
    session.setMlRunAt(Instant.now());
    session.ensureLegacyColumns();

    AssessmentSession saved;
    try {
      saved = repository.save(session);
    } catch (DataIntegrityViolationException ex) {
      log.error("No se pudo guardar assessment_session para userId={}: {}", userId, ex.getMessage(), ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "No se pudo guardar la partida. Revisa migraciones Flyway en Render (V7-V9)."
      );
    } catch (Exception ex) {
      log.error("Error al persistir assessment_session userId={}: {}", userId, ex.getMessage(), ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error al guardar la partida en el servidor."
      );
    }

    try {
      updateCognitiveSummary(user, gameType, prediction.riskScore(), request.metrics());
    } catch (Exception ex) {
      log.warn("Partida guardada pero falló actualización de resumen cognitivo: {}", ex.getMessage());
    }

    return AssessmentResponse.from(saved, userId, objectMapper);
  }

  @Transactional(readOnly = true)
  public AssessmentResponse get(UUID id, UUID userId) {
    return repository.findByIdAndUserId(id, userId)
        .map(s -> AssessmentResponse.from(s, userId, objectMapper))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
  }

  @Transactional(readOnly = true)
  public List<AssessmentResponse> getByUser(UUID userId) {
    return repository.findByUserId(userId).stream()
        .map(s -> AssessmentResponse.from(s, userId, objectMapper))
        .toList();
  }

  @Transactional(readOnly = true)
  public CognitiveSummaryResponse getSummary(UUID userId) {
    return summaryRepository.findById(userId)
        .map(CognitiveSummaryResponse::from)
        .orElseGet(() -> new CognitiveSummaryResponse(
            userId, 0, 0d, null, 0, 0, 0, 0, null, null
        ));
  }

  @Transactional(readOnly = true)
  public AnalyticsResponse getAnalytics(UUID userId) {
    CognitiveSummaryResponse summary = getSummary(userId);
    List<AssessmentSession> sessions = repository.findByUserId(userId);

    Map<String, List<AssessmentSession>> grouped = sessions.stream()
        .collect(Collectors.groupingBy(
            s -> s.getGameType() != null ? s.getGameType() : "memory",
            LinkedHashMap::new,
            Collectors.toList()
        ));

    List<AnalyticsResponse.GameTypeStat> byGameType = grouped.entrySet().stream()
        .map(entry -> {
          List<AssessmentSession> list = entry.getValue();
          double avgRisk = list.stream()
              .mapToDouble(AssessmentSession::getRiskScore)
              .average()
              .orElse(0d);
          var accuracyAvg = list.stream()
              .map(s -> readAccuracy(safeMetrics(s)))
              .filter(a -> a != null)
              .mapToDouble(Double::doubleValue)
              .average();
          Double avgAccuracy = accuracyAvg.isPresent() ? accuracyAvg.getAsDouble() : null;
          return new AnalyticsResponse.GameTypeStat(
              entry.getKey(),
              list.size(),
              avgRisk,
              avgAccuracy
          );
        })
        .sorted(Comparator.comparingInt(AnalyticsResponse.GameTypeStat::sessions).reversed())
        .toList();

    List<AnalyticsResponse.RiskTrendPoint> riskTrend = sessions.stream()
        .sorted(Comparator.comparing(AssessmentSession::getCreatedAt).reversed())
        .limit(20)
        .map(s -> new AnalyticsResponse.RiskTrendPoint(
            s.getCreatedAt(),
            s.getRiskScore(),
            s.getGameType() != null ? s.getGameType() : "memory"
        ))
        .collect(Collectors.toCollection(ArrayList::new));
    riskTrend.sort(Comparator.comparing(AnalyticsResponse.RiskTrendPoint::playedAt));

    return new AnalyticsResponse(summary, byGameType, riskTrend);
  }

  private Map<String, Object> safeMetrics(AssessmentSession session) {
    Map<String, Object> m = session.getMetrics();
    return m != null ? m : Map.of();
  }

  private Map<String, Object> sanitizeMetrics(Map<String, Object> raw) {
    Map<String, Object> out = new LinkedHashMap<>();
    if (raw == null) {
      return out;
    }
    for (Map.Entry<String, Object> e : raw.entrySet()) {
      Object v = e.getValue();
      if (v instanceof Double d && (d.isNaN() || d.isInfinite())) {
        continue;
      }
      if (v instanceof Float f && (f.isNaN() || f.isInfinite())) {
        continue;
      }
      out.put(e.getKey(), v);
    }
    return out;
  }

  @Transactional(readOnly = true)
  public AssessmentAnalysisResponse getDetailedAnalysis(UUID id, UUID userId) {
    AssessmentSession session = repository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
    List<AssessmentSession> recentSessions = repository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    String analysis = groqCognitiveAnalysisService.analyze(session, recentSessions);
    return new AssessmentAnalysisResponse(session.getId(), groqCognitiveAnalysisService.modelName(), analysis);
  }

  private void updateCognitiveSummary(User user, String gameType, double riskScore, Map<String, Object> metrics) {
    UserCognitiveSummary summary = summaryRepository.findById(user.getId())
        .orElseGet(() -> {
          UserCognitiveSummary created = new UserCognitiveSummary();
          created.setUser(user);
          created.setTotalSessions(0);
          created.setAvgRiskScore(0);
          created.setMemorySessions(0);
          created.setStroopSessions(0);
          created.setNavigationSessions(0);
          created.setWhackamoleSessions(0);
          return created;
        });

    int previousTotal = summary.getTotalSessions();
    double previousAvgRisk = summary.getAvgRiskScore();
    int newTotal = previousTotal + 1;
    summary.setTotalSessions(newTotal);
    summary.setAvgRiskScore((previousAvgRisk * previousTotal + riskScore) / newTotal);
    summary.setLastPlayedAt(Instant.now());

    switch (gameType) {
      case "stroop" -> summary.setStroopSessions(summary.getStroopSessions() + 1);
      case "navigation" -> summary.setNavigationSessions(summary.getNavigationSessions() + 1);
      case "whackamole" -> summary.setWhackamoleSessions(summary.getWhackamoleSessions() + 1);
      case "memory", "digitspan", "corsi" -> summary.setMemorySessions(summary.getMemorySessions() + 1);
      case "orientation" -> summary.setNavigationSessions(summary.getNavigationSessions() + 1);
      case "arithmetic" -> summary.setStroopSessions(summary.getStroopSessions() + 1);
      default -> summary.setMemorySessions(summary.getMemorySessions() + 1);
    }

    Double accuracy = readAccuracy(metrics);
    if (accuracy != null) {
      Double prevAccuracy = summary.getAvgAccuracy();
      if (prevAccuracy == null) {
        summary.setAvgAccuracy(accuracy);
      } else {
        summary.setAvgAccuracy((prevAccuracy * previousTotal + accuracy) / newTotal);
      }
    }

    summaryRepository.save(summary);
  }

  private static Double readAccuracy(Map<String, Object> metrics) {
    Object v = metrics.get("accuracy");
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    return null;
  }

  private MlPrediction resolvePrediction(UUID userId, Map<String, Object> metrics) {
    try {
      return mlAnalysisService.analyze(userId, metrics);
    } catch (Exception ex) {
      log.error("Análisis ML falló para userId={}, fallback reglas: {}", userId, ex.getMessage(), ex);
    }
    try {
      return ruleBasedMlAnalysisService.analyze(userId, metrics);
    } catch (Exception ex) {
      log.error("Rule-based analysis falló para userId={}: {}", userId, ex.getMessage(), ex);
      return new MlPrediction("fallback-v1", 0.5d, false, AlertLevel.NORMAL, "");
    }
  }

  private static String resolveGameType(Map<String, Object> metrics) {
    Object gameType = metrics.get("gameType");
    if (gameType != null && !String.valueOf(gameType).isBlank()) {
      return String.valueOf(gameType);
    }
    return "memory";
  }

}

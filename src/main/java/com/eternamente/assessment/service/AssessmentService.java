package com.eternamente.assessment.service;

import com.eternamente.assessment.AssessmentSession;
import com.eternamente.assessment.AssessmentSessionRepository;
import com.eternamente.assessment.UserCognitiveSummary;
import com.eternamente.assessment.UserCognitiveSummaryRepository;
import com.eternamente.assessment.api.AssessmentAnalysisResponse;
import com.eternamente.assessment.api.AssessmentResponse;
import com.eternamente.assessment.api.CognitiveSummaryResponse;
import com.eternamente.assessment.api.CreateAssessmentRequest;
import com.eternamente.assessment.ml.GroqCognitiveAnalysisService;
import com.eternamente.assessment.ml.MlAnalysisService;
import com.eternamente.assessment.ml.MlPrediction;
import com.eternamente.user.User;
import com.eternamente.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentService {

  private final AssessmentSessionRepository repository;
  private final UserRepository userRepository;
  private final UserCognitiveSummaryRepository summaryRepository;
  private final MlAnalysisService mlAnalysisService;
  private final GroqCognitiveAnalysisService groqCognitiveAnalysisService;
  private final ObjectMapper objectMapper;

  public AssessmentService(
      AssessmentSessionRepository repository,
      UserRepository userRepository,
      UserCognitiveSummaryRepository summaryRepository,
      MlAnalysisService mlAnalysisService,
      GroqCognitiveAnalysisService groqCognitiveAnalysisService,
      ObjectMapper objectMapper
  ) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.summaryRepository = summaryRepository;
    this.mlAnalysisService = mlAnalysisService;
    this.groqCognitiveAnalysisService = groqCognitiveAnalysisService;
    this.objectMapper = objectMapper;
  }

  public AssessmentResponse create(CreateAssessmentRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

    MlPrediction prediction = mlAnalysisService.analyze(request.metrics());
    String metricsJson = toMetricsJson(request.metrics());
    String gameType = resolveGameType(request.metrics());

    AssessmentSession session = new AssessmentSession();
    session.setUser(user);
    session.setAge(request.age());
    session.setMetricsJson(metricsJson);
    session.setGameType(gameType);
    session.setModelVersion(prediction.modelVersion());
    session.setRiskScore(prediction.riskScore());
    session.setPredictedDcl(prediction.predictedDcl());
    session.setMlRunAt(Instant.now());

    AssessmentSession saved = repository.save(session);
    updateCognitiveSummary(user, gameType, prediction.riskScore(), request.metrics());
    return AssessmentResponse.from(saved, objectMapper);
  }

  public AssessmentResponse get(UUID id, UUID userId) {
    return repository.findByIdAndUserId(id, userId)
        .map(s -> AssessmentResponse.from(s, objectMapper))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
  }

  public List<AssessmentResponse> getByUser(UUID userId) {
    return repository.findByUserId(userId).stream()
        .map(s -> AssessmentResponse.from(s, objectMapper))
        .toList();
  }

  public CognitiveSummaryResponse getSummary(UUID userId) {
    return summaryRepository.findById(userId)
        .map(CognitiveSummaryResponse::from)
        .orElseGet(() -> new CognitiveSummaryResponse(
            userId, 0, 0d, null, 0, 0, 0, 0, null, null
        ));
  }

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

  private static String resolveGameType(Map<String, Object> metrics) {
    Object gameType = metrics.get("gameType");
    if (gameType != null && !String.valueOf(gameType).isBlank()) {
      return String.valueOf(gameType);
    }
    return "memory";
  }

  private String toMetricsJson(Map<String, Object> metrics) {
    try {
      return objectMapper.writeValueAsString(metrics);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metrics inválidos: no se pueden serializar");
    }
  }
}

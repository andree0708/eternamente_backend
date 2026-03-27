package com.eternamente.assessment.service;

import com.eternamente.assessment.AssessmentSession;
import com.eternamente.assessment.AssessmentSessionRepository;
import com.eternamente.assessment.api.AssessmentAnalysisResponse;
import com.eternamente.assessment.api.AssessmentResponse;
import com.eternamente.assessment.api.CreateAssessmentRequest;
import com.eternamente.assessment.ml.MlAnalysisService;
import com.eternamente.assessment.ml.MlPrediction;
import com.eternamente.assessment.ml.OllamaCognitiveAnalysisService;
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
  private final MlAnalysisService mlAnalysisService;
  private final OllamaCognitiveAnalysisService ollamaCognitiveAnalysisService;
  private final ObjectMapper objectMapper;

  public AssessmentService(
      AssessmentSessionRepository repository,
      UserRepository userRepository,
      MlAnalysisService mlAnalysisService,
      OllamaCognitiveAnalysisService ollamaCognitiveAnalysisService,
      ObjectMapper objectMapper
  ) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.mlAnalysisService = mlAnalysisService;
    this.ollamaCognitiveAnalysisService = ollamaCognitiveAnalysisService;
    this.objectMapper = objectMapper;
  }

  public AssessmentResponse create(CreateAssessmentRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

    MlPrediction prediction = mlAnalysisService.analyze(request.metrics());
    String metricsJson = toMetricsJson(request.metrics());

    AssessmentSession session = new AssessmentSession();
    session.setUser(user);
    session.setAge(request.age());
    session.setMetricsJson(metricsJson);
    session.setModelVersion(prediction.modelVersion());
    session.setRiskScore(prediction.riskScore());
    session.setPredictedDcl(prediction.predictedDcl());
    session.setMlRunAt(Instant.now());

    AssessmentSession saved = repository.save(session);
    return AssessmentResponse.from(saved);
  }

  public AssessmentResponse get(UUID id) {
    return repository.findById(id)
        .map(AssessmentResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
  }

  public List<AssessmentResponse> getByUser(UUID userId) {
    return repository.findByUserId(userId).stream()
        .map(AssessmentResponse::from)
        .toList();
  }

  public AssessmentAnalysisResponse getDetailedAnalysis(UUID id, UUID userId) {
    AssessmentSession session = repository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
    List<AssessmentSession> recentSessions = repository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    String analysis = ollamaCognitiveAnalysisService.analyze(session, recentSessions);
    return new AssessmentAnalysisResponse(session.getId(), ollamaCognitiveAnalysisService.modelName(), analysis);
  }

  private String toMetricsJson(Map<String, Object> metrics) {
    try {
      return objectMapper.writeValueAsString(metrics);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metrics inválidos: no se pueden serializar");
    }
  }
}


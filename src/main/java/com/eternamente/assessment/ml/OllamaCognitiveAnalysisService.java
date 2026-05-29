package com.eternamente.assessment.ml;

import com.eternamente.assessment.AssessmentSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OllamaCognitiveAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(OllamaCognitiveAnalysisService.class);
  private final ObjectMapper objectMapper;

  @Value("${ollama.url:http://127.0.0.1:11434/api/generate}")
  private String ollamaUrl;

  @Value("${ollama.model:phi3:mini}")
  private String ollamaModel;

  @Value("${ollama.connect-timeout-ms:5000}")
  private int connectTimeoutMs;

  @Value("${ollama.read-timeout-ms:180000}")
  private int readTimeoutMs;

  @Value("${ollama.max-tokens:520}")
  private int maxTokens;

  public OllamaCognitiveAnalysisService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String analyze(AssessmentSession session, List<AssessmentSession> history) {
    String prompt = buildPrompt(session, history);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", ollamaModel);
    body.put("stream", false);
    body.put("prompt", prompt);
    Map<String, Object> options = new LinkedHashMap<>();
    options.put("num_predict", maxTokens);
    body.put("options", options);

    try {
      String requestJson = objectMapper.writeValueAsString(body);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(ollamaUrl))
          .header("Content-Type", "application/json")
          .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 1000)))
          .POST(HttpRequest.BodyPublishers.ofString(requestJson))
          .build();

      HttpClient client = HttpClient.newBuilder()
          .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
          .build();
      HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
      String response = httpResponse.body();
      if (httpResponse.statusCode() >= 400) {
        String ollamaError = extractOllamaError(response);
        String reason = humanizeOllamaError(ollamaError == null ? ("HTTP " + httpResponse.statusCode()) : ollamaError);
        log.error("Ollama http error. status={}, url={}, model={}, error={}", httpResponse.statusCode(), ollamaUrl, ollamaModel, ollamaError);
        return fallbackAnalysis(session, history, reason);
      }
      if (response == null || response.isBlank()) {
        return fallbackAnalysis(session, history, "Ollama respondio vacio.");
      }
      JsonNode node = objectMapper.readTree(response);
      String analysis = node.path("response").asText("").trim();
      if (analysis.isEmpty()) {
        return fallbackAnalysis(session, history, "Ollama no devolvio el campo response.");
      }
      analysis = normalizeAnalysisText(analysis);
      log.info("Ollama analysis generated with model {}", ollamaModel);
      return analysis;
    } catch (Exception ex) {
      String reason = humanizeOllamaError(ex.getMessage());
      log.error("Ollama connection/processing error. url={}, model={}, connectTimeoutMs={}, readTimeoutMs={}, message={}",
          ollamaUrl, ollamaModel, connectTimeoutMs, readTimeoutMs, ex.getMessage());
      return fallbackAnalysis(session, history, reason);
    }
  }

  public String modelName() {
    return ollamaModel;
  }

  private String buildPrompt(AssessmentSession session, List<AssessmentSession> history) {
    String historyBlock = summarizeHistory(history);
    return """
        Eres un asistente clinico de apoyo (no diagnostico) para evaluaciones cognitivas en adultos mayores.
        Con base en los datos de sesiones de juego de memoria, genera un analisis breve en espanol claro.

        Reglas de salida:
        1) Responde SOLO en espanol.
        2) Usa exactamente 3 secciones y estos titulos:
           "Interpretacion", "Senales relevantes", "Recomendaciones".
        3) No mezcles portugues, ingles u otro idioma.
        4) No des diagnostico definitivo.
        5) Incluye recomendacion de seguimiento profesional si hay riesgo moderado/alto.
        6) Considera tendencia entre sesiones (si mejora, empeora o se mantiene).
        7) Maximo 320 palabras.

        Sesion objetivo:
        - edad: %d
        - riskScore: %.4f
        - predictedDcl: %s
        - modelVersion: %s
        - metrics(json): %s

        Historial reciente del usuario (mas nuevo primero):
        %s
        """
        .formatted(
            session.getAge() == null ? 0 : session.getAge(),
            session.getRiskScore() == null ? 0d : session.getRiskScore(),
            String.valueOf(session.getPredictedDcl()),
            session.getModelVersion() == null ? "unknown" : session.getModelVersion(),
            session.getMetrics() == null ? "{}" : String.valueOf(session.getMetrics()),
            historyBlock
        );
  }

  private String fallbackAnalysis(AssessmentSession session, List<AssessmentSession> history, String reason) {
    double risk = session.getRiskScore() == null ? 0d : session.getRiskScore();
    String level = risk >= 0.7 ? "alto" : (risk >= 0.4 ? "moderado" : "bajo");
    String trend = describeTrend(history);
    int count = history == null ? 0 : history.size();
    return """
        Interpretacion:
        No se pudo generar analisis con Ollama en este momento (%s). El puntaje de riesgo actual es %.2f (%s).

        Senales relevantes:
        Resultado basado en metricas de la sesion y modelo %s. Sesiones consideradas: %d. Tendencia reciente: %s.

        Recomendaciones:
        Repetir la evaluacion en condiciones similares y, si el riesgo se mantiene moderado o alto, consultar a un profesional de salud para valoracion formal.
        """
        .formatted(
            reason,
            risk,
            level,
            session.getModelVersion() == null ? "unknown" : session.getModelVersion(),
            count,
            trend
        );
  }

  private String summarizeHistory(List<AssessmentSession> history) {
    if (history == null || history.isEmpty()) {
      return "- Sin sesiones previas registradas.";
    }
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
    StringBuilder sb = new StringBuilder();
    int i = 1;
    for (AssessmentSession s : history) {
      String date = s.getCreatedAt() == null ? "sin-fecha" : fmt.format(s.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
      double risk = s.getRiskScore() == null ? 0d : s.getRiskScore();
      sb.append("- #").append(i++)
          .append(" fecha=").append(date)
          .append(", risk=").append(String.format("%.3f", risk))
          .append(", dcl=").append(Boolean.TRUE.equals(s.getPredictedDcl()) ? "si" : "no")
          .append(", model=").append(s.getModelVersion() == null ? "unknown" : s.getModelVersion())
          .append('\n');
    }
    return sb.toString();
  }

  private String describeTrend(List<AssessmentSession> history) {
    if (history == null || history.size() < 2) {
      return "insuficiente informacion";
    }
    double newest = history.get(0).getRiskScore() == null ? 0d : history.get(0).getRiskScore();
    double oldest = history.get(history.size() - 1).getRiskScore() == null ? 0d : history.get(history.size() - 1).getRiskScore();
    double delta = newest - oldest;
    if (Math.abs(delta) < 0.03d) {
      return "estable";
    }
    return delta < 0 ? "mejora (riesgo a la baja)" : "empeora (riesgo al alza)";
  }

  private String humanizeOllamaError(String raw) {
    if (raw == null || raw.isBlank()) {
      return "No fue posible conectar con Ollama.";
    }
    String lower = raw.toLowerCase();
    if (lower.contains("model requires more system memory")) {
      return "Ollama no pudo cargar el modelo por memoria insuficiente. Cierra apps pesadas o usa un modelo mas liviano.";
    }
    if (lower.contains("read timed out")) {
      return "Ollama tardo demasiado en responder (timeout).";
    }
    if (lower.contains("connection refused")) {
      return "Ollama no esta escuchando en el endpoint configurado.";
    }
    if (lower.contains("connect timed out")) {
      return "No se pudo establecer conexion con Ollama a tiempo.";
    }
    return "No fue posible conectar con Ollama.";
  }

  private String extractOllamaError(String responseBody) {
    try {
      if (responseBody == null || responseBody.isBlank()) {
        return null;
      }
      JsonNode node = objectMapper.readTree(responseBody);
      String error = node.path("error").asText("");
      return error.isBlank() ? null : error;
    } catch (Exception ignored) {
      return null;
    }
  }

  private String normalizeAnalysisText(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }
    String normalized = text;
    normalized = normalized.replace("Sinais relevantes", "Senales relevantes");
    normalized = normalized.replace("Sinais", "Senales");
    normalized = normalized.replace("Interpretação", "Interpretacion");
    normalized = normalized.replace("# Interpretación:", "Interpretacion:");
    normalized = normalized.replace("# Interpretacion:", "Interpretacion:");
    normalized = normalized.replace("# Senales relevantes:", "Senales relevantes:");
    normalized = normalized.replace("# Recomendaciones:", "Recomendaciones:");
    return normalized;
  }
}

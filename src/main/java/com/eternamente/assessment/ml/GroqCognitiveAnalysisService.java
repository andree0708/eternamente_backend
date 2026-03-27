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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqCognitiveAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(GroqCognitiveAnalysisService.class);
  private final ObjectMapper objectMapper;

  private String groqApiKey;
  private String groqUrl;
  private String groqModel;
  private int connectTimeoutMs;
  private int readTimeoutMs;
  private int maxTokens;

  public GroqCognitiveAnalysisService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.groqApiKey = System.getenv("GROQ_API_KEY");
    this.groqUrl = System.getenv().getOrDefault("GROQ_URL", "https://api.groq.com/openai/v1/chat/completions");
    this.groqModel = System.getenv().getOrDefault("GROQ_MODEL", "llama-3.1-8b-instant");
    this.connectTimeoutMs = Integer.parseInt(System.getenv().getOrDefault("GROQ_CONNECT_TIMEOUT_MS", "10000"));
    this.readTimeoutMs = Integer.parseInt(System.getenv().getOrDefault("GROQ_READ_TIMEOUT_MS", "60000"));
    this.maxTokens = Integer.parseInt(System.getenv().getOrDefault("GROQ_MAX_TOKENS", "520"));
    
    log.info("GroqCognitiveAnalysisService inicializado - Groq URL: {}, Model: {}", groqUrl, groqModel);
  }

  public String analyze(AssessmentSession session, List<AssessmentSession> history) {
    log.info("Groq API Key configurada: {}", groqApiKey != null && !groqApiKey.isBlank() ? "SI" : "NO");
    log.info("Groq Model a usar: {}", groqModel);
    
    if (groqApiKey == null || groqApiKey.isBlank()) {
      log.warn("Groq API key no configurada o vacia. Usando fallback.");
      return fallbackAnalysis(session, history, "Groq API key no configurada.");
    }

    String prompt = buildPrompt(session, history);

    List<Map<String, Object>> messages = new ArrayList<>();
    Map<String, Object> systemMsg = new LinkedHashMap<>();
    systemMsg.put("role", "system");
    systemMsg.put("content", "Eres un asistente clinico de apoyo para evaluaciones cognitivas en adultos mayores.");
    messages.add(systemMsg);

    Map<String, Object> userMsg = new LinkedHashMap<>();
    userMsg.put("role", "user");
    userMsg.put("content", prompt);
    messages.add(userMsg);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", groqModel);
    body.put("messages", messages);
    body.put("max_tokens", maxTokens);
    body.put("temperature", 0.7);

    try {
      log.info("Enviando peticion a Groq - URL: {}, Model: {}", groqUrl, groqModel);
      String requestJson = objectMapper.writeValueAsString(body);
      log.debug("Request JSON: {}", requestJson);
      
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(groqUrl))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + groqApiKey)
          .timeout(Duration.ofMillis(readTimeoutMs))
          .POST(HttpRequest.BodyPublishers.ofString(requestJson))
          .build();

      HttpClient client = HttpClient.newBuilder()
          .connectTimeout(Duration.ofMillis(connectTimeoutMs))
          .build();
      log.info("Esperando respuesta de Groq...");
      HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
      String response = httpResponse.body();
      log.info("Groq response status: {}, body: {}", httpResponse.statusCode(), response);

      if (httpResponse.statusCode() >= 400) {
        String groqError = extractGroqError(response);
        String reason = humanizeGroqError(groqError == null ? ("HTTP " + httpResponse.statusCode()) : groqError);
        log.error("Groq HTTP error. status={}, url={}, model={}, error={}", httpResponse.statusCode(), groqUrl, groqModel, groqError);
        return fallbackAnalysis(session, history, reason);
      }

      JsonNode node = objectMapper.readTree(response);
      JsonNode choices = node.path("choices");
      if (choices.isEmpty() || !choices.has(0)) {
        log.error("Groq response sin choices: {}", response);
        return fallbackAnalysis(session, history, "Groq no devolvio respuesta.");
      }
      String analysis = choices.get(0).path("message").path("content").asText("").trim();
      if (analysis.isEmpty()) {
        log.error("Groq response con contenido vacio: {}", response);
        return fallbackAnalysis(session, history, "Groq no devolvio contenido.");
      }
      analysis = normalizeAnalysisText(analysis);
      log.info("Groq analysis generated successfully with model {}", groqModel);
      return analysis;
    } catch (Exception ex) {
      String reason = humanizeGroqError(ex.getMessage());
      log.error("Groq connection/processing error. url={}, model={}, message={}, stack={}", groqUrl, groqModel, ex.getMessage(), ex.getStackTrace());
      return fallbackAnalysis(session, history, reason);
    }
  }

  public String modelName() {
    return "groq:" + groqModel;
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
            session.getMetricsJson() == null ? "{}" : session.getMetricsJson(),
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
        No se pudo generar analisis con IA en este momento (%s). El puntaje de riesgo actual es %.2f (%s).

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

  private String humanizeGroqError(String raw) {
    if (raw == null || raw.isBlank()) {
      return "No fue posible conectar con el servicio de IA.";
    }
    String lower = raw.toLowerCase();
    if (lower.contains("invalid_api_key") || lower.contains("api key")) {
      return "API key de Groq invalida o no configurada.";
    }
    if (lower.contains("rate_limit")) {
      return "Limite de solicitudes alcanzado. Intenta mas tarde.";
    }
    if (lower.contains("insufficient_quota")) {
      return "Cuota de Groq agotada.";
    }
    if (lower.contains("read timed out") || lower.contains("timeout")) {
      return "El servicio de IA tardo demasiado en responder.";
    }
    return "No fue posible conectar con el servicio de IA.";
  }

  private String extractGroqError(String responseBody) {
    try {
      if (responseBody == null || responseBody.isBlank()) {
        return null;
      }
      JsonNode node = objectMapper.readTree(responseBody);
      JsonNode error = node.path("error");
      String message = error.path("message").asText("");
      return message.isBlank() ? null : message;
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

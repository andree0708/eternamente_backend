package com.eternamente.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException ex) {
    String code = "HTTP_" + ex.getStatusCode().value();
    String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
    return ApiResponse.error(HttpStatus.valueOf(ex.getStatusCode().value()), code, message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Datos inválidos")
        .orElse("Revisa los datos del formulario");
    return ApiResponse.error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
    log.error("Violación de integridad en BD: {}", ex.getMostSpecificCause().getMessage());
    String detail = ex.getMostSpecificCause().getMessage();
    String message = detail != null && detail.contains("user_external_id")
        ? "Error al guardar la partida: columna legacy en base de datos. Despliega la migración V7 del backend."
        : "No se pudo guardar en la base de datos. Revisa que las migraciones Flyway estén aplicadas.";
    return ApiResponse.error(HttpStatus.CONFLICT, "DB_INTEGRITY_ERROR", message);
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataAccess(org.springframework.dao.DataAccessException ex) {
    log.error("Error de acceso a datos: {}", ex.getMostSpecificCause().getMessage(), ex);
    return ApiResponse.error(
        HttpStatus.CONFLICT,
        "DB_ERROR",
        "Error de base de datos. Comprueba que Flyway aplicó las migraciones V7-V9."
    );
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<ApiResponse<Void>> handleTransactionSystem(TransactionSystemException ex) {
    Throwable cause = ex.getRootCause();
    log.error("Error de transacción: causa raíz={}: {}", cause != null ? cause.getClass().getSimpleName() : "null", ex.getMessage(), ex);
    if (cause instanceof ResponseStatusException rse) {
      return handleStatus(rse);
    }
    return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "TRANSACTION_ERROR",
        "Error en la transacción: " + (cause != null ? cause.getMessage() : ex.getMessage()));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
    return ApiResponse.error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ruta no encontrada: " + ex.getRequestURL());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
    return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método no soportado: " + ex.getMethod());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
    return ApiResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
        "Tipo de contenido no soportado: " + ex.getContentType());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
    String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
    log.warn("Error de lectura del body: {}", detail, ex);
    return ApiResponse.error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "El cuerpo de la solicitud es inválido: " + detail);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
    log.error("Error no controlado: {}", ex.getMessage(), ex);
    String msg = ex.getMessage();
    if (msg != null && msg.contains("user_external_id")) {
      return ApiResponse.error(
          HttpStatus.CONFLICT,
          "DB_LEGACY_COLUMN",
          "Error al guardar: actualiza el backend (migración V10) en Render."
      );
    }
    return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor");
  }
}

package com.eternamente.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
    log.error("Error no controlado: {}", ex.getMessage(), ex);
    return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor");
  }
}

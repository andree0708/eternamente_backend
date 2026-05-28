package com.eternamente.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
    log.error("Error no controlado: {}", ex.getMessage(), ex);
    return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor");
  }
}

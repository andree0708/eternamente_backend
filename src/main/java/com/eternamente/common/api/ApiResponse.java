package com.eternamente.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiMeta meta,
    ApiError error
) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, ApiMeta.now(), null);
  }

  public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ok(data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> entity(T data) {
    return ResponseEntity.ok(ok(data));
  }

  public static ApiResponse<Void> fail(String code, String message) {
    return new ApiResponse<>(false, null, ApiMeta.now(), new ApiError(code, message));
  }

  public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(fail(code, message));
  }
}

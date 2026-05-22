package com.eternamente.user.api;

import com.eternamente.common.api.ApiResponse;
import com.eternamente.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** POST /api/v1/auth/login — Inicio de sesión */
  @PostMapping("/auth/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ApiResponse.entity(response);
  }

  /** POST /api/v1/users — Registro de usuario */
  @PostMapping("/users")
  public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ApiResponse.created(response);
  }
}

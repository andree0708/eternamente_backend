package com.eternamente.user.api;

import com.eternamente.common.api.ApiResponse;
import com.eternamente.user.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final AuthService authService;

  public UserController(AuthService authService) {
    this.authService = authService;
  }

  /** GET /api/users/me — Perfil del usuario autenticado */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.entity(authService.getCurrentUser(userId));
  }
}

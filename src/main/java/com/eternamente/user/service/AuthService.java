package com.eternamente.user.service;

import com.eternamente.user.User;
import com.eternamente.user.UserRepository;
import com.eternamente.user.api.AuthResponse;
import com.eternamente.user.api.LoginRequest;
import com.eternamente.user.api.RegisterRequest;
import com.eternamente.user.api.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este correo ya está registrado");
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(ClientPasswordSupport.normalizeForStorage(request.password())));
    user.setRole(request.role());
    user.setFullName(request.fullName());
    user.setAge(request.age());

    User saved = userRepository.save(user);
    String token = jwtService.generateToken(saved.getId(), saved.getEmail());

    return new AuthResponse(token, saved);
  }

  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

    String normalized = ClientPasswordSupport.normalizeForStorage(request.password());
    if (!passwordEncoder.matches(normalized, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }

    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, user);
  }

  public UserResponse getCurrentUser(UUID userId) {
    return userRepository.findById(userId)
        .map(UserResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
  }
}

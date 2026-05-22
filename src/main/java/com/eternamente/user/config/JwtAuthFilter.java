package com.eternamente.user.config;

import com.eternamente.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return "/".equals(path)
        || "/health".equals(path)
        || path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authHeader = resolveAuthHeader(request);
    if (authHeader == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

    try {
      if (jwtService.isTokenValid(token)) {
        UUID userId = jwtService.extractUserId(token);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);
      } else if (request.getRequestURI().startsWith("/api/")) {
        log.warn("JWT inválido o expirado en {} {}", request.getMethod(), request.getRequestURI());
      }
    } catch (Exception e) {
      log.warn("JWT no procesable en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  private static String resolveAuthHeader(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      return authorization;
    }
    String xAuth = request.getHeader("X-Auth-Token");
    if (xAuth != null && !xAuth.isBlank()) {
      return "Bearer " + xAuth.trim();
    }
    return null;
  }
}

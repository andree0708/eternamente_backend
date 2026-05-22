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
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authHeader = resolveAuthHeader(request);
    log.info("JWT Filter - Method: {}, Path: {}, Auth: {}", request.getMethod(), request.getRequestURI(), authHeader != null ? "present" : "null");

    if (authHeader == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    log.info("JWT Filter - Token present: {}", token.substring(0, Math.min(20, token.length())));

      try {
      if (jwtService.isTokenValid(token)) {
        UUID userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        log.info("JWT Filter - Token valid for user: {} ({})", email, userId);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.info("JWT Filter - Authentication set in context. Authorities: {}", 
            SecurityContextHolder.getContext().getAuthentication().getAuthorities());
      } else {
        log.warn("JWT Filter - Token invalid or expired");
      }
    } catch (Exception e) {
      log.error("JWT Filter - Error processing token: {}", e.getMessage());
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

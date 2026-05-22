package com.eternamente.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro CORS con prioridad máxima: refleja el Origin cuando es Vercel o localhost.
 * Complementa la configuración de Spring Security para preflight OPTIONS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilterConfig extends OncePerRequestFilter {

  private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String origin = request.getHeader("Origin");

    if (origin != null && isAllowedOrigin(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Credentials", "true");
      response.setHeader("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
      response.setHeader("Access-Control-Allow-Headers", "Authorization, X-Auth-Token, Content-Type, Accept, Origin");
      response.setHeader("Access-Control-Expose-Headers", "Authorization");
      response.setHeader("Access-Control-Max-Age", "3600");
    }

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static boolean isAllowedOrigin(String origin) {
    if (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:")) {
      return true;
    }
    if (origin.startsWith("https://") && origin.endsWith(".vercel.app")) {
      return true;
    }
    return false;
  }
}

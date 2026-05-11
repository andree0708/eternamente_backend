package com.eternamente.user.config;

import com.eternamente.user.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
  private final JwtAuthFilter jwtAuthFilter;

  @Value("${cors.origins:}")
  private String corsOrigins;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/api/assessments/**").authenticated()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex
            .accessDeniedHandler((req, res, e) -> {
              log.error("Access denied: {}", e.getMessage());
              res.setStatus(403);
              res.getWriter().write("{\"error\":\"Access denied\"}");
            })
            .authenticationEntryPoint((req, res, e) -> {
              log.error("Authentication failed: {}", e.getMessage());
              res.setStatus(401);
              res.getWriter().write("{\"error\":\"Authentication required\"}");
            })
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    log.info("CORS_ORIGINS configurado: '{}'", corsOrigins);
    
    if (corsOrigins != null && !corsOrigins.isBlank()) {
      List<String> origins = Arrays.stream(corsOrigins.split(","))
          .map(String::trim)
          .map(s -> s.replaceAll("/$", ""))
          .filter(s -> !s.isEmpty())
          .toList();
      configuration.setAllowedOrigins(origins);
      log.info("Origenes CORS permitidos: {}", origins);
    } else {
      configuration.setAllowedOrigins(List.of(
        "http://localhost:4321", 
        "http://localhost:3000", 
        "http://127.0.0.1:4321", 
        "http://127.0.0.1:3000",
        "https://eterna-mente-4brwsb4mp-andree0708s-projects.vercel.app"
      ));
      log.warn("No se configuró CORS_ORIGINS, usando origenes locales por defecto");
    }
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
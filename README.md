# EternaMente — Backend

API REST Spring Boot para evaluaciones cognitivas.

## Patrones de software utilizados

| Patrón | Dónde | Para qué |
|--------|-------|----------|
| **Layered Architecture** | `api` → `service` → `repository` | Separación de responsabilidades |
| **Repository** | `*Repository.java` | Acceso a datos JPA |
| **Service Layer** | `*Service.java` | Lógica de negocio |
| **DTO / Record** | `api/*Response.java`, `*Request.java` | Contratos de entrada/salida |
| **Facade** | `ApiResponse.java` | Respuesta HTTP uniforme |
| **Strategy** | `MlAnalysisService` + implementaciones | Análisis por reglas / Groq / Ollama |
| **Template Method** | `RuleBasedMlAnalysisService` switch por `gameType` | Algoritmo según juego |
| **Filter Chain** | `JwtAuthFilter`, `CorsFilterConfig` | Seguridad y CORS |
| **Dependency Injection** | Spring `@Service`, `@RestController` | Inversión de control |
| **Adapter** | `GroqCognitiveAnalysisService` | Integración API externa Groq |
| **Builder** | JWT `Jwts.builder()` | Construcción de tokens |
| **Migration (Flyway)** | `db/migration/V*.sql` | Evolución del esquema |

## Estructura de paquetes

```
com.eternamente/
├── common/api/          # ApiResponse, GlobalExceptionHandler
├── user/              # Auth, JWT, usuarios
└── assessment/        # Partidas, ML, resumen cognitivo
```

## API

Ver [docs/API.md](docs/API.md).

## Variables de entorno

Ver `.env.example`.

## Ejecutar

```bash
./gradlew bootRun
```

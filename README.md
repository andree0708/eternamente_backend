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
| **Strategy** | `MlAnalysisService` + implementaciones | Pipeline cognitivo (IF + estadístico) o reglas por juego |
| **Template Method** | `RuleBasedMlAnalysisService` switch por `gameType` | Fallback si hay &lt; 3 sesiones |
| **Pipeline ML** | `CognitiveMlAnalysisService` | FeatureExtractor → Isolation Forest → alerta NORMAL/WATCH/ALERT |
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
    └── ml/            # FeatureExtractor, IsolationForest, CognitiveAnalyzer
```

### Motor ML (`ml.engine`)

- `cognitive` (default): 14 features, Isolation Forest, estimador estadístico (fallback TFLite), alertas según doc. migración.
- `rules`: solo heurísticas por juego (partidas 1–2 del usuario).

## API

Ver [docs/API.md](docs/API.md).

## Variables de entorno

Ver `.env.example`.

## Ejecutar

```bash
./gradlew bootRun
```

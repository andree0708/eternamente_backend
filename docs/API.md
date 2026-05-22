# API EternaMente — Estructura uniforme

Todas las respuestas exitosas y de error siguen el mismo formato:

```json
{
  "success": true,
  "data": { },
  "meta": { "timestamp": "2026-05-22T...", "version": "v1" },
  "error": null
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "meta": { "timestamp": "...", "version": "v1" },
  "error": { "code": "UNAUTHORIZED", "message": "Autenticación requerida" }
}
```

## Autenticación (`/api`)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Iniciar sesión → `AuthResponse` |
| POST | `/api/users` | No | Registro → `AuthResponse` |

## Usuarios (`/api/users`)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/users/me` | Sí | Perfil del usuario actual |

## Evaluaciones (`/api/assessments`)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/assessments` | Sí | Guardar partida → `AssessmentResponse` |
| GET | `/api/assessments` | Sí | Listar mis partidas |
| GET | `/api/assessments/summary` | Sí | Resumen cognitivo acumulado |
| GET | `/api/assessments/{id}` | Sí | Detalle de una partida |
| GET | `/api/assessments/{id}/analysis` | Sí | Análisis IA de la partida |

Headers de autenticación: `Authorization: Bearer <token>` o `X-Auth-Token: <token>`.

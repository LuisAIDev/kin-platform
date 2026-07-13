# KIN Platform — Documentación Completa de la Sesión

## Visión General

Plataforma full-stack de gestión de proyectos con evaluación de viabilidad asistida por IA.
Arquitectura: Spring Boot 3.2.5 (backend) + Next.js 16 App Router (frontend) + PostgreSQL/H2 (base de datos).

---

## 1. Estructura del Proyecto

| Directorio | Propósito |
|---|---|
| `kin-backend/` | Backend Spring Boot 3.2.5 / Java 17 — Maven |
| `kin-frontend/` | Frontend Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| `kin-database/` | Scripts de inicialización PostgreSQL (usados por Docker) |
| `kin-docs/` | Documentación del proyecto |

---

## 2. Commits Realizados

### Commit 1 — `43044d6` — Initial commit
- Commit inicial del proyecto
- KIN Platform con Spring Boot + Next.js + autenticación, proyectos y chat IA

### Commit 2 — `42b20c2` — Implementar streaming SSE en chat IA y corregir bug de mensajes duplicados
- **Fecha:** 2026-07-03
- **Archivos modificados:** 7
- **Cambios:**
  - `AiEngineService.java` — Nuevo método `generateAiResponseStream()` que devuelve `Flux<String>` para streaming desde Ollama con fallback a mock
  - `ChatController.java` — Nuevo endpoint `POST /chat/stream` que produce `TEXT_EVENT_STREAM_VALUE` usando `SseEmitter`
  - `ChatOrchestratorService.java` — Nueva interfaz `processMessageStream()` que retorna `SseEmitter`
  - `ChatOrchestratorServiceImpl.java` — Implementación completa del streaming: suscripción al Flux, envío de eventos SSE (token/error/done), guardado de mensajes al completar
  - `application.yml` — Aumento de `max-swallow-size: 2MB`; cambio de JWT secret a valor hardcodeado
  - `page.tsx` (frontend) — Refactor completo del envío de mensajes: usa streaming, muestra cursor pulsante, IDs temporales con prefijo `streaming-`
  - `chat.ts` (frontend) — Nuevo método `sendMessageStream()` con `fetch` nativo, lectura de SSE con `ReadableStream`, callbacks `onToken/onDone/onError`, soporte de `AbortController`

---

## 3. Arquitectura del Backend

### Paquetes

| Paquete | Responsabilidad |
|---|---|
| `com.kinplatform.auth` | Registro, login, emisión de JWT |
| `com.kinplatform.user` | Entidad User, roles (FREE, PREMIUM, FACILITADOR, ADMIN) |
| `com.kinplatform.project` | CRUD de proyectos + categorías, estados, puntuación de viabilidad |
| `com.kinplatform.chat` | Historial de mensajes, streaming SSE (`/chat/stream`), orquestación |
| `com.kinplatform.ai` | Integración con Ollama + fallback mock en español |
| `com.kinplatform.common.config` | CORS + cadena de filtros Security (stateless JWT) |
| `com.kinplatform.common.security` | `JwtService`, `JwtAuthenticationFilter` |

### Flujo de Streaming SSE

1. Frontend llama a `POST /api/v1/projects/{projectId}/chat/stream`
2. `ChatController` delega a `ChatOrchestratorServiceImpl.processMessageStream()`
3. El servicio guarda el mensaje del usuario, carga el historial, e invoca `AiEngineService.generateAiResponseStream()`
4. `AiEngineService` llama a Ollama vía Spring AI con `chatClient.prompt().stream().content()` (Flux reactivo)
5. Si Ollama falla, se usa `Flux.just(mockResponse(...))` como fallback
6. `ChatOrchestratorServiceImpl` se suscribe al Flux y envía eventos SSE:
   - `event: token` → `data: {"token": "..."}` (por cada token generado)
   - `event: error` → `data: {"error": "..."}` (si hay error)
   - `event: done` → `data: {"done":true, "userMessageId":"...", "assistantMessageId":"...", "content":"...", "tokensUsed":N}` (al completar)
7. El mensaje del asistente se persiste en BD al finalizar el stream

### Endpoints de Chat

- `POST /api/v1/projects/{projectId}/chat` — Chat clásico (respuesta completa)
- `POST /api/v1/projects/{projectId}/chat/stream` — Chat con streaming SSE
- `POST /api/v1/projects/{projectId}/messages` — Guardar mensaje
- `GET /api/v1/projects/{projectId}/messages` — Obtener historial

---

## 4. Arquitectura del Frontend

### Streaming de Chat

El frontend usa `fetch()` nativo con `ReadableStream` para leer eventos SSE:

1. **Inicio:** Se crean dos mensajes optimistas (usuario + asistente vacío)
2. **Streaming:** Cada token recibido se concatena al contenido del mensaje asistente, actualizando el estado en React
3. **Finalización:** Al recibir `event: done`, se reemplaza el ID temporal del mensaje asistente con el ID real del backend
4. **Cancelación:** Se usa `AbortController` para limpiar el stream al desmontar el componente
5. **Indicador visual:** Cursor pulsante (`animate-pulse`) mientras el mensaje asistente está en streaming

### Servicio `chatService.sendMessageStream()`

- Parámetros: `projectId`, `content`, `callbacks (onToken, onDone, onError)`
- Retorna: `AbortController` para cancelación
- Parseo SSE: buffer de líneas con manejo de eventos `event:` y `data:`
- Soporta cancelación vía `AbortError` (sin llamar a `onError`)
- URL construida desde `NEXT_PUBLIC_API_URL`

### Configuración de Frontend

- Alias `@/*` → `./src/*`
- `NEXT_PUBLIC_API_URL` → default `http://localhost:8080/api/v1`
- Auth middleware en `src/proxy.ts`: protege `/dashboard`, redirige `/login`

---

## 5. Configuración de Desarrollo

### Backend (H2, sin Docker)

```bash
cd kin-backend && ./mvnw spring-boot:run
# http://localhost:8080/api/v1
```

- `ddl-auto: update` — esquema creado automáticamente por JPA
- Base de datos H2 file-based en `data/kindb`
- PostgreSQL dependencia comentada en `pom.xml` (solo se usa con Docker)

### Frontend

```bash
cd kin-frontend && npm install && npm run dev
# http://localhost:3000
```

- `npm run lint` para ESLint 9 (core-web-vitals + TypeScript)

### Full Stack con Docker

```bash
docker compose up --build  # desde la raíz del repo
```

- Usa PostgreSQL con `ddl-auto: validate`
- Esquema desde `kin-database/init.sql`
- `.env` es gitignored — copiar `.env.example` a `.env`

---

## 6. Quirks y Notas Importantes

- **Dual CORS:** Tanto `CorsConfig.java` como `SecurityConfig.java` configuran CORS. `SecurityConfig` tiene prioridad (Spring Security filter chain). Agregar nuevos orígenes en ambos.
- **AI Engine:** `AiEngineService.java` llama a Ollama (`llama3.2`) pero cae a respuestas mock en español si falla — seguro para desarrollo sin Ollama.
- **JWT Secret:** Hardcodeado en `application.yml` como `RuSedLdVxrG4HOIwc2YNFOKcwtOA3umG8KSCBMMy7Xo=` (no commitear a producción).
- **No hay tests** aún (en roadmap).
- **`max-swallow-size: 2MB`** configurado en Tomcat para manejar peticiones grandes.

---

## 8. Configuración de opencode

Archivo `opencode.json` en la raíz:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "context7": {
      "type": "local",
      "command": ["npx", "-y", "@upstash/context7-mcp"],
      "enabled": true
    }
  }
}
```

MCP habilitado: **context7** (documentación de librerías vía `@upstash/context7-mcp`).
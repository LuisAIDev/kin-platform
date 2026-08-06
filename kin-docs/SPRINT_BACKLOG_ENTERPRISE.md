# Sprint Backlog Enterprise — Proyecto KIN

> 6 sprints (2 semanas c/u) construidos sobre `TECHNICAL_DEBT_REGISTER.md` (`T#`) y `ENTERPRISE_REMEDIATION_PLAN.md` (Oleadas).
> Cada ticket define: **ID, título, descripción, archivo(s) afectados, impacto, prioridad, esfuerzo (S/M/L/XL), dependencias y Criterios de Aceptación (CA)**.
> Estimación total: **~26 pt**.

---

## Sprint 0 — Fundación y quick wins (objetivo: riesgo P0)

| ID | Título | Esf. | Depende de | Archivos | CA |
|---|---|---|---|---|---|
| K-401 | Rotar secretos commitidos + detección en CI | S | — | `.github/workflows/*`, `.env`, secrets manager | Secretos fuera del repo; gateway de fallo si falta el secret; detector (gitleaks/trufflehog) bloqueante en CI |
| K-402 | Cerrar `/actuator/**` + errores genéricos | S | — | `SecurityConfig.java`, `GlobalExceptionHandler.java` | Actuator restringido a roles/red; `GlobalExceptionHandler` devuelve mensaje genérico + `errorId`; logs detallados server-side |
| K-202 | Arreglar caché `activeSubscription` (o deshabilitarla) | S | — | `SubscriptionValidatorService.java` | `getActiveSubscription` deja de ser self-invocation (se llama a través de proxy anotado) o se elimina el `@Cacheable`; test verifica invalidación/relectura |
| K-203 | Evict `projectLimit` al borrar proyecto | S | K-202 | `ProjectServiceImpl`, `SubscriptionValidatorService` | Tras borrar proyecto, `canCreateProject` refleja el nuevo límite (test de integración) |
| K-505 | Frontend: no hacer `forceLogout` en cualquier error | S | — | `projects/page.tsx:34-46`, `[id]/page.tsx:48-59` | Los 4xx/5xx temporales no cierran sesión; solo 401/403 disparan logout |
| K-507 | Reducir logs de contenido sensible + deduplicar token | S | — | `ChatController.java`, `src/services/*.ts` | El mensaje del usuario no se loguea; un solo módulo `token` reutilizado en los 5 servicios |

## Sprint 2 — Transacción y correcciones de núcleo (K-101)

| ID | Título | Esf. | Deps | Archivos | CA |
|---|---|---|---|---|---|
| K-101 | Sacar I/O IA del `@Transactional` | M | K-402 | `ChatOrchestratorServiceImpl.java:73` | `processMessage` ya no ejecuta IA dentro de la transacción; la persistencia de mensaje es su propia tx corta; test de no bloqueo de pool |
| K-102 | Tests de la ruta de chat (bloqueante + stream) con backend real | M | K-101 | tests integración | Test de la ruta completa con H2 de test; verifica ownership (IDOR) y flujo SSE |
| K-506 | Consolidar CORS (una sola fuente de verdad) | S | — | `CorsConfig`, `SecurityConfig` | Solo una clase define CORS; test verifica preflight |

## Sprint 3 — Pagos e idempotencia (K-301)

| ID | Título | Esf. | Deps | Archivos | CA |
|---|---|---|---|---|---|
| K-301 | Idempotencia de webhook Stripe | M | — | `StripeWebhookController`, entidad `webhook_event` | Repetir el mismo `event.id` se aplica 1 vez; test de replay; reconciliación manual documentada |
| K-302 | Añadir test de integración del webhook (Testcontainers opc) | M | K-301 | test | Verde en reentregas/duplicados |
| K-303 | Validar checkout y licencias con Premium/FACILITADOR | S | K-301 | `SubscriptionService`, `SubscriptionController` | Estados PREMIUM/FACILITADOR correctos tras checkout y webhook |

## Sprint 4 — Auth y rate limiting (K-501…506)

| ID | Título | Esf. | Deps | Archivos | CA |
|---|---|---|---|---|---|
| K-501 | Rate limiting por IP real (no spoofeable) | M | — | `RateLimitingFilter`, gateway | `getClientIP` solo confía en proxy autenticado; token bucket compartido; cobertura endpoint |
| K-502 | Fuerza de password + anti-enumeración | S | — | `AuthServiceImpl.register` | Min 12 chars con variación; mensaje genérico en register; test negativo |
| K-503 | Migrar token a HttpOnly cookie segura | M | K-504 | `session.ts`, `proxy.ts`, JwtFilter | Token no accesible por JS; `SameSite=Lax`+Secure; test XSS del flujo |
| K-504 | Cachear `/auth/me` (evitar por-request) | S | — | `proxy.ts` | Validación de sesión cacheada con TTL corto; test del middleware |
| K-506 | Headers de seguridad (HSTS, CSP, nosniff) | S | — | `src/proxy.ts`, nginx/Render | Cabeceras presentes; CSP sin unsafe-inline; test de respuesta |

## Sprint 5 — Testing y calidad (K-401..410 reales / integración)

| ID | Títias | Esf. | Deps | Archivos | CA |
|---|---|---|---|---|---|
| K-401 | Introducir Testcontainers (Postgres) | L | — | `pom.xml`, módulo de datos | Migraciones Flyway se ejecutan contra Postgres real; al menos un `@SpringBootTest` con TC |
| K-404 | Gates de cobertura en CI (JaCoCo `<check>`) | S | — | CI, `pom.xml` | Fallo de build si ramas < umbral en módulos objetivo |
| K-410 | E2E del flujo de viabilidad (Playwright) | M | K-102 | `tests/*.spec.ts` | Crear proyecto → chat → reporte con backend real; accesible en CI |
| K-411 | Tests de concurrencia/idempotencia (webhook) | S | K-301 | test | Replay concurrente del mismo evento=1 aplicación |

## Sprint 6 — Observabilidad e infraestructura (K-601..606)

| ID | Título | Esf. | Deps | Archivos | CA |
|---|---|---|---|---|---|
| K-601 | `DATABASE_URL` JDBC + secrets en Render | S | K-401 | `render.yaml`, `application-prod.properties` | Arranque en prod con DB externa |
| K-602 | Logs JSON + tracing distribuido (OTel) | M | K-101 | `application.yml`, dependencias | Cada línea log con `traceId`/`spanId`; export OTLP |
| K-603 | Métricas HTTP (RED) + JVM por endpoint | M | K-602 | Micrometer/Prometheus | Endpoints con counters/ histograms; dashboards |
| K-605 | Métricas de coste/tokens LLM | M | — | `PromptAssembler`/`ProviderRouter`| Registro de tokens por turno; agregación de coste |
| K-606 | Caché distribuida (Redis) configurable | M | K-101 | `application.yml`, caché | `spring.cache.type=redis` en prod; invalidation correcta |
| K-701 | Sincronizar docs (ADR 19, etapas 13, cobertura) | S | — | README/BASELINE/AGENTS | Docs reflejan el estado real del sistema |
| K-702 | Playbook de incidentes + SLOs | S | K | docs | Alertas de error rate y p95; Runbook redacción |

---

## Notas

- Los sprints 1-2 cubren **todo P0** y la corrección de transacción; los sprints 3-4 resuelven pagos + auth; los 5-6 sanean la infraestructura de testing, observabilidad y docs.
- **Dependencia clave**: `K-101` (transacciones) debe preceder a `K-102`/tests de ruta; `K-304/K-303` preceden a `K-410` (E2E).
- El contrato `kin/engine` y las APIs estables no se modifican.
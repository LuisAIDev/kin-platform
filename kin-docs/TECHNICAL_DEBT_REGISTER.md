# Registro de Deuda Técnica — Proyecto KIN

> Registro vivo de la deuda técnica identificada en la **Segunda Auditoría Crítica**.
> Formato: cada item tiene ID (`T#`), título, descripción, evidencia (`archivo:línea` o comando), impacto (Usuarios/Negocio/Seguridad/Rendimiento/Mantenibilidad), prioridad (P0..P3), esfuerzo (S/M/L/XL), dependencias y ticket de backlog asociado (`K-###`).
> Fuentes: `target/site/jacoco/jacoco.csv`, `frontend-ci.yml:99`, `SecurityConfig.java:52`, `GlobalExceptionHandler.java`, `SubscriptionValidatorService.java`, `ChatOrchestratorServiceImpl.java:73`, `ChatController.java`, `RateLimitingFilter.java`, `AuthServiceImpl.java`, `render.yaml`, `src/services/session.ts`, `src/proxy.ts`, `projects/page.tsx:34`.

---

## Resumen ejecutivo

- Total items: **24** (9 nuevos de auditoría 2 + 15 heredados de la auditoría 1).
- P0 (inmediato): **6** · P1 (sprint 1-2): **8** · P2 (sprint 3-5): **7** · P3 (backlog): **3**.
- Deuda estimada: **~26 pt de esfuerzo** (S=1, M=2, L=3, XL=5). Distribución sugerida en `SPRINT_BACKLOG_ENTERPRISE.md`.

---

## Deuda nueva (Auditoría 2)

### P0 — Crítico (resolver de inmediato)

| ID | Título | Descripción / Impacto | Evidencia | Esf. | Ticket |
|---|---|---|---|---|---|
| T1 | Caché `activeSubscription` muerta | `@Cacheable("activeSubscription")` en `getActiveSubscription`, pero el método **solo se invoca por self-invocation** (sin proxy AOP) → la caché nunca se lee ni se invalida. Impacto: lógica de licencias incorrecta (si la caché estuviera viva) y código engañoso. | `SubscriptionValidatorService.java:132`; sin llamadores externos | S | K-202 |
| T2 | `projectLimit` no se invalida al borrar proyecto | `ProjectServiceImpl.delete` no evicta la clave `projectLimit` de caché; el límite de proyectos puede quedar desactualizado (se pueden crear menos de los permitidos tras borrar). | `SubscriptionValidatorService.evictProjectLimitCache` solo en create/update; `ProjectServiceImpl.java:35` | S | K-203 |
| T3 | `@Transactional` cubre llamadas IA | `processMessage` es `@Transactional` (L73) y dentro invoca la IA (segundos) + streaming Reactor. Mantiene conexión JDBC durante I/O externa → riesgo de agotar pool de conexiones y de transacciones largas. | `ChatOrchestratorServiceImpl.java:73` | M | K-101 |
| T4 | Webhook Stripe sin idempotencia | `StripeWebhookController` procesa eventos sin validar `event.id`; reentregas de Stripe pueden aplicarse dos veces (cambio doble de licencia). | `StripeWebhookController.java` (`constructWebhookEvent`) | M | K-301 |
| T5 | JWT_SECRET commitida | Secret de producción en el repo CI (valor base64). Crítico para JWT firmados (HS256). | `.github/workflows/frontend-ci.yml:99` | S | K-401 |
| T6 | `/actuator/**` abierto y detalles de error expuestos | `permitAll` en actuator + `GlobalExceptionHandler` devuelve `ex.getMessage()` (detalles internos, stack, config) a clientes. | `SecurityConfig.java:52`; `GlobalExceptionHandler.java:20,39,46,53` | S | K-402 |

### P1 — Alta

| ID | Título | Descripción / Impacto | Evidencia | Esf. | Ticket |
|---|---|---|---|---|---|
| T7 | Rate limiting spoofeable por IP | `RateLimitingFilter` confía en `getClientIP()` (headers que el cliente puede setear) y usa `synchronized` + `ConcurrentHashMap` por bucket. Un atacante rota el header para bypasar el límite; solo cubre `/auth/**`. | `RateLimitingFilter.java` (conteo de path `/auth`, `getClientIP`) | M | K-501 |
| T8 | Enumeración de cuentas + password sin fuerza | `register` responde "Email already registered: {email}" (enumera) y no valida longitud/complejidad de password. | `AuthServiceImpl.register` | S | K-502 |
| T9 | Token de sesión en `localStorage` (XSS) | JWT en `localStorage` + cookie; en caso de XSS se exfiltra el token. Se debe migrar a HttpOnly cookie segura. | `src/services/session.ts:38-43` | M | K-503 |
| T10 | `/auth/me` en cada request del middleware | El proxy llama a `/auth/me` por cada request protegido → latencia y fricción. | `src/proxy.ts:16,38` | S | K-504 |
| T11 | Frontend: `Promise.all(...).catch(() => forceLogout())` | Cualquier error de fetch (500 temporal) cierra la sesión del usuario en `projects/page.tsx:34-46` y `[id]/page.tsx:48-59` → falsos logout. | `src/app/dashboard/projects/page.tsx:34-46`, `[id]/page.tsx:48-59` | S | K-505 |
| T12 | CORS duplicado (dual config) | Reglas de CORS en `CorsConfig` y `SecurityConfig` a la vez → riesgo de divergencia. | `CorsConfig.java`, `SecurityConfig.java:20-21,44,89,105` | S | K-506 |
| T13 | Logs con contenido sensible | `ChatController.chat` registra el texto del mensaje del usuario en `log.info`. Datos de negocio/clientes en logs. | `ChatController.java` ("Mensaje del usuario: {}", getContent()) | S | K-507 |
| T14 | `DATABASE_URL` no JDBC en Render | `render.yaml` usa `connectionString` de Postgres (`postgresql://`) mientras Spring espera `jdbc:postgresql://...` → riesgo de falla de arranque en prod. | `render.yaml`; `application-prod.properties` | S | K-601 |

### P2 — Media

| ID | Título | Descripción / Impacto | Evidencia | Esf. | Ticket |
|---|---|---|---|---|---|
| T15 | Sin Testcontainers / integración real | Solo 1 `@SpringBootTest`, 0 `@WebMvcTest`, 0 Testcontainers; las integraciones con repos/JPA dependen de mocks y del H2 de memoria. | `pom.xml` (sin testcontainers); conteo de anotaciones | L | K-401 |
| T16 | Sin tracing distribuido | No hay Micrometer Tracing / OpenTelemetry; imposible trazar una request end-to-end (frontend→API→IA). | `pom.xml` | M | K-602 |
| T17 | Logs no estructurados | Sin appender JSON/logback; logs legibles solo en texto plano, sin correlación de trace id. | `application.yml` | M | K-603 |
| T18 | Sin gate de calidad en CI | JaCoCo configurado pero sin `<check>`; no hay Checkstyle/Spotless/Sonar. La cobertura puede degradarse sin aviso. | `pom.xml` (solo plugin JaCoCo sin check) | S | K-604 |
| T19 | Métricas de IA/coste no registradas | No hay registro de tokens/coste por turno; imposible controlar el gasto LLM. | (sin servicio de métricas) | M | K-605 |
| T20 | Sesión/multi-instancia | Caché local `simple` no sirve para multi-replica; `spring.cache.type` no configurable para Redis. | `application.yml` | M | K-606 |
| T21 | Duplicación de lógica de token en frontend | 5 servicios duplican manejo de token (api/auth/chat/session). | `src/services/api.ts:10`, `auth.ts`, `chat.ts:31`, `session.ts:49` | M | K-507 |

### P3 — Baja

| ID | Título | Descripción / Impacto | Evidencia | Esf. | Ticket |
|---|---|---|---|---|---|
| T22 | Docs desincronizadas | README dice 18 ADR (hay 19); BASELINE dice 12 etapas (hay 13); cobertura reportada vs real. | `README`, `BASELINE_ARCHITECTURE.md` vs `KinConfig`/`jacoco.csv` | S | K-701 |
| T23 | Sin playbook de incidentes / alertas | No hay SLO/SLA documentados ni alertas de p95/error-rate. | (sin doc) | S | K-702 |
| T24 | Sin IaC (Terraform) | `render.yaml` + `docker-compose.yml` manuales; sin replicas ni DR documentados. | `render.yaml`, `docker-compose.yml` | L | K-703 |

---

## Deuda heredada (Auditoría 1, mantenida)

| ID | Título | Impacto | Prioridad | Estado |
|---|---|---|---|---|
| H1 | Coverage de ramas sin gate (58.83% real) | Calidad | P1 | Abierto |
| H2 | Docs desincronizadas (ADR/etapas) | Mantenibilidad | P2 | En revisión (ver T22) |
| H3 | Detalles de error expuestos | Seguridad | P0 | Ver T6 |
| H4 | JWT en localStorage | Seguridad | P1 | Ver T9 |
| H5 | Sin tests E2E del flujo de viabilidad | Calidad | P2 | Abierto |
| H6 | Sin métricas de coste LLM | Gobernanza IA | P2 | Ver T19 |
| H7 | Single-replica / caché local | Escalabilidad | P2 | Ver T20 |
| H8 | Sin observabilidad (tracing/logs JSON) | Operabilidad | P1 | Ver T16/T17 |
| H9 | Sin playwright coverage para errores del backend | Calidad | P2 | Abierto |
| H10 | CORS duplicado | Seguridad | P2 | Ver T12 |
| H11 | Sin migración V4+ de Flyway para nuevas tablas | Datos | P2 | Abierto |
| H12 | `RateLimitingFilter` solo `/auth` | Seguridad | P1 | Ver T7 |
| H13 | Error handling del frontend agresivo (forceLogout) | UX | P1 | Ver T11 |
| H14 | No hay pruebas de concurrencia/idempotencia | Calidad | P2 | Abierto |
| H15 | `data: ` globales en CSS sin tokens de diseño | Mantenibilidad | P3 | Abierto |

---

## Priorización (matriz impacto × esfuerzo)

- **Quick wins (P0, esfuerzo S)**: T5, T6, T2, T8, T11, T13 — todos resolubles en un mismo sprint.
- **Alto impacto, esfuerzo medio**: T3 (transacción), T4 (idempotencia), T7 (rate-limit), T9 (cookie).
- **Estructurales (L)**: T15 (Testcontainers), T24 (IaC).

Ver `SPRINT_BACKLOG_ENTERPRISE.md` para la asignación a sprints y los acceptance criteria de cada ticket.

# Auditoría Técnica Integral — Proyecto KIN

**Fecha:** 2026-08-06 · **Branch:** `main` (HEAD `80c5382`) · **104 commits**
**Alcance:** `kin-backend` (Spring Boot 3.2.5 / Java 17), `kin-frontend` (Next.js 16 / React 19 / TS strict), `kin-database`, `kin-docs`.

**Fuentes de evidencia:** código fuente (archivo:línea), reporte JaCoCo en `kin-backend/target/site/jacoco/index.html` y `jacoco.csv`, `package.json`, `pom.xml`, `docker-compose.yml`, `application*.yml`, workflows de CI (`.github/workflows`), `AGENTS.md`, `README.md`, `BASELINE_ARCHITECTURE.md`, ADRs y documentos de fase de `kin-docs/`.

> Nota metodológica: 5 de 6 agentes de exploración iniciales retornaron vacíos; la evidencia de arquitectura, calidad, testing y frontend se obtuvo y verificó directamente con el tooling del repositorio.

---

## 1. Resumen Ejecutivo

### Estado general
KIN es una plataforma full-stack de gestión de proyectos con asesoría de viabilidad **impulsada por IA**, construida sobre una arquitectura **domain-driven** y gobernada por **ADR**. El núcleo de dominio (`kin.*`) es una pieza de **calidad excepcionalmente alta**: motores (`DomainEngine`), un pipeline de **13 etapas** y un fuerte compromiso con preservar la pureza del dominio (puertos `kin.*` ← adaptadores `ai.*`). El repositorio es muy rico en documentación (19 ADRs, 22 fases, 16 specs Enterprise).

Los puntos débiles no están en el dominio, sino en la **capa de frontier/lastre**: seguridad, acceso a datos, persistencia y configuración de despliegue están poco testeada y con varias brechas de seguridad y/o disposición para producción.

### Nivel de madurez
**Beta avanzada con pretensiones Enterprise.** El dominio "inteligente" está sólido y testeado, pero la envoltura web/persistencia/ops todavía muestra huecos de seguridad, cobertura de las capas finas, observabilidad incompleta y documentación desactualizada respecto al código real. **No está listo para producción pública** sin resolver los hallazgos 🔴.

### Principales fortalezas
1. Arquitectura hexagonal/por dominio limpia y coherente (ADR-driven).
2. Excelente testeo del dominio: cobertura **95–100 %** en `kin.conversation`, `kin.engine`, `kin.reporting`, `kin.knowledge`, `kin.interview`, `kin.enrichment`, `kin.enterprise`.
3. Pipeline de IA orquestado, con decisión determinista en Java y streaming SSE.
4. Compresión HTTP, caché en memoria, Hikari pool y paginación ya aplicadas.
5. Documentación muy rica (19 ADRs), CI/CD con 5 workflows, Docker con healthchecks.

### Principales riesgos
1. **🔴 Secreto JWT estático commiteado** en `.github/workflows/frontend-ci.yml:99`.
2. **🔴 Capa de seguridad/persistencia/pricing casi sin tests** (8–17% cobertura en `common.security`, `auth`, `pricing.*`, `stripe`) — justo donde ocurrió el incidente de Redis.
3. **🔴 `/actuator/**` abierto (`permitAll`)** en `SecurityConfig.java:52`, exponiendo métricas en producción.
4. **🔴 Fuga de detalle de excepciones** al cliente (`GlobalExceptionHandler` devuelve `ex.getMessage()` en 500).
5. **🟡 Documentación desincronizada** del código objetivo (BASELINE/AGENTS dicen "12 stages"; el código tiene 13; README dice "18 ADRs", hay 19).
6. **🟢/🟡** Token JWT en `localStorage` (XSS), CORS duplicado, falta stack observabilidad, sin rastreo de errores frontend.

---

## 2. Arquitectura

| Criterio | Veredicto | Evidencia |
|---|---|---|
| Organización por módulos | **Fuerte** — paquetes de dominio puro (`kin.*`) vs adaptadores (`ai.*`, JPA). Lista de 60+ paquetes coherentes. | árbol de `com.kinplatform` |
| Acoplamiento | Bajo en el dominio (puertos `AIResponder`, `KnowledgeSource`, repositorios). Alto riesgo solo en `KinConfig` (585 líneas) que ensambla todo. | `KinConfig.java:585` |
| Cohesión | Alta en motores; media en `EnterpriseController` (474 líneas) y `EnterpriseProject` (574). | `EnterpriseController.java:474`, `EnterpriseProject.java:574` |
| SOLID | Bien aplicado en dominio; SRP más lienzo en configuración y controller Enterprise. | — |
| Clean Architecture | Domain nìtid, adapters infra. Los DTO/valor anémcos no vehiculan lógica de negocio indebida. | — |
| Escalabilidad | Horizontal no habilitado aún: único 9090/H2 file dev, sin multi-tenancy cargado, sin cola/workers ni auth externalizado. | `docker-compose.yml:35-61` |
| Cohesión | Alta dentro de `kin.*`, enterprise/web/dto 97% de cobertura refuerza empaques. | JaCoCo |

**Aciertos:** el uso de `DomainEngine<E,R>` + `EngineRegistry` (auto-discovery) + `EngineExecutor` para orquestar los 13 stages es un diseño maduro y testeable.

**Hallazgos de arquitectura:**
- `BASELINE_ARCHITECTURE.md:113` y `AGENTS.md` declaran "12 stages"; **el pipeline real tiene 13** (`EnrichmentStage`/ADR-016). Evidencia: `KinConfig.java:534-549` construye `Analyzer/Evaluator/Strategist/Scoring/Recommendation/Risk/Opportunity/Report/Event/Knowledge/Enrichment/Consultor/Interview` (13). Inconsistencia documentada en parte del ciclo de vida — la doc de referencia afirma ser el contrato.
- **CORS duplicado:** `SecurityConfig.java` (filtro, precedencia) y `CorsConfig.java:12-37` (WebMvcConfigurer). Riesgo de desincronización de orígenes. | `CorsConfig.java`, `SecurityConfig.java:88-108`
- Controlador Enterprise grande (474 líneas) con WebMapper (278) — foco de deuda de mantenibilidad.

**Calificación Arquitectura:** **78/100**

---

## 3. Backend (Spring Boot)

- **Spring Boot 3.2.5 / Java 17**; `@EnableCaching` (KinApplication:5). Dev en H2 file; prod PostgreSQL (Flyway, composer).
- **Persistencia:JPA** con `open-in-view: false` (correcto), `default_batch_fetch_size: 50`, `ddl-auto: update` en dev. **`show-sql: true`** activo en dev — fuera de destro. `application.yml:48-61`.
- **Transacciones:** `@Transactional` en servicios (p.ej. `SubscriptionValidatorService.incrementMessageCount`). No se detectaron usos indebidos generales.
- **Cache:** `spring.cache.type: simple` (corrección del incidente previo) con `@Cacheable("projectLimit"|"messageLimit"|"activeSubscription")` en `SubscriptionValidatorService.java:42,61,132`. Redis queda opcional (adapter condicionado a `kin.cache.redis.enabled=true`).
- **Redis:** adapter detrás de `RedisKnowledgeRepository`/`RedisCacheConfig` (`@ConditionalOnProperty`). Bien aislado, pero la *autoconfig del `CacheManager`* habilitaba RedisCacheManager sin `spring.cache.type` (ver sección Deuda → es un incidente ya resuelto con nota).
- **Manejo de excepciones:** `GlobalExceptionHandler` (`common/`) y `EnterpriseApiExceptionHandler`. Ambos devuelven `ex.getMessage()` al cliente en 500 — **fuga de detalle interno**. | `GlobalExceptionHandler.java:49-54`
- **Rendimiento:** `server.tomcat.max-swallow-size: 2MB`, compresión gzip (json/plain, min 1 KB), pool Hikari (max 10). Streaming `Flux` + `HistoryWindow` (20 turnos) limita contexto. | `application.yml:9-15`

**Calidad backend:** ¥82 para dominio testera; lastre en security/pricing infra.

---

## 4. Frontend

**Next.js 16.2.9 / React 19.2.4 / TS strict / Tailwind 4.6.121** ts+tsx en `src`, 52 componentes, dependencias mínimas (jspdf, next, react — **bundle liviano**, sin UI kit, sin query lib).

- **Packages:** `next, react, react-dom, jspdf`. devDeps: eslint 9, vitest 3, playwright, tailwind 4, apres test. `package.json`.
- **Páginas:** todas las rutas `/dashboard/**` son `"use client"` con `useEffect`+fetch → **no usa RSC/SWR/React Query**; lo correcto sería SSR para preliminary dashboards. | `grep "use client"` en 14 rutas
- **Estado:** hooks locales + `localStorage`. El token se lee en **5 servicios distintos** (`api.ts:10`, `auth.ts:44,55`, `chat.ts:31`, `enterpriseApi.ts:11`, `session.ts:49`) — **duplicación/divergencia de clientes HTTP.**
- **Manejo de errores fragile:** `projects/page.tsx:34-46` y `projects/[id]/page.tsx:48-59` llaman `forceLogout()` en el `.catch` genérico (aunque fuese un 5xx). Verificado (incidente). Sigue abierto.
- **Pattern flaky detectado:** el select de categorías carga por atribución asíncrona; el `new/page.tsx` hace `GET /categories`.
- **Lint:** `npm run lint` → **0 errores / 8 warnings** (variables sin uso en `pricing/page.tsx:56`, `subscription/page.tsx:18`, `error.tsx:7`, `ToastProvider.tsx:6`, `analytics.ts:26`, `api.test.ts:3`).
- **UX técnica:** rutas 100% client + renders pesado; sin React Query para caching/pendings; el select de gestión falla en CI.

**Calidad frontend:** ¥70/100 — funcional y liviano, pero "client-heavy", fetch manual, duplicación de token y manejo duro de errores.

---

## 5. Seguridad

| Área | Hallazgo | Severidad |
|---|---|---|
| JWT | HS256, secreto `${JWT_SECRET}` **obligatorio, sin default** (bueno). Expiración 24h. **Sin** `iss`/`aud`, sin rotación/revocación (stateless). | 🟢/🟡 |
| JWT (logs) | `JwtAuthenticationFilter.java:36-41,56-59` hace `log.info` con el **primeros 20 chars del token** y detalle de cada request en **INFO**. Volumen + fuga parcial en logs. | 🟡 |
| Secreto | **`.github/workflows/frontend-ci.yml:99` contiene un valor real de `JWT_SECRET` (120 chars) commiteado.** | 🔴 |
| AuthZ | Matchers públicos razonables; `/test/**` ADMIN, `/subscriptions/**` auth, `anyRequest().authenticated()` cubre `/categories`, `/projects`. Correcto (403 sin token en `/categories`, verificado). | 🟢 |
| CORS | Dual (SecurityConfig + CorsConfig) con métodos/headers explícitos; **sin `*`**, credenciales con orígenes de desarrollo+vercel. | 🟡 duplicación |
| Headers | CSP `default-src 'none'; frame-ancestors 'none'`, frameOptions deny, HSTS, Permissions-Policy, Referrer-Policy (`SecurityConfig.java:63-80`). | 🟢 |
| Rate limiting | `RateLimitingFilter.java:20-21` (5 req/min/IP en `/auth/**`); `MAX_REQUESTS/WINDOW` hardcodeados, solo switch `enabled`. | 🟡 |
| Actuator | `/actuator/**` `permitAll` → `/actuator/prometheus` y `/actuator/metrics` abiertos en prod sin auth. `show-details: never` mitiga parte. | 🔴 en prod |
| Info leak | `GlobalExceptionHandler` devuelve `ex.getMessage()` en 500 → detalla clues/cadenas. | 🟡 |
| Token storage | `session.ts:38-43` guarda token en **localStorage + cookie** → vulnerable a XSS (necesita mitigación). | 🟡 |
| Stripe/LLM keys | No hardcodeadas en `application.yml` (referencias `${...}`). | 🟢 |
| Dockerfile frontend | `kin-frontend/Dockerfile:15` copia `.env.local` dentro de la imagen → clave env incrustada en imagen. | 🟡 |
| docker-compose | Solo inyecta `JWT_SECRET`+`DEEPSEEK_API_KEY`; faltó `STRIPE_*`/`OPENAI_API_KEY`. | 🟡 |

**Waz matriz OWASP:** A07/secretos (secreto en workflow), A05/falla de access (actuator), A04/design (assets/sanitización LLM), A06 (fuga de detalle en errors). Riesgo moderado-alto concentrado en secretos + actuator.

**Calificación seguridad:** ¥58/100 — buenas bases (JWT config, headers, rate limit, sin `*` CORS) pero conjack secret offcial y superficie expuesta.

---

## 6. Calidad del código

- **Lint backend:** **sin** plugin checkstyle/spotbugs/format en `pom.xml` (solo JaCoCo) → estilo y unused **no forzados** en CI. | `pom.xml:85-105` solo jacoco.
- **Métricas:** 550 clases/29k LOC main vs 306/29k LOC test (ratio ~1, fuerte esfuerzo de pruebas).
- **Duplicación real:** config CORS duplicada (Security/Cors); lógica de token duplicada en 5 servicios frontend; mappers DTO repetidos.
- **Eureka clases grandes:** `KinConfig.java:585`, `EnterpriseProject.java:574`, `EnterpriseController.java:474`, `KnowledgeOrchestrator.java:471`, `EnterpriseGenerationService.java:438`, `RecommendationEngine.java:393`.
- **Smells menores:** `log.info` a exceso en filtros, catch genéricos/aparéntesi, 8 warnings lint frontend.

**Calidad:** ¥74/100.

---

## 7. Base de datos

- **Dos esquemas:** dev H2 (`ddl-auto: update`) vs prod PostgreSQL (Flyway `V1..V10`) — OK por diseño pero replica lógica en `init.sql`. | `db/migration/`
- **Migraciones:** `V1` base, `V6` categorías, `V7` enterprise, `V8` features JSONB, `V9` current_plan, `V10` user_subscriptions. Buena integridad.
- **Config serenada:** `open-in-view:false` + `batch_fetch_size:50`; pool Hikari.
- **Índices:** no se auditaron en definición; solo se constata `default_batch_fetch_size`. Ej.: `pricing/services` con 6% cobertura y trazas `SubscriptionValidatorService` . 
- **Integridad:** `@ManyToOne` a `Category` resolve `category_id`; cascade en entities.

**Riesgo:** capa de persistencia/pricing **muy poco testeada** (6–20%) junto con los endpoint más sensibles a cuota de su plan.

**Calificación DB:** ¥70/100.

---

## 8. IA

- **Arquitectura IA:** `ProviderRouter` (DeepSeek→OpenAI→Ollama) con **mock español de respaldo** si falla → desarrollo sin LLM. | `ai.provider`, `AiEngineService`
- **Prompt:** `PromptAssembler` (frontera ADR-012) con `SectionFormatter`×10; el LLM **solo formula**; la decisión es **Java** (`DefaultTurnPolicy`, `InterviewBlueprint` = "Java decide"). Muy robusto.
- **Modelo:** `deepseek-v4-flash` (migrado 2026-07-30 desde v3-flash inválido), OpenAI `gpt-4o-mini` fallback. | `application.yml:119,106-108`
- **Conocimiento externo:** `KnowledgeEngine` offline-first, `SourceValidator` (allowlist/HTTPS/freshness/trust) → **seguro** por diseño.
- **Riesgos:** costo/ratio, no-verificabilidad del LLM, prompt injection del usuario→mitigado por `PromptGuardrail`+`ResponseGuard`, modelo/cocs base.
- Hay ya `kin/enterprise` máquina (aggregate/engine/renderer) para reporte Enterprise.

**Calificación IA:** ¥85/100 — el mejor componente, muy testeado, determinista en decisiones que cuentan.

---

## 9. Rendimiento

- **Backend:** streaming `Flux`, `HistoryWindow` (20 msgs) cap del presupuesto, batch fetch, compresión, pool 10. ✓. Bloqueo/`sleep` no detectado en rutas críticas.
- **Cache:** `simple` en memoria; sin TTL ni control del tamaño para `@Cacheable("activeSubscription")` etc. (caché simple sin expiración → riesgo de memoria en items pequeños no problemático aquí).
- **Frontend:** páginas client-emden; fetch manual (sin SWR/React Query); falta de RSC → waterfall de requests (`/projects`+`/subscriptions/status`+`/auth/me` por layout); bundle pequeño (2 deps) pero menu click-to-clients.
- **Aquí el hallazgo de duplication:** `/auth/me` por request en el middleware (proxy.ts) sumado a llamadas duplicadas.

**Calificación:** ¥72/100.

---

## 10. Observabilidad

- **Backend:** Micrometer + actuator (`health,info,metrics,prometheus`) ✓; probes readiness/liveness; `redis` health excluido (arreglo). **Tracing no** configurado (sin Micrometer Tracing/OTel en pom). Métricas: varias custom (timers/counters en `ai/observability`, ej. `ProviderMetrics`, `KnowledgeMetrics`). 
- **Stack:** **no hay** compose Prometheus/Grafana/Loki; no hay alerting config.
- **Frontend:** `analytics.ts` (localStorage, 100 eventos, `console.info`) — **sin envío**, **sin `Sentry` ni error tracking**.
- **Breakdown:** se contrastan los health/liveness para orquestadores.

**Calificación:** ¥68/100 — bien en metrics/actuator; `alerting`+`tracing`+`error-Monitor` ausentes.

---

## 11. Testing

- **Backend:** **306 archivos / ~2.189 métodos** `@Test`/`@ParameterizedTest`; **29k LOC de tests**. Reportadero jaCoCo global: **73.4% instrucciones / 41.8% branches** (`2.485` missed of `6.589` instruction units). Desglose por contratos:

| Área | Cobertura (instr.) | 
|---|---|
| `kin.conversation` / `kin.engine` / `kin.reporting.report[model]` / `kin.enrichment` / `kin.knowledge` / `kin.interview` | **99–100%** |
| `kin.reporting` (agregado) | 96.6% |
| `ai.observability` / `kin.enterprise.*` | 90–100% |
| `common.config` | 60.5% |
| `project` / `chat` / `ai`/ `user` | 16–44% |
| `pricing.service` / `pricing.stripe` / `auth` / `common.security` / `ai.provider` | **6–10%** |
| `*.dto` (pricing/auth/common) | **0%** |

**Puntos críticos sin cobertura:** el propio componente del incidente (`SubscriptionValidatorService`, `pricing.service` 6.4%), filtros JWT/rate/subscription (`common.security` 8.2%), el router a los `.ai.provider` 9.9%, y los webhooks Stripe (`pricing.stripe` 6.2%). Los domain engines están sobretestimulados **enmascarando** que el 90% del edge web/persistencia no tiene guard.

- **Frontend:** 44 archivos de test (Vitest, 30 unitarios + 6 hoooks +8 services) + **3 Playwright specs** (`auth`, `dashboard-flow`, `diagnostics/debug-flow`). E2E cubre login→dashboard→crear→logout, no cambia Enterprise ni situaciones de error/fallback.
- **Bench:** hay 7 benchmark tests en `ai/performance` (JMH-ish) — sin escenario de carga real de endpoints.
- **CI:** 4 jobs (backend `mvn verify`+JaCoCo; frontend lint+vitest+build+e2e; quality-gate opcional; security gitleaks; release) — **pero no hay gate de cobertura (`jacoco-maven-plugin` sin `<check>`) ni Sonar real.** `quality-gate.yml` es opcional si faltan secrets.

**Calificación testing:** ¥72/100 — dominio súper testeado, edge bajo, E2E plant.

---

## 12. Documentación

- **Excepcionalmente rica:** 19 ADRs + 22 fases + 16 specs Enterprise + 6 releases + `README` (634 líneas) + `AGENTS.md` + `CHANGELOG` + `GALERIA`. Strong point.— **Desactualizaciones clave:** 
  - `README.md:57,321` dice **18 ADR**s (hay 19).
  - `README.md:437-450` vs `AGENTS.md`: cifras de cobertura divergentes (my actual reporte global 73% en JaCoCo, no los viejas de dominio).
  - `BASELINE_ARCHITECTURE.md:113`/`AGENTS.md` → "12 stages" (real: 13).
  - `BASELINE_ARCHITECTURE.md:4` lista enmiendas de .5ADRs sólo hasta ADR-015 (faltan 016–019).
- **APIs:** springdoc presente pero **solo controllers Enterprise** anotados con `@Operation/@Tag`; no para auth/project/chat/subscriptions/stripe. No hay config springdoc. 
- **Decisiones implementadas sin ADR:** catálogo de categorías SaaS, rate limiting, cache simple, Redis adapter, fases 14–17 y 20.
- **Despliegue:** `docker-compose.yml` + `render.yaml` + 5 workflows + Flyway V1..V10. Falta `vercel.json`.

**Calificación:** ¥80/100 (mucha mezcla documental, necesidad de actualizar `Usos`).

---

## 13. Deuda Técnica

### 🔴 Alta
| # | Deuda | Impacto | Riesgo | Esfuerzo | Prioridad |
|---|---|---|---|---|---|
| DT-1 | **Secreto JWT commiteado** en `frontend-ci.yml:99` | Cualquiera con acceso al repo mintrea tokens | Alto | Bajo (Rotar + inyectar `${{ secrets.JWT_SECRET }}`) | **1** |
| DT-2 | **Cobertura ≤10%** en `common.security`, `auth`, `pricing.service`, `ai.provider`, `pricing.stripe` | Incidentes de suscripción/ auth no detectados (ya pasó con Redis) | Alto | Medio | **2** |
| DT-3 | `/actuator/**` abierto (`permitAll`) en prod → metrics/prometheus expuestos | Exfiltración de métricas | Medio | Bajo (restringir por IP/red o lock a admin+) | 4 |
| DT-4 | `GlobalExceptionHandler` devuelve `ex.getMessage()` en 500 | Fuga de detalle/directiva | Medio | Bajo | 5 |
| DT-5 | Manejo agresivo de errores frontend (`forceLogout()` en `.catch` de proyectos) (sigue abierto) | Pills de sesión por 5xx | **Alto** | Bajo (solo forceLogout en 401/403) | 3 |
| DT-6 | Docs que son el contrato (BASELINE/AGENTS) **desactualizadas** (12 vs 13 stages, 18 vs 19 ADRs) | Gobernanza arq. débil | Medio | Bajo | 6 |

### 🟡 Media
- DT-7 CORS duplicado (Security + WebMvc) — desize conf subjects.
- DT-8 Token en `localStorage` (XSS) + `/auth/me` por request; migrar a cookie HttpOnly + refresh.
- DT-9 `docker-compose` no inyecta `STRIPE_*`/`OPENAI_API_KEY`; frontend Dockerfile incrusta `.env.local`.
- DT-10 Sin stack de observabilidad (Prometheus/Grafana/Loki) ni alertas; sin tracing.
- DT-11 Sin checkstyle/Spotless/format en backend; unused sin enforzar.
- DT-12 Swagger incompleto (solo Enterprise) y sin config/env.
- DT-13 Duplicación de cliente/token en 5 services frontend; páginas "use client" sin RSC/query.
- DT-14 Decisiones implementadas sin ADR (categorías, rate-limit, cache, Redis, fases 14–20).
- DT-15 Sin error-tracking frontend (Sentry/RUM).

### 🟢 Baja
- DT-16 Fuga de token en logs (log.info per request) — bajar a DEBUG.
- DT-17 `show-sql:true` en aplicación.yml (dev) — mover a perfil.
- DT-18 30 warnings lint frontend (vars sin uso) y 5 statements, típicos.
- DT-19 Modelo DeepSeek en doc vs código desync.
- DT-20 Clases grandes (KinConfig 585, EnterpriseProject 574, EnterpriseController 474) — dividir.

---

## 14. Roadmap Técnico

### FASE 1 — Correcciones críticas
1. Rotar y parametrizar `JWT_SECRET` en CI (DT-1). 🔴 Urgente.
2. Corregir manejo de errores del frontend: `forceLogout()` solo con 401/403; show error UI on 5xx (DT-5).
3. Restringir acceso a `/actuator/metrics`+`/actuator/prometheus` (network/role) (DT-3).
4. `GlobalExceptionHandler`: no exponer `getMessage()` en 500 (DT-4).

### FASE 2 — Seguridad
- `auth` a cookie HttpOnly + CSRF; eliminar token env huge; `token` in logs a DEBUG.
- Completar tests de `common.security`+`auth`+`stripe` (DT-2).
- Inyectar todos los secrets en `docker-compose`/Dockerfile; no copiar `.env.local` en imagen.
- Revisar sanitización de output LLM y webhook Stripe firma.

### FASE 3 — Arquitectura
- Actualizar `BASELINE`/`AGENTS`/`README` al estado real (13 stages, 19 ADRs, cobertura JaCoCo).
- Reducir CORS a una única fuente; extraer `KinConfig` + split EnterpriseController.
- Emitirlo ADRs para categorías, rate-limit, cache simple, Redis y fases 14–20 (cerrar socar 008).

### FASE 4 — Rendimiento
- 1 11ª RSC/sR en frontend (dashboard), dedupe cliente HTTP, implementar SWR/React Query.
- Tribar N+1/índices en las querías más pesadas; métricas por endpoint.
- Validar `Cacheable simple` sin VT; considerar TTL.

### FASE 5 — Calidad
- `add` checkstyle/spotless en pom; `rit` backend en CI.
- Garantizar gate de cobertura JaCoCo `<check>` sobre paquetes → objetivo ≥80% en `common.*`, `pricing`, `auth`.
- Reducir fallos en frontend (8 warnings); dividir clases >400 líneas.

### FASE 6 — Testing
- Cobertura orientada a **edge**: controllers (Auth/Project/Chat/Stripe/Subscription), JWT filters, `SubscriptionValidatorService`, `ProgramEnroll`, webhook StGro.
- Añadir tests de integración `@SpringBootTest` ) y fallback, con `MockMvc`, y tests de seguridad (401/403).
- E2E Playwright ampliar: flujo Enterprise, error/fallback de LLM, cambio de plan/quota.

### FASE 7 — Observabilidad
- Levantar stack Prometheus/Grafana (docker-compose) + alertas.
- Añadir tracing distribuido (Micrometer Tracing/OTel) e IDs de trace-key.
- Integrar error-tracking frontend (Sentry).

### FASE 8 — Escalabilidad
- Habilitar Redis opcional correctamente + cache manager condicionado (migrate off simple en prod).
- Desplegar con PostgreSQL/Mitra; revisar pooling y vibriatura.
- Sseza delitive para un path de arranque.

### FASE 9 — Preparación para Producción
- SSL/TLS, domain, gestión de secretos (Vault/SSM), `.env` prods, `actuator` segur.
- Backup/DR de BD, sin `ddl-auto` en prod (Flyway asida), health probes en K8s/Render.
- Auditoría de seguridad antes del GA (OWASP / Pentest externo).

### FASE 10 — Roadmap Enterprise
- Multi-tenant real (org → team), RBAC fino, facturación completa (suscriptions Stripe avanzado).
- Alta disponibilidad backend (2+ réplicas), autoscaling, Kube/ECS, RDS.
- Cumplimiento: audit trails, DPA, GDPR (exportar/borrar datos), SSO/SAML/OKTA.
- **Nivel Enterprise pleno:** gobernanza de IA (guardrails de trazabilidad, alucinocer, registro de decisiones deterministas ya existen), plan de costos LLM.

---

## 15. Calificación Final

| Dimensión | Puntaje |
|---|---|
| Arquitectura | **80** |
| Backend | **82** |
| Frontend | **70** |
| Seguridad | **58** |
| Testing | **70** |
| Escalabilidad | **62** |
| Rendimiento | **72** |
| Documentación | **78** |
| Mantenibilidad | **74** |
| Calidad general | **73** |
| **Promedio** | **≈ 72** |

### ¿Qué necesita KIN para alcanzar un nivel Enterprise?

1. **Seguridad de primera clase:** rotar/gestar secretos de forma segura (Vault/SSM + secrets managers), cerrar `/actuator` en prod, no exponer detalles de excepciones, y endaderificar el token de sesión en la parte del navegador.
2. **Cobertura del borde:** financiada obligatoria de smoke coverage ≥90% en autenticación/autorización/Pricing/Stripe (las `gate` de JaCoCo ahora inesplícito).
3. **Documentación como fuente de verdad sincronizada:** BASELINE/AGENTS/README que reflejen el 100% del contrato real (13 etapas, 19 ADRs, cobertura real).
4. **Observabilidad end-to-end:** tracing, métricas por endpoint, alertas, y monitoreo de errores del frontend; sin esto "Enterprise" carece de SLO/SLA.
5. **Despliegue y escalabilidad real:** multi-replíca con Redis/proxy, multi-tenancy, DR/backup, y despliegue cloud gestionado con IaC (el actual docker-compose es de vlan/dev).
6. **Gobernanza de IA/Enterprise:** ya muy avanzada (decisión en Java, guardrails) → capitalizarla con métricas de coste LLM, curso de trazabilidad y reporte Enterprise transparente.

---

## 16. Segunda Auditoría Crítica (Verificación + Correcciones)

> Este anexo registra la SEGUNDA auditoría, cuyo objetivo fue **verificar cada afirmación** de las secciones 1-15 y **hallar deuda/riesgo nueva**, con evidencia concreta (`archivo:línea` o comando ejecutado). Sustituye a la sección anterior donde haya conflicto.

### 16.1 Correcciones a la auditoría original

| # | Afirmación original | Situación real (evidencia) | Verdicto |
|---|---|---|---|
| C1 | Cobertura de ramas ≈ 41.8% | JaCoCo real: BRANCH **58.83%** (3730/6340), INSTRUCTION **73.59%** (45113/61304), LINE **85.5%** — fuente `target/site/jacoco/jacoco.csv` | **INCORRECTO**, corregir |
| C2 | "18 ADR" (README) | **19 ADRs** (001…015 + posteriores enum) | **INCORRECTO**, corregir |
| C3 | Pipeline de "12 etapas" (BASELINE) | Instancia real (KinConfig): **13 etapas** (incluye InterviewStage entre Strategist y Knowledge) | **INCORRECTO**, corregir |
| C4 | Deuda: "cobertura de la frontera" | Añadir: solo **1 `@SpringBootTest`**, **0 `@WebMvcTest`**, **0 Testcontainers**, sin REST-Assured/TestRestTemplate | Confirmado y ampliado |

### 16.2 Verificaciones que se confirman (pasan)

| Hallazgo | Evidencia |
|---|---|
| JWT_SECRET hardcodeada en CI | `.github/workflows/frontend-ci.yml:99` (valor base64 `a2luLXBsYXRmb...`) — **crítico** |
| `/actuator/**` en `permitAll` | `common/config/security/SecurityConfig.java:52` |
| `GlobalExceptionHandler` devuelve `ex.getMessage()` | L20, L39, L46, L53 — filtra detalles internos |
| CORS duplicado (dual config) | `CorsConfig.java` + `SecurityConfig.java` (reglas en ambas) |
| JWT en `localStorage` (XSS) | `src/services/session.ts:38-43` (localStorage + cookie) |
| `/auth/me` en cada request | `src/proxy.ts:16,38` (middleware) |
| Sin repo mongo de frontend | sin axios/react-query/SWR/Sentry/lodash en `package.json` |
| Sin tracing (Micrometer Tracing/OTel) | sin dependencia en `pom.xml` |
| Logs sin formato estructurado (logback JSON) | `application.yml` no define appender JSON |

### 16.3 Hallazgos NUEVOS (auditoría 2)

**Criticos:**
- **T1 — Caché `activeSubscription` muerta**: `@Cacheable("activeSubscription", key="#userId")` en `SubscriptionValidatorService.getActiveSubscription`, pero este método **solo es invocado por self-invocation** (sin pasar por el proxy de AOP de Spring), por lo que la caché **nunca se usa / nunca se invalida**. (L132 + callers internos; confirmado: sin llamadores externos).
- **T2 — Invalidación incompleta de `projectLimit`**: al **borrar** un proyecto no se evicta la clave `projectLimit`; `ProjectServiceImpl.delete` solo evicta en create/update (`SubscriptionValidatorService.evictProjectLimitCache`). Con caché `simple` el conteo quedar desactualizado.
- **T3 — `@Transactional` cubre llamadas IA (bloqueante y streaming)**: `ChatOrchestratorServiceImpl.processMessage` es `@Transactional` (L73) y dentro se invoca la IA (segundos) + IO Reactor. Retén la conexión JDBC durante I/O externa → riesgo de agotar el pool y de transacciones largas.
- **T4 — Stripe sin idempotencia**: `StripeWebhookController.constructWebhookEvent` no valida `event.getIdempotencyKey`; eventos reentregados por Stripe pueden procesarse dos veces (doble cambio de licencia/proyectos).

**Media:**
- **T5 — Rate limiting por IP spoofeable**: `RateLimitingFilter` confía en `getClientIP()` (header/spoof) y usa `ConcurrentHashMap` + `synchronized` por bucket; un atacante rota la cabecera para bypasear el limite. Solo cubre `/auth/**`.
- **T6 — Enumeración de cuentas + password débil**: `AuthServiceImpl.register` devuelve "Email already registered + {email}" (enumera cuentas) y no valida complejidad/fuerza de password (sin mínimo).
- **T7 — Logs con contenido sensible**: `ChatController.chat` registra el texto del mensaje del usuario (`log.info("Mensaje del usuario: {}", request.getContent())`) — datos de negocio del cliente en logs.
- **T8 — `DATABASE_URL` de Render no es JDBC**: `render.yaml` expone `connectionString` de Postgres (`postgresql://`), mientras Spring espera `jdbc:postgresql://...` → riesgo de falla de arranque en prod sin override.
- **T9 — Frontend `Promise.all(...).catch(() => forceLogout())`** en `projects/page.tsx:34-46` y `[id]/page.tsx:48-59`: cualquier fallo (incl. 500 temporal de un fetch) cierra sesión del usuario → mala UX y falsos "logout".

### 16.4 Cobertura real (corregida)

| Métrica | Valor |
|---|---|
| Líneas | **85.5%** |
| Instrucciones | **73.59%** |
| Ramas | **58.83%** |

Pruebas: backend `306` archivos de test, `@SpringBootTest` = 1, `@WebMvcTest` = 0, Testcontainers = 0; frontend `44` archivos Vitest + `3` specs Playwright. El resto de métricas sin gates de JaCoCo (no `<check>`).

### 16.5 Artefactos asociados

Los hallazgos detallados, la remediación y el backlog están en los nuevos docs: `ENTERPRISE_REMEDIATION_PLAN.md`, `TECHNICAL_DEBT_REGISTER.md`, `SPRINT_BACKLOG_ENTERPRISE.md`, `SECURITY_HARDENING_CHECKLIST.md`, `PRODUCTION_READINESS_CHECKLIST.md`.

> El núcleo de dominio y la disciplina de arquitectura por ADR son la mayor fortaleza de KIN. El camino a Enterprise no es escribir más dominio; es **endurecer la periferia (seguridad, cobertura, observabilidad, despliegue) y sincronizar el contrato documental con el código**.
# Plan de Remediación Enterprise — Proyecto KIN

> **Versión** de referencia: KIN 2.0 Alpha 1 (commit `89b39b9`).
> Documento derivado de la **Segunda Auditoría Crítica** (verificación de `AUDITORIA_TECNICA_INTEGRAL.md`, sección 16). Todos los hallazgos tienen evidencia `archivo:línea` o comando ejecutado.
> Los IDs `T#` referencian `TECHNICAL_DEBT_REGISTER.md`; las tareas `KIN-###` referencian `SPRINT_BACKLOG_ENTERPRISE.md`.

---

## 0. Contenido

1. [Contexto y principios de llegada a Enterprise](#1-contexto)
2. [Modelo de madurez y estándar de referencia](#2-estandar)
3. [Línea base auditada (hechos verificados)](#3-baseline)
4. [Top 5 brechas críticas](#4-brechas)
5. [Plan por oleadas (W1..W6)](#5-plan)
6. [Gráfico de dependencias](#6-dependencias)
7. [Criterios de aceptación / Definition of Done](#7-dod)
8. [Riesgos y contramedidas](#8-riesgos)
9. [KPI de éxito y SLOs](#9-kpi)

---

<a name="1-contexto"></a>
## 1. Contexto y objetivo

KIN es una plataforma de gestión de proyectos con evaluación de viabilidad guiada por IA. El núcleo inteligente (orquestador de turno, entrevista estratégica, adquisición de conocimiento, scoring y reporte) está implementado como dominio **puro POJO** con decisión determinista en Java y guardrails — un punto fuerte real frente al estándar del mercado.

El objetivo de este plan es llevar KIN de "Alpha estable con buena base de dominio" a **disponibilidad tipo Google/Netflix** (multi-replica, observabilidad end-to-end, seguridad de primera clase, testing de borde y gobernanza de costes LLM), sin romper el contrato `kin/engine` ni las APIs estables.

---

<a name="2-estandar"></a>
## 2. Estándar de llegada

Se comparará contra prácticas de ingeniería de Google/Netflix (exigente):

- **Seguridad**: secrets gestionados (nunca en el repo), Zero-Trust de red, auth por capabilities, endpoint seguro por defecto, sesión segura frente a XSS, rate limiting by-real-IP no spoofeable.
- **Observabilidad**: 3 pilares (logs estructurados/tracing distribuido W3C/métricas RED+USE), alertas con SLO/SLA.
- **Testing**: pirámide sana, integraciones con dependencias reales (Testcontainers/Postgres/Redis), pruebas de unicidad/concurrencia, gates de cobertura en CI, pruebas de carga.
- **Arquitectura**: transacciones de corta vida (sin I/O externa dentro de la tx), cachés correctamente invalidadas, idempotencia de eventos, multi-replica sin estado compartido.
- **Gobernanza**: backlog con Definition of Done, revisión de seguridad, dora-metrics, playbooks de incidentes.

**Puntaje actual referencial**: Seguridad 58, Testing 70, Escalabilidad 62, Rendimiento 72, Frontend 70, Arquitectura 80, Backend 82, Documentación 78, Mantenibilidad 74 → **promedio ≈ 72**. Objetivo: **≥ 88** en las 3 dimensiones críticas (Seguridad, Testing, Observabilidad) y **≥ 92** de promedio.

---

<a name="3-baseline"></a>
## 3. Línea base auditada (hechos verificados)

| Hecho | Evidencia |
|---|---|
| Pipeline real | **13 etapas** (`KinConfig` construye Analyzer…Interview) — `BASELINE` dice 12 ❌ |
| ADRs | **19** — `README` dice 18 ❌ |
| Cobertura | Instrucciones **73.59%**, Ramas **58.83%**, Líneas **85.5%** (`target/site/jacoco/jacoco.csv`) |
| Tests backend | 306 archivos test; solo **1** `@SpringBootTest`, **0** `@WebMvcTest`, **0** Testcontainers |
| Tests frontend | 44 Vitest + 3 specs Playwright |
| Secrets en repo | `frontend-ci.yml:99` (JWT_SECRET base64) |
| Endpoint abierto | `/actuator/**` `permitAll` (`SecurityConfig.java:52`) |
| Manejo de errores | `GlobalExceptionHandler` expone `ex.getMessage()` (L20/39/46/53) |
| Ranurado IA | `ChatOrchestratorServiceImpl.processMessage` `@Transactional` (L73) durante I/O LLM |
| Caché | `getActiveSubscription` solo self-invocation → caché **muerta** (nunca leída/invalidada) |
| Pago | Stripe webhook sin idempotencia a nivel de negocio |

---

<a name="4-brechas"></a>
## 4. Top 5 brechas críticas

1. **[SEC-01] Secretos commitidos** → rotación inmediata + secrets manager + detector en CI.
2. **[SEC-02] Endpoint definition blowback** `/actuator/**` abierto + detalles de errores expuestos.
3. **[TX-01] Transacciones largas con IA** → extraer I/O del `@Transactional`.
4. **[CAC-01] Corrección de caché** → `activeSubscription` muerta y poco invalidado `projectLimit`.
5. **[INT-01] Idempotencia de webhook Stripe** + sin test de integración real (Testcontainers).

En el backlog estas se descomponen en tickets `K-###` con acceptance criteria.

---

## 5. Plan de trabajo

### **Oleada 1 — Resguardo de seguridad (crítico, paralelo inmediato)**
- Rotar JWT_SECRET y cualquier secreto commitido; revocar tokens emitidos.
- Mover secrets a variables de secretos (GitHub) / secrets manager (prod); hacer fallo MANDATORY si no están.
- Añadir detector de secretos en CI (gitleaks/trufflehog) con gate bloqueante.
- Cerrar `/actuator/**` (solo roles/actuator o red interna) y parsear las exposiciones públicas.
- `GlobalExceptionHandler`: devolver mensajes genéricos + `errorId` correlacionado; log detallado server-side.
- `DATABASE_URL` JDBC en Render override.

**Entregable**: ninguna clave en el repo; auditor de seguridad sin item M en secreto.

### **Oleada 2 — Corregir transacción y caché (Sprint 1)**
- Sustituir `@Transactional` en `processMessage` por un servicio de mensajes que persista en su propio método transaccional y ejecute la IA fuera de la tx (y dentro de `pipeline` si aplica).
- `activeSubscription`: exponer a través del proxy (usar `SubscriptionValidatorService` desde un `@Service` llamado externamente o `@CacheEvict` correctos) o eliminar el `@Cacheable` si no aporta; enmendar documentación.
- `projectLimit`: evict `projectLimit` en `delete` de proyecto.
- Tests de unicidad de caché (TTL, invalidation) y de no-bloqueod de pool.

### **Oleada 3 — Idempotencia y límites de pago (Sprint 3)**
- `StripeWebhookController`: implementar idempotencia por `event.id` (tabla `webhook_events`/`processed_events`) + `@Transactional` correcto de corto plazo.
- Confirmar idempotencia de checkout session y manejar replay/duplicado.
- Añadir idempotency-key del lado del cliente para create/checkout si aplica.

### **Oleada 4 — Auth y rate limiting (Sprint 4)**
- Rate limiting por IP REAL (no header spoof): usar `Forwarded`/`X-Real-IP` confí­ando en un proxy auth (TrustedProxy), o limiter en gateway, compartido y con token bucket.
- `register`: validación de fuerza de password (>=12, variación), mensaje genérico anti enumeración.
- Mover tokens: en transición a HttpOnly cookie segura (same-site) + CSRF si aplica (o token de corta vida).
- OWASP header security (HSTS, CSP, nosniff, referrer-policy) en frontend/middleware.

### **Oleada 5 — Testing e integración (Sprint 5)**
- Añadir **Testcontainers** para el módulo de datos (migraciones) y al menos un `@SpringBootTest` de dominio ruta (hay 1, se amplía a ruta crítica: auth/login/chat/pipeline).
- Integración de repos (Pricing/Stripe) con mocks lo menos posible; pruebas de duplicación de webhook.
- End-to-end (Playwright) del flujo de viabilidad (crear proyecto → chat → reporte) con backend real (H2 de test).

### **Oleada 6 — Observabilidad y preparación prod (Sprint 6)**
- Logs estructurados en JSON (logback), incluir `traceId`/`spanId` (W3C trace context).
- Añadir Micrometer Tracing + export OTLP (o Sentry/DD), y métricas por endpoint (RED) + JVM.
- Prometheus + alertas SLO; playbook de incidentes.
- Desplegables con IaC (Terraform) multi-replica detrás de proxy con health-check; Redis como caché distribuida (manteniendo `spring.cache.type` configurada).

---

## 6. Grafo de dependencias

```
Oleada 1 (seguridad)  ──────────► no depende de nadie
Oleada 2 (tx+caché)   ─► requiere que SEC-? esté cerrado (para tocar servicios)
Oleada 3 (stripe)     ─► independiente; borde con páginas de billing
Oleada 4 (auth)       ─► independiente; tiene impacto en frontend (cookie/sesión)
Oleada 5 (testing)    ─► requiere base segura + tx limpia para escribir tests correctos
Oleada 6 (obs+infra)  ─► requiere testing (para no "observar" un sistema inestable)
```

Orden crítico: **SEC → TX/CACHE → IDEMPOTENCIA → AUTH → TEST → OBS**. La oleada de testing debe ir después de estabilizar transacciones/caché para no fijar bugocs actuales con tests.

---

## 6. Grafo / dependencias

Ver bloque anterior (§5.5). Nota: `Oleada 4 (auth)` afecta frontend (debe coordinarse con el flujo de `session.ts` y proxy).

---

## 7. Definition of Done (DoD) por tipo de ticket

- **Seguridad**: PR con revisión de seguridad; secretos fuera de repo; tests de regresión del vector ("el escaneo del vector falla" → prueba negativa).
- **Transacción/caché**: test de integridad (no-bloqueond / parametrizando) verde; cobertura de rama ≥ 60% para la ruta tocada.
- **Webhook/pago**: test de idempotencia (replay del mismo evento: 1 sola aplicación) verde.
- **Frontend**: lint + typecheck + Algo vitest pasando; sin regresión de proxies/middleware.
- **Documentación**: `BASELINE`/`AGENTS`/`README` sincronizados con el estado final (13 etapas, 19 ADR, cobertura real, comandos).

---

## 8. Riesgos y contramedidas

| Riesgo | Impacto | Contramedida |
|---|---|---|
| Romper el contrato `kin/engine` al tocar transacciones | Alto | Mantener el contrato; añadir tests de contrato en el router antes de refactorizar |
| Rotación de token pierde sesiones activas | Medio | Ventana de doble clave (soportar anterior por TTL) + aviso de cambio |
| Idempotencia de webhook falla en picos | Medio | Cola de procesamiento + tabla de eventos + reconciliación manual |
| Rate limiter rompe a usuarios legítimos | Medio | Token bucket con balkan por usuario mientras se detecta IP real |

---

## 9. KPI de éxito

- **Seguridad**: 0 secrets en repo → detector de secretos bloqueante; 0 endpoints administrables públicos; autenticación + autorización con @PreAuthorize.
- **Observabilidad**: 100% de endpoints con metricas HTTP; log con trace id; < 1 min de aislamiento del error.
- **Testing**: Agregar integración con Testcontainers; cobertura de ramas ≥ 70% objetivo; > 3 `@SpringBootTest` de ruta caliente.
- **Rendimiento/Arquitectura**: p95 de chat-stream objetivo definido; transacciones únicamente I/O DB; caché con TTL configurable y recuentos correctos.

---

### Aplicación de score por categoría (objetivo vs actual)

| Dimensión | Actual | Objetivo |
|---|---|---|
| Seguridad | 58 | **≥ 88** |
| Testing | 70 | **≥ 88** |
| Observabilidad | ~70 | **≥ 88** |
| Arquitectura | 80 | ≥ 88 |
| Escalabilidad | 62 | ≥ 85 |

Los entregables operativos (tickets con DoD) y el calendario de sprints están en `SPRINT_BACKLOG_ENTERPRISE.md`.
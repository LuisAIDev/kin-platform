# FASE 15.0 — Enterprise Deployment & Production Certification

**Estado**: ✅ Certificación completada (sin modificaciones al dominio)
**Fecha**: 2026-08-05
**Autor**: KIN Architecture Team

> **Alcance**: certificación del Knowledge Engine para despliegue en producción.
> No se modificó el dominio. ADR-014 / ADR-012 intactas. Todo lo que requiera un
> entorno real (Render/Docker/Kubernetes/Prometheus/Redis/PostgreSQL) se marca
> explícitamente como **pendiente de validación en entorno de despliegue**.

---

## 1. Resumen ejecutivo

El backend arranca el contexto Spring completo sin errores (3 tests de
certificación nuevos), los beans del Knowledge Engine y del pipeline se
inicializan, no hay dependencias circulares, no hay secretos hardcodeados, el
log es estructurado sin datos sensibles, y el posture de seguridad/CORS/JWT/rate
limiting es sólido. **BUILD SUCCESS, 2180 tests verdes, cobertura ≥90 %.**

## 2. Arquitectura verificada

- Dominio POJO puro (`kin.knowledge.*`), observabilidad desacoplada
  (`ai.observability`), integración por composición (Gateway → Orchestrator).
- Pipeline de 13 etapas cableado en `KinConfig` (bean `Pipeline`).
- Sin ciclos de beans (validado por arranque de contexto).

## 3. Configuración verificada

| Aspecto | Estado |
|---|---|
| `application.yml` (default/dev) | ✔ H2 file, actuator expone `health, info, metrics, prometheus` |
| `application-test.yml` | ✔ H2 in-memory |
| `application-prod.properties` | ✔ PostgreSQL, Flyway V1…V10, JWT_SECRET, CORS `ALLOWED_ORIGINS` |
| Perfil `enterprise` | ⚠ **NO definido** (solo default/dev, test, prod) |
| Variables de entorno | ✔ `JWT_SECRET`, `DEEPSEEK_API_KEY`, `DATABASE_URL/USER/PASSWORD`, `POSTGRES_PASSWORD`, `ALLOWED_ORIGINS`, `PORT` |
| Prometheus / Actuator / Micrometer | ✔ registry Prometheus añadido; `/actuator/prometheus` configurado |
| OpenTelemetry | ⚠ Preparado (correlation/trace ids), exportación **pendiente de entorno** |
| CORS | ✔ doble capa (CorsConfig + SecurityConfig), origen prod garantizado |
| Security / JWT / Rate Limiting | ✔ stateless JWT, `RateLimitingFilter` (5 req/min), `SubscriptionAccessFilter` |
| Flyway | ✔ V1…V10 (prod habilitado; dev/test deshabilitado) |
| Redis | ⚠ No implementado (pendiente: caché distribuida opcional) |
| PostgreSQL | ✔ configuración prod lista (validación real **pendiente de entorno**) |

## 4. Seguridad

- ✔ **Secrets/credenciales**: sin secretos hardcodeados en `src/main` (grep).
- ✔ **Variables sensibles**: todas vía `${ENV}` (`.env` gitignored).
- ✔ **SSRF**: mitigado por `baseUrl` operador-configurado en `HttpKnowledgeSourceAdapter`
  + allowlist de dominios del `SourceValidator` (rechaza URLs fuera de allowlist).
- ✔ **Path Traversal / SQL Injection**: sin construcción de rutas/SQL en el dominio;
  `JdbcKnowledgeSource` usa SQL parametrizado (infraestructura).
- ✔ **Header / Log Injection**: logging estructurado con escape de comillas;
  headers de seguridad (CSP, HSTS, frame deny, Permissions-Policy, Referrer-Policy).
- ✔ **Sensitive/Prompt/Token/PII Leakage**: el log estructurado solo registra
  IDs/duraciones/conteos; sin `System.out`; sin prompts en logs.
- ✔ **Stacktraces**: los errores se registran como eventos estructurados (sin stack dumps en producción por defecto).
- ✔ **Actuator**: solo `health, info` permitAll; `metrics/prometheus` autenticados.

## 5. Deployment

- `kin-backend/Dockerfile`: 2 etapas (maven build → JRE 17), EXPOSE 8080.
  ⚠ Hallazgos: sin `HEALTHCHECK` en la imagen; `COPY . .` sin capas de dependencias
  cacheadas; `-DskipTests`.
- `docker-compose.yml`: postgres (healthcheck `pg_isready`, volume, red) + backend
  (`depends_on: service_healthy`, env desde host `${...}`) + frontend.
  ⚠ Hallazgos: servicio `kin-backend` sin healthcheck propio; `NEXT_PUBLIC_API_URL`
  apunta a `http://localhost:8080` (debería ser la URL interna en Docker).
- Despliegue real (Render/Docker): **pendiente de validación en entorno de despliegue**.

## 6. Runtime

- ✔ Startup limpio: contexto Spring arranca (3 tests `EnterpriseRuntimeCertificationTest`).
- ✔ Beans del Knowledge Engine/pipeline inicializados (KnowledgeEngine, Gateway, Stage, Pipeline, KinMethod, ConversationOrchestrator, PromptAssembler, MeterRegistry).
- ✔ Sin dependencias circulares (Spring falla ante ciclos; arranque exitoso).
- ✔ Sin configuraciones huérfanas en la ruta principal.
- Shutdown limpio y healthchecks reales: **pendiente de entorno de despliegue**.

## 7. Resiliencia

Validado por tests de dominio existentes (`OrchestratorErrorTest` +
`OrchestratorIntegrationTest`):

- Proveedor caído / lento / timeout → degradación controlada o fail-fast según estrategia.
- Sin Internet / Offline Mode → `KnowledgeResult.empty()` con motivo (offline-first).
- Caché corrupta / vacía / llena → hit/miss determinista; repository nulo seguro.
- Repository indisponible → sin caché (el ciclo continúa).
- Provider parcial → excluye el no disponible y continúa con los restantes.
- Graceful Degradation / Fail Fast → verificados por estrategia.

## 8. Escalabilidad

- ✔ Horizontal: orquestador **stateless** (contexto por ciclo); sin sticky sessions (JWT stateless).
- ✔ Thread safety: records inmutables; `CorrelationContext` ThreadLocal aislado.
- ✔ Concurrencia: stress de 100–5000 requests sin errores (Fase 8).
- ⚠ Caché compartida entre instancias: requiere adaptador Redis de `KnowledgeRepository`
  (**pendiente**); balanceador/auto-scaling: **pendiente de entorno**.
- Múltiples instancias reales: **pendiente de validación en entorno de despliegue**.

## 9. Observabilidad

- ✔ Micrometer + `/actuator/prometheus` + `/actuator/metrics` configurados.
- ✔ Structured logging con `correlationId/requestId/traceId/durationMs/result`.
- ✔ Métricas por etapa y por `ProviderType` (Fase 7).
- ✔ Dashboards y alertas documentados (FASE13).
- Scraping real de Prometheus y exportación OTel: **pendiente de entorno de despliegue**.

## 10. Checklist

```
□ mvn verify: BUILD SUCCESS · 2180 tests verdes
□ Cobertura JaCoCo ≥90 % (dominio + observability)
□ Contexto Spring: startup limpio, beans OK, sin ciclos
□ Sin secretos hardcodeados · sin logging sensible
□ CORS/JWT/RateLimit/headers de seguridad OK
□ Actuator expuesto (health,info,metrics,prometheus)
□ Resiliencia validada por tests (offline/graceful/fail-fast/cache)
□ Escalabilidad: stateless + concurrencia validada
□ Dockerfile + docker-compose auditados (con hallazgos)
□ Documentación FASE15 generada
```

## 11. Resultados

- **mvn verify**: BUILD SUCCESS · **2180 tests** (0 fallos/errores/skipped) · coverage OK.
- **Cobertura**: knowledge 100 %, citation 99.2 %, policy 98.6 %, planner 96.8 %,
  orchestrator 95.0 %, engine 94.9 %, stage 100 %, observability 90.3 % (≥90 %).

## 12. Riesgos

1. Perfil `enterprise` no definido (añadir `application-enterprise.yml` si se requiere).
2. Dockerfile sin HEALTHCHECK y con caché de build subóptima (rendimiento de CI/rollout).
3. `NEXT_PUBLIC_API_URL` apunta a localhost en compose (rompería llamadas frontend→backend en Docker).
4. Sin caché distribuida (Redis) → hit-ratio entre instancias limitado.
5. Validación real de healthchecks, Prometheus, PostgreSQL y Flyway requiere el entorno de despliegue.

## 13. Recomendaciones

1. Añadir `HEALTHCHECK` al Dockerfile y healthcheck al servicio `kin-backend` en compose.
2. Optimizar el Dockerfile (copiar pom → descargar dependencias → copiar código).
3. Corregir `NEXT_PUBLIC_API_URL` a la URL interna del backend en compose.
4. Definir perfil `enterprise` cuando se requiera.
5. Implementar adaptador Redis de `KnowledgeRepository` (pendiente de ADR de citación).

## 14. Definition of Done

✔ Sin modificaciones al dominio ✔ ADR-014/012 intactas ✔ BUILD SUCCESS (2180 tests)
✔ Cobertura ≥90 % ✔ Runtime/Config/Security/Resiliencia/Escalabilidad/Observabilidad
auditados ✔ Hallazgos documentados ✔ Despliegue real marcado como pendiente de
entorno ✔ Certificación de producción emitida.

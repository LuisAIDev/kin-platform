# FASE 17.0 — Enterprise Cloud Infrastructure & Production Platform

**Estado**: ✅ Implementado (solo infraestructura; dominio intacto)
**Fecha**: 2026-08-05
**Autor**: KIN Architecture Team

> **Alcance**: infraestructura Cloud Enterprise para producción: Render Blueprint,
> Docker no-root con HEALTHCHECK, caché Redis opcional detrás de `KnowledgeRepository`,
> perfiles `render`/`enterprise`, compresión + health probes, backups/DR y costos.
> ADR-012/014 intactas. Todo lo que depende de un entorno real (Render, Redis,
> PostgreSQL, TLS, Prometheus) se marca **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

---

## 1. Render Production

- **`render.yaml`** (Blueprint) creado: base de datos `kin-db` (PostgreSQL) +
  servicio web `kin-backend` (runtime docker, `rootDir: kin-backend`,
  `healthCheckPath: /api/v1/actuator/health`, `autoDeploy: true`) + servicio
  `kin-frontend` (runtime node, `npm ci && npm run build`, `npm start`).
- Variables y secrets (`JWT_SECRET`, `DEEPSEEK_API_KEY`, `STRIPE_*`, `DATABASE_*`)
  definidos; `DATABASE_URL` enlazado desde la DB del blueprint.
- Perfil `render` (`application-render.properties`) incluye `prod`.
- Validación del despliegue real en Render: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

## 2. Docker Enterprise

- `kin-backend/Dockerfile`: multi-stage, capa de dependencias (`dependency:go-offline`),
  **HEALTHCHECK** (Actuator health) y **usuario no-root** (`app`).
- `kin-frontend/Dockerfile`: multi-stage, **HEALTHCHECK** y **usuario no-root** (`node`).
- `docker-compose.yml`: healthchecks en `postgres-db`, `kin-backend` y `kin-frontend`,
  `restart: unless-stopped`, redes y volúmenes ya presentes.
- Build de imágenes real: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL** (Docker).

## 3. Redis

- Dependencia `spring-boot-starter-data-redis` añadida (solo build; no conecta en
  arranque sin uso).
- **`RedisKnowledgeRepository`** (infraestructura, `ai.knowledge.adapter`): implementa el
  puerto `KnowledgeRepository` (ADR-014) **sin modificar el dominio**; almacena resultados
  validados serializados en JSON con TTL.
- Activación opcional: `kin.cache.redis.enabled=true` (`RedisCacheConfig` con
  `@ConditionalOnProperty`). Por defecto **deshabilitado** → comportamiento de
  dev/test/prod sin cambios.
- ⚠ **Limitación del contrato congelado**: `save(KnowledgeResult, Duration)` no recibe la
  `KnowledgeQuery` (ADR-014), por lo que `find` y `save` derivan claves distintas (consulta
  vs contenido). El cruce hit/miss por turno requiere **ADR aditiva** de clave de caché.
- Validación con Redis real: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

## 4. PostgreSQL

- Pool Hikari configurado (máx 10 en prod; 20 en enterprise), timeouts, `SELECT 1` init,
  driver PostgreSQL, Flyway V1…V10, `ddl-auto: none` en prod.
- SSL/backups/restore reales: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

## 5. Production Profiles

- Creados: `application-render.properties` (incluye `prod` + PORT, probes, compresión) y
  `application-enterprise.properties` (pool 20, probes, compresión, HTTP/2 documentado,
  Redis opcional). Perfiles existentes (`dev`=default, `test`, `prod`) intactos.

## 6. Variables de entorno (centralizadas)

`JWT_SECRET`, `POSTGRES_PASSWORD`, `DEEPSEEK_API_KEY`, `OPENAI_API_KEY` (opcional),
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `DATABASE_URL/USER/PASSWORD`, `PORT`,
`ALLOWED_ORIGINS`, `KIN_REDIS_ENABLED`, `SPRING_PROFILES_ACTIVE` — documentadas en
`.env.example` y README.

## 7. Observabilidad productiva

- Prometheus `/actuator/prometheus`, metrics, Micrometer, structured logs con
  correlación, MDC — ya implementados (Fase 7).
- **Health probes** añadidas: `management.endpoint.health.probes.enabled=true` +
  grupos `readiness` (readinessState, db) y `liveness` (livenessState).
- Scraping real: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

## 8. Seguridad Cloud

- HTTPS/HTTP2: HTTP/2 documentado (requiere TLS); terminación TLS en Render/LB:
  **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.
- CORS producción (`ALLOWED_ORIGINS` + origen Vercel garantizado), JWT stateless,
  headers de seguridad, usuario no-root en contenedores, secrets vía variables.

## 9. Backups & Disaster Recovery

- **Backup PostgreSQL**: `pg_dump -Fc -U kin_admin -d kin_platform > backup.dump`
  (documentado; programar con cron/Render Scheduled Jobs).
- **Restore**: `pg_restore -U kin_admin -d kin_platform backup.dump`.
- **RTO/RPO**: objetivo RTO < 1 h (restore de dump + Flyway) y RPO según frecuencia de
  backup (recomendado diario) — **documentado; validación pendiente de entorno real**.

## 10. Escalabilidad

- Backend **stateless** (contexto por turno) y sin sticky sessions → horizontal.
- Caché compartida multi-instancia vía `RedisKnowledgeRepository` (pendiente de ADR
  de clave + entorno Redis).
- Auto-scaling/Load Balancer real: **PENDIENTE DE VALIDACIÓN EN ENTORNO REAL**.

## 11. Cost Optimization (referencia Render)

| Recurso | Free | Starter | Pro |
|---|---|---|---|
| Backend (Render) | 0.1 CPU / 512 MB | 0.5 CPU / 1 GB | 1 CPU / 2 GB |
| Frontend (Render) | igual | igual | igual |
| PostgreSQL (Render) | 0.1 CPU / 1 GB | 0.5 CPU / 1 GB | 1 CPU / 4 GB |
| Redis (Render/Upstash) | — | pequeño | estándar |

Recomendación: iniciar en Starter (prod real) y escalar según demanda; compresión y
caché reducen ancho de banda/latencia.

## 12. Documentación

- `render.yaml` · `application-render.properties` · `application-enterprise.properties`
- `.env.example` ampliado · README (sección Cloud) · este documento.
- Runbooks (backup/restore/rollback): documentados arriba.

## 13. Checklist de producción

```
□ mvn verify BUILD SUCCESS (2180 tests) · cobertura ≥90 %
□ npm test (130) · lint 0 · tsc 0 · next build OK
□ render.yaml (blueprint) creado
□ Dockerfiles no-root + HEALTHCHECK · compose con healthchecks
□ Redis adapter detrás de KnowledgeRepository (off por defecto)
□ Perfiles render/enterprise creados
□ Compresión + health probes configurados
□ ADR-012/014 intactas · dominio sin modificar
□ Validación en entorno real PENDIENTE (Render/Docker/Redis/PostgreSQL/TLS/Prometheus)
```

## 14. Definition of Done

✔ Infraestructura implementada sin tocar el dominio ✔ BUILD SUCCESS ✔ Cobertura ≥90 %
✔ ADR-012/014 intactas ✔ Conocimiento Engine/Orquestador/Pipeline/Prompt Builder intactos
✔ Frontend/Backend funcionales ✔ GitHub Actions funcionales ✔ Redis adapter detrás del
puerto ✔ Perfiles render/enterprise ✔ Documentación de backups/DR/costos ✔ Pendientes
de entorno real claramente marcados.

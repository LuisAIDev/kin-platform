# Production Readiness Checklist — Proyecto KIN

> Checklist de puesta en producción (estilo Netflix/Google Production Readiness Review).
> Estado: **[ ]** pendiente · **[x]** cumplido · **[~]** parcial.
> Referencias a `TECHNICAL_DEBT_REGISTER.md` (T#) y `SPRINT_BACKLOG_ENTERPRISE.md` (K-###).

---

## 1. Despliegue y configuración

| # | Criterio | Estado | Evidencia / Ticket |
|---|---|---|---|
| P1 | Build reproducible y versionado (Maven wrapper, lock de dependencias) | [x] | `mvnw`, `package-lock.json` |
| P2 | Secrets fuera del repo y por ambiente (dev/test/prod) | [ ] | JWT_SECRET en CI (T5/K-401); `.env` gitignored |
| P3 | Configuración por ambiente sin valores hardcodeados sensibles | [ ] | `application-prod.properties` con secrets desde env |
| P4 | `DATABASE_URL` válida (JDBC) en prod | [ ] | T14/K-601 |
| P5 | Health checks usados por el orquestador/load balancer | [ ] | `/actuator/health` existe; pero `/actuator/**` abierto (T6) |
| P6 | Despliegue idempotente (migraciones Flyway correctas) | [x] | Flyway V1..V3 + `init.sql` (dev sin Flyway) |
| P7 | Rollback/blue-green o canary definido | [ ] | — |

## 2. Base de datos y datos

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| P8 | Postgres en prod con credenciales rotadas y acceso limitado | [ ] | Render DB configurada; verificación pendiente |
| P9 | Backups automáticos + restauración probada (RPO/RTO) | [ ] | T24/K-703 |
| P10 | Migraciones con `ddl-auto: none` y sin divergencia dev/prod | [x] | prod Flyway; dev H2 `update` |
| P11 | Índices para consultas críticas (proyectos, mensajes, categorías) | [~] | sin revisión de índices documentada |
| P12 | Purgas/archivado de mensajes (historial grande) | [ ] | `clearConversation` solo borra; sin retención |
| P13 | Pool de conexiones dimensionado y monitoreado | [ ] | default Hikari sin tuning |

## 3. Rendimiento y concurrencia

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| P14 | Sin I/O externa dentro de transacciones (IA fuera de tx) | [ ] | T3/K-101 |
| P15 | Caché correctamente invalidada (projectLimit, activeSubscription) | [ ] | T1/T2/K-202/K-203 |
| P16 | Rate limiting efectivo y por IP real | [ ] | T7/K-501 |
| P17 | Streaming SSE con backpressure/terminación controlada | [~] | SSE existe; verificar cancelación |
| P18 | Pruebas de carga (chat, streaming, generación de reporte) | [ ] | sin benchmark real |
| P19 | P95 objetivo documentado para chat-stream y reporte | [ ] | — |

## 4. Seguridad en runtime

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| P20 | TLS en el punto de terminación | [ ] | — |
| P21 | Cabeceras de seguridad (HSTS, CSP, nosniff, Referrer-Policy) | [ ] | K-506 |
| P22 | `/actuator/**` restringido | [ ] | T6/K-402 |
| P23 | Errores sin detalles internos | [ ] | T6/K-402 |
| P24 | Token de sesión seguro (HttpOnly cookie) | [ ] | T9/K-503 |
| P25 | Idempotencia de webhooks | [ ] | T4/K-301 |
| P26 | Detección de secretos en CI (gate) | [ ] | T5/K-401 |

## 5. Observabilidad

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| P27 | Logs estructurados JSON con `traceId`/`spanId` | [ ] | T17/K-602 |
| P28 | Tracing distribuido end-to-end (W3C trace context) | [ ] | T16/K-602 |
| P29 | Métricas HTTP (RED) + JVM por endpoint | [ ] | T19/K-603 |
| P30 | Métricas de coste/tokens LLM | [ ] | K-605 |
| P31 | Alertas de error rate, p95, 5xx, pool de conexiones | [ ] | T23/K-702 |
| P32 | Dashboard de estado (Grafana/obs) | [ ] | — |
| P33 | Playbook de incidentes + runbooks | [ ] | T23/K-702 |
| P34 | SLO/SLA definidos | [ ] | — |

## 6. Testing en el camino a producción

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| P35 | Cobertura real conocida (instr 73.59% / ramas 58.83%) | [x] | `jacoco.csv` |
| P36 | Gates de cobertura en CI | [ ] | T18/K-404 |
| P37 | Tests de integración con Postgres real (Testcontainers) | [ ] | T15/K-401 |
| P38 | Tests E2E del flujo de viabilidad | [ ] | K-410 |
| P39 | Tests de idempotencia/concurrencia (webhook, caché) | [ ] | K-411 |
| P40 | Smoke test post-despliegue | [ ] | — |

## 7. Operación y continuidad

| # | Criterio | Estado |
|---|---|---|
| P41 | Documentación de arquitectura sincronizada (19 ADR, 13 etapas, cobertura) | [ ] (K-701) |
| P42 | Plan de escalado multi-replica (caché distribuida, sesión) | [ ] (K-606) |
| P43 | Gestión de errores de pagos (reembolsos, conciliación) | [ ] |
| P44 | Compliance: GDPR/DPA (exportar/borrar datos) | [ ] |
| P45 | Monitoreo de dependencias (CVE, actualizaciones) | [ ] (dependabot) |

---

## Gate de go to production

Para pasar de Alpha a Producción en el entorno Render, se exige el 100% de los items **P0**:

| Gate | Criterio |
|---|---|
| G1 | 0 secrets en repo + secretos desde secrets manager |
| G2 | `/actuator/**` cerrado + errores genéricos |
| G3 | `DATABASE_URL` JDBC + backups |
| G4 | Transacciones sin I/O IA + caché invalidada |
| G5 | Idempotencia de webhooks |
| G6 | Logs JSON + métricas HTTP básicas |
| G7 | Tests de integración (Testcontainers) verdes en CI |
| G8 | Cabeceras de seguridad + TLS + cookie HttpOnly |

Sin G1..G8 **no** se habilita el tráfico público. Secuencia de ejecución: `K-401, K-402, K-101, K-202/203, K-301, K-602, K-601`.

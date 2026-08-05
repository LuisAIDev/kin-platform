# KIN_OPERATIONAL_EXCELLENCE_SPECIFICATION.md — FASE 21

**Especificación Oficial de Excelencia Operacional (Operational Excellence) de KIN.**
*Consolida exclusivamente F12–F20 sin modificar ninguno de esos documentos. Compatible al 100 % con Hexagonal, DDD, Clean, CQRS, Event Driven, SemVer, Contract Versioning, Evolutionary Architecture, GitOps, DevSecOps, Zero Trust y Defense in Depth. Filosofía inalterable: **Java decide, la IA comunica**.*

---

## 1. Resumen Ejecutivo

La **FASE 21** define el **modelo oficial mediante el cual KIN es operado durante 10–15 años**: Excelencia Operacional, SRE, Reliability, Availability, Error Budgets, Incident/Problem/Change/Release Management, Capacity Planning, Cost Optimization, Operational Governance, Platform Health y Continuous Improvement. Es **documentación Enterprise exclusivamente**: no implementa, no modifica arquitectura y no agrega funcionalidades. Todo se describe como **capa adicional, mecanismo operativo, política, proceso o guía** — jamás como modificación del Core.

---

## 2. Objetivo

Definir cómo KIN se **opera con excelencia** a escala mundial: disponibilidad sostenida (99.5 %), errores dentro de presupuesto, incidentes resueltos con MTTR ≤ 1 h, capacidad dimensionada de 100 a 10M de usuarios, costos optimizados por `CostEngine`/`PlanAccessPort`, y mejora continua — todo con el Core, el DAG, el AI Boundary y la filosofía **"Java decide, la IA comunica"** intactos.

---

## 3. Alcance

**Incluye**: modelo operacional, SRE, reliability/availability, incidentes y problemas, cambio y release, capacidad, performance, monitoreo y observabilidad, seguridad operativa, gobernanza, KPIs, checklists, mejora continua, roadmap y riesgos operacionales.

**Excluye**: cualquier modificación de Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, Document Generation, Flyway, AI Boundary, DAG, Bounded Contexts, contratos, puertos, adapters, eventos, Aggregates, APIs o ADR.

---

## 4. Principios de Excelencia Operacional

| # | Principio | Fuente |
|---|---|---|
| OX1 | **La operación respeta el Core**: todo es capa/política/proceso, nunca modificación | F19 §24 |
| OX2 | **Error Budget como gobernanza**: sin SLO no hay release | F12B §9, F17 §14 |
| OX3 | **Fail-open determinista**: nada externo rompe la decisión en Java | F12B §6 |
| OX4 | **Observabilidad obligatoria** (logs/métricas/tracing/DecisionRecord) | F17 §11–12 |
| OX5 | **GitOps y automatización por defecto** | F17 §2/§9 |
| OX6 | **Blameless y mejora continua** | F12C §21 |
| OX7 | **Costo gobernado** por plan y región | F12A §4 |
| OX8 | **Excelencia = confianza**: dato + fuente + fecha + score en operación | F12A §6 |
| OX9 | **Java decide, IA comunica** (invariante operacional) | F12B §7 |

---

## 5. Modelo Operacional de KIN

```
Plano de Control (GitOps: ArgoCD/Flux · FeatureRegistry · Config versionada)
      │
      ├── Plano de Runtime (servicios stateless · datos · cache · read models por región)
      ├── Plano de Observabilidad (OTel · Prometheus/Grafana · Loki · dashboards SLO)
      └── Plano de Seguridad (Vault · mTLS · Zero Trust · gates)
```

- **Promoción**: Local → Development → QA → Staging → Production → DR (F17 §3).
- **Release**: Alpha → Beta → RC → GA → LTS (F12C §10).
- **Operación por región** (F20 §10): GitOps idéntico, canary por región, residencia.

---

## 6. Operational Architecture

| Capa | Componentes | Responsabilidad |
|---|---|---|
| Control | GitOps, `FeatureRegistry`, config | Promoción y rollback determinista |
| Runtime | API, BIL, Enterprise, Cache, Datasets | Operar el producto |
| Datos | PostgreSQL, Redis, TimeSeries, Warehouse, Vault | Persistir y servir con inmutabilidad |
| Observabilidad | OTel, Prometheus, Grafana, Loki | Medir todo |
| Seguridad | Vault, mTLS, SBOM, gates | Proteger todo |

*Sin nuevos componentes: consolida F17 §8 y F20 §5.*

---

## 7. SRE Model

- **SRE como práctica transversal** (no un módulo): define SLIs/SLOs, error budgets, on-call, toil reduction y runbooks.
- **On-call**: rotación por equipo/región; P0/P1 con respuesta ≤ 15 min; escalamiento Developer→TL→Owner→Board→CTO (F12C §14).
- **Toil reduction**: toda tarea manual repetitiva se automatiza. El objetivo **toil ≤ 20 % del tiempo** es una **política operativa definida oficialmente por la FASE 21** (no proviene de una fase previa); su valor no altera ningún KPI ni modelo de fases anteriores.
- **Runbooks**: por componente (Vault, Ingestion, Datasets, Router, Market/Experience, LLM, Redis, PostgreSQL) (F17 §13).

---

## 8. Reliability Model

| Concepto | Definición | Fuente |
|---|---|---|
| SLI | Disponibilidad, latencia p95, cache hit, freshness | F12B §9 |
| SLO | 99.0 % (F17) → **99.5 % (F19/F20)** | F13 §9 |
| SLA | Compromiso comercial por plan (Enterprise) | F17 §14 |
| Error Budget | 100 % − SLO; consumo mensual monitoreado | F17 §14 |

**Redundancia**: multi-AZ → multi-región; datasets inmutables con versiones anteriores siempre servibles; fallback determinista (`ResponseFallback`, cache, read models).

---

## 9. Availability Model

- Disponibilidad objetivo **99.5 % por región y global** (F19/F20).
- Topologías HA: multi-AZ para F17; multi-región activo-activo (read) con failover para F19/F20.
- Modos degradados: cache-only, dataset anterior, proveedores reducidos — nunca fallo de Core.
- Ventanas de mantenimiento declaradas y dentro del error budget.

---

## 10. Incident Management

| Etapa | Definición | Fuente |
|---|---|---|
| Detección | Alertas P0–P3 (F17 §13) + SLO breach + threat detection | F17/F18 |
| Clasificación | Incidentes **P0–P4** (F18 §17); impacto en datos/IA/tenants | F18 §17 |
| Respuesta | On-call → diagnóstico → contención (kill-switch vía `FeatureRegistry`, revocación de tokens, rotación de secrets) | F18 §17 |
| Recuperación | Rollback determinista (F12C §20) + restore (F17 §15) | F17/F12C |
| Postmortem | Blameless + RCA + acciones con dueño y fecha | F12C §21 |
| Lessons | Actualización de runbooks y threat model | F18 §17 |

---

## 11. Problem Management

- **Problema ≠ incidente**: un incidente es el síntoma; el problema es la causa raíz.
- Proceso: RCA por incidente P0/P1 → `known errors` → fix permanente con dueño y fecha → verificación en QA/Chaos.
- Análisis de tendencias trimestral (recurrencias) → alimenta el plan de mejora.

---

## 12. Change Management

| Tipo de cambio | Flujo | Fuente |
|---|---|---|
| Cosmético | PR estándar | F12C §15 |
| Refactor | PR + tests | F12C §15 |
| Feature | RFC + DoR + PR + Increment Gate | F12C §15 |
| Architecture | ADR + Board + CTO | F12C §15 |
| Infrastructure | RFC + SRE + Board | F12C §15 |
| Security | Revisión de seguridad + CTO | F12C §15 |
| Breaking Change | ADR + deprecación + MAJOR + CTO | F12C §15 |

Cambios de bajo riesgo y repetitivos se automatizan (GitOps, estándar); los de alto riesgo requieren Change Advisory; los de emergencia, aprobación post-hoc ≤ 24 h.

---

## 13. Release Management

| Release | Gate | Fuente |
|---|---|---|
| Alpha | suite completa; flags off | F16 §20/F17 §19 |
| Beta | perf + mutation + contract + pilot | F16 §20 |
| RC | chaos + security + SBOM + SLOs | F16 §20 |
| GA | Release Gate + scorecard ≥ 8.0 | F16 §20 |
| LTS | Core estable + soporte 1 año | F12C §10 |

Progressive delivery: FeatureRegistry (canary/rollout/kill-switch) · Feature freeze → Code freeze → RC → GA (F12C §10).

---

## 14. Capacity Planning

| Escala | Infraestructura (F17 §17) |
|---|---|
| 100 | Compose + H2/PostgreSQL pequeño |
| 1K | Compose + Redis |
| 100K | K8s HA multi-AZ + PG replicado + read models |
| 1M | Multi-AZ + CDN + sharding de series + outbox/broker + residencia |
| 10M | **Multi-región** + read replicas + HPA global + DR activo |

Autoscaling por CPU/latencia en QueryService/AiContext; workers de ingesta por país; capacidad por región con presupuesto (`CostEngine`).

---

## 15. Performance Engineering

- Targets: p95 read model **< 500 ms (F17–F18) → < 300 ms (F19/F20)**; turno conversacional fluido.
- Prácticas (F14 §18): read models materializados, sin N+1, batch en ingesta, streams con backpressure, paginación con cursor.
- Perf regression gate en cada release (F16 §8/§9); carga 10M y chaos multi-región (F16 §9/§10).

---

## 16. Operational Monitoring

- **Prometheus + Micrometer**: `kin.bil.*`, `kin.provider.*`, `kin.ai.*`, `kin.business.*` (F12B §9).
- **Grafana**: dashboards por región + global; dashboards SLO/error budget.
- **Alertas P0–P3** con runbook y escalamiento (F17 §13).
- Dashboards oficiales: Proyecto, BIL/Mercado, Enterprise, Analytics, Marketplace, Health/Ops (F15 §15).

---

## 17. Observabilidad

- **Tracing**: OpenTelemetry, `queryId`/`correlationId` + `regionId` extremo a extremo (F20 §10).
- **Logs estructurados**: `[KIN] <projectId> <userId> <queryId> <traceId> <regionId> <componente> <mensaje> <durationMs>` (F17 §12).
- **Métricas** por componente y negocio; **DecisionRecord** para toda decisión (auditoría).
- **SLOs** medidos por región y globales; error budget en dashboards.

---

## 18. Operational Security

Consolidación de F18 en operación: Zero Trust (identidad por llamada, mTLS), Vault con rotación (AES-GCM, fallo cerrado), SBOM firmado por release, gates de seguridad (PR/Merge/Release/GA/LTS + Security Approval), residencia por región, PII gobernada y auditoría (`DecisionRecord`, `DataAccessAudited`).

---

## 19. Operational Governance

- Ownership de runbooks y SLOs por equipo (F12C §2).
- Change Advisory para cambios de alto riesgo (F12C §15).
- Revisión operativa trimestral: error budgets, recurrencias, toil, costos.
- Scorecard trimestral (F12B §10) con dimensión operacional; < 8.0 → plan de acción.
- Escalamiento: Developer → TL → Owner → Board → CTO (F12C §14).

---

## 20. Operational KPIs

| KPI | Objetivo | Frecuencia |
|---|---|---|
| Availability / Error Budget | 99.5 % · presupuesto sano | Mensual |
| Perf p95 (read model) | < 300 ms | Semanal |
| MTTR (P0/P1) | ≤ 1 h | Mensual |
| MTBF | tendencia creciente | Mensual |
| Deployment frequency / Lead time | ≥ 3/sem · ≤ 3 días | Mensual |
| Change Failure Rate | ≤ 15 % | Mensual |
| Cache hit ratio / Dataset freshness | ≥ 90 % · ≤ 70 % TTL | Mensual |
| Costo IA/turno · Costo operativo | presupuesto −30 % | Mensual |
| Toil | ≤ 20 % del tiempo *(política operativa definida por la FASE 21; no proviene de una fase previa)* | Trimestral |
| Architecture Health | ≥ 8.0 | Trimestral |

---

## 21. Operational Checklists

| Área | Checklist |
|---|---|
| **Release** | ¿SLOs medibles? · ¿rollback probado? · ¿flags? · ¿runbooks actualizados? · ¿SBOM firmado? |
| **Incidente** | ¿severidad correcta (P0–P4)? · ¿contención ≤ 15 min (P0)? · ¿postmortem < 48 h? · ¿RCA? |
| **Cambio** | ¿tipo clasificado? · ¿aprobación requerida? · ¿rollback? |
| **DR** | ¿backups PITR? · ¿restore probado trimestral? · ¿failover de región? |
| **Capacidad** | ¿autoscaling? · ¿presupuesto por región? · ¿cuotas (`PlanAccessPort`)? |
| **Seguridad** | ¿Vault rotado? · ¿gates de seguridad? · ¿sin secrets? |
| **Performance** | ¿p95 dentro de SLO? · ¿sin N+1? · ¿perf regression gate? |

---

## 22. Continuous Improvement

- **Retro loops**: retrospectiva de sprint (F12C §6) + revisión operativa trimestral.
- **Postmortem blameless**: cada P0/P1 produce acciones con dueño y fecha (F12C §21).
- **Toil reduction**: automatización continua de pasos manuales.
- **Capacity & cost reviews**: trimestral con `CostEngine` y capacity plan.
- **SLO reviews**: ajuste de SLOs con evidencia de error budget.
- **Scorecard trimestral** (F12B §10): dispara planes de acción si < 8.0.

---

## 23. Roadmap Operacional

| Fase | Madurez operacional |
|---|---|
| F13–F14 | CI/CD, gates QA, entornos promovidos |
| F15–F16 | Staging + perf + chaos + staging SLOs |
| F17 | SLO 99.0 %, GitOps, DR base |
| F18 | Seguridad operativa (Vault, gates, compliance) |
| F19 | Multi-región, residencia, SLO 99.5 % |
| F20 | Operación global, LTS, red de partners |
| **F21 (esta fase)** | **Excelencia operacional**: error budgets, toil, capacidad 10M, mejora continua |

---

## 24. Operational Risks

| Riesgo | Naturaleza | Mitigación |
|---|---|---|
| Error budget agotado | Operativo | SLO reviews + congelación de features |
| Recurrencia de incidentes | Operativo | Problem management + chaos + RCA |
| Costo operativo mundial | Negocio | CostEngine, TTL, presupuesto por región |
| Drift de regiones | Operativo | GitOps idéntico + detección de drift |
| Fuga de PII cross-región | Seguridad | Residencia, políticas, auditoría |
| Capacidad insuficiente en picos | Escalabilidad | Autoscaling, read models, capacity reviews |
| Toil alto (operación manual) | Operativo | Automatización + métrica de toil |

---

## 25. Compatibilidad con F12–F20

| Fase | Relación |
|---|---|
| F12 / F12A | Hipótesis de escala y DAG preservados |
| MIP | Gates, DoR/DoD, incrementos respetados |
| F12B | KPIs, scorecard, eventos, cache, AI Boundary |
| F12C | Ownership, PR/branch, release, escalamiento, incidentes |
| F13 | Fases y KPIs del roadmap |
| F14 | Reglas de implementación aditiva |
| F15 | Dashboards y boundaries de producto |
| F16 | Gates de QA, perf, chaos, load |
| F17 | SRE, SLOs, DR, GitOps, entornos |
| F18 | Seguridad operativa, compliance, incidentes |
| F19 | Arquitectura Empresarial consolidada |
| F20 | Operación multi-región y escala global |

---

## 26. Conclusiones

La **FASE 21** establece el **modelo oficial de Excelencia Operacional de KIN** para los próximos 10–15 años: SRE con error budgets, reliability y disponibilidad 99.5 %, incidentes con MTTR ≤ 1 h, capacidad de 100 a 10M de usuarios, costo gobernado y mejora continua — todo descrito como **capa adicional, mecanismo operativo, política, proceso o guía**, sin modificar el Core, el DAG, el AI Boundary ni la filosofía **"Java decide, la IA comunica"**.

---

*Especificación oficial de Excelencia Operacional de KIN (FASE 21). Documento formalizado conforme a la pre-auditoría (E1–E2 aplicados). Nombre documental oficial: `KIN_OPERATIONAL_EXCELLENCE_SPECIFICATION.md`. Compatible al 100 % con F12–F20.*

# KIN_ENTERPRISE_OBSERVABILITY_ARCHITECTURE_SPECIFICATION.md — FASE 29

**Especificación Oficial de Arquitectura de Observabilidad Empresarial (Enterprise Observability Architecture) de KIN.**
*Consolidación documental exclusiva de F12–F28 sin modificar ninguno de esos documentos. Compatible al 100 % con Hexagonal, DDD, Clean, CQRS, Event Driven, SOLID, SemVer, Contract Versioning, Evolutionary Architecture, GitOps, DevSecOps, Zero Trust y Defense in Depth. Filosofía inalterable: **Java decide, la IA comunica**.*

---

## 1. Resumen Ejecutivo

La **FASE 29** define la **Arquitectura Oficial de Observabilidad Empresarial de KIN** para los próximos 10–15 años: el **contrato documental** que gobierna observabilidad integral, telemetría, logging, tracing distribuido, métricas, dashboards, correlación, diagnóstico, auditoría operacional, detección temprana e inteligencia operacional — como **política, guideline, estrategia, estándar, contrato documental, governance, mecanismo documental o práctica**, jamás como implementación. Consolida F12–F28 sin duplicar Operaciones (F28).

---

## 2. Objetivo

Definir el **contrato documental de observabilidad de KIN**: telemetría, logs, métricas, trazas, dashboards, alertas, auditoría y diagnóstico, con correlación extremo a extremo — sin modificar el Core, el DAG, el AI Boundary ni ninguna decisión aprobada, preservando **"Java decide, la IA comunica"**.

---

## 3. Alcance

**Incluye**: principios de observabilidad, visión, arquitectura de observabilidad, telemetría, logging, métricas (SLI/SLO/KPI), tracing distribuido, observabilidad de negocio/plataforma/IA/seguridad/auditoría, diagnóstico, alertas, dashboards, métricas, KPIs, checklists y roadmap.

**Excluye** cualquier modificación de: Core, DAG, AI Boundary, Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, Flyway, ConnectorCatalog, EventCatalog, PolicyEngine, FeatureRegistry, DecisionRecord, BusinessDataset, SmartRouter, QueryService, AiContextBuilder, ResponseGuard, ResponseFallback, SecurityGateway, Vault, CostEngine, PlanAccessPort, APIs, contratos, eventos, ports, adapters, entidades, Aggregates, VOs, Domain/Application Services, casos de uso, Bounded Contexts, reglas de negocio o ADR.

---

## 4. Principios de Observabilidad Empresarial

| # | Principio |
|---|---|
| O1 | **Observabilidad antes que monitoreo** — capacidad de explicar el estado, no solo detectarlo. |
| O2 | **Telemetría obligatoria** — todo componente emite logs/métricas/trazas (F17 §2). |
| O3 | **Correlación extremo a extremo** — identificadores propagados en toda la cadena (F17 §12). |
| O4 | **Evidencia verificable** — dato + fuente + fecha + score (F12A §6). |
| O5 | **Datos observables** — datasets y decisiones trazables (`DecisionRecord`). |
| O6 | **Diagnóstico reproducible** — runbooks y trazas reutilizables. |
| O7 | **Automatización** — alertas y dashboards automáticos por defecto. |
| O8 | **Java decide, IA comunica** — la observabilidad mide, jamás decide. |

---

## 5. Enterprise Observability Vision

La observabilidad empresarial de KIN es la **capa documental que hace explicable la plataforma durante 15 años**: toda operación, dato y decisión es trazable, medible y diagnosticable — con correlación de extremo a extremo y evidencia verificable, sin delegar jamás decisiones a la IA.

---

## 6. Arquitectura General de Observabilidad

| Pilar | Contenido |
|---|---|
| **Logs** | Estructurados, con identificadores oficiales (F17 §12) |
| **Metrics** | `kin.*` por componente y negocio (F12B §9) |
| **Traces** | OTel, correlación extremo a extremo |
| **Dashboards** | Por servicio, región y negocio (F15 §15, F24) |
| **Alertas** | P0–P3 con runbook (F17 §13) |
| **Auditoría** | `DecisionRecord`, `DataAccessAudited` (F18) |
| **Telemetría/Diagnóstico** | Detección temprana y reproducción |

---

## 7. Telemetry Architecture

- Telemetría continua de logs, métricas y trazas en toda la plataforma (F17 §11).
- Misma telemetría por región (residencia, `regionId`).
- Almacenamiento por región; PII gobernada en telemetría (F18 §9).

---

## 8. Logging Architecture

Consolidación de F17 §12, F20 §10 y F24: logs estructurados con formato oficial

```
[KIN] <projectId> <userId> <queryId> <traceId> <regionId> <componente> <mensaje> <durationMs>
```

Niveles: ERROR/WARN/INFO/DEBUG/TRACE con uso definido; `projectId`/`userId` cuando estén disponibles.

---

## 9. Metrics Architecture

| Tipo | Ejemplos |
|---|---|
| **SLIs** | Disponibilidad, latencia p95, cache hit, freshness (F12B §9) |
| **SLOs** | 99.0 % (F17) → 99.5 % (F19/F20) |
| **KPIs** | DORA, calidad, costo (F17 §22, F21 §20) |
| **Business Metrics** | Proyectos, consultas, países (F15 §20) |
| **Platform Metrics** | `kin.*` de plataforma (F24) |
| **AI Metrics** | Consumo, latencia, costo, fallback (F12B §9) |

---

## 10. Distributed Tracing

- **OpenTelemetry** como estándar (F17 §11).
- **Correlación extremo a extremo** con identificadores oficiales consolidados:

| ID | Propósito |
|---|---|
| `queryId` / `correlationId` | Consulta/turno extremo a extremo |
| `projectId` / `userId` | Contexto de usuario (F17 §12) |
| `conversationId` | Turno de conversación |
| `providerId` | Provider de datos/IA (F12A) |
| `regionId` | Región (F20 §10) |
| `tenantId` | Contexto de plan/tenant |

> **Nota editorial (E1, conforme a la Pre-Auditoría FASE 29)**: `conversationId` y `tenantId` son **identificadores de correlación consolidados documentalmente por la propia FASE 29**. No estaban documentados explícitamente en F12–F28, **no modifican ninguna decisión previamente aprobada**, **no reemplazan ningún identificador existente** y se incorporan únicamente como **consolidación documental de observabilidad**.

---

## 11. Business Observability

- Métricas de negocio: proyectos, consultas, países, señales, documentos (F15 §20).
- Dashboards de negocio: Proyecto, BIL/Mercado, Enterprise, Analytics, Marketplace (F15 §15).
- KPIs funcionales y conversión (F15 §21).

---

## 12. Platform Observability

- Observabilidad de la plataforma (F24): CI/CD, GitOps, entornos, Service Catalog.
- Métricas de despliegue (DORA), recursos por región, costos (`CostEngine`).
- SLOs por servicio y scorecard (F12B §10).

---

## 13. AI Observability

Documenta únicamente: **consumo, latencia, providers, presupuesto, fallback y routing** de los modelos — **nunca decisiones IA**.

| Métrica | Política |
|---|---|
| Consumo | Tokens/turno, costo (`AiCostPolicy`, F12B §7) |
| Latencia | p95 por modelo/región |
| Providers | `providerId`, disponibilidad (`AIToolRegistry`) |
| Presupuesto | `CostEngine`/`PlanAccessPort` |
| Fallback | `ResponseFallback` y fallos registrados |
| Routing | `ModelSelected` registrado (`DecisionRecord`) |

---

## 14. Security Observability

Consolidación exclusiva de F18: `bil.security.*`, alertas de seguridad, threat detection, audit logs, trazado con `regionId` y detección de incidentes — sin modificar políticas.

---

## 15. Audit Observability

- **`DecisionRecord`**: append-only de decisiones (F12A/F12B).
- **`DataAccessAudited`**: accesos a datos registrados (F18).
- **Audit Events**: catálogo en `EventCatalog`; trazables por `queryId`/`tenantId`.

---

## 16. Diagnostic Architecture

- **Runbooks** por componente (F17 §13) + trazas reutilizables.
- Diagnóstico reproducible: reproducir estado con dataset/versión inmutables (F12B §5).
- Detección temprana: SLO breach + tendencias + alertas.

---

## 17. Alerting Architecture

- Alertas **P0–P3** con severidad, runbook y escalamiento (F17 §13).
- Reglas de alerta versionadas; umbrales por SLO/error budget.
- Sin alertas ruidosas: gestión de alertas y priorización (F28 §14).

---

## 18. Dashboard Governance

- Dashboards por servicio, región y negocio con owner (F24 §11).
- Versionado de dashboards; datos con fuente/fecha/confianza (F15).
- Acceso por RBAC; dashboards críticos con SLO/error budget.

---

## 19. Observability Metrics

| Métrica | Objetivo |
|---|---|
| Cobertura de telemetría | 100 % de servicios |
| Correlación (trazas completas) | ≥ 95 % |
| Cache hit ratio | ≥ 90 % |
| Freshness | ≤ 70 % TTL |
| SLO breach detectado | en ≤ 1 min |
| MTTD (detección) | ≤ 5 min |
| MTTR (P0/P1) | ≤ 1 h |

> **Nota editorial (E2, conforme a la Pre-Auditoría FASE 29)**: **MTTD ≤ 5 minutos** y **Correlación extremo a extremo ≥ 95 %** son **métricas oficiales de observabilidad definidas por la propia FASE 29**. Estas métricas **no provienen de F12–F28**, **no reemplazan métricas existentes**, **no modifican decisiones anteriores** y únicamente formalizan objetivos documentales de observabilidad para esta fase. Ninguna otra métrica fue modificada.

---

## 20. Enterprise Observability KPIs

| KPI | Objetivo |
|---|---|
| SLO / Error budget | 99.5 % · sano |
| MTTD / MTTR | ≤ 5 min / ≤ 1 h |
| Correlación extremo a extremo | ≥ 95 % de operaciones |
| Dashboards gobernados | 100 % con owner |
| Alertas accionables | ≥ 80 % |
| Scorecard de observabilidad | ≥ 8.0 trimestral |

> **Nota editorial (E2, conforme a la Pre-Auditoría FASE 29)**: los objetivos **MTTD ≤ 5 minutos** y **Correlación extremo a extremo ≥ 95 %** son **métricas oficiales de observabilidad definidas por la propia FASE 29**. No provienen de F12–F28, **no reemplazan métricas existentes**, **no modifican decisiones anteriores** y únicamente formalizan objetivos documentales de observabilidad para esta fase. Ninguna otra métrica fue modificada.

---

## 21. Enterprise Checklists

| Área | Checklist |
|---|---|
| **Telemetría** | ¿logs/métricas/trazas en todo servicio? |
| **Correlación** | ¿`queryId`/`correlationId`/`regionId`/`tenantId`? |
| **Métricas** | ¿SLIs/SLOs/KPIs definidos? · ¿AI metrics? |
| **Alertas** | ¿P0–P3 con runbook? · ¿sin ruido? |
| **Dashboards** | ¿owner? · ¿versionado? |
| **Auditoría** | ¿`DecisionRecord`? · ¿`DataAccessAudited`? |
| **Filosofía** | ¿Java decide, IA comunica verificado? |

---

## 22. Enterprise Observability Roadmap

```
F12 ── F13 ── F14 ── F15 ── F16 ── F17 ── F18 ── F19 ── F20 ── F21 ── F22 ── F23 ── F24 ── F25 ── F26 ── F27 ── F28 ── F29
Fund. · BIL · MIP  · Playbook · Producto · QA · DevOps/OTel+Micrometer · Seguridad · Enterprise · Multi-región · Excelencia · Datos · Integración · Plataforma · Gobernanza · Calidad · Resiliencia · Operaciones · **Observabilidad**
```

---

## 23. Riesgos

| Riesgo | Naturaleza | Mitigación |
|---|---|---|
| Telemetría incompleta | Operativo | Cobertura 100 % + gates |
| Trazas sin correlación | Operativo | Identificadores propagados |
| Alertas ruidosas | Operativo | Gestión de alertas (F28) |
| Datos de telemetría con PII | Seguridad | Redacción + residencia (F18) |
| Dashboards sin owner | Operativo | Dashboard Governance |

---

## 24. Compatibilidad

F12/F12A (DAG, BIL) · MIP (gates, DoR/DoD) · F12B (KPIs, scorecard, métricas `kin.*`) · F12C (gobernanza, alertas) · F13 (roadmap) · F14 (playbook) · F15 (dashboards de negocio) · F16 (QA) · F17 (DevOps: OTel, Micrometer, logs, SLOs) · F18 (seguridad/auditoría) · F19 (Enterprise) · F20 (escala global: `regionId`) · F21 (excelencia operacional) · F22 (datos) · F23 (integración) · F24 (plataforma) · F25 (gobernanza) · F26 (calidad) · F27 (resiliencia) · F28 (operaciones).

---

## 25. Referencias Arquitectónicas

F12B (Constitución, §9 KPIs) · F17 (DevOps: OTel, Micrometer, logs, alertas, SLOs) · F18 (seguridad/auditoría) · F20 (escala global) · F21 (excelencia operacional) · F24 (plataforma) · F26 (calidad) · F27 (resiliencia) · F28 (operaciones).

---

## 26. Conclusiones

La **FASE 29** consolida la **Arquitectura Oficial de Observabilidad Empresarial de KIN** como una **capa documental**: telemetría, logging, métricas (SLI/SLO/KPI), tracing distribuido con correlación extremo a extremo, observabilidad de negocio/plataforma/IA/seguridad/auditoría, diagnóstico, alertas, dashboards y métricas — sin modificar el Core, el DAG, el AI Boundary ni ninguna decisión aprobada, y preservando íntegramente la filosofía **"Java decide, la IA comunica"**. La observabilidad **mide y diagnostica; nunca decide**.

---

*Especificación oficial de Arquitectura de Observabilidad Empresarial de KIN (FASE 29). Documento formalizado conforme a la pre-auditoría (E1–E2 aplicados). Compatible al 100 % con F12–F28.*

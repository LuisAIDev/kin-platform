# KIN_PLATFORM_ARCHITECTURE_SPECIFICATION.md — FASE 24

**Especificación Oficial de Arquitectura de Plataforma (Platform Architecture) de KIN.**
*Candidata para `kin-docs/`. Consolidación documental exclusiva de F12–F23 sin modificar ninguno de esos documentos. Compatible al 100 % con Hexagonal, DDD, Clean, CQRS, Event Driven, SemVer, Contract Versioning, Evolutionary Architecture, GitOps, DevSecOps, Zero Trust y Defense in Depth. Filosofía inalterable: **Java decide, la IA comunica**.*

---

## 1. Resumen Ejecutivo

La **FASE 24** define la **Arquitectura Oficial de Plataforma de KIN** para los próximos 10–15 años: cómo se construye, opera, gobierna y evoluciona la plataforma — como **política, guideline, estrategia, estándar, proceso, contrato documental, mecanismo, práctica o capa documental**, jamás como implementación. Consolida lo aprobado en F12–F23 y formaliza las prácticas de **Platform Engineering**: Internal Developer Platform, Golden Paths, Service Catalog, platform APIs y gobernanza de plataforma.

---

## 2. Objetivo

Definir el **contrato documental de la plataforma**: cómo los equipos (~100 ingenieros) construyen, despliegan, operan y evolucionan KIN de forma **self-service, reproducible y gobernada**, preservando el Core, el DAG, el AI Boundary y la filosofía **"Java decide, la IA comunica"**.

---

## 3. Alcance

**Incluye**: IDP, platform services, runtime, entornos, DevEx, service catalog, platform APIs, gobernanza, seguridad, reliability, observabilidad, automatización, provisioning, compliance, KPIs, checklists, roadmap y riesgos de plataforma.

**Excluye**: cualquier modificación de Core, DAG, AI Boundary, BIL, Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, Flyway, ConnectorCatalog, EventCatalog, PolicyEngine, FeatureRegistry, DecisionRecord, BusinessDataset, SmartRouter, QueryService, AiContextBuilder, ResponseGuard, ResponseFallback, SecurityGateway, Vault, CostEngine, PlanAccessPort, APIs, eventos, contratos, puertos, adapters, Aggregates, entidades, VOs, Domain/Application Services, casos de uso, Bounded Contexts, reglas de negocio o ADR.

---

## 4. Principios de Arquitectura de Plataforma

| # | Principio |
|---|---|
| P1 | **Self-Service por defecto**: los equipos avanzan sin colas manuales (IDP). |
| P2 | **Golden Paths**: un camino recomendado por tipo de cambio; los desvíos requieren justificación. |
| P3 | **Plataforma como producto**: el IDP es un producto con consumidores (los equipos). |
| P4 | **Automation first**: todo paso repetible es automatizado (GitOps, CI/CD). |
| P5 | **Runtime y entorno gobernados**: entornos promovidos por gates (F17 §3). |
| P6 | **Zero Trust en la plataforma**: acceso por identidad, mínimo privilegio, mTLS. |
| P7 | **Observabilidad nativa**: toda plataforma emite logs/métricas/trazas. |
| P8 | **Java decide, IA comunica**: la plataforma nunca delega decisiones de negocio a la IA. |

---

## 5. Platform Architecture Vision

```
Consumidores (equipos KIN) → Developer Portal → IDP (Golden Paths, Service Catalog, Templates)
      → Runtime (GitOps, entornos, entornos por región) → Observabilidad → Gobernanza
```

La plataforma es la **capa que hace de KIN un producto operable por ~100 ingenieros durante 10–15 años**, sin tocar el dominio.

---

## 6. Internal Developer Platform (IDP)

| Bloque | Definición documental |
|---|---|
| **Golden Paths** | Ruta recomendada por tipo de cambio (feature, connector, plugin, policy) |
| **Self-Service** | Scaffolding, entornos y despliegues bajo demanda |
| **Developer Portal (documental)** | Punto de acceso a templates, estándares, docs y runbooks |
| **Platform Templates** | Plantillas de módulos (F14 §4), conectores y políticas |
| **Platform Standards** | Estándares de código, testing, observabilidad y seguridad |

---

## 7. Platform Services

| Servicio | Propósito (documental) |
|---|---|
| CI/CD | Orquesta gates (F16 §11, F17 §5) |
| GitOps | Promoción y drift (F17 §9) |
| Feature Registry | Rollout/kill-switch (F12A) |
| Secretos | Vault (F18) |
| Observabilidad | OTel/Prometheus/Grafana/Loki (F17 §11) |
| Service Catalog | Registro de servicios y sus contratos |

---

## 8. Runtime Platform

| Capa | Componentes |
|---|---|
| Orquestación | Kubernetes multi-AZ → multi-región (F17 §8, F19/F20) |
| Red | Ingress + WAF + NetworkPolicies + mTLS |
| Datos | PostgreSQL, Redis, TimeSeries, Warehouse, Vault, Object Storage |
| Ejecución | Servicios stateless; read models por región |

---

## 9. Environment Strategy

`Local → Development → QA → Staging → Production → DR` (F17 §3), con residencia por región (F20) y promoción solo por gates. **Environment Governance**: paridad de config, drift detectado por GitOps, kill-switch por entorno.

---

## 10. Developer Experience (DevEx)

- Golden Paths + templates + portal documental (F14 §4).
- Feedback rápido: gates en el PR, previews de entorno, dashboards por equipo.
- Menos toil: automatización (F21 §22); documentación auto-generada.

---

## 11. Service Catalog

| Campo | Definición |
|---|---|
| Servicio | Nombre, BC, owner, contratos, puertos |
| Estado | Estable/Evolucionando/Experimental (gobernanza §1.7) |
| Métricas | SLOs, cobertura, dependencias |
| Acceso | Owner + reviewers (F12C §2) |

---

## 12. Platform APIs

- APIs de plataforma (provisioning, feature flags, observabilidad) **internas y documentales**.
- Versionadas con SemVer y CDCT (F12B §4, F16 §7).
- Nunca exponen secretos ni capacidades de administración sin RBAC.

---

## 13. Platform Governance

- Ownership por equipo/BC (F12C §2); aprobación por RFC/ADR.
- Scorecard trimestral (F12B §10) con dimensión de plataforma.
- Change Management (F12C §15) y escalamiento (F12C §14) aplicados a la plataforma.

---

## 14. Platform Security

Consolidación de F18 aplicada a la plataforma: Zero Trust, Vault (AES-GCM, rotación), mTLS, SBOM firmado, gates de seguridad (PR/Merge/Release/GA/LTS + Security Approval), residencia por región y auditoría (`DecisionRecord`, `DataAccessAudited`).

---

## 15. Platform Reliability

- SLO 99.5 % (F19/F20) por servicio y región; error budget (F21 §8).
- DR: RPO ≤ 15 min, RTO ≤ 1 h (F17 §15).
- Fail-open determinista; datasets inmutables como capa de recuperación (F12B §6).

---

## 16. Platform Observability

- Tracing OTel con `queryId`/`correlationId`/`regionId` (F20 §10).
- Logs estructurados (F17 §12); métricas por servicio (`kin.*`).
- DecisionRecord para decisiones de plataforma (auditoría).

---

## 17. Platform Automation

- CI/CD con gates (F16 §11); GitOps (F17 §9).
- Provisioning automatizado (§18); pruebas automatizadas (unit/contract/mutation/perf/chaos).
- Toil reduction (F21 §22): toda tarea repetible se automatiza.

---

## 18. Platform Provisioning

- **IaC**: infraestructura como código (F17 §2); Terraform/Helm.
- Self-service: entornos y servicios bajo demanda vía IDP.
- **Multi-tenant / Multi-region**: aislamiento por tenant y residencia por región (F20).

---

## 19. Platform Compliance

- GDPR y residencia por región (F18 §14).
- SBOM y firmas por release (F17 §7).
- Auditoría de acceso a datos y decisiones (`DecisionRecord`, `DataAccessAudited`).

---

## 20. Platform KPIs

| KPI | Objetivo |
|---|---|
| Deployment frequency | ≥ 3/sem |
| Lead time | ≤ 3 días |
| MTTR (P0/P1) | ≤ 1 h |
| Change Failure Rate | ≤ 15 % |
| Toil | ≤ 20 % (política F21) |
| Availability / Error budget | 99.5 % · presupuesto sano |
| Service Catalog cobertura | 100 % de servicios |
| Scorecard de plataforma | ≥ 8.0 trimestral |

---

## 21. Platform Checklists

| Área | Checklist |
|---|---|
| **IDP** | ¿Golden Path documentado? · ¿template? · ¿portal? |
| **Entornos** | ¿promoción por gates? · ¿paridad? · ¿residencia? |
| **Seguridad** | ¿Vault? · ¿mTLS? · ¿SBOM? · ¿gates? |
| **Reliability** | ¿SLOs? · ¿DR probado? · ¿rollback? |
| **Observabilidad** | ¿logs/métricas/trazas? · ¿DecisionRecord? |
| **Automatización** | ¿CI/CD? · ¿GitOps? · ¿provisioning self-service? |
| **Gobernanza** | ¿owner? · ¿RFC/ADR? · ¿scorecard? |

---

## 22. Platform Roadmap

| Fase | Madurez de plataforma |
|---|---|
| F13–F16 | CI/CD, entornos, gates QA |
| F17 | GitOps, SLOs, DR |
| F18 | Seguridad de plataforma |
| F19–F20 | Multi-región, residencia |
| F21–F23 | Excelencia operacional, datos, integración |
| **F24 (esta fase)** | **Platform Engineering**: IDP, Golden Paths, Service Catalog, DevEx |

---

## 23. Platform Risks

| Riesgo | Mitigación |
|---|---|
| Drift de entornos/regiones | GitOps + paridad |
| Puerta de acceso lenta (DevEx) | Self-service + golden paths |
| Costo de plataforma | Autoscaling + presupuesto por región |
| Multi-tenant mal aislado | Tenant isolation + RBAC + auditoría |
| Complejidad del IDP | Plataforma como producto + KPIs |

---

## 24. Compatibilidad con F12–F23

F12/F12A (DAG, BIL) · MIP (gates, incrementos) · F12B (KPIs, scorecard, AI Boundary) · F12C (gobernanza) · F13 (roadmap) · F14 (reglas aditivas) · F15 (producto) · F16 (QA) · F17 (DevOps, entornos) · F18 (seguridad) · F19 (Enterprise) · F20 (escala global) · F21 (excelencia operacional) · F22 (datos) · F23 (integración).

---

## 25. Referencias Arquitectónicas

- F17 (DevOps/Operations) · F18 (Security) · F19 (Enterprise Architecture) · F20 (Global Scale) · F21 (Operational Excellence) · F22 (Data Architecture) · F23 (Integration Architecture) · F12B §10 (Scorecard) · F12C (Governance).

---

## 26. Conclusiones

La **FASE 24** consolida la **Arquitectura Oficial de Plataforma de KIN**: IDP, Golden Paths, Service Catalog, plataforma APIs, gobernanza de runtime y entornos, seguridad, reliability, observabilidad, automatización, provisioning y compliance — como **política, guideline, estrategia, estándar, proceso, contrato documental, mecanismo, práctica o capa documental**, sin modificar el Core, el DAG, el AI Boundary ni ningún contrato aprobado. La plataforma sirve a los equipos para construir KIN durante 10–15 años preservando **"Java decide, la IA comunica"**.

---

*Especificación Oficial de Arquitectura de Plataforma de KIN (FASE 24) — candidata a `kin-docs/`. Sin archivos modificados ni commits; preparada para el proceso oficial de Pre-Auditoría → Formalización → Auditoría Final → Aprobación.*

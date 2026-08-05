# KIN_INTEGRATION_ARCHITECTURE_SPECIFICATION.md — FASE 23

**Especificación Oficial de Arquitectura de Integración Empresarial de KIN.**
*Candidata para `kin-docs/`. Consolidación documental exclusiva de F12–F22. Compatible al 100 % con Hexagonal, Clean, DDD, CQRS, Event Driven, SemVer, Contract Versioning, Evolutionary Architecture, GitOps, DevSecOps, Zero Trust y Defense in Depth. Filosofía inalterable: **Java decide, la IA comunica**.*

---

## 1. Resumen Ejecutivo

La **FASE 23** documenta la **integración empresarial completa de KIN**: APIs, eventos, terceros, patrones Enterprise Integration y seguridad/observabilidad/gobernanza de integración — como **política, guideline, estrategia, contrato, mecanismo, proceso, estándar o capa documental**, jamás como implementación. Consolida lo aprobado en F12–F22 sin modificar Core, DAG, AI Boundary, contratos, eventos, puertos, adapters, APIs ni ADR.

---

## 2. Objetivo

Definir el **contrato oficial de integración de KIN**: cómo se integra internamente (BCs, eventos, outbox), con terceros (ERP, CRM, pagos, gobierno, open data, open banking, cloud, geo, identity, email/SMS/push, storage, analytics, marketplace, SDK, plugins) y cómo se gobiernan las APIs — preservando **"Java decide, la IA comunica"**.

---

## 3. Alcance

**Incluye**: APIs, Event Driven, terceros, arquitectura/gobernanza/lifecycle de APIs, EIP, seguridad, observabilidad, gobernanza, KPIs, checklists y roadmap de integración.

**Excluye**: cualquier modificación de Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, Flyway, ConnectorCatalog, EventCatalog, PolicyEngine, FeatureRegistry, DecisionRecord, BusinessDataset, SmartRouter, QueryService, AiContextBuilder, ResponseGuard, ResponseFallback, SecurityGateway, Vault, contratos, eventos, puertos, adapters, APIs, Aggregates, Bounded Contexts, modelos, reglas o ADR.

---

## 4. Principios de Integración Empresarial

| # | Principio |
|---|---|
| I1 | **Ports & Adapters**: integración siempre tras puertos, nunca saltándolos. |
| I2 | **Aditividad**: nuevas integraciones por puerto nuevo, overload o flag. |
| I3 | **Event Driven para desacople**; síncrono solo donde aporta valor (gobernanza §1.8). |
| I4 | **Idempotencia y correlación** en toda integración. |
| I5 | **Zero Trust en cada canal** (mTLS, scopes, least privilege). |
| I6 | **Consumidores gobernados** (CDCT, OpenAPI, versionado). |
| I7 | **Java decide, IA comunica** (la integración nunca delega decisiones a la IA). |

---

## 5. Integration Architecture Vision

```
BCs (puertos) ←→ APIs (REST/SSE/Webhooks) · Eventos (EventCatalog/outbox→broker) · Terceros
             └→ Observabilidad (correlationId) · Seguridad (mTLS/Vault) · Gobernanza (CDCT)
```

Integración interna por puertos/eventos; externa por API Gateway + vault; siempre con trazabilidad.

---

## 6. Modelo de Integración de KIN

| Tipo | Canal | Ejemplo |
|---|---|---|
| Interna BC→BC | Puertos + eventos | `BilContextProvider`→Conversación (MIP inc 21) |
| Interna app→app | REST/SSE internos | BIL REST (`BilApiPort`) |
| Externa entrante | REST/SSE públicos | `/api/v1/bil/*` |
| Externa saliente | APIs de providers vía vault | Connectors, geo, FX |
| Asíncrona | Eventos + outbox | Analytics, cache warming, salud |

---

## 7. Integración por APIs

| Tipo | Uso en KIN |
|---|---|
| REST | APIs públicas/internas (recursos versionados) |
| SSE | Streaming de conversación y dashboards (existente) |
| Webhooks | Notificaciones de providers/terceros (documental) |
| gRPC | **Patrón futuro documental (no implementado actualmente)**, intra-cluster |
| Streaming | Ingestion/FX en vivo (Reactor) |
| Async APIs | Contratos de eventos (`EventCatalog`) |

---

## 8. Integración Event Driven

| Mecanismo | Política documental |
|---|---|
| **EventCatalog** | Registro versionado de eventos (F12A §7, F12B §4) |
| **Outbox** | Persistencia transaccional de eventos → broker a escala (F17 §8) |
| **Inbox** | Consumidores idempotentes por `eventId` |
| **Replay** | Eventos append-only; snapshots en series largas |
| **Idempotencia** | Consumidores idempotentes; CDCT por evento |
| **Correlation IDs** | `queryId`/`correlationId` propagado (F17 §12) |
| **Event Versioning** | Campos aditivos MINOR; deprecación 2 fases (F12B §4) |
| **Event Routing** | Por tipo/versión; ownership del productor |
| **Dead Letter Queue** | Documental: eventos no procesables a DLQ con revisión y replay |

---

## 9. Integración con Terceros

| Tercero | Mecanismo (documental) |
|---|---|
| ERP / CRM | APIs con CDCT; ACL (Anti-Corruption Layer) |
| Pagos | Webhooks idempotentes; seguridad de firma |
| Gobierno / Open Data | `SourceLevel` 1/2 (F12A §8) |
| Open Banking | API Keys/OAuth; scopes; residencia |
| Cloud / Geo Services | GeoService/connectors vía `SecurityGateway` |
| Identity Providers | OIDC: **patrón futuro documental (no implementado actualmente)** |
| Email / SMS / Push | Puertos de notificación (política) |
| Storage | Object Storage (documentos Enterprise) |
| Analytics | Eventos (`EventCatalog`) — asíncrono |
| Marketplace / SDK / Plugins | `PluginLoadPort`, SPI, vetting (F12A §13) |

---

## 10. Arquitectura de APIs

- Recursos versionados (`/api/v1/...`); ownership en cada endpoint.
- Errores con Problem Details (RFC 7807) (F14 §10).
- Paginación con cursor en read models; filtering/sorting con whitelist.
- Contrato expuesto en OpenAPI y validado por CDCT.

---

## 11. API Governance

- Owners por API; RFC para nuevas APIs; revisión del Board.
- Compatibilidad: SemVer; ruptura MAJOR solo con deprecación (F12B §4).
- Rate limits y quotas por plan vía `CostEngine`/`PlanAccessPort`.
- Registro de APIs en el catálogo con metadata (owner, versión, SLA).

---

## 12. API Lifecycle

`Diseño (RFC) → Contrato (OpenAPI/CDCT) → Implementación (puerto+adapter) → PR/Gates → QA → Release → Operación (SLO) → Deprecación (2 fases) → Retiro (MAJOR)`.

---

## 13. Versionado y Backward Compatibility

- **SemVer** en contratos; campos aditivos MINOR; sin ruptura sin deprecación (F12B §4).
- **Matrix de compatibilidad** por API/evento; CDCT entre producer/consumer.
- Consumidores antiguos siguen leyendo versiones nuevas (compatibilidad F12B §4).

---

## 14. Consumer Driven Contracts y OpenAPI

- **CDCT** obligatorio entre BCs y en SDK/Marketplace (F16 §7).
- **OpenAPI** como fuente del contrato; validación en CI.
- Contract tests por conector y por evento (schema registry).

---

## 15. Rate Limits, Quotas y API Keys

- **Rate limits** por usuario/plan/IP; límites de `CostEngine` y cuotas de providers.
- **Quotas** por plan (`PlanAccessPort`).
- **API Keys** (terceros/marketplace) gestionadas y rotables; jamás en código.

---

## 16. Autenticación y Autorización de APIs

- **JWT** (`JwtService`/`JwtAuthenticationFilter`) para la plataforma (F18 §4).
- **OAuth / OIDC**: **patrón futuro documental (no implementado actualmente)** para integración externa e Identity Providers.
- **Scopes** por recurso; RBAC por rol (FREE/PREMIUM/FACILITADOR/ADMIN); ownership en cada endpoint.

---

## 17. Enterprise Integration Patterns

| Patrón | Uso documental |
|---|---|
| Adapter | Adaptadores tras puertos (`ai.*.adapter`) |
| Anti-Corruption Layer | Traducción de terceros (ERP/CRM/Open Banking) |
| Facade | `BusinessIntelligenceService`, `ConnectorOrchestrator` (fachada fina) |
| Gateway | API Gateway / Ingress para APIs públicas |
| Proxy | Capa de traducción/protección en salida |
| Broker | Eventos a escala (F17 §8) |
| Publisher/Subscriber | `DomainEventBus`/outbox→broker |

---

## 18. Seguridad de Integración

Consolidación de F18 aplicada a integración: mTLS interno, API Gateway con WAF, Vault para secrets de terceros, scopes y least privilege, Zero Trust por canal, `DecisionRecord`/`DataAccessAudited`, SBOM y gates de seguridad.

---

## 19. Observabilidad de Integración

- **Tracing** OTel con `correlationId`/`queryId`/`regionId` extremo a extremo.
- **Metrics** por API/evento/tercero: latencia, error rate, retry/replay.
- **Logs** estructurados (F17 §12).
- **Audit**: `DecisionRecord` + `DataAccessAudited` en integraciones.

---

## 20. Gobernanza de Integración

Owners por API/evento (F12C §2) · Approval por RFC/ADR · Review Board para contratos cross-BC · Scorecard trimestral (F12B §10) con dimensión de integración.

---

## 21. KPIs de Integración

| KPI | Objetivo |
|---|---|
| API Availability | 99.5 % |
| API Latency (p95) | < 500 ms (F17–F18) → < 300 ms (F19/F20) |
| API Error Rate | dentro de error budget |
| Integration Success Rate | ≥ 99 % |
| Retry / Replay Rate | ≤ 1 % |
| Event Freshness | ≤ 70 % TTL |
| Cobertura de eventos (EventCatalog) | 100 % de eventos documentados |
| CDCT coverage | 100 % de contratos |

---

## 22. Checklists

| Área | Checklist |
|---|---|
| **Arquitectura** | ¿puerto respetado? · ¿aditivo? · ¿sin ciclos? |
| **Integración** | ¿CDCT? · ¿idempotencia? · ¿correlación? · ¿matrix de compatibilidad? |
| **Eventos** | ¿EventCatalog? · ¿versionado? · ¿replay? · ¿DLQ documentado? |
| **Seguridad** | ¿mTLS? · ¿scopes/least privilege? · ¿Vault? · ¿sin secrets? |
| **Gobernanza** | ¿owner? · ¿RFC/ADR si aplica? · ¿scorecard? |
| **QA** | ¿contract tests? · ¿integration tests? · ¿chaos? |
| **Release** | ¿SLOs? · ¿rollback? · ¿flags? |

---

## 23. Roadmap

| Fase | Madurez de integración |
|---|---|
| F12–F15 | Puertos, eventos, conectores, BIL REST |
| F16–F17 | CDCT, outbox, observabilidad, GitOps |
| F18 | Seguridad de integración (Vault, gates) |
| F19–F20 | Multi-región, residencia, integración global |
| F21–F22 | Excelencia operacional y datos |
| **F23 (esta fase)** | **Integración empresarial consolidada**: APIs, eventos, terceros, EIP, gobernanza |

---

## 24. Compatibilidad

F12/F12A (puertos, DAG) · MIP (gates, incrementos) · F12B (eventos, cache, AI Boundary) · F12C (gobernanza) · F13 (roadmap) · F14 (reglas aditivas) · F15 (producto) · F16 (QA/contract) · F17 (DevOps, outbox) · F18 (seguridad) · F19 (Enterprise) · F20 (escala global) · F21 (excelencia operacional) · F22 (arquitectura de datos).

---

## 25. Riesgos

| Riesgo | Naturaleza | Mitigación |
|---|---|---|
| Ruptura de contrato con tercero | Integración | CDCT + versionado + fallback |
| Eventos no idempotentes | Operativo | Inbox + replay + DLQ |
| Fuga de credenciales de terceros | Seguridad | Vault + rotación + mTLS |
| Acoplamiento a proveedores externos | Arquitectura | Puertos + adaptadores |
| Latencia agregada de integraciones | Performance | Timeout/circuit breaker por integración |

---

## 26. Conclusiones

La **FASE 23** consolida la **Arquitectura Oficial de Integración Empresarial de KIN**: APIs, eventos (outbox/inbox/replay/idempotencia/correlación), terceros, patrones EIP y gobernanza — como **política, guideline, estrategia, contrato, mecanismo, proceso, estándar o capa documental**, sin modificar el Core, el DAG, el AI Boundary ni ningún contrato aprobado. Toda integración respeta ports & adapters, CDCT, Zero Trust y la filosofía **"Java decide, la IA comunica"**. Queda lista para incorporarse a `kin-docs/KIN_INTEGRATION_ARCHITECTURE_SPECIFICATION.md`.

---

*Especificación Oficial de Arquitectura de Integración Empresarial de KIN (FASE 23). Documento formalizado conforme a la pre-auditoría (E1–E2 aplicados). Compatible al 100 % con F12–F22.*

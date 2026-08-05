# KIN_ENTERPRISE_ARCHITECTURE_SPECIFICATION.md — FASE 19

**Especificación Oficial de Arquitectura Empresarial (Enterprise Architecture) de KIN.**
*Contrato oficial entre CTO, Architecture Board, Enterprise/Solution Architects, Backend, Frontend, DevOps, SRE, QA, Security, IA, Enterprise, Product y Marketplace. Consolida F12–F18 sin modificar ninguno de esos documentos. Compatible con Hexagonal, DDD, Clean, Event Driven, CQRS, SemVer, Contract Versioning, Evolutionary Architecture, GitOps y DevSecOps. Filosofía preservada: **Java decide, la IA comunica**.*

---

## 1. Enterprise Architecture Vision

KIN es el **Sistema Operativo Mundial para la Creación de Negocios**: una arquitectura empresarial en la que el **Core de decisión (Java)** permanece congelado, una **capa de inteligencia (BIL)** aporta el mundo real con evidencia (dato + fuente + fecha + confianza), y la **IA solo redacta**. La visión Enterprise es que la plataforma opere durante 10–15 años en 100+ países, 10M+ de usuarios, cientos de APIs y miles de providers **sin romper jamás** el Core, los contratos ni la filosofía fundacional.

**Ejes**: Decisión en Java · Datos materializados con confianza · IA gobernada · Evolución aditiva · Operación mundial (GitOps, SLOs, residencia).

---

## 2. Principios Arquitectónicos

| # | Principio | Fuente |
|---|---|---|
| EA1 | **Java decide, IA comunica** | Invariante (F12B §7) |
| EA2 | **Dominio puro** (Hexagonal/DDD/Clean) | Gobernanza §1.1–1.6 |
| EA3 | **DAG sin ciclos** | F12A §2 |
| EA4 | **Materialización antes que consulta en vivo** (CQRS-lite) | F12A §5 |
| EA5 | **Confianza única y determinista** | F12A §6/§11 |
| EA6 | **Evolución aditiva** (puertos, overloads, flags) | MIP §24 |
| EA7 | **SemVer + Contract Versioning** | F12B §4 |
| EA8 | **GitOps + DevSecOps + Observabilidad** | F17, F18 |
| EA9 | **Seguridad como atributo** (Zero Trust, Defense in Depth) | F18 §1–2 |
| EA10 | **Extensibilidad** (Catalog unificado, SDK, Marketplace) | F12A §12–13 |

---

## 3. Business Architecture

| Capability de negocio | Propósito | BC | Criticidad |
|---|---|---|---|
| Conversation | El usuario habla con KIN | Core (frozen) | Crítica |
| Decision Intelligence | Viabilidad en Java | Core (frozen) | Crítica |
| Knowledge | Hechos validados | `kin.knowledge` | Crítica |
| Business Intelligence | El mundo por país | `BilAcquisition`+`BilIntelligence` | Crítica |
| Semantic & Knowledge Core | Comparabilidad | `BilIntelligence` | **Cimientos** |
| Market / Experience Intelligence | Señales con confianza | `BilIntelligence` | Alta |
| Analytics | Operar | `BilGovernance` | Media |
| Enterprise | Documentos/dashboards | `kin.enterprise` (frozen) | Alta |
| AI | Redactar | AI Layer | Crítica |
| Governance / Security | Decidir/proteger | `BilGovernance` | Crítica |
| Extensibility (Marketplace/SDK) | Extender | `BilExtensibility` | Media |

---

## 4. Capability Map (F12B §1, consolidado)

```
KIN ── OS de Creación de Negocios
├── Conversation · Decision Intelligence · Knowledge
├── Business Intelligence · Semantic Core · Knowledge Graph
├── Market Intelligence · Experience Intelligence · Analytics
├── Enterprise · AI · Governance · Security · Extensibility
```

Cada capability se mapea a: propósito · BC · owner · consumidores · dependencias · criticidad · roadmap (F13) — matriz completa en F12B §2. Este capítulo la consolida como **arquitectura de negocio** (qué se entrega, a quién, con qué criticidad).

---

## 5. Value Streams

| Value Stream | Flujo | Capabilities (F12B §1) | Dueño |
|---|---|---|---|
| **Validar una idea** | Idea → Entrevista → BIL (mercado) → Viabilidad | Conversation, Decision, BIL, Market | Product |
| **Consultar el mundo** | Pregunta → CountryResolver → Dataset → AiContext → Respuesta | BIL, Semantic, AI, Conversation | Delivery |
| **Crear la empresa** | Viabilidad → Planes → Documentos → DOFA | Decision, Enterprise, BIL | Enterprise |
| **Hacer crecer** | Seguimiento → Señales → Expansión multi-país | Experience, Market, BIL, Analytics | Product |
| **Extender la plataforma** | SDK → Vetting → Marketplace → Datos | Extensibility, Governance, Security | Marketplace |

---

## 6. Organization Architecture

Distribución ~100 ingenieros (F12C §4, F13 §5):

| Equipo | Tamaño | Owner | BC primario |
|---|---|---|---|
| Core & Governance | 12 | Core Owner | Core (frozen) |
| Foundations | 10 | Foundations TL | Semantic, Provider, Confidence, Policies |
| Acquisition | 14 | Acquisition Owner | `BilAcquisition` |
| Data & Materialization | 14 | Data TL | Ingesta, Series, Fusion, Datasets |
| Intelligence | 12 | Intelligence TL | Graph, Market, Experience |
| Delivery & AI | 12 | Delivery TL | Query, AiContext, AI Registry |
| Enterprise & Product | 10 | Product TL | Enterprise feed, Frontend |
| Extensibility | 8 | Extensibility TL | SDK, Marketplace |
| SRE/Platform | 8 | SRE TL | Security, Observabilidad, Releases |
| QA & Architecture | 6 | QA Lead | Gates, contract, mutation |

---

## 7. Domain Architecture

DAG oficial (F12A §1/§2), consolidado como arquitectura de dominio:

```
L0 SemanticCore · ProviderEntity · ConfidenceUnified · PolicyEngine · EventCatalog · FeatureRegistry · DecisionRecord
L1 VersionManager · SecurityGateway · GeoService · FxUnits · ConnectorCatalog
L2 HealthMonitor · CostEngine · ConnectorImplementations · ScraperPolicy
L3 Ingestion/ETL → TimeSeries
L4 DataFusion → Materialization (BusinessDataset)
L5 KnowledgeGraph · SmartRouter · QueryService
L6 MarketIntelligence · ExperienceIntelligence · AiContextBuilder · Analytics
L7 ConversationIntegration · EnterpriseFeed · AIToolRegistry · AIGovernance
L8 PluginSDK · Marketplace · MultiRegion
L9 MarketTwin
```

**Invariantes**: Delivery→Intelligence→Acquisition (unidireccional) · dominio puro · sin ciclos (ArchUnit).

---

## 8. Bounded Context Architecture

| BC | Estado | Puertos clave |
|---|---|---|
| Core (Conversation, Pipeline, Engines, Reporting) | **CONGELADO** | `PipelineStage`, `AIResponder`, `DomainEventBus` |
| `kin.knowledge` | Estable | `KnowledgeSource`, `KnowledgeRepository` |
| `kin.interview` | Estable | `InterviewRepository` |
| `kin.enterprise` | CONGELADO | `EnterpriseProjectTrigger`, `EnterprisePipelineResultStore` |
| `BilAcquisition` | Nuevo | `ConnectorPort`, `SecretStore`, `HealthProbePort`, `PlanAccessPort` |
| `BilIntelligence` | Nuevo | `MetricQueryPort`, `FactStorePort`, `DatasetReadPort`, `GraphIndexPort`, `SignalQueryPort` |
| `BilDelivery` | Nuevo | `BilApiPort`, `BilContextProvider` |
| `BilGovernance` | Nuevo | `PolicyEvaluatePort`, `VaultPort`, `UsagePort`, `AuditPort` |
| `BilExtensibility` | Nuevo | `PluginLoadPort` |

**Comunicación entre BC**: solo por puertos y eventos (`EventCatalog`, F12A §7); nunca dependencia directa.

> **Nota editorial (H2, conforme a la auditoría FASE 19)**: el componente `CostEngine` consolida la decisión **vigente de F13 (incremento 11 → BilAcquisition)**, que es la que esta Arquitectura Empresarial adopta. Existe una diferencia documental histórica con F12A §4.4 (que lo ubicaba en `BilGovernance`); dicha diferencia es únicamente documental, no arquitectónica, y queda resuelta por la decisión de F13. No se mueve ningún componente ni se modifica el DAG.

---

## 9. Information Architecture

| Entidad de información | Origen | Consumidores | Integridad |
|---|---|---|---|
| `ProjectContext` | Conversación (durable) | Pipeline, Decision | Persistido/versionado |
| `Statistic` / `Measure` | Ingesta + SemanticCore | Fusion, Dataset | Inmutable, con `Provenance` |
| `BusinessDataset` | Materialización | Query, Graph, Market, Experience | **Inmutable, versionado, ID determinista** |
| `MarketSignal` / `ExperienceSignal` | Motores de señal | AiContext, Recommendation (aditivo) | Con confianza, vigencia |
| `AiContext` | AiContextBuilder | Conversación (aditivo) | Redactado, PII-gobernado |
| `DecisionRecord` | Toda decisión | Auditoría | Append-only |
| `ConsultingReport` / `EnterpriseProject` | Reporting/Enterprise | Usuario | Versionado |

**Trust boundaries** (F18 §16): Internet → Ingress → App → Datos; Vault; LLM (solo redacta); Marketplace (firmado).

---

## 10. Data Architecture

| Store | Contenido | Carácter |
|---|---|---|
| PostgreSQL | Operativo: proyectos, users, providers, contratos, plugins, policies, audit, datasets (meta) | Mutable + metadatos versionados |
| Redis | Cache L2, salud derivada, budget, flags | Efímero (nunca PII/AiContext) |
| TimeSeries/Fact Store | Hechos, series de mercado/experiencia | **Append-only inmutable** |
| Warehouse | Agregados analíticos (star schema) | Materializado por periodo |
| Vault | Credenciales cifradas (AES-GCM) | Mutable (rotación) |
| Object Storage | Documentos Enterprise, datasets grandes | Cifrado en reposo |

**Reglas**: inmutabilidad de hechos/datasets/decisiones · content addressing (ID determinista) · residencia por región (F19) · versionado con SemVer.

---

## 11. Integration Architecture

- **Patrón dominante**: Ports & Adapters + Event Driven (outbox → broker a escala; puerto `DomainEventBus` intacto).
- **Interfaces de BC**: solo puertos (nunca clases concretas cross-BC).
- **Integraciones aditivas al Core**: `BilContextProvider` → PromptAssembler (frontera ADR-012/013) · `EnterpriseFeed` → Enterprise (puerto) · `ProviderRouter` → `AIToolRegistry` (evolución).
- **Contract testing**: CDCT entre productores/consumidores (F16 §7).
- **ID determinista**: `DeterministicId` para datasets (idempotencia, cache distribuida).

---

## 12. Application Architecture

| Application Service | BC | Responsabilidad |
|---|---|---|
| `AcquisitionService` | BilAcquisition | Orquesta adquisición (nunca en el request path) |
| `IngestionService` | BilIntelligence | ETL programado → hechos |
| `MaterializationService` | BilIntelligence | Datasets inmutables |
| `QueryService` | BilDelivery | Lee read models (request path) |
| `MeteringService` / `Auditor` | BilGovernance | Uso y auditoría |
| `PluginLoader` / `PluginVerifier` | BilExtensibility | SDK/Marketplace |
| `AiEngineService` (existente) | AI | Implementa `AIResponder` (frontera intacta) |

> **Nota editorial (H1, conforme a la auditoría FASE 19)**: `BilContextProvider` **no es un Application Service**. Es el **puerto oficial de BilDelivery** (F12A §5, MIP incremento 20), a través del cual la conversación consume el `AiContext` de forma aditiva. Clasificación documental corregida; sin cambio de interfaces ni de comportamiento.

---

## 13. Technology Architecture

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.2 · Maven · Reactor |
| Frontend | Next.js · TypeScript strict · Tailwind |
| Datos | PostgreSQL + Flyway · Redis · (TimeSeries/Warehouse futuro) |
| Cache | Caffeine (L1) · Redis (L2) |
| IA | DeepSeek hoy · `AIToolRegistry` futuro (OpenAI, Claude, Gemini, Llama…) |
| Observabilidad | Micrometer · OpenTelemetry · Prometheus/Grafana · Loki |
| Seguridad | `common.security` (JWT) · Vault · cosign/SBOM |
| Infra | Docker/K8s · GitOps (ArgoCD/Flux) |

---

## 14. Infrastructure Landscape

Entornos (F17 §3): Local → Development → QA → Staging → Production → DR. Objetivo: K8s multi-AZ → multi-región (F19) con residencia por región, CDN, Ingress+WAF, NetworkPolicies, mTLS, Vault, backups PITR y DR con RPO ≤ 15 min / RTO ≤ 1 h.

---

## 15. Enterprise Integration

| Integración | Mecanismo | Fuente |
|---|---|---|
| Conversación ← BIL | `BilContextProvider` (aditivo) | MIP inc 21 |
| Enterprise ← BIL | `EnterpriseFeed` por puerto | MIP inc 22 |
| Recommendation ← Señales | `SignalQueryPort` (aditivo) | F12A §8 |
| Knowledge ↔ Confidence | Confianza única unificada | MIP inc 3 |
| ProviderRouter → AIToolRegistry | Evolución aditiva (puerto `AIProvider` intacto) | F12A §10 |
| Analytics ← Eventos | `EventCatalog` (asíncrono) | F12A §7 |

---

## 16. Decision Architecture

**Java decide todo; la IA solo comunica:**

| Decisión | Componente | Registro |
|---|---|---|
| Directiva de turno | `TurnPolicy` (Core) | — |
| Viabilidad / Scoring | Pipeline 13 / `ScoringEngine` | explicación |
| Selección de modelo | `AIToolRegistry` / `ModelSelectionPolicy` | `DecisionRecord` |
| Confianza del dato | `ConfidenceEngine` | `DecisionRecord` |
| Contenido del AiContext | `AiContextBuilder` + políticas | `DecisionRecord` |
| Validez de la respuesta | `ResponseGuard` | issues |
| Respuesta segura | `ResponseFallback` | — |
| Ruteo de providers | `SmartRouter` + `PolicyEngine` | `DecisionRecord` |

---

## 17. AI Architecture

- **AI Boundary** (F18 §8): la IA solo redacta; `AiContext` es el único insumo (frontera ADR-012).
- **Pipeline IA**: AiContextBuilder → políticas (`PiiPolicy`→`RedactionPolicy`→`ContextBudgetPolicy`→`ModelSelectionPolicy`) → PromptAssembler → LLM → ResponseGuard → ResponseFallback.
- **AIToolRegistry**: Java selecciona modelo por capacidad/costo/salud; `ModelSelected` registrado.
- **Gobernanza IA**: versión de prompt, costo por turno, auditoría de redacción.

---

## 18. Security Architecture

Consolidación de F18:
- Zero Trust · Least Privilege · Default Deny · Defense in Depth · Deterministic Security.
- Identity: roles FREE/PREMIUM/FACILITADOR/ADMIN · JWT (`JwtService`) · ownership · `PlanAccessPort`.
- Secrets: Vault AES-GCM · rotación · mTLS · firma de artefactos/plugins · SBOM.
- Data: PII gobernada · datasets inmutables · residencia · cifrado en reposo/tránsito.
- Gates de seguridad: PR/Merge/Release/GA/LTS + Security Approval.

---

## 19. DevOps Architecture

Consolidación de F17:
- GitOps (ArgoCD/Flux), infraestructura inmutable, artefactos firmados con SBOM.
- CI/CD: lint→compile→unit→archunit→contract→mutation→security→build→package→SBOM→publish→staging→acceptance→canary→production.
- Entornos promovidos por gates; Feature Flags (`FeatureRegistry`) para rollout/kill-switch.
- Observabilidad: OTel, Prometheus/Grafana, logs estructurados con `queryId`/`projectId`/`userId`/`traceId`.
- SLO 99.5 % (F19), error budget, MTTR ≤ 1 h, DR con RPO/RTO definidos.

---

## 20. Governance

Consolidación de F12C:
- Ownership Matrix (Owners, Backups, Reviewers, Maintainers, Board, CTO).
- PR Rules, Branch Strategy, Code Review Constitution.
- ADR/RFC flow, Change Management, Escalation (Developer→TL→Owner→Board→CTO).
- Scorecard trimestral (F12B §10): salud arquitectónica ≥ 8.0.

---

## 21. Evolution Strategy

- **Aditividad obligatoria**: nuevas capacidades por puertos/overloads/flags; nunca reescritura del Core.
- **SemVer + Contract Versioning**: eventos/datasets/contratos versionados; deprecación 2 fases MINOR; retiro solo MAJOR.
- **Materialización creciente**: cada país/ciudad agrega datasets sin tocar el request path.
- **Extensión**: ConnectorCatalog unificado + SDK (SPI) + Marketplace; IA multi-modelo gobernada.
- **Invariantes a 15 años**: DAG sin ciclos · dominio puro · Java decide · IA comunica.

---

## 22. Enterprise KPIs

| KPI | Objetivo | Frecuencia |
|---|---|---|
| Architecture Health (scorecard) | ≥ 8.0 | Trimestral |
| ArchUnit / dependency violations | 0 | PR/Trimestral |
| Coverage dominio / mutation críticos | ≥ 90 % / ≥ 80 % | Trimestral |
| Perf p95 (read model) | < 500 ms | Semanal |
| Availability / Error Budget | 99.5 % (F19) | Mensual |
| Deployment frequency / Lead time | ≥ 3/sem · ≤ 3 días | Mensual |
| MTTR / Change Failure Rate | ≤ 1 h · ≤ 15 % | Mensual |
| Providers / Datasets / Señales | F13 §9 | Trimestral |
| Costo IA/turno · Cache hit | presupuesto · ≥ 85 % | Mensual |
| Cumplimiento (GDPR/residencia) | 0 violaciones | Mensual |

---

## 23. Enterprise Checklists

| Dominio | Checklist |
|---|---|
| **Arquitectura** | ¿DAG respetado? · ¿dominio puro? · ¿sin ciclos? |
| **Datos** | ¿datasets inmutables + versionados? · ¿PII gobernada? · ¿residencia? |
| **IA** | ¿AI Boundary? · ¿políticas antes de invocar? · ¿DecisionRecord? |
| **Seguridad** | ¿Vault? · ¿gates? · ¿sin secrets? · ¿SBOM firmado? |
| **DevOps** | ¿GitOps? · ¿SLOs? · ¿DR? · ¿rollback? |
| **Negocio** | ¿capacidad mapeada a BC/incremento? · ¿consumidor identificado? |
| **Enterprise** | ¿documentos con ownership? · ¿aislamiento de tenant? |

---

## 24. Long-Term Enterprise Strategy (15 años)

1. **Core inmortal**: Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, DocGen, Flyway y ADR permanecen congelados; la evolución es exclusivamente aditiva.
2. **El mundo se materializa**: de 1 país (Cartagena) a 100+ países; los datasets crecen sin tocar el request path.
3. **La confianza gobierna el dato**: fuente + fecha + confianza en cada afirmación; confianza única y determinista.
4. **La IA escala gobernada**: más modelos, misma regla — Java decide, IA comunica.
5. **La plataforma se extiende**: SDK + Marketplace con firma y vetting; terceros aportan sin tocar el núcleo.
6. **La operación madura**: GitOps, SLOs, residencia, multi-región y DR sostienen la escala mundial.
7. **La identidad persiste**: cada capa nueva es Defense in Depth del mismo sistema — nunca una modificación del original.

---

## Conclusión

Este documento **constituye la Arquitectura Empresarial Oficial de KIN**: consolida F12–F18 en un único contrato Enterprise (visión, principios, business/domain/information/data/integration/application/technology/decision/AI/security/devops architecture, gobernanza, evolución, KPIs y checklists) para los próximos 10–15 años. **No modifica el Core ni el AI Boundary**; respeta Hexagonal, DDD, Clean, Event Driven, CQRS, SemVer, Contract Versioning, Evolutionary Architecture, GitOps y DevSecOps; y preserva íntegra la filosofía **"Java decide, la IA comunica"**.

---

*Especificación oficial de Arquitectura Empresarial de KIN (FASE 19). Documento formalizado conforme a la auditoría (H1–H2 aplicados). Compatible al 100 % con F12–F18.*

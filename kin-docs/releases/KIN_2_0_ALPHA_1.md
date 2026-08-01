# KIN 2.0 Alpha 1

> **Release**: Primera release estable del núcleo inteligente de KIN
> **Fecha**: 31 de julio de 2026
> **Commit**: `89b39b9` (`feat(kin): implement Conversation Orchestrator (phase 5.6)`)
> **Branch**: `main`
> **Tag**: `v2.0.0-alpha1`
> **Estado**: `ALPHA STABLE` — Build reproducible, contratos congelados, arquitectura validada

---

## Resumen ejecutivo

KIN 2.0 Alpha 1 es la **primera release estable del núcleo inteligente** de la plataforma: un
pipeline de dominio de 10 etapas que recibe el contexto de un proyecto y produce un
`ConsultingReport` (10 secciones) con recomendaciones, riesgos, oportunidades y un score de
viabilidad, además de una conversación dirigida por **decisiones tomadas en Java** en las que
el LLM únicamente comunica.

Esta release cierra las fases **5.4 (ReportEngine)**, **5.5 (PromptAssembler)** y
**5.6 (Conversation Orchestrator)**, que completan el núcleo iniciado en el milestone
arquitectónico original (fases 4.0–5.3, tag `v2.0.0-alpha.1`). A partir de este punto el
núcleo es una **línea base estable** desde la cual se iniciará la siguiente etapa (Fase 6 —
KnowledgeEngine + RAG).

## Arquitectura implementada

- **Clean Architecture + DDD Táctico + Pipeline Pattern + Event-Driven**.
- Dominio puro `com.kinplatform.kin.*` (21 paquetes, 0 dependencias Spring/JPA).
- **Infraestructura de motores** (`kin.engine`): `DomainEngine`, `EngineRegistry`,
  `EngineExecutor`, `EngineStage` — contrato congelado.
- **Motores de dominio** canonizados bajo `DomainEngine`: `ScoringEngine`,
  `RecommendationEngine`, `RiskEngine`, `OpportunityEngine` y `ReportEngine`.
- **Runtime único**: `KinMethod.execute` (bloqueante) y `executeStream` (Flux) como punto de
  entrada único; `ChatOrchestratorServiceImpl` es I/O puro.
- **Contexto durable**: `ProjectContext` persistido vía `ContextRepository` (adaptador JPA,
  tabla `project_context`).
- **IA por puertos**: dominio depende de `AIResponder`/`PromptAssembler`, no de adaptadores;
  `AiEngineService` enruta a proveedores (DeepSeek/OpenAI/Ollama) con fallback en español.
- **Ciclo de conversación dirigido**: `ConversationOrchestrator` (dominio POJO) compone
  `HistoryWindow` + `TurnPolicy` + `KinMethod` + `ResponseGuard` + `ContextRepository`.

## Listado de ADR implementados

| # | ADR | Contenido |
|---|-----|-----------|
| **ADR-001** | Reporting BC | Bounded context de reporting |
| **ADR-002** | Pipeline context | `PipelineContext` como flujo de datos del pipeline |
| **ADR-003** | Recommendation engine | `RecommendationEngine` + `RecommendationResult`/`Stage` |
| **ADR-004** | Risk engine | `RiskEngine` + analizadores de riesgo (proceso, pricing, modelo de negocio) |
| **ADR-005** | Engine infrastructure | `kin/engine`: `DomainEngine`, `EngineRegistry`, `EngineExecutor`, `EngineStage` |
| **ADR-006** | Runtime consolidation | Pipeline único para `/chat` y `/chat/stream` (`KinMethod`) |
| **ADR-007** | Context repository | `ContextRepository` + `ProjectContext` durable (JPA) |
| **ADR-008** | AI responder / prompt assembler | Puerto `AIResponder` + `PromptAssembler` en `kin.ai` |
| **ADR-009** | Engine canonization | `ScoringEngine` canonizado bajo `DomainEngine` |
| **ADR-010** | Opportunity engine | `OpportunityEngine` + 8 analizadores auto-descubiertos |
| **ADR-011** | Report engine | `ReportEngine` — orquestador puro del `ConsultingReport` (10 secciones) |
| **ADR-012** | Prompt assembler | `PromptAssembler` fachada pura; frontera REPORT solo consume `ConsultingReport` |
| **ADR-013** | Conversation orchestrator | `kin.conversation`: orquestador, `TurnPolicy`, `ResponseGuard`, `HistoryWindow` |

## Fases completadas

| Fase | Contenido | Estado |
|------|-----------|--------|
| **5.4 — ReportEngine** | `ReportEngine` (ADR-011): orquestador puro del `ConsultingReport` (10 secciones), `ReportBuilder` (contrato estricto), `ReportMetadata`, 10 `SectionAssembler`, `ReportStage` (10.ª etapa) | Completada |
| **5.5 — PromptAssembler** | `PromptAssembler` fachada pura (ADR-012): `ConversationPromptBuilder` + `ReportPromptBuilder` + 10 `SectionFormatter`; `ConsultorStage` reposicionado tras `ReportStage`; frontera REPORT (solo `ConsultingReport`) | Completada |
| **5.6 — Conversation Orchestrator** | `ConversationOrchestrator` (ADR-013): directiva en Java pre-pipeline (`TurnPolicy`), `ResponseGuard` (guardrail), `HistoryWindow` (presupuesto de contexto), 7 tipos de turno; `/chat` y `/chat/stream` delegan en el orquestador | Completada (cerrada oficialmente) |

## Estado del Pipeline

Pipeline de 10 etapas (`KinMethod` → orden oficial):

| Etapa | Componente | Estado |
|-------|------------|--------|
| **Analyzer** | `AnalyzerStage` — extrae dimensiones del mensaje → `ProjectContext.update(...)` | Operativa |
| **Evaluator** | `EvaluatorStage` — `CompletenessEvaluator` → `CompletenessEvaluation` | Operativa |
| **Strategist** | `StrategistStage` — `ConversationStrategist` → `ConversationDecision` | Operativa |
| **Scoring** | `ScoringStage` → `ScoringEngine` → `ScoreResult` | Operativa |
| **Recommendation** | `RecommendationStage` → `RecommendationEngine` → `RecommendationResult` | Operativa |
| **Risk** | `RiskStage` → `RiskEngine` → `RiskResult` | Operativa |
| **Opportunity** | `OpportunityStage` → `OpportunityEngine` → `OpportunityResult` | Operativa |
| **ReportEngine** | `ReportStage` → `ReportEngine` → `ConsultingReport` (10 secciones) | Operativa |
| **Consultor** | `ConsultorStage` → `PromptAssembler` + `AIResponder` (bloqueante/streaming; `ResponseGuard` en streaming) | Operativa |
| **Events** | `EventStage` — publica eventos según decisión (ASK→Question, REPORT→Report+Score, siempre ConversationCompleted) | Operativa |

## Principio arquitectónico

> **Java decide. El LLM únicamente comunica.**

Toda decisión de negocio — fase, modo, restricciones, presupuesto de contexto, validación de la
comunicación, canonización de resultados — se toma en Java de forma determinista. El LLM recibe
un prompt ensamblado (nunca el contexto crudo) y produce únicamente texto de comunicación. Nunca
se parsean decisiones del texto del LLM.

## Métricas

| Métrica | Valor |
|---------|-------|
| **Número total de tests** | **468** (`./mvnw clean verify`, 0 fallos, 0 errores, 0 skipped) |
| **Cobertura JaCoCo** | `kin.conversation*` **100 %** (738/738); `kin.ai*` 99.7 %; `kin.ai.prompt` 99.7 %; `kin.ai.prompt.formatter` 99.9 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %; `kin.scoring` 95.1 % — requisito de dominio ≥ 90 % cumplido |
| **Número de paquetes** | **21** paquetes de dominio (`com.kinplatform.kin.*`) |
| **Número de ADR** | **13** (ADR-001 … ADR-013) |
| **Número de fases cerradas** | **3** (Fases 5.4, 5.5 y 5.6) — el núcleo acumula las fases 4.0–5.6 |

## Resumen del núcleo inteligente

### Qué hace Java

- Decide la **directiva del turno** (fase/modo/restricciones) desde la decisión previa
  persistida (`TurnPolicy`, ADR-013), antes del pipeline.
- Aplica el **presupuesto de contexto** (`HistoryWindow`) y valida la comunicación
  (`ResponseGuard`).
- Ejecuta los **motores de dominio** deterministas (scoring, recomendaciones, riesgos,
  oportunidades, reporte) y canoniza los resultados.
- Orquesta el **runtime único** (`KinMethod`), el pipeline y la persistencia del contexto.

### Qué hace el LLM

- Recibe únicamente el prompt ensamblado por `PromptAssembler`.
- Produce **texto de comunicación** (preguntas de exploración o explicación del reporte).
- Su respuesta se valida con `ResponseGuard` (vacío, longitud, pregunta única, marcadores
  prohibidos); con fallback en español si los proveedores fallan.

### Qué hace PromptAssembler

- Fachada pura (ADR-012): `assemble(PromptRequest) → String`, sin lógica, reglas ni fallback.
- Delegación por tipo: `ConversationPromptBuilder` (modo conversación, con sección
  `## DIRECTIVA DE COMUNICACIÓN` cuando hay directiva) o `ReportPromptBuilder` (las 10
  secciones del `ConsultingReport` formateadas por 10 `SectionFormatter` deterministas).
- Frontera ADR-012: en modo REPORT solo consume `ConsultingReport`; las fuentes crudas están
  prohibidas.

### Qué hace Conversation Orchestrator

- Fachada del ciclo de turno (ADR-013): `orchestrate(ConversationTurn) → TurnResult` y
  `orchestrateStream(ConversationTurn) → Flux<String>`.
- Compone `HistoryWindow` + `TurnPolicy` + `KinMethod` + `ResponseGuard` + `ContextRepository`;
  decide la directiva en Java y la propaga aditivamente al pipeline.
- `ChatOrchestratorServiceImpl` delega ambos endpoints (`/chat` y `/chat/stream`).

### Qué hace ReportEngine

- Orquestador puro del `ConsultingReport` (ADR-011): compone los 4 resultados del pipeline
  (scoring, recomendaciones, riesgos, oportunidades) en un VO inmutable de 10 secciones con ID
  determinista y `ReportMetadata`.
- Prioridad 70, fase REPORTING; `ReportStage` es la 10.ª etapa del pipeline.

## Estado del proyecto

| Criterio | Estado |
|----------|--------|
| **Alpha estable** | ✅ Núcleo inteligente completo y cerrado (fases 4.0–5.6) |
| **Build reproducible** | ✅ `./mvnw clean verify` → BUILD SUCCESS, 468 tests, 0 fallos |
| **Contratos congelados** | ✅ `kin/engine` + APIs estables de `BASELINE_ARCHITECTURE.md` (§4); cualquier cambio requiere ADR aprobada |
| **Arquitectura validada** | ✅ 13 ADRs aprobadas; dominio con cobertura ≥ 90 %; auditoría de cierre sin hallazgos críticos |

## Roadmap siguiente

- ✅ **Preparado para iniciar la Fase 6** — `KnowledgeEngine` + RAG consumiendo el
  `ProjectContext` durable y el turno tipado/directiva (`TurnResult`, `TurnDirective`) como
  punto de extensión, sin tocar los contratos estables.
- KIN 2.1: consumir `ResponseValidation` (fallback/reintento ante `accepted=false`) y la
  semántica completa de eventos.

---

*KIN 2.0 Alpha 1 — primera release estable del núcleo inteligente. Las APIs marcadas como
estables en `BASELINE_ARCHITECTURE.md` no deben modificarse sin una ADR aprobada.*

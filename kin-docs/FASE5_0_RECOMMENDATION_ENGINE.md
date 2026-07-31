# FASE 5.0 — RECOMMENDATION ENGINE (Implementación)

> Fecha: 2026-07-30 · Estado: **Completado** · Alcance: solo `RecommendationEngine` (Risk/Opportunity/Report en fases siguientes).

## 1. OBJETIVO

Generar recomendaciones accionables, tipadas y auditables sobre el proyecto, usando **solo** información producida por Java (contexto, evaluación de completitud, decisión, score). El motor es **determinista**, **reproducible**, **sin LLM**, **sin Spring** y **sin heurísticas aleatorias**.

## 2. COMPONENTES CREADOS

| Clase | Paquete | Tipo |
|---|---|---|
| `RecommendationCategory` | `kin.reporting` | Enum (VALIDATION, MARKETING, FINANCIAL, PRODUCT, STRATEGY, OPERATIONS, INNOVATION, TEAM) |
| `ImpactLevel` / `EffortLevel` | `kin.reporting` | Enums (LOW, MEDIUM, HIGH, CRITICAL) |
| `RecommendationExplanation` | `kin.reporting` | Record (usedInformation, appliedRule, reason) |
| `Recommendation` | `kin.reporting` | Record inmutable + factory `create(...)` con id determinista |
| `RecommendationResult` | `kin.reporting` | Record inmutable (recommendations, priority, confidence, category, explanation, generatedBy, engineVersion) |
| `RecommendationModel` | `kin.reporting` | Config de umbrales (low=40, high=70, minCoverage=0.6, v1) |
| `RecommendationInput` | `kin.reporting` | Record de entrada tipada |
| `RecommendationEngine` | `kin.reporting` | Domain Service puro |
| `RecommendationStage` | `kin.pipeline.stage` | Etapa del pipeline |

Modificados: `PipelineContext` (campo `recommendationResult`), `KinConfig` (beans + inserción de etapa).

## 3. DIAGRAMA DE CLASES (UML)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        kin.reporting                                │
│                                                                     │
│  RecommendationInput ─────────────────────────────┐                 │
│  (ProjectContext, CompletenessEvaluation,         │                 │
│   ConversationDecision, ScoreResult)              ▼                 │
│                                           ┌──────────────────┐      │
│  RecommendationModel                     │ RecommendationEngine│     │
│  (lowScore=40, highScore=70,            │  evaluate(input)   │      │
│   minCoverage=0.6, v1)                   └────────┬─────────┘      │
│         ▲                                        │ returns         │
│         │ (inyectado)                             ▼                 │
│  RecommendationStage ───► RecommendationResult ◄────────────────┐  │
│  (pipeline.stage)      (List<Recommendation>, priority,        │  │
│                         confidence, category, explanation,     │  │
│                         generatedBy, engineVersion)            │  │
│                                                               │  │
│  Recommendation ──◄────────────────────────────────────────────┘  │
│  (id determinista, category, title, description, priority,        │
│   impactLevel, effortLevel, relatedDimension,                     │
│   actionableSteps, expectedOutcome, explanation)                  │
│        │                                                          │
│        ├── RecommendationExplanation (usedInformation, appliedRule, reason)
│        └── RecommendationCategory / ImpactLevel / EffortLevel
└─────────────────────────────────────────────────────────────────────┘

PipelineContext: + recommendationResult: RecommendationResult (getter/setter)
```

## 4. ALGORITMO (determinista)

```
RecommendationResult evaluate(input):
  si input == null || projectContext == null || evaluation == null || score == null
     → RecommendationResult.empty()

  recomendaciones = []
  + coverageRecommendations(input)   # regla 1
  + scoreRecommendations(input)      # regla 2
  + maturityRecommendations(input)   # regla 3
  ordenar por priority desc (estable, orden de inserción = determinista)

  priority    = max(priorities) o 0
  category    = categoría más frecuente (tie-break: primera aparición)
  confidence  = 0.15 + 0.35*coverage + 0.25*qualityOfInformation + 0.25*(totalScore/100), en [0,1]
  explanation = "Se generaron N recomendaciones (X cobertura, Y score, Z madurez) con confianza P%"
```

### Regla 1 — Cobertura de dimensiones

Por cada `d in project.missingDimensions()`:
- Prioridad 9 si `d ∈ evaluation.criticalMissingDimensions()`, si no 6.
- Categoría/impacto/esfuerzo/pasos según tabla por dimensión (PROBLEM→STRATEGY, REVENUE_MODEL→FINANCIAL, TARGET_CUSTOMER→VALIDATION, MVP→PRODUCT, RISKS→OPERATIONS, etc.).
- `explanation` = información usada (dimensión + cobertura actual) + regla + motivo.

### Regla 2 — Score de viabilidad

- Si `totalScore < lowScoreThreshold(40)`:
  - Si existe dimensión cubierta con puntaje > 0 → recomienda reforzar la de menor puntaje (FINANCIAL, prioridad 8, CRITICAL/MEDIUM).
  - Si ninguna dimensión tiene puntaje > 0 → recomendación general de recolección de información (VALIDATION, prioridad 8).
- Si `totalScore >= highScoreThreshold(70)` **y** `project.missingDimensions()` no vacío → recomendación de innovación sostenible (INNOVATION, prioridad 5).

### Regla 3 — Madurez

- `maturityLevel == EARLY` → priorizar validación temprana del cliente (VALIDATION, prioridad 7).
- `maturityLevel == MATURE` y hay dimensiones pendientes → consolidar plan de escalamiento (STRATEGY, prioridad 5).

### Trazabilidad

Cada `Recommendation` lleva `RecommendationExplanation` con: qué información se usó, qué regla se aplicó y por qué se generó. El id de cada recomendación es `UUID.nameUUIDFromBytes(category|title|description)` → **reproducible**.

## 5. FLUJO DEL PIPELINE (actualizado)

```
Antes:  Analizador → Evaluador → Estratega → Consultor → Scoring → Eventos
Ahora:  Analizador → Evaluador → Estratega → Consultor → Scoring → Recomendaciones → Eventos
```

- `RecommendationStage.supports()`: requiere `projectContext != null && evaluation != null && decision != null && decision.shouldGenerateReport() && scoreResult != null`.
- Se inserta entre `ScoringStage` y `EventStage` en `KinConfig.chatPipeline(...)`.
- **Compatibilidad**: `EventStage` y los eventos existentes (`ConversationCompletedEvent`, `ScoreCalculatedEvent`, `ReportGeneratedEvent`, etc.) no se modifican. No cambia el contrato de `PipelineStage`, `AIProvider`, `DomainEventBus` ni `KinMethod`. No cambian endpoints REST ni SSE.

## 6. COBERTURA DE PRUEBAS (JaCoCo)

Paquete `com.kinplatform.kin.reporting`:

| Clase | Instrucciones | Ramas |
|---|---|---|
| `RecommendationEngine` | 95.4% | 85.2% |
| `Recommendation` | 97.8% | 87.5% |
| `RecommendationResult` | 100% | 100% |
| `RecommendationModel` | 92.9% | — |
| `RecommendationExplanation` | 92.3% | 50% |
| `RecommendationCategory` / `ImpactLevel` / `EffortLevel` / `Input` | 100% | — |
| **Paquete total** | **96.5%** | **90.4%** |

**Requisito**: ≥90% de cobertura en dominio → **CUMPLIDO** (96.5% instrucciones, 90.4% ramas).

### Escenarios cubiertos (test)

| Escenario | Test | Resultado |
|---|---|---|
| Entrada nula / incompleta | `evaluate_deberiaRetornarVacio_*` | `empty()` |
| Proyecto inmaduro | `evaluate_deberiaGenerarRecomendaciones_cuandoProyectoInmaduro` | 13 recs (11 brechas + validación temprana + recolección) |
| Proyecto maduro completo + score alto | `..._cuandoProyectoMaduroCompleto` | Sin recomendaciones |
| Score alto con pendencias | `..._cuandoScoreAltoYPendencias` | Innovación + brecha SCALABILITY |
| Score bajo | `..._cuandoScoreBajo` | Refuerzo del pilar más débil |
| Información insuficiente | `..._cuandoInformacionInsuficiente` | Recomendación general de recolección |
| Determinismo | `evaluate_deberiaSerDeterminista` / `create_deberiaGenerarIdDeterminista` | Mismas entradas → mismos ids/títulos/prioridades |
| Ordenamiento desc | `evaluate_deberiaOrdenarRecomendacionesPorPrioridadDescendente` | OK |
| Categoría dominante | `evaluate_deberiaCalcularCategoriaDominante` | La más frecuente |
| Confianza en [0,1] | `evaluate_deberiaCalcularConfianzaDeterminista` | OK |
| Stage `supports()` / `execute()` | `RecommendationStageTest` (5 tests) | gates + escritura en contexto |
| Inmutabilidad / clamping | `RecommendationResultTest` (13 tests) | OK |

**Total**: 42 tests (10 previos + 32 nuevos), todos en verde. `./mvnw test` ✔ · `./mvnw compile` ✔.

## 7. COMPATIBILIDAD CON FASES 1–4

| Área | Impacto | Verificación |
|---|---|---|
| `AiEngineServiceTest` (5) | Sin cambios | ✔ pasan |
| `ChatOrchestratorServiceImplTest` (5) | Sin cambios | ✔ pasan |
| Contrato `PipelineStage` | Sin cambios | ✔ |
| Contratos `AIProvider`, `ProviderRouter`, `DomainEventBus` | Sin cambios | ✔ |
| Endpoints REST / SSE / DTOs | Sin cambios | ✔ |
| `KinMethod` | Sin cambios | ✔ |
| Eventos existentes | Sin cambios | ✔ |
| `ScoringEngine` / `ScoreResult` | Consumidos, no modificados | ✔ |
| Persistencia / JPA / H2 / PostgreSQL | Sin cambios | ✔ |

**Aditividad**: la Fase 5.0 agrega componentes nuevos (`kin.reporting`, `RecommendationStage`) y un campo tipado nuevo en `PipelineContext`. No modifica ni elimina comportamiento existente.

## 8. ADRs

- `kin-docs/adr/ADR-001-reporting-bc.md` — Bounded Context Reporting.
- `kin-docs/adr/ADR-002-pipeline-context.md` — Campo `recommendationResult` en PipelineContext.
- `kin-docs/adr/ADR-003-recommendation-engine.md` — Motor determinista (sin LLM).

## 9. PENDIENTE (fuera de alcance, fases siguientes)

- `RiskEngine`, `OpportunityEngine`, `ReportEngine`.
- Refactor de consolidación: `ConsultationResult`, `EngineRegistry`/`EngineExecutor`, `RendererRegistry`, `PromptAssembler`.
- Explicación del resultado por el LLM (LlmExplanationStage) — respetando "Java decide, LLM comunica".

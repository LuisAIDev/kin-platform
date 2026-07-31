# FASE 5.1 — RISK ENGINE (Implementación)

> Fecha: 2026-07-30 · Estado: **Completado** · Alcance: solo `RiskEngine` (Opportunity/Report en fases siguientes). RecommendationEngine permanece **congelado** y no fue modificado.

## 1. OBJETIVO

Identificar y clasificar los riesgos del proyecto de forma **determinista, explicable y sin LLM**, mediante un **sistema compuesto de analizadores especializados** con un contrato común. El `RiskEngine` es un orquestador que **no contiene reglas de negocio**: solo coordina y consolida.

## 2. COMPONENTES CREADOS (`com.kinplatform.kin.reporting.risk`)

| Clase | Tipo | Responsabilidad |
|---|---|---|
| `RiskAnalyzer` | Interfaz (contrato) | `category()`, `analyze(RiskInput)`, `version()` |
| `BusinessRiskAnalyzer` | Servicio de dominio puro | Riesgos de negocio: PROBLEM, VALUE_PROPOSITION, OBJECTIVES, SOLUTION |
| `TechnicalRiskAnalyzer` | Servicio de dominio puro | Riesgos técnicos: MVP, SCALABILITY, SOLUTION |
| `FinancialRiskAnalyzer` | Servicio de dominio puro | Riesgos financieros: REVENUE_MODEL, RESOURCES, score < 40 |
| `MarketRiskAnalyzer` | Servicio de dominio puro | Riesgos de mercado: TARGET_CUSTOMER, COMPETITION, SECTOR |
| `RiskEngine` | Orquestador stateless | Auto-descubre analizadores (`List<RiskAnalyzer>`), consolida, ordena, agrega |
| `Risk` | VO inmutable | id, categoría, título, descripción, severidad, probabilidad, impacto, confianza, explicación, reglas, dimensión, engineVersion |
| `RiskExplanation` | VO inmutable | información utilizada, regla aplicada, motivo, **evidencia** |
| `RiskResult` | VO inmutable | risks, overallRiskLevel, topRisks, confidence, explanation, generatedBy, engineVersion |
| `RiskLevel` / `RiskCategory` | Enums | LOW..CRITICAL / BUSINESS, TECHNICAL, FINANCIAL, MARKET |
| `RiskModel` | Config | highSeverityCoverageThreshold=40, v1 |
| `RiskInput` | VO inmutable | ProjectContext, CompletenessEvaluation, ConversationDecision, ScoreResult |

Modificados: `PipelineContext` (campo `riskResult`), `KinConfig` (beans de analizadores + `RiskEngine` + `RiskStage`). **`RecommendationEngine` y `kin.reporting` (Recommendation) NO se tocaron.**

## 3. DIAGRAMA DE CLASES (UML)

```
                          kin.reporting.risk
┌───────────────────────────────────────────────────────────────────────┐
│  <<interface>> RiskAnalyzer                                            │
│  ├── category(): RiskCategory                                          │
│  ├── analyze(RiskInput): List<Risk>                                    │
│  └── version(): String                                                 │
│        ▲                    ▲                    ▲                      │
│        │                    │                    │                      │
│  ┌─────┴──────────┐  ┌──────┴────────┐  ┌───────┴────────┐             │
│  │BusinessRisk    │  │TechnicalRisk  │  │FinancialRisk   │             │
│  │Analyzer        │  │Analyzer       │  │Analyzer        │             │
│  └────────────────┘  └───────────────┘  └────────────────┘             │
│            ▲                                                           │
│  ┌─────────┴─────────────────────────┐                                 │
│  │ MarketRiskAnalyzer                │                                 │
│  └───────────────────────────────────┘                                 │
│                                                                        │
│  RiskEngine ──────────► List<RiskAnalyzer> (auto-descubrimiento DI)   │
│  ├── evaluate(RiskInput): RiskResult                                    │
│  └── NO contiene reglas de negocio (solo coordina/ordena/agrega)       │
│                                                                        │
│  RiskResult ───► List<Risk> ───► RiskExplanation (evidencia)          │
│  (overallRiskLevel, topRisks,                                          │
│   confidence, explanation,                                             │
│   generatedBy, engineVersion)                                          │
│                                                                        │
│  RiskLevel ◄── severity/probability/impact                             │
│  RiskCategory ◄── categoría del analizador                             │
│  RiskInput ◄── ProjectContext/Evaluation/Decision/Score                │
└───────────────────────────────────────────────────────────────────────┘

PipelineContext: + riskResult: RiskResult (getter/setter)
Pipeline:  ... → Scoring → Recomendaciones → Riesgos → Eventos
```

## 4. ALGORITMO

### RiskEngine.evaluate(RiskInput)

```
si input/projectContext/evaluation/score == null → RiskResult.empty()

riesgos = []
para cada analyzer en analyzers (auto-descubiertos):
    riesgos += analyzer.analyze(input)

ordenar riesgos: severityScore desc, luego categoría (ordinal), luego título

si riesgos vacío → RiskResult(riesgos vacíos, LOW, [], 0, "No se identificaron riesgos.",
                              "RiskEngine", model.version())

overallRiskLevel = max(severity de los riesgos)
topRisks         = primeros 3
confidence       = promedio de confianzas de los riesgos
explicación      = "Se identificaron N riesgos: X de negocio, Y técnicos, Z financieros, W de mercado."
```

### Cada RiskAnalyzer (reglas por categoría, 100% Java)

Regla genérica por dimensión no cubierta (`!project.isDimensionCovered(dim)`):

| Analizador | Dimensión | Severidad | Probabilidad | Impacto | Regla |
|---|---|---|---|---|---|
| Business | PROBLEM | HIGH | HIGH | HIGH | PROBLEM_NO_DEFINIDO |
| Business | VALUE_PROPOSITION | HIGH | MEDIUM | HIGH | VALUE_PROPOSITION_NO_DEFINIDA |
| Business | OBJECTIVES | MEDIUM | MEDIUM | MEDIUM | OBJECTIVES_NO_DEFINIDOS |
| Business | SOLUTION | MEDIUM | MEDIUM | HIGH | SOLUTION_NO_DOCUMENTADA |
| Technical | MVP | HIGH | MEDIUM | HIGH | MVP_NO_DEFINIDO |
| Technical | SCALABILITY | MEDIUM | MEDIUM | HIGH | SCALABILITY_NO_EVALUADA |
| Technical | SOLUTION | MEDIUM | LOW | MEDIUM | SOLUTION_SIN_DETALLE |
| Financial | REVENUE_MODEL | CRITICAL | HIGH | CRITICAL | REVENUE_MODEL_NO_DEFINIDO |
| Financial | RESOURCES | MEDIUM | MEDIUM | MEDIUM | RESOURCES_NO_PLANIFICADOS |
| Financial | (score < 40) | HIGH | MEDIUM | HIGH | SCORE_GLOBAL_BAJO |
| Market | TARGET_CUSTOMER | HIGH | HIGH | HIGH | TARGET_CUSTOMER_NO_IDENTIFICADO |
| Market | COMPETITION | HIGH | MEDIUM | MEDIUM | COMPETITION_NO_ANALIZADA |
| Market | SECTOR | MEDIUM | MEDIUM | MEDIUM | SECTOR_NO_CARACTERIZADO |

### Trazabilidad y evidencia

Cada `Risk` incluye:
- `explanation.usedInformation`: cobertura del proyecto, dimensiones cubiertas/total.
- `explanation.appliedRule`: la regla aplicada (p. ej. `REVENUE_MODEL_NO_DEFINIDO`).
- `explanation.reason`: por qué la dimensión ausente incrementa ese tipo de riesgo.
- `explanation.evidence`: evidencia concreta (p. ej. "Dimensión REVENUE_MODEL no cubierta", "Score total: 25/100").
- `appliedRules`: lista de reglas que se aplicaron.
- `engineVersion`: versión del analizador (`v1`).
- `confidence`: `clamp(0.35 + 0.35*coverage + 0.3*qualityOfInformation, 0, 1)`.
- `id`: `UUID.nameUUIDFromBytes(category|title|description)` → reproducible.

## 5. FLUJO DEL PIPELINE (actualizado)

```
Antes (Fase 5.0):  Analizador → Evaluador → Estratega → Consultor → Scoring → Recomendaciones → Eventos
Ahora (Fase 5.1):  Analizador → Evaluador → Estratega → Consultor → Scoring → Recomendaciones → Riesgos → Eventos
```

- `RiskStage.supports()`: `projectContext != null && evaluation != null && decision != null && decision.shouldGenerateReport() && scoreResult != null` (mismo patrón que `RecommendationStage`).
- `RiskStage.execute()`: construye `RiskInput`, invoca `riskEngine.evaluate()`, escribe `context.riskResult(result)`.
- **No se modificó ninguna otra etapa** (Analyzer, Evaluator, Strategist, Consultor, Scoring, Recommendation, Event).

## 6. COBERTURA DE PRUEBAS (JaCoCo)

Paquete `com.kinplatform.kin.reporting.risk`:

| Clase | Instrucciones | Ramas |
|---|---|---|
| `RiskEngine` | 100% | — |
| `Risk` | 100% | — |
| `RiskResult` | 100% | — |
| `RiskExplanation` | 100% | — |
| `RiskModel` / `RiskInput` / `RiskLevel` / `RiskCategory` | 100% | — |
| `BusinessRiskAnalyzer` | 100% | — |
| `TechnicalRiskAnalyzer` | 100% | — |
| `FinancialRiskAnalyzer` | 100% | — |
| `MarketRiskAnalyzer` | 100% | — |
| **Paquete total** | **100%** | **98.6%** |

**Requisito**: ≥90% en dominio → **CUMPLIDO** (100% instrucciones).

### Escenarios cubiertos

| Escenario | Test | Verificación |
|---|---|---|
| Proyecto sin riesgos | `evaluate_deberiaRetornarSinRiesgos_cuandoProyectoCompleto` | 0 riesgos, LOW, generatedBy=RiskEngine |
| Riesgo financiero | `..._deberiaIdentificarRiesgoFinanciero` | REVENUE_MODEL CRITICAL + explicación/evidencia |
| Riesgo técnico | `..._deberiaIdentificarRiesgoTecnico` | MVP |
| Riesgo de mercado | `..._deberiaIdentificarRiesgoDeMercado` + `..._deRiesgoDeSector` | TARGET_CUSTOMER, COMPETITION, SECTOR |
| Múltiples riesgos | `..._deberiaIdentificarMultiplesRiesgos_cuandoProyectoInmaduro` | ≥1 por categoría, overall CRITICAL, topRisks ordenado desc |
| Datos insuficientes | `..._cuandoEntradaNula` / `..._cuandoFaltanDatos` / `..._cuandoEvaluacionEsNula` | `RiskResult.empty()` |
| Determinismo | `evaluate_deberiaSerDeterminista` | Mismas entradas → mismos ids/severidades/confianzas |
| Auto-descubrimiento | `engine_deberiaDescubrirAnalizadoresRegistrados` | Solo analiza la categoría registrada |
| Inmutabilidad | `RiskResultTest` (13 tests) | Listas defensivas, clamping, ids deterministas |

**Total**: 71 tests en verde (`./mvnw test` ✔). `./mvnw compile` ✔.

## 7. COMPATIBILIDAD CON FASES 1–5.0

| Área | Impacto | Verificación |
|---|---|---|
| `RecommendationEngine` / `kin.reporting` (Recommendation) | **Sin cambios (congelado)** | ✔ 27 tests siguen pasando |
| `ScoringEngine` / `ScoreResult` | Consumidos, no modificados | ✔ |
| `AiEngineServiceTest`, `ChatOrchestratorServiceImplTest` | Sin cambios | ✔ 10 tests |
| Contrato `PipelineStage`, `AIProvider`, `DomainEventBus`, `KinMethod` | Sin cambios | ✔ |
| REST / SSE / DTOs / eventos | Sin cambios | ✔ |
| Persistencia / JPA / H2 / PostgreSQL | Sin cambios | ✔ |

**Aditividad**: solo se agregan componentes nuevos (`kin.reporting.risk`, `RiskStage`) y un campo tipado `riskResult` en `PipelineContext`.

## 8. ADRs

- `kin-docs/adr/ADR-004-risk-engine.md` — Sistema compuesto de analizadores especializados + RiskEngine orquestador.

## 9. PENDIENTE (fuera de alcance, fases siguientes)

- `OpportunityEngine`, `ReportEngine`.
- Refactor de consolidación (`ConsultationResult`, `EngineRegistry`, `RendererRegistry`, `PromptAssembler`).

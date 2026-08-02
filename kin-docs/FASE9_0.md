# FASE 9.0 - Pipeline Resilience & Response Fallback (KIN 2.1 — Pipeline Estabilizado)

> **Estado**: **FINALIZADA** (todas las etapas E1–E7 completadas y certificadas).
> **Base**: Release `v1.0.0-phase8` - FASE 8 COMPLETADA (ADR-016 Aprobado). Pipeline de 13 etapas.
> **ADR**: 017 (pipeline resilience & response fallback) - **Estado: Aprobado**.
>
> Secuencia oficial del proyecto: Fase 5.6 → Conversation Orchestrator (CERRADA), Fase 6 →
> KnowledgeEngine + RAG (CERRADA), Fase 7 → Strategic Interview Engine (CERRADA), Fase 8 →
> Knowledge-Enhanced Analysis (CERRADA), **Fase 9 → Pipeline Stabilized / KIN 2.1 (E1–E7 —
> COMPLETADAS; FASE FINALIZADA)**.

---

## 1. Objetivo

Hito documentado como **KIN 2.1 — "Pipeline Estabilizado"** (`ARQUITECTURA_BASE_KIN_2.0.md` §9):
*pipeline completamente funcional y testeado en ambos flujos*. Objetivos concretos:

1. **Resiliencia del pipeline**: manejo de errores por stage con estrategias retry/fail, timeout
   por stage y métricas (duración, tasas de éxito/fallo) — manteniendo el contrato congelado
   `PipelineStage.execute`.
2. **Semántica completa de eventos** en `EventStage` (no siempre `ConversationCompleted`).
3. **Consumo de `ResponseValidation`**: fallback determinista (respuesta enlatada / reintento)
   ante `accepted=false`, en bloqueante y streaming.
4. **Test de integración end-to-end**: ChatController → Orchestrator → KinMethod → Pipeline → DB.
5. **Preservar** la compatibilidad total: 1049 tests verdes, contratos congelados intactos
   (solo aditivos sancionados) y cobertura de dominio ≥ 90 %.

Principio rector (intacto):

> **Java decide. El LLM únicamente comunica.**
> Java decide políticas de reintento/fallo, timeout, métricas, semántica de eventos y el contenido
> del fallback de respuesta; el LLM solo comunica.

---

## 2. Motivación

### 2.1 Brechas detectadas (documentadas en el proyecto)

| # | Brecha | Impacto |
|---|--------|---------|
| 1 | El pipeline se ejecuta de forma secuencial simple: **sin error handling, sin timeout, sin métricas** por stage | Un stage que falle o se cuelgue rompe el turno sin control ni observabilidad |
| 2 | `EventStage` **no cubre la semántica completa de eventos** (hoy distingue ASK/REPORT) | Los eventos no reflejan el flujo real completo |
| 3 | `ResponseValidation` es un **artefacto de auditoría sin consumidor** | Una respuesta inválida del LLM (`accepted=false`) se entrega sin fallback |
| 4 | No existe **test de integración end-to-end** (Controller → Orchestrator → KinMethod → Pipeline → DB) | La cadena completa no está probada como unidad |

Fuentes: `ARQUITECTURA_BASE_KIN_2.0.md` §9 (KIN 2.1), `BASELINE_ARCHITECTURE.md` §5.2/§7.5/R2,
`ADR-013` (nota KIN 2.1+), `CHANGELOG.md` Known Issues.

### 2.2 Por qué ahora

La FASE 8 cerró el núcleo inteligente de 13 etapas y dejó la plataforma en su primera release
estable (`v1.0.0-phase8`). El roadmap documentado asigna estas brechas al siguiente hito (KIN 2.1);
la Prioridad 1 del baseline (consumir `KnowledgeResult`) ya fue cumplida en la FASE 8, por lo que
la Prioridad 2 (`ResponseValidation`) y la estabilización del pipeline son el trabajo pendiente
natural de esta fase.

---

## 3. Responsabilidades

| Actor | Responsabilidad |
|-------|-----------------|
| `Pipeline` | Algoritmo con error handling (retry/fail), timeout por stage y métricas; firma `execute(PipelineContext)` intacta |
| `StagePolicy` / `StageRetryPolicy` / `StageTimeoutConfig` | Políticas deterministas por stage (reintentos, acción ante fallo, timeout) — decisión 100 % Java |
| `PipelineMetrics` / `StageExecutionStats` | Métricas inmutables de duración/éxito/fallo por stage |
| `EventStage` | Semántica completa de eventos según decisión y flujo real |
| `ResponseFallback` | Resolución determinista de respuesta ante `accepted=false` (enlatada en español / reintento acotado) |
| `ConversationOrchestrator` / `ConsultorStage` / `KinMethod` | Consumen `ResponseValidation` (bloqueante / streaming) y aplican el fallback sin cambiar firmas |
| LLM | Comunica; nunca decide resiliencia ni fallbacks |

---

## 4. Componentes

### 4.1 Componentes nuevos (`kin.pipeline` / `kin.conversation` — dominio POJO puro)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `StagePolicy` | Tipo puro (E2 ⏳) | Política determinista por stage: reintentos máximos, acción ante fallo (`FAIL`/`RETRY`/`SKIP`), timeout en ms |
| `StageRetryPolicy` | Tipo puro (E2 ⏳) | Estrategia de reintento: número máximo, backoff, stages elegibles (seguros) |
| `StageTimeoutConfig` | Tipo puro (E2 ⏳) | Umbral de timeout por stage y acción ante timeout |
| `PipelineMetrics` | record inmutable (E2 ⏳) | Métricas agregadas del pipeline |
| `StageExecutionStats` | record puro (E2 ⏳) | Estadística individual de ejecución de un stage |
| `PipelineExecutionException` | excepción de dominio (E2 ⏳) | Clasifica el fallo del pipeline (stage, causa, timeout/retry) |
| `ResponseFallback` | Clase pura (E2 ⏳) | Fallback determinista de respuesta ante `accepted=false` |

### 4.2 Cambios aditivos propuestos (se sancionarán en E3…E6)

| Contrato | Cambio propuesto | Tipo |
|----------|------------------|------|
| `Pipeline` | Error handling (retry/fail), timeout por stage, métricas; firma intacta | Aditivo (sancionado por ADR-017) |
| `EventStage` | Semántica completa de eventos | Aditivo |
| `kin.event` | Nuevos tipos de eventos aditivos | Aditivo |
| `ConversationOrchestrator` | Consumo de `ResponseValidation` (bloqueante) | Aditivo |
| `ConsultorStage` / `KinMethod` | Consumo de `responseValidation` (streaming) | Aditivo |
| `KinConfig` | Beans de políticas/métricas/fallback + wiring | Cableado |

**Sin cambios**: `PipelineStage` (interfaz congelada), `TurnPolicy`/`ResponseGuard`/`HistoryWindow`,
`PromptAssembler`, `AIResponder`, `KnowledgeEngine`/`KnowledgeGateway`/`SourceValidator`,
`InterviewEngine`/`InterviewBlueprint`/`AnswerValidator`, `EnrichmentEngine`/`FactRanker`,
`ScoringEngine`/`RecommendationEngine`/`RiskEngine`/`OpportunityEngine`/`ReportEngine`,
`ProjectContext`, `ConversationDecision`, `kin/engine`, Flyway, REST, Controllers.

---

## 5. Diagrama lógico

```
┌────────────────────────────────────────────────────────────────────┐
│                    DOMINIO (kin.pipeline / kin.conversation)          │
│                                                                      │
│  StagePolicy / StageRetryPolicy / StageTimeoutConfig                 │
│  PipelineMetrics / StageExecutionStats / PipelineExecutionException  │
│  ResponseFallback (kin.conversation)                                 │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ consumido por
┌──────────────────────────────▼───────────────────────────────────────┐
│                          PIPELINE (algoritmo)                         │
│                                                                       │
│  Pipeline.execute(ctx) → por stage:                                   │
│     1. verificar supports                                             │
│     2. timeout por stage                                              │
│     3. error handling (retry/fail según StagePolicy)                  │
│     4. métricas (StageExecutionStats → PipelineMetrics)               │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────────┐
│                    CICLO DE RESPUESTA (ResponseValidation)            │
│                                                                       │
│  Bloqueante: ConversationOrchestrator → TurnResult.validation        │
│  Streaming:  ConsultorStage → PipelineContext.responseValidation     │
│      └── accepted=false ──► ResponseFallback (enlatada / reintento)   │
└───────────────────────────────────────────────────────────────────────┘
```

### Pipeline (13 etapas, intacto)

`Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Enriquecimiento → Scoring →
Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`

La FASE 9 **no agrega etapas**: refuerza el `Pipeline` (resiliencia/métricas), completa la
semántica de `EventStage` y consume `ResponseValidation`.

---

## 6. Integración con las fases anteriores

| Fase / ADR | Integración |
|-----------|-------------|
| **Fase 5.2.1 / ADR-006…009 (runtime)** | `KinMethod`/`Pipeline` son el punto de integración; ADR-017 sanciona el cambio de algoritmo |
| **Fase 5.6 / ADR-013 (orquestador)** | `ConversationOrchestrator` consume `ResponseValidation` sin cambiar firmas |
| **Fase 5.5 / ADR-012 (prompt)** | `PromptAssembler`/`ReportPromptBuilder` no cambian |
| **Fases 6–8 (knowledge/interview/enrichment)** | Stages reciben políticas de resiliencia sin cambios internos |
| **Fases 5.0–5.4 (motores de análisis)** | Motores intactos; solo el `Pipeline` gestiona su ejecución |

---

## 7. Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Retry sobre stages no idempotentes que dupliquen efectos | Alta | Retry solo en stages seguros; fail-fast por defecto |
| R2 | Cambio del algoritmo de `Pipeline` (contrato congelado §4.1) | Alta | Sanción explícita por ADR-017; firmas intactas |
| R3 | Overhead de timeout/métricas | Media | Mediciones ligeras; timeout solo donde aplique |
| R4 | Romper el streaming SSE con el fallback | Media | Fallback determinista que respeta el contrato SSE |
| R5 | Crecimiento de `PipelineContext` (God Class, R6 baseline) | Baja | Métricas en registro inmutable separado |
| R6 | Fallback mal diseñado (contenido inventado) | Media | Respuestas enlatadas deterministas; sin inferir intención |
| R7 | Métricas expuestas sin control de acceso | Baja | Acceso restringido (rol ADMIN) si se exponen |

---

## 8. Roadmap completo E1…E7

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico: ADR-017 (Propuesto) + FASE9_0 (objetivo, motivación, responsabilidades, componentes, diagrama lógico, integración, riesgos, roadmap, criterios de aceptación) | Documentación | ✅ **Completada (2026-08-02)** |
| **E2** | Modelo de dominio `kin.pipeline`/`kin.conversation`: `StagePolicy`/`StageRetryPolicy`/`StageTimeoutConfig`, `PipelineMetrics`/`StageExecutionStats`, `PipelineExecutionException`, `ResponseFallback` + tests de dominio | Código de dominio | ✅ **Completada (2026-08-02)** |
| **E3** | `Pipeline` con error handling (retry/fail), timeout por stage y métricas (aditivo, firma intacta) + tests | Resiliencia del pipeline | ✅ **Completada (2026-08-02)** |
| **E4** | `EventStage` con semántica completa de eventos + nuevos eventos aditivos + tests | Semántica de eventos | ✅ **Completada (2026-08-02)** |
| **E5** | Consumo de `ResponseValidation`: fallback/reintento bloqueante (`ConversationOrchestrator`) y streaming (`ConsultorStage`/`KinMethod`) + tests | Fallback de respuesta | ✅ **Completada (2026-08-02)** |
| **E6** | Test de integración end-to-end (ChatController → Orchestrator → KinMethod → Pipeline → DB) + regresión | Integración | ✅ **Completada (2026-08-02)** |
| **E7** | Auditoría de cierre: ADR-017 → Aprobado, contratos intactos, `./mvnw clean verify`, cobertura ≥ 90 % en paquetes afectados, cierre oficial de la FASE 9 | Cierre de fase | ✅ **Completada (2026-08-02)** |

---

## 9. Criterios de aceptación

- [x] ADR-017 en estado **Aprobado** tras E1…E7 (actualmente **Aprobado**).
- [x] `Pipeline` con error handling (retry/fail), timeout por stage y métricas, firma congelada intacta.
- [x] `EventStage` con semántica de eventos completa por decisión/flujo real.
- [x] `ResponseValidation` consumido en bloqueante y streaming con fallback determinista ante `accepted=false`.
- [x] Test de integración end-to-end verde (ChatController → Orchestrator → KinMethod → Pipeline → DB).
- [x] Contratos congelados sin cambios (solo aditivos sancionados por ADR-017).
- [x] 1210 tests previos verdes sin modificación de aserciones.
- [x] `./mvnw clean verify` → **BUILD SUCCESS**; cobertura ≥ 90 % en `kin.pipeline` y `kin.conversation` (JaCoCo).

---

## 10. Estado del entregable

| Etapa | Estado |
|-------|--------|
| **E1 — Diseño arquitectónico (ADR-017 Aprobado + FASE9_0)** | ✅ **Completada** |
| **E2 — Modelo de dominio (políticas, métricas, fallback)** | ✅ **Completada** |
| **E3 — Pipeline resiliente (retry/fail/timeout/métricas)** | ✅ **Completada** |
| **E4 — Semántica de eventos (EventStage)** | ✅ **Completada** |
| **E5 — Consumo de ResponseValidation (bloqueante + streaming)** | ✅ **Completada** |
| **E6 — Test de integración end-to-end + regresión** | ✅ **Completada** |
| **E7 — Auditoría de cierre (ADR-017 → Aprobado)** | ✅ **Completada** |

**FASE 9 — ETAPAS E1–E7 COMPLETADAS. FASE FINALIZADA Y CERRADA OFICIALMENTE.**
**Resultados de la auditoría de cierre: `./mvnw clean verify` → BUILD SUCCESS; 1210 tests
(0 failures, 0 errors, 0 skipped); cobertura de dominio ≥ 90 % (`kin.pipeline` 93.7 %,
`kin.pipeline.stage` 97.4 %, `kin.pipeline.resilience` 99.7 %, `kin.conversation` 99.2 %);
contratos congelados de `BASELINE_ARCHITECTURE.md` intactos; integración de extremo a extremo
verificada (ChatController → Orchestrator → KinMethod → Pipeline de 13 etapas → Response).**

*Fase 9 — Pipeline Resilience & Response Fallback (KIN 2.1). Etapas E1–E7 cerradas oficialmente.
ADR-017 en estado Aprobado (contrato congelado); el pipeline, la semántica de eventos y el
fallback de respuesta se implementaron de forma aditiva sin modificar contratos congelados.
La FASE 9 queda oficialmente FINALIZADA y el proyecto queda preparado para iniciar la siguiente
fase.*

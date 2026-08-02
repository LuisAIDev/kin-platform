# ADR-017: Pipeline Resilience & Response Fallback — pipeline estabilizado, semántica de eventos y consumo de la validación de respuesta

**Estado**: **Aprobado** (Etapas E1–E7 completadas: diseño arquitectónico, modelo de dominio,
resiliencia del pipeline, semántica de eventos, consumo de `ResponseValidation`/fallback,
integración end-to-end y auditoría de cierre implementados y certificados mediante auditoría técnica)
**Fecha**: 2026-08-02
**Autor**: KIN Architecture Team

> **Alcance**: este ADR propone y congela (una vez aprobada) la arquitectura del siguiente hito
> del proyecto, denominado **FASE 9 (KIN 2.1 — "Pipeline Estabilizado")** en el roadmap documentado
> en `kin-docs/ARQUITECTURA_BASE_KIN_2.0.md` (§9) y `kin-docs/BASELINE_ARCHITECTURE.md` (§5.2, §7.5),
> documentada en `kin-docs/FASE9_0.md`. En estado **Aprobado**, sus decisiones quedan congeladas
> y constituyen contrato. Todos los cambios sancionados son **aditivos** (patrón
> ADR-011/014/015/016) y mantienen el principio rector del proyecto:
> **Java decide. El LLM únicamente comunica.**

---

## Problema

El pipeline de dominio está funcional (13 etapas) y certificado (FASE 8, ADR-016), pero la
documentación del proyecto documenta tres brechas pendientes para el hito KIN 2.1:

| # | Brecha | Evidencia |
|---|--------|-----------|
| 1 | **El pipeline no maneja errores, timeouts ni métricas por stage**: la ejecución es secuencial simple sin estrategias de reintento/fallo, sin timeout y sin métricas de duración/éxito/fallo. | `ARQUITECTURA_BASE_KIN_2.0.md` §9 "KIN 2.1 — Pipeline Estabilizado"; `BASELINE_ARCHITECTURE.md` §5.2 (`kin.pipeline`: "Error handling, timeout y métricas por stage → KIN 2.1") |
| 2 | **`EventStage` aún no cubre la semántica completa de eventos** (hoy ya distingue ASK/REPORT pero no la semántica total del flujo real). | `ARQUITECTURA_BASE_KIN_2.0.md` §9; `BASELINE_ARCHITECTURE.md` §5.2 y riesgo R2 |
| 3 | **`ResponseValidation` es un artefacto de auditoría sin consumidor**: el guardrail valida la respuesta del LLM (`accepted = issues.isEmpty()`) pero no define el fallback (respuesta enlatada / reintento) ante `accepted=false`. | `BASELINE_ARCHITECTURE.md` §7.5 Prioridad 2; `ADR-013` (nota KIN 2.1+ definirá el consumidor); `CHANGELOG.md` Known Issues |

El resultado es un pipeline **sin resiliencia** y una validación de comunicación **sin uso efectivo**
en producción (bloqueante y streaming).

---

## Contexto

KIN 2.0 Alpha 1 + Fases 6, 7 y 8 están cerradas oficialmente (ADR-014, ADR-015 y ADR-016
Aprobados). El pipeline actual tiene **13 etapas**:

`Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Enriquecimiento → Scoring →
Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`

El roadmap documentado para **KIN 2.1 — Pipeline Estabilizado** (`ARQUITECTURA_BASE_KIN_2.0.md` §9)
define el objetivo: *"Pipeline completamente funcional y testeado en ambos flujos"*, con estos
ítems pendientes:

- Pipeline error handling con estrategias retry/fail.
- Pipeline timeout por stage.
- Pipeline metrics (duración por stage, tasas de éxito/fallo).
- `EventStage` dispara los eventos correctos (no siempre `ConversationCompleted`).
- Test de integración: ChatController → Orchestrator → KinMethod → Pipeline → DB.

La Prioridad 1 del baseline (§7.5) — consumir el `KnowledgeResult` en los analizadores — **ya fue
cumplida por la FASE 8** (ADR-016); la Prioridad 2 — consumir `ResponseValidation` (fallback
respuesta enlatada / reintento) — es parte de este ADR.

---

## Objetivo

1. **Resiliencia del pipeline**: manejo de errores por stage con estrategias de reintento/fallo
   (fail-fast por defecto, retry limitado a stages seguros), timeout por stage y métricas
   (duración, éxito/fallo, reintentos) — sin cambiar el contrato congelado `PipelineStage.execute`.
2. **Semántica completa de eventos**: `EventStage` dispara los eventos correctos según la
   decisión y el flujo real (ya distingue ASK/REPORT; se completa la semántica).
3. **Consumo de `ResponseValidation`**: definir en Java el fallback determinista (respuesta
   enlatada / reintento) ante `accepted=false`, tanto en el flujo bloqueante
   (`ConversationOrchestrator`) como en el streaming (`ConsultorStage`/`KinMethod`).
4. **Test de integración end-to-end**: ChatController → Orchestrator → KinMethod → Pipeline → DB.
5. **Preservar** la compatibilidad total: 1049 tests verdes, contratos congelados intactos
   (solo aditivos sancionados) y cobertura de dominio ≥ 90 %.

**Java será responsable exclusivamente de**: las políticas de reintento/fallo, el timeout, las
métricas, la semántica de eventos y el contenido del fallback de respuesta. El LLM únicamente
comunica y nunca decide resiliencia ni fallbacks.

---

## Decisión

Se introduce un **conjunto aditivo de políticas y métricas de resiliencia** en el pipeline y un
**consumidor determinista de `ResponseValidation`** en el ciclo de conversación, alineados con los
patrones ya congelados (`DomainEngine`, coordinadores, stages aditivos, ADR-013):

1. **`Pipeline` (algoritmo de ejecución)**: el bucle secuencial actual gana manejo de errores por
   stage (estrategias retry/fail), timeout por stage y recolección de métricas, **manteniendo la
   firma congelada** `execute(PipelineContext) → PipelineContext` y la API de `PipelineStage`
   (`name()`, `supports()`, `execute()`). El cambio del algoritmo se sanciona por este ADR.
2. **Políticas de stage puras** (`kin.pipeline`): `StagePolicy` / `StageRetryPolicy` /
   `StageTimeoutConfig` — configuración determinista por stage (máximo de reintentos, acción ante
   fallo: `FAIL`/`RETRY`/`SKIP`, timeout en ms). Retry limitado a stages seguros (idempotentes o
   sin efectos duplicados); fail-fast por defecto.
3. **Métricas de pipeline** (`kin.pipeline`): `PipelineMetrics` / `StageExecutionStats` — registro
   inmutable de duración, estado (éxito/fallo), reintentos y timeout por stage. Se registran de
   forma separada al `PipelineContext` (mitigación del riesgo R6 "God Class").
4. **`EventStage`**: semántica completa de eventos según decisión y flujo real, con nuevos tipos
   de eventos aditivos en `kin/event` (p. ej. eventos de fallo/estadística) sin alterar los
   eventos existentes.
5. **`ResponseFallback`** (`kin.conversation`): resolución determinista del fallback de respuesta
   ante `accepted=false` — respuestas enlatadas en español o reintento acotado — sin inferir
   intención del texto (misma garantía que `ResponseGuard`).
6. **Consumo aditivo de `ResponseValidation`**:
   - Bloqueante: `ConversationOrchestrator.orchestrate(...)` aplica el fallback cuando
     `TurnResult.validation.accepted == false` (firma congelada intacta).
   - Streaming: `ConsultorStage` / `KinMethod` consumen `PipelineContext.responseValidation`.
7. **Test de integración end-to-end** (E6): ChatController → Orchestrator → KinMethod → Pipeline →
   DB, cubriendo ambos flujos.
8. **Sin cambios en contratos congelados**: `PipelineStage` (interfaz), `ConversationOrchestrator`
   (firmas `orchestrate`/`orchestrateStream`), `PromptAssembler`, `AIResponder`, `KinMethod`
   (firmas), motores de dominio (`KnowledgeEngine`, `InterviewEngine`, `EnrichmentEngine`,
   `ScoringEngine`, `RecommendationEngine`, `RiskEngine`, `OpportunityEngine`, `ReportEngine`),
   `ProjectContext`, `ConversationDecision`, `kin/engine`.

### Componentes nuevos propuestos (`kin.pipeline` / `kin.conversation`)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `StagePolicy` | Tipo puro (E2 ⏳) | Política determinista por stage: reintentos máximos, acción ante fallo (`FAIL`/`RETRY`/`SKIP`), timeout en ms |
| `StageRetryPolicy` | Tipo puro (E2 ⏳) | Estrategia de reintento: número máximo, backoff (fijo/exponencial), stages elegibles (seguros/idempotentes) |
| `StageTimeoutConfig` | Tipo puro (E2 ⏳) | Umbral de timeout por stage (ms) y acción ante timeout |
| `PipelineMetrics` | record (EngineResult, E2 ⏳) | Métricas inmutables: duración por stage, estado (éxito/fallo), reintentos, timeouts |
| `StageExecutionStats` | record puro (E2 ⏳) | Estadística individual de ejecución de un stage |
| `PipelineExecutionException` | excepción de dominio (E2 ⏳) | Clasifica el fallo del pipeline (tipo de stage, causa, timeout/retry) |
| `ResponseFallback` | Clase pura (E2 ⏳) | Resolución determinista de respuesta ante `accepted=false` (enlatada en español / reintento acotado) |

### Cambios aditivos propuestos (se sancionarán en E3…E6)

| Contrato | Cambio propuesto | Tipo |
|----------|------------------|------|
| `Pipeline` | Algoritmo de ejecución con error handling (retry/fail), timeout por stage y métricas; firma `execute(PipelineContext)` intacta | Aditivo (sancionado por ADR-017) |
| `PipelineContext` | Campos aditivos de estado de ejecución si fuesen necesarios (p. ej. fallo del último stage); API de acceso intacta | Aditivo |
| `EventStage` | Semántica completa de eventos según decisión/flujo real | Aditivo |
| `kin.event` | Nuevos tipos de eventos aditivos (p. ej. fallo/estadística); eventos existentes intactos | Aditivo |
| `ConversationOrchestrator` | Consumo de `ResponseValidation` en el flujo bloqueante (fallback/reintento); firmas intactas | Aditivo |
| `ConsultorStage` / `KinMethod` | Consumo de `responseValidation` en el flujo streaming (fallback/reintento) | Aditivo |
| `KinConfig` | Beans de políticas, métricas y fallback + wiring | Cableado |
| `application.yml` / `application-prod.properties` | Umbrales de timeout/retry (config) | Configuración |

**Sin cambios**: `PipelineStage` (interfaz congelada), `TurnPolicy`/`DefaultTurnPolicy`,
`ResponseGuard` (reglas de validación intactas), `HistoryWindow`, `PromptAssembler`,
`AIResponder`, `KnowledgeEngine`/`KnowledgeGateway`/`SourceValidator`, `InterviewEngine`/
`InterviewBlueprint`/`AnswerValidator`, `EnrichmentEngine`/`FactRanker`,
`ScoringEngine`, `RecommendationEngine`/`RiskEngine`/`OpportunityEngine`/`ReportEngine`,
`ProjectContext`, `ConversationDecision`, `kin/engine`, Flyway, REST, Controllers.

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Introducir reintentos incondicionales en todos los stages** | Los stages mutan `PipelineContext` (no son idempotentes); reintentar a ciegas duplicaría efectos. Retry limitado a stages seguros, fail-fast por defecto |
| **Agregar manejo de errores dentro de cada stage** | Duplica lógica y acopla cada stage a resiliencia; el manejo es transversal y debe vivir en el `Pipeline` (SRP) |
| **Cambiar la interfaz `PipelineStage.execute`** (p. ej. devolver resultado tipado) | Rompe el contrato congelado de `BASELINE §4.1`; la resiliencia se implementa sin cambiar la API |
| **Almacenar métricas dentro de `PipelineContext`** | Riesgo R6 "God Class"; las métricas van en un registro inmutable separado |
| **Dejar `ResponseValidation` como artefacto de auditoría** | Documentado como brecha (BASELINE §7.5 Prioridad 2, ADR-013); el fallback es decisión Java determinista |
| **Delegar el fallback al LLM** (que el modelo reescriba la respuesta) | Violenta *Java decide*; el fallback es determinista en Java (enlatado/reintento acotado) |

---

## Consecuencias

### Positivas

- **Pipeline resiliente**: errores, timeouts y reintentos controlados por stage con estrategias
  explícitas, manteniendo la API congelada.
- **Observabilidad**: métricas de duración/éxito/fallo por stage (base para monitoreo en Render).
- **`ResponseValidation` deja de ser auditoría**: el fallback determinista (respuesta enlatada /
  reintento) da una experiencia robusta ante respuestas inválidas del LLM, en bloqueante y streaming.
- **Semántica de eventos completa** en `EventStage` (mitiga el riesgo R2 del baseline).
- **Aditividad total**: mismos patrones ADR-011/014/015/016; ningún contrato congelado se modifica.
- **Compatibilidad**: 1049 tests verdes sin modificar aserciones; cobertura ≥ 90 % en los paquetes
  afectados.

### Negativas

- **Complejidad del runtime**: el `Pipeline` deja de ser un bucle simple (políticas, timeouts,
  métricas, reintentos) — exige tests dedicados de resiliencia.
- **Crecimiento del dominio**: +4 tipos de política/métricas, +1 excepción, +1 fallback de
  respuesta, +1..2 eventos y consumidores en bloqueante/streaming.
- **Riesgo de reintentos con efectos**: mitigado con política conservadora (retry solo en stages
  seguros).

---

## Integración con fases anteriores

| Fase / ADR | Integración |
|-----------|-------------|
| **Fase 5.2.1 / ADR-006…009 (runtime)** | `KinMethod` y `Pipeline` son el punto de integración; el cambio del algoritmo se sanciona por ADR-017 |
| **Fase 5.6 / ADR-013 (orquestador)** | `ConversationOrchestrator` consume `ResponseValidation` (bloqueante) sin cambiar firmas; `ResponseGuard`/`TurnPolicy`/`HistoryWindow` no cambian |
| **Fase 5.5 / ADR-012 (prompt)** | `PromptAssembler`/`ReportPromptBuilder` no cambian; el fallback solo afecta el ciclo de respuesta |
| **Fase 6 / ADR-014 (knowledge)** | `KnowledgeStage` es un stage más: recibe políticas de resiliencia sin cambios internos |
| **Fase 7 / ADR-015 (interview)** | `InterviewStage` es un stage más: recibe políticas de resiliencia sin cambios internos |
| **Fase 8 / ADR-016 (enrichment)** | `EnrichmentStage` es un stage más: recibe políticas de resiliencia sin cambios internos |
| **Fases 5.0–5.4 (scoring/recommendation/risk/opportunity/report)** | Motores intactos; solo el `Pipeline` gestiona su ejecución |

---

## Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Retry sobre stages no idempotentes que dupliquen efectos | Alta | Políticas conservadoras: retry solo en stages seguros; fail-fast por defecto; tests de resiliencia |
| R2 | Cambio del algoritmo de `Pipeline` (contrato congelado §4.1) | Alta | Sanción explícita por este ADR; firma `execute` y API `PipelineStage` intactas |
| R3 | Overhead de timeout/métricas que afecte el rendimiento | Media | Mediciones ligeras; timeout solo donde aplique; sin bloqueos |
| R4 | Romper el streaming SSE al introducir fallback en `ConsultorStage` | Media | Fallback determinista que respeta el contrato SSE (`token`/`error`/`done`) |
| R5 | Crecimiento de `PipelineContext` (riesgo God Class, R6 baseline) | Baja | Métricas en registro inmutable separado, no en el contexto |
| R6 | Fallback de respuesta mal diseñado (contenido inventado) | Media | Respuestas enlatadas deterministas en español; nunca inferir intención (garantía de `ResponseGuard`) |
| R7 | Exponer métricas sin control de acceso | Baja | Métricas con acceso restringido (rol ADMIN) si se exponen vía Actuator |

---

## Roadmap E1…E7

> Estado: **E1–E7 COMPLETADAS** — este ADR (estado **Aprobado**) + `FASE9_0.md`. El diseño
> (E1), el modelo de dominio (E2), la resiliencia del pipeline (E3), la semántica de eventos (E4),
> el consumo de `ResponseValidation`/fallback (E5) y la integración end-to-end (E6) están
> implementados; la auditoría de cierre (E7) certificó la fase como APROBADA.

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico: ADR-017 (Propuesto) + documento FASE9_0 (objetivo, motivación, componentes, diagrama, integración, riesgos, roadmap, criterios) | Documentación | ✅ **Completada (2026-08-02)** |
| **E2** | Modelo de dominio `kin.pipeline`/`kin.conversation`: `StagePolicy`/`StageRetryPolicy`/`StageTimeoutConfig`, `PipelineMetrics`/`StageExecutionStats`, `PipelineExecutionException`, `ResponseFallback` + tests de dominio | Código de dominio | ✅ **Completada (2026-08-02)** |
| **E3** | `Pipeline` con error handling (retry/fail), timeout por stage y métricas (aditivo, firma intacta) + tests | Resiliencia del pipeline | ✅ **Completada (2026-08-02)** |
| **E4** | `EventStage` con semántica completa de eventos + nuevos eventos aditivos + tests | Semántica de eventos | ✅ **Completada (2026-08-02)** |
| **E5** | Consumo de `ResponseValidation`: fallback/reintento bloqueante (`ConversationOrchestrator`) y streaming (`ConsultorStage`/`KinMethod`) + tests | Fallback de respuesta | ✅ **Completada (2026-08-02)** |
| **E6** | Test de integración end-to-end (ChatController → Orchestrator → KinMethod → Pipeline → DB) + regresión | Integración | ✅ **Completada (2026-08-02)** |
| **E7** | Auditoría de cierre: ADR-017 → **Aprobado**, contratos congelados intactos, `./mvnw clean verify` (BUILD SUCCESS), cobertura ≥ 90 % en paquetes afectados (JaCoCo), cierre oficial de la FASE 9 | Cierre de fase | ✅ **Completada (2026-08-02)** |

---

## Criterios de aceptación

- [x] ADR-017 en estado **Aprobado** tras E1…E7 (actualmente **Aprobado**).
- [x] `Pipeline` con error handling (retry/fail), timeout por stage y métricas, **manteniendo la
      firma congelada** `execute(PipelineContext)` y la API de `PipelineStage`.
- [x] `EventStage` con semántica de eventos completa por decisión/flujo real.
- [x] `ResponseValidation` consumido en bloqueante y streaming con fallback determinista
      (respuesta enlatada / reintento) ante `accepted=false`.
- [x] Test de integración end-to-end verde (ChatController → Orchestrator → KinMethod → Pipeline → DB).
- [x] Contratos congelados (`PipelineStage`, `ConversationOrchestrator`, `PromptAssembler`,
      `AIResponder`, `KinMethod`, motores, `kin/engine`, `ProjectContext`, `ConversationDecision`)
      **sin cambios** — solo aditivos sancionados.
- [x] Offline-first y compatibilidad: 1210 tests verdes sin modificación de aserciones.
- [x] `./mvnw clean verify` → **BUILD SUCCESS**; cobertura de dominio ≥ 90 % en los paquetes
      afectados (`kin.pipeline`, `kin.conversation`) (JaCoCo).

---

## Estado

**APROBADO** — Etapas E1–E7 de la FASE 9 (diseño arquitectónico, modelo de dominio,
resiliencia del pipeline, semántica de eventos, consumo de `ResponseValidation`/fallback,
integración end-to-end y auditoría de cierre) completadas. La implementación fue **completada y
certificada mediante auditoría técnica**: `./mvnw clean verify` → **BUILD SUCCESS**, **1210
tests** (0 failures, 0 errors, 0 skipped), cobertura de dominio ≥ 90 % en los paquetes afectados
(JaCoCo: `kin.pipeline` 93.7 %, `kin.pipeline.stage` 97.4 %, `kin.pipeline.resilience` 99.7 %,
`kin.conversation` 99.2 %), contratos congelados intactos e integración de extremo a extremo
verificada (ChatController → Orchestrator → KinMethod → Pipeline de 13 etapas → Response). Este
ADR NO modifica contratos congelados: todos los cambios sancionados son aditivos. La FASE 9 queda
**oficialmente cerrada**.

# ADR-015: Strategic Interview Engine — entrevista estratégica estructurada y determinista que recopila el contexto completo del negocio antes del análisis

**Estado**: **Aprobado** (E1…E7 completadas — diseño + implementación + cierre oficial de la Fase 7. Commit `docs(kin): complete strategic interview engine (phase 7)`)
**Fecha**: 2026-08-01
**Autor**: KIN Architecture Team

> **Alcance**: este ADR aprueba y congela la arquitectura de la **Fase 7
> (Strategic Interview Engine / Business Interview Engine)**, documentada en
> `kin-docs/FASE7_STRATEGIC_INTERVIEW_ENGINE.md`. En estado **Aprobado**, sus decisiones están
> activas: la integración al pipeline existente es **aditiva** (patrón ADR-011/014, precedente
> ADR-013) y el LLM permanece desacoplado de toda decisión de negocio.

---

## Problema

Antes de emitir recomendaciones o generar un `ConsultingReport`, KIN necesita comprender el
contexto completo del negocio. Hoy la recopilación de información se resuelve con una
**exploración ad-hoc por prioridad** (`DefaultExplorationStrategy`): en cada turno se elige la
dimensión faltante con mayor prioridad y se pide una única pregunta genérica sobre ella. Esta
aproximación presenta las siguientes brechas:

| # | Brecha | Evidencia |
|---|--------|-----------|
| 1 | **Sin plan estructurado de entrevista**: cada dimensión recibe una sola pregunta genérica; no existe una secuencia de preguntas por dimensión, ni preguntas obligatorias vs. opcionales. | `DefaultExplorationStrategy.decide(...)`, `INSTRUCTIONS` |
| 2 | **Sin validación de respuestas en Java**: una respuesta superficial o vacía marca la dimensión como cubierta; la profundización queda delegada al texto del LLM. | `ProjectContext.update(...)` (cualquier valor no vacío cubre la dimensión), `ConversationPromptBuilder` (CÓMO PROFUNDIZAR) |
| 3 | **Sin control del flujo de la entrevista**: no hay máquina de estados de entrevista, ni orden determinista garantizado, ni presupuesto de preguntas, ni regla de salida. | `ConversationDecision.Action.ASK` + `ExplorationPriority` |
| 4 | **Sin estado durable de la entrevista**: el progreso (qué se preguntó, qué se validó, qué falta) no se persiste como entidad propia; depende de `dimensionsCovered` del `ProjectContext`. | `ProjectContext` (contrato congelado) |
| 5 | **La profundidad la decide el LLM**: la instrucción "Si el usuario da una respuesta superficial… Profundizá" deja en manos del LLM cuándo seguir preguntando — viola el principio *Java decide / LLM únicamente comunica*. | `ConversationPromptBuilder` (CÓMO PROFUNDIZAR) |

El resultado es un consultor que no se comporta como un **entrevistador estratégico** real: no
sabe con certeza qué información falta, no valida lo que recibe y no puede garantizar que el
análisis posterior se alimente de un contexto completo y verificado.

---

## Contexto

KIN 2.0 Alpha 1 (`v2.0.0-alpha1`, ALPHA STABLE) liberó el núcleo inteligente: pipeline de 11
etapas (`Analizador → Evaluador → Estratega → Conocimiento → Scoring → Recomendaciones → Riesgos
→ Oportunidades → Reporte → Consultor → Eventos`), motores canonizados bajo `DomainEngine`
(ADR-005/009), runtime único `KinMethod` (ADR-006), contexto durable `ContextRepository`
(ADR-007), IA por puertos `AIResponder`/`PromptAssembler` (ADR-008/012), ciclo conversacional
dirigido por `ConversationOrchestrator` + `TurnPolicy` + `ResponseGuard` (ADR-013) y adquisición
de conocimiento externo dirigida por `KnowledgeEngine`/`KnowledgeGateway`/`SourceValidator`
(ADR-014).

El principio rector del proyecto es:

> **Java decide. El LLM únicamente comunica.**
> Ninguna decisión de negocio dependerá del LLM.

Las fases 5.6 y 6 están **cerradas oficialmente** (ADR-013 Aprobado, ADR-014 Aprobado) y KIN 2.0
Alpha 1 continúa siendo el estado oficial del proyecto. El baseline §7.5 recomendó consumir el
`KnowledgeResult` y el turno tipado (`TurnResult`, `TurnDirective`) como puntos de extensión
para una Fase 7 — **sin tocar los contratos estables**.

La Fase 7 introduce la capacidad que falta para completar el ciclo del consultor: **dirigir la
recolección de información con una entrevista estratégica estructurada y 100 % determinista**,
donde Java decide qué preguntar, en qué orden, cuándo una respuesta es válida y cuándo la
entrevista está completa; el LLM únicamente formula la pregunta en lenguaje natural y comunica
el resultado del análisis.

---

## Objetivo

Convertir a KIN en un **entrevistador estratégico** que, antes de cualquier recomendación o
`ConsultingReport`, conduce una entrevista estructurada para recopilar toda la información
necesaria del proyecto.

**Java será responsable exclusivamente de**:

1. Decidir **qué información hace falta** (análisis de brechas contra el plan de entrevista).
2. Determinar el **orden de las preguntas** (secuencia determinista del blueprint).
3. Identificar la **información faltante** (preguntas obligatorias pendientes).
4. **Controlar el flujo completo** de la entrevista (estado por turno, durable entre turnos).
5. **Validar las respuestas** necesarias para continuar (`AnswerValidator` determinista).
6. Decidir **cuándo la entrevista está completa** (y solo entonces habilitar el `REPORT`).

**El LLM únicamente**:

- Formula la pregunta indicada por Java en lenguaje natural.
- Comunica el resultado del análisis producido por los motores.

Ninguna decisión de negocio dependerá del LLM.

---

## Responsabilidades

| Actor | Responsabilidad |
|-------|-----------------|
| `InterviewEngine` | Motor canonizado (`DomainEngine<InterviewInput, InterviewResult>`): evalúa el estado de la entrevista, valida la última respuesta, decide la siguiente pregunta o la completitud y produce `InterviewResult`. Decisión 100 % Java. |
| `InterviewBlueprint` | Plan de entrevista determinista definido en Java: secuencia de `InterviewQuestion` organizadas por `AnalyzedDimension`, con orden, obligatoriedad, reglas de validación y adaptaciones (follow-ups) por tipo de respuesta. |
| `AnswerValidator` | Validación determinista de respuestas: no vacía, longitud mínima, formato, palabras clave mínimas; decide aceptar/rechazar/refinar con motivo. Nunca consulta al LLM. |
| `InterviewState` | Progreso inmutable de la entrevista por proyecto: preguntas respondidas, pendientes, respuesta actual, `complete`; reconstruible desde `InterviewRepository`. |
| `InterviewRepository` (puerto) | Persistencia/lectura del estado de la entrevista por proyecto (`findOrCreate`/`save`); adaptador en infraestructura; no modifica `project_context`. |
| `InterviewStage` | Etapa aditiva de pipeline (composición pura sobre `EngineStage`): construye `InterviewInput` desde el `ProjectContext` y la respuesta del turno, invoca `InterviewEngine` y escribe `PipelineContext.interviewResult`. |
| `ConsultorStage` (cambio aditivo sancionado) | Lee `PipelineContext.interviewResult` para enmarcar el prompt: si hay pregunta de entrevista pendiente, el LLM la formula en lenguaje natural; si la entrevista está completa, procede el flujo REPORT habitual. |
| LLM | Formula en lenguaje natural la pregunta dictada por el `InterviewDirective`; comunica el reporte. No decide el flujo. |

---

## Componentes

### Componentes nuevos (`com.kinplatform.kin.interview` — dominio POJO puro)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `InterviewEngine` | Clase canonizada | Implementa `DomainEngine<InterviewInput, InterviewResult>`; fase `VALIDATION` (no se agrega `INTERVIEW`; ver nota de implementación al final), tipo `DOMAIN`; delega en `InterviewBlueprint` + `AnswerValidator`; degrada a `InterviewResult.empty()` si el input no es viable |
| `InterviewQuestion` | record puro | `id` determinista, `AnalyzedDimension` objetivo, `topic` (tópico semántico que el LLM debe formular), `required` (obligatoria), `order` (secuencia), `AnswerRules`, `followUps` (adaptaciones por tipo de respuesta) |
| `AnswerRules` | record puro | `minLength`, `minTokens`/`minKeywords`, `requiredFormat`, `allowRefinement` (¿se puede pedir más detalle?), `maxRefinements` |
| `InterviewBlueprint` | clase pura | Plan determinista: secuencia de `InterviewQuestion` por dimensión; construido con VOs de configuración (no hardcodeado en el motor); provee `next(state)` y `isComplete(state)` |
| `AnswerValidator` | clase pura | `validate(String answer, AnswerRules) → AnswerValidation`; reglas deterministas (vacío, longitud, formato, keywords); stateless y reentrante |
| `AnswerValidation` | record puro | `accepted`, `reason`, `requiresRefinement`, `refinementCount` |
| `InterviewDirective` | record puro | Directiva de la pregunta de entrevista: `questionId`, dimensión, `topic`, `AnswerRules` — consumida por la capa de prompt para que el LLM la formule |
| `InterviewState` | record/clase inmutable | Progreso por proyecto: `projectId`, `answeredQuestionIds`, `pendingQuestionIds`, `currentQuestion`, `refinements`, `complete`, `exchangeBudget`; restaurar desde `InterviewRepository` |
| `InterviewInput` | record (EngineInput) | `ProjectContext`, `userMessage` (respuesta del turno), `InterviewState` previo |
| `InterviewResult` | record (EngineResult) | `InterviewDirective` (si hay pregunta pendiente), `state`, `complete`, `decision` (ASK/REPORT), `empty()` |
| `InterviewRepository` | puerto de dominio | `findOrCreate(UUID projectId) → InterviewState` + `save(InterviewState)` |
| `InterviewAnswer` | record puro | Respuesta del turno: `questionId` objetivo, `text` del usuario, metadatos para `AnswerValidator`; construida por `InterviewStage` desde la pregunta pendiente |
| `InterviewContext` | record puro | Vista de contexto del proyecto para la entrevista: dimensiones cubiertas, cobertura, `ProjectContext`; insumo de `InterviewInput` |
| `InterviewProgress` | record puro | Progreso computable de la entrevista: responded/answered vs. pendientes, ratio de avance; derivado de `InterviewState` |
| `InterviewRequest` | record puro | Solicitud tipada de entrevista: `projectId`, respuesta del turno y estado previo; contenedor para `InterviewInput` |

### Cambios aditivos propuestos a contratos existentes (se sancionarán en E5)

| Contrato | Cambio propuesto | Tipo |
|----------|------------------|------|
| `EnginePhase` | ~~nuevo valor `INTERVIEW`~~ → **no se agrega**; `InterviewEngine` usa `VALIDATION` | Decisión de compatibilidad (ver nota de implementación al final) |
| `PipelineContext` | nuevo campo tipado `InterviewResult interviewResult` (+ getter/setter) | Aditivo (patrón ADR-011/014) |
| `PromptRequest.forConversation(...)` | overload aditivo que recibe `InterviewResult` (los factories de ADR-012/013 se conservan) | Aditivo |
| `ConversationPromptBuilder` | nueva sección `## ENTREVISTA ESTRATÉGICA` (tópico + reglas de la pregunta pendiente) solo cuando `request.interviewResult()` tiene directiva | Aditivo (precedente enmienda M2 ADR-013) |
| `ConsultorStage` | lectura aditiva de `PipelineContext.interviewResult` para enmarcar el prompt | Aditivo (precedente ADR-013 M3) |
| `KinConfig` | beans `AnswerValidator`, `InterviewBlueprint`, `InterviewRepository` (adaptador), `InterviewEngine`, `InterviewStage`; `InterviewStage` insertado en `chatPipeline(...)` | Cableado |

**Sin cambios**: `KinMethod`, `Pipeline`, `PipelineStage`, `ConversationOrchestrator`,
`TurnPolicy`/`DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, `PromptAssembler`,
`AIResponder`, `ReportEngine`, `ConsultingReport`, `KnowledgeEngine`, `KnowledgeGateway`,
`kin/engine`, `ProjectContext` (API de actualización congelada), `ConversationDecision` y la
infraestructura `kin/engine`.

---

## Límites

| Límite | Definición |
|--------|-----------|
| Flujo de decisión | La secuencia, validación y completitud de la entrevista son **100 % Java** (`InterviewBlueprint` + `AnswerValidator` + `InterviewEngine`). El LLM jamás decide qué preguntar ni cuándo terminar |
| Alcance | La entrevista recopila información del negocio para el análisis; no ejecuta análisis, no genera reporte, no decide viabilidad |
| Preguntas | Preguntas deterministas del blueprint; el LLM solo las formula en lenguaje natural (no inventa preguntas nuevas) |
| Persistencia | El estado de la entrevista vive en un almacén propio (`InterviewRepository`, tabla nueva `interview_state`); **no** modifica `project_context` ni tablas del core |
| Contratos | `KinMethod`, `Pipeline`, `PipelineStage`, `ConversationOrchestrator`, `PromptAssembler`, `AIResponder`, `ReportEngine`, `ConsultingReport`, `KnowledgeEngine`, `KnowledgeGateway`, `kin/engine`, `ProjectContext`, `ConversationDecision` **no cambian** (solo aditivos sancionados por este ADR) |
| Presupuesto | Presupuesto máximo de preguntas/refinamientos por entrevista (`InterviewState.exchangeBudget`) para evitar un interrogatorio sin fin; el usuario puede aportar información adicional libremente |
| LLM | El LLM recibe únicamente el prompt ensamblado por la capa de dominio (frontera ADR-012 intacta); la sección `## ENTREVISTA ESTRATÉGICA` consume solo datos de dominio (`InterviewDirective`), nunca texto crudo |

---

## Arquitectura

### 3.1 Principio rector

> **Java conduce la entrevista. Java valida las respuestas. Java decide cuándo está completa.**
> **El LLM únicamente formula las preguntas y comunica el resultado del análisis.**

Reglas derivadas (vinculantes una vez aprobado):

1. El orden, la validación, la completitud y el presupuesto de la entrevista son decisiones de
   Java (deterministas y testeables).
2. El LLM **nunca** decide qué preguntar, qué orden seguir, si una respuesta es válida ni cuándo
   terminar la entrevista.
3. El LLM recibe un `InterviewDirective` (datos de dominio) y lo formula en lenguaje natural;
   no recibe fuentes crudas ni decide sobre conocimiento (ADR-014) ni sobre el reporte
   (ADR-012).
4. La integración al pipeline es **aditiva** (patrón ADR-011/014, precedente ADR-013): campo
   tipado nuevo en `PipelineContext`, una etapa nueva (`InterviewStage`) y cableado en
   `KinConfig`. Ningún stage existente se modifica salvo la lectura aditiva de
   `ConsultorStage`/`ConversationPromptBuilder` sancionada por este ADR.
5. El estado de la entrevista es **durable** por proyecto vía un puerto (`InterviewRepository`);
   la infraestructura implementa el adaptador (nueva tabla `interview_state`).

### 3.2 Arquitectura (nuevo bounded context `com.kinplatform.kin.interview`)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         DOMINIO (kin.interview - POJO puro)                    │
│                                                                              │
│  InterviewEngine : DomainEngine<InterviewInput, InterviewResult>              │
│        │                                                                      │
│        ├──► InterviewBlueprint ──► InterviewQuestion / AnswerRules            │
│        ├──► AnswerValidator ──► AnswerValidation (reglas deterministas)       │
│        ├──► InterviewState (progreso inmutable por proyecto)                  │
│        └──► InterviewRepository (puerto de persistencia)                     │
│                                                                              │
│  InterviewInput / InterviewResult / InterviewDirective (records inmutables)   │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │  (implementación en infraestructura)
┌───────────────────────────────────▼──────────────────────────────────────────┐
│                         INFRAESTRUCTURA (adaptadores)                          │
│                                                                              │
│  JpaInterviewRepository / InterviewStateEntity  (tabla interview_state)      │
│                                                                              │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │  (cableado en KinConfig - E5)
                 Pipeline existente (aditivo: InterviewStage + interviewResult)
```

### 3.3 Posición en las capas

```
kin/interview   → Domain Layer (0 dependencias del proyecto; solo java.* y org.slf4j)
ai/…/adapter    → Infrastructure Layer (implementa InterviewRepository)
common/config   → Infrastructure Layer (KinConfig: cablea los beans)
kin.ai.prompt   → Domain Layer (sección aditiva ## ENTREVISTA ESTRATÉGICA, sancionada)
```

---

## UML conceptual

### 4.1 Diagrama de clases (diseño)

```
InterviewEngine implements DomainEngine<InterviewInput, InterviewResult>
  + evaluate(input: InterviewInput): InterviewResult
  + metadata(): EngineMetadata                (fase VALIDATION, tipo DOMAIN; no INTERVIEW)

InterviewBlueprint
  + next(state: InterviewState): Optional<InterviewQuestion>
  + isComplete(state: InterviewState): boolean
  + question(id): InterviewQuestion

AnswerValidator
  + validate(answer: String, rules: AnswerRules): AnswerValidation

InterviewRepository  <<puerto>>
  + findOrCreate(projectId: UUID): InterviewState
  + save(state: InterviewState): void

InterviewQuestion  (record)
  + id: String  + dimension: AnalyzedDimension  + topic: String
  + required: boolean  + order: int  + rules: AnswerRules  + followUps: Map<String, InterviewQuestion>

InterviewState  (inmutable)
  + projectId: UUID  + answered: Set<String>  + pending: List<String>
  + current: Optional<String>  + refinements: Map<String, Integer>
  + complete: boolean  + exchangeBudget: int

InterviewResult implements EngineResult
  + directive(): Optional<InterviewDirective>
  + state(): InterviewState  + complete(): boolean
  + decision(): ConversationDecision.Action   (ASK si hay pregunta pendiente; REPORT si completa)
```

### 4.2 Diagrama de secuencia (turno típico de entrevista)

```
Usuario ──► ConversationOrchestrator ──► KinMethod ──► Pipeline
                                                          │
                                   ┌──────────────────────┼──────────────────────┐
                                   │                      ▼                      │
                                   │        InterviewStage (nueva, aditiva)      │
                                   │        • carga InterviewState (repo)        │
                                   │        • valida respuesta (AnswerValidator) │
                                   │        • decide siguiente pregunta/completa │
                                   │        • escribe PipelineContext.interviewResult
                                   │                      ▼                      │
                                   │        ConsultorStage (lectura aditiva)     │
                                   │        • si pregunta pendiente → prompt
                                   │          con ## ENTREVISTA ESTRATÉGICA      │
                                   │        • si completa → flujo REPORT habitual│
                                   ▼                      │                      │
                            Scoring/Recommendation/…      ▼                      │
                                   │        ConversationPromptBuilder (aditivo)  │
                                   │                      ▼                      │
                                   ▼                  LLM (formula la pregunta   │
                                   │                  o comunica el reporte)     │
                                   ▼                                             │
                            Respuesta (pregunta o reporte) ◄─────────────────────┘
```

---

## Integración con el Pipeline

La integración sigue el **patrón aditivo sancionado por ADR-011/014** (y el precedente de
cambios aditivos de ADR-013):

1. **Nueva etapa `InterviewStage`** insertada entre `StrategistStage` y `KnowledgeStage`
   (antes de Scoring). Compone `EngineStage` → `InterviewEngine` (mismo patrón que
   `KnowledgeStage`): construye `InterviewInput` (proyecto + respuesta del turno + estado
   previo), invoca `InterviewEngine.evaluate(...)` y escribe
   `PipelineContext.interviewResult` (campo tipado aditivo).
2. **Gating de la transición REPORT**: cuando `InterviewResult.complete() == true`, el
   `InterviewResult.decision()` es `REPORT` y las etapas de análisis (Scoring,
   Recomendaciones, Riesgos, Oportunidades, Reporte) se ejecutan en ese mismo turno. Cuando la
   entrevista está incompleta, `InterviewResult.decision()` es `ASK` con la pregunta de
   entrevista pendiente (`InterviewDirective`), y las etapas de análisis se omiten por
   predicado (comportamiento actual preservado).
3. **Enmarcado del prompt (aditivo)**: `ConsultorStage` lee `PipelineContext.interviewResult`
   y pasa la directiva a `PromptRequest.forConversation(...)` mediante un overload aditivo;
   `ConversationPromptBuilder` emite la sección `## ENTREVISTA ESTRATÉGICA` con el tópico y las
   reglas de la pregunta pendiente, de modo que el LLM **solo formula** la pregunta dictada por
   Java. En modo completo el prompt de reporte (ADR-012) queda intacto.
4. **Durabilidad**: `InterviewStage` persiste el estado vía `InterviewRepository` al final de
   cada turno; el siguiente turno reconstruye el progreso (nunca se vuelve a preguntar lo
   respondido, garantizado en Java).
5. **Compatibilidad con el orquestador**: `ConversationOrchestrator` y `TurnPolicy` no cambian.
   La directiva de turno pre-pipeline sigue saliendo de `DefaultTurnPolicy` (fase
   EXPLORATION/REPORTING); la autoridad sobre la **siguiente pregunta de la entrevista** reside
   en `InterviewStage` (post-pipeline, dentro del pipeline), evitando tocar la política de
   turno congelada.
6. **Cableado**: `KinConfig` registra `AnswerValidator`, `InterviewBlueprint`,
   `InterviewRepository` (adaptador), `InterviewEngine` y `InterviewStage`, e inserta
   `InterviewStage` en `chatPipeline(...)` entre `StrategistStage` y `KnowledgeStage`.

---

## Compatibilidad con ADR-001…014

| ADR | Compatibilidad |
|-----|----------------|
| ADR-001 (reporting BC) | ✅ La entrevista alimenta el `ProjectContext` que consumen los motores de reporting; el bounded context no cambia |
| ADR-002 (pipeline context) | ✅ `PipelineContext.interviewResult` es un campo aditivo (mismo patrón ADR-011/014) |
| ADR-003 / 004 / 010 (recommendation/risk/opportunity) | ✅ Los motores reciben un contexto más completo; sus contratos intactos |
| ADR-005 (engine infrastructure) | ✅ `InterviewEngine` implementa `DomainEngine`; sin cambios en `kin/engine`; no se agrega fase `INTERVIEW` (el motor usa `VALIDATION`; ver nota de implementación) |
| ADR-006 (runtime) | ✅ `KinMethod` no cambia; `InterviewStage` se inserta en el mismo pipeline |
| ADR-007 (context repository) | ✅ `ProjectContext` (API de actualización) intacto; `InterviewRepository` es un puerto nuevo y separado |
| ADR-008 (AI responder) | ✅ `AIResponder` no cambia; la entrevista no pasa por el puerto de IA como decisión |
| ADR-009 (engine canonization) | ✅ `InterviewEngine` se canoniza bajo `DomainEngine` desde el diseño |
| ADR-010 (opportunity engine) | ✅ Las oportunidades se calculan sobre un contexto validado por la entrevista |
| ADR-011 (report engine) | ✅ `ReportEngine`/`ConsultingReport` no cambian; el reporte solo se genera cuando la entrevista está completa |
| ADR-012 (prompt assembler) | ✅ Frontera REPORT↔`ConsultingReport` intacta; el cambio en `ConversationPromptBuilder`/`PromptRequest` es **aditivo y sancionado por este ADR** (precedente enmienda M2 ADR-013); la sección ENTREVISTA consume solo `InterviewDirective` |
| ADR-013 (conversation orchestrator) | ✅ `ConversationOrchestrator`, `TurnPolicy`, `ResponseGuard`, `HistoryWindow` no cambian; la directiva de turno sigue siendo Java; `ConsultorStage` recibe una lectura aditiva (precedente M3) |
| ADR-014 (external knowledge) | ✅ `KnowledgeEngine`/`KnowledgeGateway`/`KnowledgeStage` no cambian; `InterviewStage` corre antes de `KnowledgeStage` y el conocimiento externo sigue enriqueciendo el análisis |

---

## Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Doble fuente de dirección (precedente ADR-013): `DefaultExplorationStrategy` y `InterviewEngine` coexisten | Media | `InterviewStage` es autoritativo cuando la entrevista está activa; `DefaultExplorationStrategy` queda como base compatible sin modificar; documentación clara de responsabilidades |
| R2 | Fricción con `CompletenessEvaluator.readyForReport()` (el gating de REPORT pasa a depender de la entrevista) | Media | El evaluador se conserva como insumo; la transición la decide `InterviewResult.decision()`; el `ProjectContext` ya recibe los datos vía `AnalysisResult` (sin tocar su API) |
| R3 | Crecimiento de `PipelineContext` (otro campo aditivo) | Baja | Mismo patrón ADR-011/014; monitoreo continuo |
| R4 | Persistencia del estado de entrevista (tabla nueva, versionado del JSON) | Media | Puerto + adaptador JPA; migración Flyway V4 (prod) / `ddl-auto` (dev); formato versionado |
| R5 | Complejidad del blueprint (validaciones adaptativas por respuesta) | Media | `InterviewBlueprint` como VO configurable con VOs de `AnswerRules`; determinismo y tests por pregunta |
| R6 | Violación accidental de la frontera ADR-012 (el prompt solo consume datos de dominio) | Media | La sección `## ENTREVISTA ESTRATÉGICA` consume únicamente `InterviewDirective`; cambio aditivo sancionado |
| R7 | Bucle/interrogatorio sin fin (frustración del usuario) | Media | Presupuesto de intercambios (`InterviewState.exchangeBudget`), regla de completitud alcanzable, refinamientos acotados (`AnswerRules.maxRefinements`) |
| R8 | Respuestas que el analizador heurístico no extrae como dimensión | Media | La validación usa `AnswerValidator` (Java) sobre la respuesta del turno; la extracción heurística existente se mantiene |
| R9 | El LLM no formula correctamente la pregunta dictada | Baja | La directiva incluye tópico y reglas; `ResponseGuard` (ADR-013) ya valida la comunicación (pregunta única, longitud) |

---

## Consecuencias

### Positivas

- **Entrevista real**: KIN actúa como consultor empresarial, recopilando el contexto completo
  con preguntas estructuradas y deterministas antes de analizar.
- **Java decide en la entrevista**: qué falta, en qué orden, qué es válido y cuándo terminar —
  decisiones auditables y testeables; la profundidad deja de depender del LLM.
- **Calidad del análisis**: `ScoringEngine`, recomendaciones, riesgos, oportunidades y reporte
  se calculan sobre un contexto validado y completo.
- **Aditividad total**: integración al pipeline por patrón ADR-011/014; contratos congelados
  intactos; `ConversationOrchestrator` y `KnowledgeEngine` no se tocan.
- **Durabilidad**: el estado de la entrevista se persiste por proyecto; "nunca vuelvo a
  preguntar lo respondido" queda garantizado en Java.
- **OCP**: nuevas preguntas/dimensiones = nuevos `InterviewQuestion` en el blueprint; el motor
  no cambia.

### Negativas

- **Crecimiento del dominio**: +1 bounded context, +1 motor canonizado, +1 puerto, +1 campo
  aditivo en `PipelineContext` y una sección aditiva en el prompt (complejidad adicional).
- **Doble mecanismo de exploración**: la entrevista coexiste con la exploración por prioridad
  existente; exige documentación clara para evitar regresión.
- **Costo de la entrevista**: más turnos hasta el reporte (mitigado con presupuesto y
  completitud alcanzable).
- **Nueva tabla de persistencia** y su migración (Flyway V4 en prod).

---

## Roadmap E1…E7

> Estado: **E1…E7 — COMPLETADAS** (diseño + implementación + cierre oficial de la Fase 7).
> ADR-015 **APROBADA** (2026-08-01).

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico: ADR-015 + documento FASE7 (arquitectura, componentes, flujo, integración, roadmap) | Documentación | ✅ **Completado (2026-08-01)** |
| **E2** | Modelo de dominio `kin.interview`: tipos (`InterviewQuestion`, `AnswerRules`, `AnswerValidation`, `InterviewDirective`, `InterviewState`, `InterviewInput`, `InterviewResult`) + `InterviewBlueprint` + `AnswerValidator` + puerto `InterviewRepository` + `InterviewEngine` canonizado (`DomainEngine`, fase `VALIDATION`) + tests de dominio | Código de dominio | ✅ **Completado** |
| **E3** | `AnswerValidator` determinista (vacío, longitud, formato, keywords, refinamientos acotados) + blueprint completo de las 14 dimensiones (orden, obligatoriedad, follow-ups) + lógica de adaptación por tipo de respuesta + tests | Validación y blueprint | ✅ **Completado** |
| **E4** | Adaptador `InterviewRepository` (persistencia durable del estado; tabla `interview_state`; Flyway V4 en prod / `ddl-auto` en dev) + tests | Persistencia | ✅ **Completado** |
| **E5** | Integración aditiva al pipeline: `InterviewStage` + `PipelineContext.interviewResult` + cableado en `KinConfig` + tests de integración | Integración pipeline | ✅ **Completado** |
| **E6** | Cierre del flujo completo: gating de REPORT por completitud de la entrevista, persistencia entre turnos, presupuesto de intercambios, sección `## ENTREVISTA ESTRATÉGICA` en el prompt | Flujo completo | ✅ **Completado** |
| **E7** | Auditoría de cierre: ADR-015 → **Aprobado**, validación de contratos congelados intactos, `./mvnw clean verify` (BUILD SUCCESS, tests verdes), cobertura `kin.interview` ≥ 90 % (JaCoCo), cierre oficial de la Fase 7 | Cierre de fase | ✅ **Completado** |

---

## Criterios de aceptación

- [x] ADR-015 en estado **Aprobado** (E1…E7 completadas — diseño + implementación + cierre oficial).
- [x] `kin.interview` es 100 % POJO (sin Spring, sin JPA, solo `java.*`/`org.slf4j`).
- [x] `InterviewEngine` implementa `DomainEngine` (fase `VALIDATION` — decisión de compatibilidad documentada, no `INTERVIEW`; tipo `DOMAIN`).
- [x] Java decide: qué información falta, orden de preguntas, validación de respuestas, control del flujo y completitud.
- [x] El LLM únicamente formula las preguntas dictadas por `InterviewDirective` y comunica el resultado; ninguna decisión de negocio depende del LLM.
- [x] Contratos congelados (`KinMethod`, `Pipeline`, `PipelineStage`, `ConsultorStage`, `PromptAssembler`, `AIResponder`, `ConversationOrchestrator`, `TurnPolicy`, `ResponseGuard`, `HistoryWindow`, `ReportEngine`, `ConsultingReport`, `KnowledgeEngine`, `KnowledgeGateway`, `kin/engine`, `ProjectContext`, `ConversationDecision`) **sin cambios** — solo aditivos sancionados por este ADR.
- [x] Integración al pipeline **aditiva** (patrón ADR-011/014): `PipelineContext.interviewResult` + `InterviewStage` + cableado en `KinConfig`.
- [x] Estado de entrevista durable vía `InterviewRepository` (puerto) con adaptador de infraestructura y tabla propia.
- [x] (Cierre, E7) `./mvnw clean verify` → **BUILD SUCCESS**; tests verdes; cobertura de dominio ≥ 90 % (JaCoCo) en `kin.interview`.

---

## Estado

**APROBADO** — **Fase 7 cerrada oficialmente** (2026-08-01). E1…E7 **COMPLETADAS**:
diseño, implementación de dominio, validación/blueprint, persistencia, integración aditiva al
pipeline, cierre del flujo completo (gating de REPORT por completitud, persistencia entre turnos,
sección `## ENTREVISTA ESTRATÉGICA`) y auditoría de cierre. `./mvnw clean verify` → **BUILD
SUCCESS** (902 tests verdes, 0 fallos/errores/omitidos); cobertura `kin.interview` ≥ 90 %
(JaCoCo). Contratos congelados verificados intactos. Commit: `docs(kin): complete strategic
interview engine (phase 7)`.

**Nota de implementación — `EnginePhase`**: el `InterviewEngine` declara la fase
`EnginePhase.VALIDATION` en lugar del valor aditivo `INTERVIEW` previsto en este ADR. La decisión
mantiene intacto el contrato congelado `kin/engine` y preserva la compatibilidad con **ADR-013
(Conversation Orchestrator)** y **ADR-014 (External Knowledge)**, que no dependen de una fase de
entrevista propia. Esta nota documental no modifica código ni contratos. Las decisiones de este
ADR están **activas** desde su aprobación.

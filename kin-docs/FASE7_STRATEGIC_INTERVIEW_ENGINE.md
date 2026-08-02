# FASE 7 - Strategic Interview Engine (Business Interview Engine)

> **Estado**: **CERRADA OFICIALMENTE (Etapas E1…E7 COMPLETADAS)** — 2026-08-01. E1…E7
> completados (diseño + implementación + cierre de fase).
> **Base**: KIN 2.0 Alpha 1 (`v2.0.0-alpha1`) - `ALPHA STABLE` (release oficial, commit `89b39b9`,
> enmendada por ADR-014 / Fase 6, commit `fdf8ad4`, y por ADR-015 / Fase 7).
> **ADRs**: 015 (strategic interview engine) - **Estado: Aprobado** (Fase 7 implementada y cerrada).
>
> Secuencia oficial del proyecto: Fase 5.6 → Conversation Orchestrator (CERRADA), Fase 6 →
> KnowledgeEngine + RAG (CERRADA), **Fase 7 → Strategic Interview Engine (CERRADA)**.

---

## 1. Objetivo general

Antes de emitir recomendaciones o generar un `ConsultingReport`, KIN conducirá una **entrevista
estratégica estructurada** para recopilar toda la información necesaria del proyecto mediante
preguntas inteligentes y deterministas definidas por Java.

El objetivo es que KIN actúe como un **consultor empresarial real**: guía al usuario a través de
una conversación estratégica para comprender completamente el contexto del negocio **antes** de
realizar cualquier análisis.

**Java será responsable exclusivamente de**:

1. Decidir **qué información hace falta**.
2. Determinar el **orden de las preguntas**.
3. Identificar la **información faltante**.
4. **Controlar el flujo completo** de la entrevista.
5. **Validar las respuestas** necesarias para continuar.
6. Decidir **cuándo la entrevista está completa**.

**El LLM únicamente** formula las preguntas en lenguaje natural y comunica las respuestas
generadas por Java. **Ninguna decisión de negocio dependerá del LLM.**

---

## 2. Motivación

### 2.1 Brechas de la exploración actual

Hoy la recopilación de información se resuelve con una exploración ad-hoc por prioridad
(`DefaultExplorationStrategy`): se elige la dimensión faltante con mayor prioridad y se hace una
pregunta genérica. La auditoría arquitectónica identifica:

| # | Brecha | Impacto |
|---|--------|---------|
| 1 | Sin plan estructurado de entrevista (una pregunta genérica por dimensión) | El consultor no profundiza como un consultor real |
| 2 | Sin validación de respuestas en Java (una respuesta vacía cubre la dimensión) | El análisis puede alimentarse de datos superficiales |
| 3 | Sin control del flujo ni presupuesto de preguntas | Sin garantía de completitud ni de salida |
| 4 | Sin estado durable de la entrevista | El progreso depende de `dimensionsCovered` (contrato congelado) |
| 5 | La profundidad la decide el LLM ("profundizá con una pregunta específica") | Viola *Java decide / LLM únicamente comunica* |

### 2.2 Por qué ahora

Las fases 5.6 (Conversation Orchestrator) y 6 (External Knowledge) están **cerradas
oficialmente** y el núcleo está estable (`v2.0.0-alpha1`). El baseline §7.5 recomendó consumir el
`ProjectContext` durable, el turno tipado (`TurnResult`/`TurnDirective`) y el `KnowledgeResult`
como puntos de extensión para la Fase 7 — **sin tocar los contratos estables**. La entrevista
estratégica es el eslabón que completa el ciclo del consultor: **recopilar → validar → analizar
→ comunicar**, con la recopilación dirigida por Java.

### 2.3 Valor esperado

- Análisis (scoring, recomendaciones, riesgos, oportunidades, reporte) sobre un contexto
  **completo y validado**.
- Comportamiento de consultor senior: preguntas estructuradas, profundización determinista y
  cierre claro de la entrevista.
- Principio *Java decide* extendido a la fase de recopilación; el LLM no decide nada.

---

## 3. Arquitectura

### 3.1 Principio rector

> **Java conduce la entrevista. Java valida las respuestas. Java decide cuándo está completa.**
> **El LLM únicamente formula las preguntas y comunica el resultado del análisis.**

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

### 3.3 Posición en las capas (Clean Architecture + DDD + Ports & Adapters)

| Capa | Paquete | Contenido |
|------|---------|-----------|
| Dominio | `com.kinplatform.kin.interview` | `InterviewEngine`, `InterviewBlueprint`, `InterviewQuestion`, `AnswerRules`, `AnswerValidator`, `AnswerValidation`, `InterviewDirective`, `InterviewState`, `InterviewInput`, `InterviewResult`, puerto `InterviewRepository` — 100 % POJO, sin Spring, sin JPA |
| Dominio (aditivo) | `com.kinplatform.kin.ai.prompt` | `ConversationPromptBuilder` + sección aditiva `## ENTREVISTA ESTRATÉGICA` (sancionada por ADR-015) |
| Aplicación | `com.kinplatform.ai.*` | Sin cambios (los servicios de aplicación orquestan, no implementan reglas de entrevista) |
| Infraestructura | `com.kinplatform.ai.interview.adapter` (nuevo) | `JpaInterviewRepository`/`InterviewStateEntity` (tabla `interview_state`) |
| Infraestructura | `com.kinplatform.common.config` | `KinConfig`: cablea `AnswerValidator`, `InterviewBlueprint`, `InterviewRepository`, `InterviewEngine`, `InterviewStage` |

Dependencias unidireccionales: `kin.interview` no depende de nada del proyecto; la
infraestructura implementa los puertos; la aplicación no contiene lógica de entrevista.

---

## 4. Componentes

### 4.1 Dominio (`kin.interview`)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `InterviewEngine` | Motor canonizado | `DomainEngine<InterviewInput, InterviewResult>`; fase `VALIDATION` (no se agrega `INTERVIEW`; decisión de compatibilidad), tipo `DOMAIN`; delega en `InterviewBlueprint` + `AnswerValidator`; degrada a `InterviewResult.empty()` (nunca lanza por datos insuficientes) |
| `InterviewBlueprint` | Clase pura | Plan determinista de la entrevista: secuencia de `InterviewQuestion` por dimensión, obligatoriedad, orden y follow-ups; `next(state)` / `isComplete(state)` |
| `InterviewQuestion` | record puro | `id`, `AnalyzedDimension`, `topic`, `required`, `order`, `AnswerRules`, `followUps` |
| `AnswerRules` | record puro | `minLength`, `minKeywords`, `requiredFormat`, `allowRefinement`, `maxRefinements` |
| `AnswerValidator` | Clase pura | `validate(String, AnswerRules) → AnswerValidation`; determinista, stateless, reentrante; nunca consulta al LLM |
| `AnswerValidation` | record puro | `accepted`, `reason`, `requiresRefinement`, `refinementCount` |
| `InterviewDirective` | record puro | Pregunta de entrevista para el LLM: `questionId`, dimensión, `topic`, `AnswerRules` |
| `InterviewState` | Clase inmutable | Progreso por proyecto: `projectId`, `answered`, `pending`, `current`, `refinements`, `complete`, `exchangeBudget` |
| `InterviewInput` | record (`EngineInput`) | `ProjectContext`, `userMessage`, `InterviewState` previo |
| `InterviewResult` | record (`EngineResult`) | `InterviewDirective` (opcional), `state`, `complete`, `decision` (ASK/REPORT), `empty()` |
| `InterviewRepository` | puerto | `findOrCreate(UUID) → InterviewState` + `save(InterviewState)` |

### 4.2 Infraestructura

| Componente | Responsabilidad |
|------------|-----------------|
| `JpaInterviewRepository` / `InterviewStateEntity` | Persistencia durable del estado de entrevista en tabla nueva `interview_state` (Flyway V4 en prod, `ddl-auto` en dev); no modifica `project_context` |

### 4.3 Cambios aditivos sancionados por ADR-015

| Contrato | Cambio | Tipo |
|----------|--------|------|
| `EnginePhase` | ~~valor `INTERVIEW`~~ → **no se agrega**; `InterviewEngine` usa `VALIDATION` | Decisión de compatibilidad (ADR-013/ADR-014) |
| `PipelineContext` | campo `InterviewResult interviewResult` | Aditivo (patrón ADR-011/014) |
| `PromptRequest.forConversation(...)` | overload aditivo con `InterviewResult` | Aditivo |
| `ConversationPromptBuilder` | sección `## ENTREVISTA ESTRATÉGICA` (solo con directiva) | Aditivo (precedente ADR-013 M2) |
| `ConsultorStage` | lectura aditiva de `interviewResult` | Aditivo (precedente ADR-013 M3) |
| `KinConfig` | beans nuevos + `InterviewStage` en `chatPipeline(...)` | Cableado |

---

## 5. Flujo completo

### 5.1 Turno típico de entrevista (incompleta)

1. El usuario envía un mensaje; `ConversationOrchestrator` (sin cambios) resuelve la directiva de
   turno pre-pipeline (`DefaultTurnPolicy`) y delega en `KinMethod`.
2. El pipeline ejecuta: `Analizador → Evaluador → Estratega`.
3. **`InterviewStage` (nueva, aditiva)**:
   - Carga el `InterviewState` del proyecto (`InterviewRepository`).
   - Valida la respuesta del turno contra las `AnswerRules` de la pregunta pendiente
     (`AnswerValidator`); registra aceptaciones y refinamientos.
   - Decide en Java la siguiente `InterviewQuestion` del blueprint (o la completitud).
   - Escribe `PipelineContext.interviewResult` (campo aditivo).
4. `ConsultorStage` (lectura aditiva) detecta la pregunta pendiente y construye el prompt
   `## ENTREVISTA ESTRATÉGICA` con el `InterviewDirective`.
5. El LLM **formula en lenguaje natural** la pregunta dictada por Java.
6. `ResponseGuard` valida la comunicación (pregunta única, longitud) — ADR-013 intacto.
7. `InterviewStage` persiste el estado; el turno termina con `TurnResult` (pregunta formulada).

### 5.2 Turno de cierre (entrevista completa)

1. Mismo arranque (pasos 1-2).
2. `InterviewStage` valida la última respuesta, decide `complete = true` y
   `InterviewResult.decision() = REPORT`.
3. Las etapas de análisis se ejecutan en el mismo turno: `Conocimiento → Scoring →
   Recomendaciones → Riesgos → Oportunidades → Reporte`.
4. `ConsultorStage` selecciona `PromptRequest.forReport(consultingReport)` (frontera ADR-012
   intacta); el LLM comunica el `ConsultingReport`.
5. `InterviewStage` persiste el estado completo (la entrevista no se reabre).

### 5.3 Garantías

- **Nunca se repite una pregunta respondida** (garantizado en Java vía `InterviewState`).
- **Nunca se genera un reporte antes de completar la entrevista** (gating de REPORT por
  completitud).
- **El LLM nunca decide** qué preguntar, en qué orden, si una respuesta es válida ni cuándo
  terminar.

---

## 6. Integración con el Pipeline

### 6.1 Posición del stage (implementada)

```
Pipeline antes de Fase 7 (11 etapas):
Analizador → Evaluador → Estratega → Conocimiento → Scoring → Recomendaciones → Riesgos →
Oportunidades → Reporte → Consultor → Eventos

Pipeline actual con Fase 7 (12 etapas — aditivo, implementado):
Analizador → Evaluador → Estratega → ENTREVISTA → Conocimiento → Scoring → Recomendaciones →
Riesgos → Oportunidades → Reporte → Consultor → Eventos
```

`InterviewStage` se insertó entre `StrategistStage` y `KnowledgeStage` (mismo patrón aditivo de
`KnowledgeStage` en ADR-014). Ningún stage existente se elimina ni se reordena.

### 6.2 Patrón aditivo (ADR-011/014)

- `PipelineContext.interviewResult` (campo tipado nuevo, getter/setter).
- `InterviewStage` = composición pura sobre `EngineStage` → `InterviewEngine`.
- Cableado en `KinConfig.chatPipeline(...)`.
- `ConsultorStage` y `ConversationPromptBuilder` reciben **cambios aditivos sancionados por
  ADR-015** (lectura de `interviewResult` y sección `## ENTREVISTA ESTRATÉGICA`).

### 6.3 Comportamiento por decisión

| `InterviewResult` | Decisión | Etapas de análisis | LLM |
|-------------------|----------|---------------------|-----|
| Incompleta (hay pregunta pendiente) | `ASK` | Omitidas (predicado actual) | Formula la pregunta de la entrevista |
| Completa | `REPORT` | Se ejecutan en el turno | Comunica el `ConsultingReport` |

### 6.4 Compatibilidad con el orquestador

`ConversationOrchestrator`, `TurnPolicy`/`DefaultTurnPolicy`, `ResponseGuard` y `HistoryWindow`
**no cambian**. La directiva de turno pre-pipeline sigue siendo Java (ADR-013); la autoridad
sobre la siguiente pregunta de la entrevista reside en `InterviewStage` (dentro del pipeline),
sin tocar la política de turno congelada.

---

## 7. Principios

1. **Java decide. El LLM únicamente comunica.** Ninguna decisión de negocio dependerá del LLM;
   la entrevista lo extiende a la fase de recopilación.
2. **Aditividad**: la Fase 7 se integra por campos/stages/beans aditivos (patrón ADR-011/014);
   ningún contrato congelado se rompe sin ADR aprobada.
3. **Dominio puro**: `kin.interview` es 100 % POJO (sin Spring, sin JPA; solo `java.*` y
   `org.slf4j`).
4. **Ports & Adapters**: el dominio define `InterviewRepository`; la infraestructura lo
   implementa (tabla propia).
5. **Determinismo**: orden, validación y completitud son reglas de Java, auditables y testeables
   (sin LLM en la decisión).
6. **Durabilidad**: el progreso de la entrevista se persiste por proyecto; nunca se repite lo
   respondido.
7. **Presupuesto**: número máximo de preguntas/refinamientos por entrevista (evita el
   interrogatorio sin fin).
8. **Compatibilidad**: compatible con ADR-001…014 y con el baseline `v2.0.0-alpha1` (ALPHA
   STABLE). El reporte solo se genera con la entrevista completa.

---

## 8. Responsabilidades

| Actor | Responsabilidad |
|-------|-----------------|
| `InterviewEngine` | Decidir en Java qué falta, la siguiente pregunta, la completitud y la transición ASK/REPORT |
| `InterviewBlueprint` | Definir el plan de preguntas (orden, obligatoriedad, follow-ups) por dimensión |
| `AnswerValidator` | Validar respuestas en Java (vacío, longitud, formato, keywords, refinamientos) |
| `InterviewState` | Mantener el progreso inmutable de la entrevista por proyecto |
| `InterviewRepository` | Persistir/reconstruir el estado (puerto; adaptador en infraestructura) |
| `InterviewStage` | Integrar la entrevista al pipeline (composición pura sobre `EngineStage`) |
| `ConsultorStage` + `ConversationPromptBuilder` | Enmarcar el prompt con la directiva de la entrevista (aditivo) |
| LLM | Formular la pregunta dictada por Java en lenguaje natural y comunicar el reporte |

---

## 9. Roadmap E1…E7

> **E1…E7 — COMPLETADOS (diseño + implementación + cierre oficial de la Fase 7).**

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico (ADR-015 + este documento) | Documentación | ✅ **Completado (2026-08-01)** |
| **E2** | Modelo de dominio `kin.interview`: tipos + `InterviewBlueprint` + `AnswerValidator` + puerto `InterviewRepository` + `InterviewEngine` canonizado (fase `VALIDATION`) + tests | Código de dominio | ✅ **Completado** |
| **E3** | `AnswerValidator` determinista + blueprint completo de las 14 dimensiones (orden, obligatoriedad, follow-ups) + adaptación por respuesta + tests | Validación y blueprint | ✅ **Completado** |
| **E4** | Adaptador `InterviewRepository` (persistencia durable; tabla `interview_state`; Flyway V4 / `ddl-auto`) + tests | Persistencia | ✅ **Completado** |
| **E5** | Integración aditiva al pipeline: `InterviewStage` + `PipelineContext.interviewResult` + `KinConfig` + tests | Integración pipeline | ✅ **Completado** |
| **E6** | Cierre del flujo completo: gating de REPORT por completitud, persistencia entre turnos, presupuesto de intercambios, sección `## ENTREVISTA ESTRATÉGICA` | Flujo completo | ✅ **Completado** |
| **E7** | Auditoría de cierre: ADR-015 → **Aprobado**, contratos congelados intactos, `./mvnw clean verify` (BUILD SUCCESS), cobertura `kin.interview` ≥ 90 % (JaCoCo), cierre oficial de la Fase 7 | Cierre de fase | ✅ **Completado** |

> **Bitácora**: E1…E7 se ejecutaron bajo instrucciones explícitas, respetando las prohibiciones
> de la fase (sin tocar contratos congelados; el LLM nunca decide; dominio POJO puro sin Spring/JPA;
> integración aditiva por patrón ADR-011/014). El cierre (E7) es **solo documentación**: no se
> añadió funcionalidad fuera de lo ya implementado en E2…E6.

---

## 10. Criterios de aceptación (Fase 7)

- [x] ADR-015 **Aprobada** al cierre de la fase.
- [x] `kin.interview` es 100 % POJO (sin Spring, sin JPA; solo `java.*`/`org.slf4j`).
- [x] `InterviewEngine` implementa `DomainEngine<InterviewInput, InterviewResult>` (fase `VALIDATION`, tipo `DOMAIN`).
- [x] Java decide: qué falta, orden, validación, completitud y presupuesto; el LLM solo formula.
- [x] Contratos congelados (`KinMethod`, `Pipeline`, `PipelineStage`, `ConversationOrchestrator`,
      `PromptAssembler`, `AIResponder`, `ReportEngine`, `ConsultingReport`, `KnowledgeEngine`,
      `KnowledgeGateway`, `kin/engine`, `ProjectContext`, `ConversationDecision`) **sin cambios**.
- [x] Integración al pipeline **aditiva** (patrón ADR-011/014): `PipelineContext.interviewResult` +
      `InterviewStage` + cableado en `KinConfig`.
- [x] Estado de entrevista durable vía `InterviewRepository` (puerto) + adaptador + tabla `interview_state`.
- [x] Gating de REPORT por completitud: mientras la entrevista está incompleta la decisión es `ASK`
      y las etapas de análisis se omiten; solo con `InterviewResult.complete() == true` se habilita
      el `REPORT`.
- [x] `./mvnw clean verify` → **BUILD SUCCESS**; **902 tests verdes** (0 fallos, 0 errores, 0 skipped).
- [x] Cobertura de dominio ≥ 90 % (JaCoCo) en `kin.interview` (**98.72 %** agregado dominio+adaptador).
- [x] Persistencia entre turnos: nunca se repite una pregunta respondida (garantizado en Java).

---

## 11. Límites

| Límite | Definición |
|--------|-----------|
| Flujo de decisión | Secuencia, validación y completitud 100 % Java; el LLM jamás decide |
| Alcance | La entrevista solo recopila contexto; no analiza ni genera reporte |
| Preguntas | Deterministas del blueprint; el LLM solo las formula |
| Persistencia | `InterviewRepository`/tabla `interview_state`; no toca `project_context` |
| Contratos | Sin cambios a contratos congelados; solo aditivos sancionados por ADR-015 |
| Presupuesto | Máximo de preguntas/refinamientos; el usuario puede aportar información libremente |
| LLM | Recibe solo el prompt ensamblado (frontera ADR-012); la sección ENTREVISTA consume solo `InterviewDirective` |

---

## 12. Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Doble fuente de dirección (`DefaultExplorationStrategy` + `InterviewEngine`) | Media | `InterviewStage` autoritativo cuando la entrevista está activa; `DefaultExplorationStrategy` intacto |
| R2 | Fricción con `CompletenessEvaluator.readyForReport()` | Media | El gating lo decide `InterviewResult.decision()`; el evaluador queda como insumo |
| R3 | Crecimiento de `PipelineContext` | Baja | Patrón ADR-011/014; monitoreo |
| R4 | Persistencia nueva (tabla `interview_state`) | Media | Puerto + adaptador; Flyway V4; JSON versionado |
| R5 | Complejidad del blueprint adaptativo | Media | VOs configurables; tests por pregunta |
| R6 | Violación de la frontera ADR-012 | Media | Cambio aditivo sancionado; solo datos de dominio en el prompt |
| R7 | Bucle/interrogatorio sin fin | Media | `exchangeBudget` + refinamientos acotados + completitud alcanzable |
| R8 | Extracción heurística incompleta | Media | `AnswerValidator` valida la respuesta directa; la extracción existente se mantiene |
| R9 | LLM no formula bien la pregunta | Baja | Directiva explícita; `ResponseGuard` (ADR-013) |

---

## 13. Estado del entregable (Fase 7)

| Etapa | Estado |
|-------|--------|
| **E1 — Diseño arquitectónico (ADR-015 + este documento)** | ✅ **Completado** |
| **E2 — Modelo de dominio** | ✅ **Completado** |
| **E3 — Validación y blueprint** | ✅ **Completado** |
| **E4 — Persistencia** | ✅ **Completado** |
| **E5 — Integración pipeline** | ✅ **Completado** |
| **E6 — Flujo completo** | ✅ **Completado** |
| **E7 — Auditoría de cierre** | ✅ **Completado** |

**FASE 7 CERRADA OFICIALMENTE. ADR-015 APROBADO. E1…E7 COMPLETADAS.** `./mvnw clean verify` →
**BUILD SUCCESS** (902 tests verdes); cobertura `kin.interview` ≥ 90 % (JaCoCo); contratos
congelados verificados intactos.

**Entregables de la Fase 7**:
- `kin-docs/adr/ADR-015-strategic-interview-engine.md` (Aprobado)
- `kin-docs/FASE7_STRATEGIC_INTERVIEW_ENGINE.md` (diseño + roadmap E1…E7, cerrado)
- Dominio `com.kinplatform.kin.interview` (POJO puro) + adaptador `com.kinplatform.ai.interview.adapter`
- Migración `V4__create_interview_state.sql` (prod) + `init.sql` (tabla `interview_state`)

**Integración (aditiva, sancionada por ADR-015)**: `InterviewStage` entre `StrategistStage` y
`KnowledgeStage` (pipeline de 12 etapas), `PipelineContext.interviewResult`, beans en `KinConfig`,
sección `## ENTREVISTA ESTRATÉGICA` en `ConversationPromptBuilder` y lectura aditiva en
`ConsultorStage`/`PromptAssembler`.

**Principio vigente desde la aprobación**:
> **Java decide. El LLM únicamente formula preguntas.**
> Java decide qué falta, en qué orden, qué es válido y cuándo terminar; el LLM solo formula la
> pregunta dictada por `InterviewDirective` y comunica el resultado del análisis.

**Nota de implementación — `EnginePhase`**: el `InterviewEngine` declara la fase
`EnginePhase.VALIDATION` en lugar del valor aditivo `INTERVIEW` previsto en este documento. La
decisión mantiene intacto el contrato congelado `kin/engine` y preserva la compatibilidad con
**ADR-013 (Conversation Orchestrator)** y **ADR-014 (External Knowledge)**, que no dependen de una
fase de entrevista propia. Esta nota documental no modifica código ni contratos.

*Fase 7 — Strategic Interview Engine. E1…E7 completadas y cerradas oficialmente. Ningún contrato
congelado de `BASELINE_ARCHITECTURE.md` se modificó sin una ADR aprobada.*

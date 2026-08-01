# ADR-013: Conversation Orchestrator — director de turno que decide en Java y delega la comunicación al LLM

**Estado**: Aprobado (Fase 5.6 — implementado E1…E6, cerrado E7)
**Fecha**: 2026-07-31
**Autor**: KIN Architecture Team

> **Cierre (E7, 2026-07-31)**: la auditoría independiente de la Fase 5.6 dictaminó
> **FASE 5.6 APROBADA PARA CIERRE** e incorporó tres enmiendas documentales (M1,
> M2, M3) detalladas en la sección «Enmiendas de la auditoría de cierre (E7)».

---

## Contexto

La plataforma ha alcanzado `ARCHITECTURE STABLE` en `kin/engine` (ADR-006…009),
`kin/reporting/opportunity` (ADR-010), `kin/reporting/report` (ADR-011) y
`kin.ai.prompt` (ADR-012). El principio rector establecido en ADR-012 es:

> **Java decide. El LLM únicamente comunica.**

Hasta hoy el principio se cumple en la **línea de análisis**: la viabilidad
(análisis de dimensiones, scoring, recomendaciones, riesgos, oportunidades y el
`ConsultingReport`) se calcula con motores deterministas en Java. El LLM solo
redacta texto sobre el resultado.

Sin embargo, la **capa de conversación** (el ciclo de turnos entre el usuario y
el consultor virtual) no tiene un orquestador de dominio propio. La auditoría de
la Fase 5.6 detecta:

| # | Hallazgo | Evidencia |
|---|---|---|
| 1 | No existe una máquina de estados conversacional explícita. La fase se infiere ad-hoc (`shouldGenerateReport()`, `reportGenerated`) sin un tipo `ConversationPhase`. | `ConversationDecision.java`, `ProjectContext.java` |
| 2 | El `ConversationDecision.Action` declara 7 acciones (ASK, REPORT, RECOMMEND, VALIDATE, SUMMARIZE, STOP, ESCALATE) pero solo ASK/REPORT/STOP tienen orquestación real. RECOMMEND/VALIDATE/SUMMARIZE/ESCALATE no están dirigidas. | `ConversationDecision.java` |
| 3 | La salida del LLM es un `String` crudo (`PipelineContext.aiResponse`) sin validación de dominio: ni longitud, ni unicidad de pregunta, ni marcadores prohibidos. | `PipelineContext.java`, `ConsultorStage.java` |
| 4 | El historial que se envía al LLM es el historial completo persistido, sin ventana de contexto; el presupuesto de tokens crece sin cota. | `ChatOrchestratorServiceImpl.java` |
| 5 | No existe un "turno" tipado como objeto de dominio (input/output), lo que dificulta auditoría, tests y versionado del ciclo conversacional. | `KinMethodCommand.java`, `ChatOrchestratorServiceImpl.java` |
| 6 | El conocimiento del ciclo de vida conversacional está disperso entre `ConversationStrategist`, `EventStage` y el orquestador HTTP, sin una política única de turno. | `ConversationStrategist.java`, `EventStage.java` |
| 7 | No hay guardrails sobre la respuesta del LLM: nada garantiza que "comunique dentro de la directiva" (pregunta única, sin marcadores de reporte en fase exploratoria, longitud acotada). | `ConsultorStage.java` |

El problema no es el pipeline: la ejecución de análisis (10 etapas deterministas)
es correcta y está congelada. El problema es que el **ciclo de conversación no
está dirigido**: nadie en el dominio emite una directiva de turno, nadie valida
la comunicación y nadie tipifica el resultado del turno.

---

## Decisión

Se introduce un **Conversation Orchestrator** de dominio en el nuevo paquete
`com.kinplatform.kin.conversation`, como fachada única del ciclo de conversación.
El orchestrator:

1. **Acota el contexto antes del pipeline** (`HistoryWindow`): Java decide qué
   fragmento del historial ve el LLM, con presupuesto determinista (número de
   mensajes) — nunca conteo de tokens de proveedor.
2. **Emite la directiva de turno en Java** (`TurnPolicy`): fase, modo de
   comunicación, restricciones (longitud, pregunta única, marcadores prohibidos)
   a partir del `ProjectContext` persistido y de la decisión previa. La acción
   puntual de transición (ASK/REPORT) la sigue decidiendo `ConversationStrategist`
   dentro del pipeline; la directiva **enmarca la comunicación** y la decisión
   **selecciona la transición**.
3. **Delega la ejecución al pipeline existente** (`KinMethod.execute` /
   `executeStream`), pasando la directiva de forma aditiva. `KinMethod` sigue
   siendo la entrada del pipeline (contrato congelado intacto).
4. **Valida la comunicación después del pipeline** (`ResponseGuard`): Java juzga
   la respuesta del LLM contra la directiva. Nunca se parsean decisiones desde
   texto del LLM; ante una respuesta que viola la directiva, Java decide el
   fallback (respuesta enlatada y/o marcado `ResponseValidation.accepted=false`).
5. **Devuelve un turno tipado** (`TurnResult`): proyecto, decisión, directiva,
   respuesta, validación, reporte y eventos — objeto de dominio auditable y
   testeable.

### Cambios aditivos sancionados

Siguiendo el precedente de ADR-011 (campos tipados en `PipelineContext`), la
integración es **exclusivamente aditiva**:

| Contrato | Cambio | Tipo |
|---|---|---|
| `KinMethodCommand` | nuevo campo `TurnDirective directive` vía overload (el constructor original se conserva con `null`) | Aditivo |
| `PipelineContext` | nuevos campos `turnDirective` y `responseValidation` | Aditivo |
| `PromptRequest.forConversation(...)` | overload que recibe `TurnDirective` (el factory original de ADR-012 se conserva) | Aditivo |
| `ConsultorStage` | lee la directiva para enmarcar el prompt; en streaming aplica `ResponseGuard` (`attachStreamGuard`) y deja `responseValidation` en contexto — en bloqueante el guard lo aplica el orquestador (enmienda M3) | Aditivo (sancionado) |
| `ConversationPromptBuilder` | nueva sección `## DIRECTIVA DE COMUNICACIÓN` (fase, modo, restricciones) solo cuando `request.directive() != null`; el factory original y la frontera REPORT intactos | Aditivo (sancionado por enmienda M2) |
| `ChatOrchestratorServiceImpl` | delega en `ConversationOrchestrator` en lugar de llamar directamente a `KinMethod` | Cambio de aplicación (no de contrato) |
| `KinConfig` | registra `DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, `ConversationOrchestrator` | Aditivo |

### Componentes nuevos (`kin.conversation`)

| Tipo | Naturaleza | Responsabilidad |
|---|---|---|
| `ConversationPhase` | enum `EXPLORATION`, `REPORTING`, `CLOSED` | Estado de fase del ciclo de conversación |
| `CommunicationMode` | enum `QUESTION`, `EXPLAIN_REPORT`, `SUMMARY`, `FAREWELL` | Qué debe comunicar el LLM en el turno |
| `TurnConstraints` | record puro | `maxLength`, `singleQuestion`, `forbiddenMarkers` |
| `ConversationTurn` | record puro | Input tipado del turno (proyecto, usuario, mensaje, historial) |
| `TurnDirective` | record puro | Política del turno: fase, acción, dimensión, modo, restricciones |
| `ResponseValidation` | record puro | `accepted`, `issues` |
| `TurnResult` | record puro | Output tipado del turno |
| `TurnPolicy` | interfaz de dominio | Contrato `decide(ProjectContext, ConversationDecision) → TurnDirective` |
| `DefaultTurnPolicy` | POJO | Implementación determinista de la política de turno |
| `ResponseGuard` | clase pura | `validate(String, TurnDirective) → ResponseValidation` |
| `HistoryWindow` | clase pura | `window(List<Message>, int) → List<Message>` |
| `ConversationOrchestrator` | POJO de dominio | Fachada: `orchestrate(...)`, `orchestrateStream(...)` |

Todas las clases nuevas son deterministas (POJOs), igual que el resto del dominio
`kin.*`: sin Spring en su interior, inyectadas desde `KinConfig`.

---

## Alternativas consideradas

1. **Extender `ConversationStrategist` sin orquestador** — rechazado: mantiene
   el ciclo de vida implícito y disperso; no tipifica el turno ni valida la
   comunicación.
2. **Nueva etapa de pipeline (`OrchestratorStage`)** — rechazado: el pipeline es
   ejecución de análisis (10 etapas deterministas); el ciclo de conversación es
   transversal (antes y después del pipeline, con validación sobre la salida del
   LLM) y no encaja como etapa interna.
3. **Parsear decisiones del texto del LLM (guardrails por regex)** — rechazado:
   reintroduce indeterminismo y viola "Java decide"; el texto solo se valida como
   comunicación, nunca como fuente de decisión.
4. **Orquestador en la capa de aplicación (`chat`)** — rechazado: la política de
   turno y los guardrails son regla de negocio y deben vivir en dominio puro,
   testeable sin infraestructura.
5. **Reemplazar `KinMethod` por el orquestador** — rechazado: `KinMethod` es un
   contrato estable del baseline; el orquestador lo compone, no lo sustituye.
6. **No validar la respuesta (confiar en el LLM)** — rechazado: sin guardrails no
   hay auditoría ni determinismo de la comunicación.

---

## Consecuencias

### Positivas

- **Java decide en la conversación**: fase, directiva, presupuesto de contexto y
  aceptación de la respuesta son decisiones deterministas y auditables.
- **Turno tipado**: `TurnResult` habilita auditoría, versionado y tests del ciclo
  conversacional sin acoplar a HTTP ni a streaming.
- **Guardrails**: `ResponseGuard` impide que el LLM comunique fuera de directiva
  (preguntas múltiples, marcadores de reporte en fase exploratoria, respuestas
  vacías o desmedidas).
- **Contexto acotado**: `HistoryWindow` protege la ventana del LLM en Java, sin
  depender de tokenizadores de proveedor.
- **Cobertura total de acciones**: la directiva cubre las 7 acciones de
  `ConversationDecision` (modos QUESTION/EXPLAIN_REPORT/SUMMARY/FAREWELL),
  cerrando el hueco de RECOMMEND/VALIDATE/SUMMARIZE/ESCALATE.
- **Sustrato para Fase 6 (KnowledgeEngine/RAG)**: el turno tipado y la directiva
  dan el punto de extensión para contexto semántico.

### Negativas

- **Crecimiento del dominio**: +1 paquete, +11 tipos nuevos y +2 campos aditivos
  en `PipelineContext` (mismo patrón que ADR-011).
- **Doble fuente de dirección**: `TurnPolicy` (fase/modo) y `ConversationStrategist`
  (acción) coexisten; exige documentación clara de responsabilidades para evitar
  regresión a un solo mecanismo.
- **Costo del guard en streaming**: en modo SSE la validación es posterior
  (no bloquea los chunks), por lo que el fallback en streaming se limita a
  marcar `responseValidation` — documentado y aceptado.

---

## Regla que modifica

- **KIN_ARCHITECTURE_GOVERNANCE**: regla "El LLM solo comunica" — ahora se
  formaliza con un componente de dominio (`kin.conversation`) que dirige y valida
  la comunicación.
- **BASELINE_ARCHITECTURE §4.1**: cambios aditivos a `KinMethodCommand` y
  `PipelineContext` (campos tipados nuevos), bajo la cláusula de ADR-011.
- **ADR-012**: overload aditivo de `PromptRequest.forConversation(context,
  decision, directive)` y sección `## DIRECTIVA DE COMUNICACIÓN` en
  `ConversationPromptBuilder` (enmienda M2); el factory original y la frontera
  REPORT↔`ConsultingReport` permanecen intactos.

---

## Enmiendas de la auditoría de cierre (E7)

Auditoría independiente de la Fase 5.6 (2026-07-31): **FASE 5.6 APROBADA PARA
CIERRE** con tres enmiendas documentales incorporadas en E7.

### M1 — Directiva del turno de generación de reporte (comportamiento aprobado)

El `ConversationOrchestrator` resuelve la directiva ANTES del pipeline a partir de
la decisión previa persistida (`ProjectContext.currentDecision()`, §Decisión 2).
En el primer turno que genera el `ConsultingReport`, la decisión previa es una
acción de exploración (típicamente `ASK`) y el reporte aún no existe; por tanto la
directiva inicial es `(EXPLORATION, ASK, QUESTION)`. El pipeline, en cambio,
produce la transición `REPORT` y `ConsultorStage` selecciona
`PromptRequest.forReport(consultingReport)` (frontera ADR-012 intacta).

**Comportamiento aprobado**: la directiva del turno de generación se deriva de la
decisión previa (mecanismo pre-pipeline); la comunicación del reporte se enmarca
con esa directiva. En los turnos posteriores a la generación, la decisión previa
es `REPORT` y la directiva resulta `(REPORTING, REPORT, EXPLAIN_REPORT)`. El
`ResponseGuard` podrá marcar la explicación del primer turno de reporte como
`accepted=false` al evaluarla contra las restricciones de la directiva derivada
(modo QUESTION); la validación es un artefacto de auditoría y no se consume en
producción en esta fase (KIN 2.1+ definirá el consumidor).

### M2 — Sanción aditiva de `ConversationPromptBuilder`

Durante E6 se modificó `ConversationPromptBuilder` de forma **aditiva** (frontera
ADR-012): nueva sección `## DIRECTIVA DE COMUNICACIÓN` que emite fase, modo y
restricciones únicamente cuando `request.directive() != null`. El factory original
`forConversation(context, decision)` y la frontera REPORT↔`ConsultingReport`
permanecen intactos. Esta enmienda sanciona explícitamente el cambio, que la
cláusula original de «Cumplimiento» omitía.

### M3 — Responsabilidad del `ResponseGuard` (bloqueante vs. streaming)

- **Modo bloqueante**: el `ResponseGuard` es responsabilidad del
  `ConversationOrchestrator` — valida `result.aiResponse()` contra la directiva y
  emite `ResponseValidation` en el `TurnResult`.
- **Modo streaming**: la responsabilidad reside en `ConsultorStage`
  (`attachStreamGuard`) — envuelve el `Flux` de tokens y deja `ResponseValidation`
  en `PipelineContext.responseValidation` al completar, sin romper chunks.
- `ConsultorStage` NO aplica el guard en modo bloqueante; el orquestador es
  autoritativo en ese flujo.

---

## Cumplimiento

No se modifican: API REST (`/chat`, `/chat/stream`), contratos SSE/eventos,
frontend, `AIResponder`, `AIRequest`, `AiEngineService`, `ProviderRouter`,
`PromptAssembler`/`ReportPromptBuilder`, `ReportEngine`/`ConsultingReport`/10
`SectionAssembler`, `DomainEngine`/`EngineExecutor`/`EngineRegistry`/
`DeterministicId`, `ScoringEngine`, `ConversationDecision`,
`ConversationStrategist` ni el contrato de `KinMethod` (aunque su ejecución
interna deja de ser el punto de entrada directo del orquestador HTTP).
`ConversationPromptBuilder` SÍ fue modificado de forma aditiva durante E6
(sección `## DIRECTIVA DE COMUNICACIÓN`) — sancionado por la enmienda M2.

Los 338 tests existentes permanecen verdes sin modificación de aserciones (468
tests en el cierre de la fase); los cambios aditivos añaden pruebas nuevas en
`kin.conversation` y pruebas aditivas en `kin.ai`, `kin.ai.prompt` y
`pipeline.stage`.

# FASE 5.6 — Conversation Orchestrator

> **Estado**: **Implementado y cerrado (E1…E7)** — **FASE 5.6 CERRADA OFICIALMENTE** (2026-07-31)
> **Base**: Fase 5.5 (ADR-012) — `ARCHITECTURE STABLE` (enmendado por ADR-001…ADR-013)
> **ADRs**: 013 (conversation orchestrator) — **Estado: Aprobado**
> **Secuencia oficial del proyecto**: Fase 5.4 → ReportEngine · Fase 5.5 → PromptAssembler · **Fase 5.6 → Conversation Orchestrator** · Fase 6 → KnowledgeEngine + RAG
>
> Las secciones 1…11 son el **diseño arquitectónico congelado** (Etapa 1) que rigió la
> implementación (E1…E6). El cierre de la fase (E7) añadió la sección 14 con las
> enmiendas de la auditoría (M1/M2/M3) y el resultado final de la implementación.

---

## 1. Objetivo

Centralizar la orquestación del **ciclo de conversación** en dominio puro,
aplicando el principio rector heredado de ADR-012 a la capa conversacional:

> **Java decide. El LLM únicamente comunica.**

Hoy la ejecución de análisis está canonizada (ADR-006…011) y la construcción de
prompts está canonizada (ADR-012), pero el **turno conversacional no está
dirigido**: la fase se infiere ad-hoc, la salida del LLM no se valida, el
historial viaja sin ventana y no existe un objeto tipado de turno. Esta fase
introduce el **Conversation Orchestrator** (`kin.conversation`), una fachada de
dominio que decide la directiva de comunicación en Java, acota el contexto que ve
el LLM, valida la respuesta contra la directiva y devuelve un turno tipado.

**Alcance**: solo el diseño arquitectónico (ADR-013 + este documento). No se
escribe código, no se modifica repositorio, no se hacen commits. La
implementación (E1…E7) vendrá en una fase posterior, como se hizo en 5.5.

---

## 2. Auditoría del estado actual

Fuentes leídas (solo lectura, sin modificación).

### 2.1 Componentes auditados

| Componente | Ubicación | Rol actual | Hallazgo |
|---|---|---|---|
| `KinMethod` | `com.kinplatform.kin` | Entrada única del pipeline (`execute`/`executeStream`); usa `Pipeline`, `DomainEventBus`, `ContextRepository` | Correcto y congelado. Javadoc menciona el orden viejo de etapas. |
| `KinMethodCommand` | `com.kinplatform.kin` | Record de entrada: `projectId, userId, userMessage, history, projectTitle, projectDescription, projectCategory` | Input sin directiva de turno: el pipeline no recibe política conversacional. |
| `PipelineContext` | `com.kinplatform.kin.pipeline` | Contexto mutable del pipeline: evaluación, decisión, `aiResponse`, resultados de motores, `consultingReport`, eventos | Campo `aiResponse` es `String` crudo; no hay `turnDirective` ni `responseValidation`. |
| `ConversationDecision` | `com.kinplatform.kin.decision` | Record con `Action {ASK, REPORT, RECOMMEND, VALIDATE, SUMMARIZE, STOP, ESCALATE}` + factories `ask/generateReport/stop` | Declara 7 acciones pero solo ASK/REPORT/STOP tienen orquestación real. |
| `ChatOrchestratorServiceImpl` | `com.kinplatform.chat` | Orquestador HTTP/SSE (I/O puro): persiste mensajes, emite SSE, llama a `KinMethod` | Sin lógica de negocio (correcto por 5.2.1), pero es el punto donde el ciclo conversacional queda "huérfano". |
| `ProjectContext` | `com.kinplatform.kin.context` | Contexto durable: `data` EnumMap, `dimensionsCovered`, `currentDecision`, `exchangeCount`, `reportGenerated` | Persiste estado pero no hay fase explícita; `reportGenerated` es booleano, no `ConversationPhase`. |
| `ConversationStrategist` | `com.kinplatform.kin.context.strategy` | `decide(context, evaluation)` delega en `ExplorationStrategy` (default + sectoriales) | Decide la acción (transición) correctamente; no decide fase/modo/restricciones de comunicación. |
| `EventStage` | `com.kinplatform.kin.pipeline.stage` | Emite `QuestionGeneratedEvent`, `ReportGeneratedEvent`, `ScoreCalculatedEvent`, `ConversationCompletedEvent` | Conoce el ciclo de vida implícito; sin directiva tipada para enriquecer eventos. |
| `ContextRepository` | `com.kinplatform.kin.context` | Port: `findOrCreate`, `find`, `save`, `delete` (JSON en `project_context`) | Listo como fuente de estado para `TurnPolicy`. |
| `PromptAssembler` + builders | `kin.ai` / `kin.ai.prompt` | Façade pura; CONVERSATION vía `ConversationPromptBuilder`; REPORT solo consume `ConsultingReport` (ADR-012) | Listo; el overload aditivo de `forConversation(context, decision, directive)` es la extensión. |

### 2.2 Problemas detectados

| # | Problema | Severidad |
|---|---|---|
| P1 | No existe máquina de estados conversacional: la fase se infiere con `shouldGenerateReport()`/`reportGenerated` sin un tipo `ConversationPhase`. | Alta |
| P2 | La salida del LLM (`aiResponse`) no se valida contra ninguna directiva: longitud, pregunta única, marcadores prohibidos, vacío. | Alta |
| P3 | El historial enviado al LLM es el historial completo persistido: presupuesto de contexto sin cota. | Media |
| P4 | No hay objeto tipado de "turno" (input/output de dominio), dificultando auditoría, tests y versionado. | Media |
| P5 | Las acciones RECOMMEND/VALIDATE/SUMMARIZE/ESCALATE de `ConversationDecision.Action` no tienen modo de comunicación asociado. | Media |
| P6 | El conocimiento del ciclo de vida conversacional está disperso entre `ConversationStrategist`, `EventStage` y `ChatOrchestratorServiceImpl`. | Media |
| P7 | No hay guardrails: nada garantiza que el LLM "comunique dentro de la directiva". | Alta |

---

## 3. Diseño arquitectónico

### 3.1 Principio rector

`ConversationPhase`, `CommunicationMode`, `TurnConstraints`, la política de turno
(`TurnPolicy`), el guardrail (`ResponseGuard`) y la ventana de contexto
(`HistoryWindow`) son **decisiones 100 % Java**. El LLM solo produce texto; ese
texto se enmarca (directiva) y se valida (guardrail) en el dominio. **Nunca se
parsean decisiones desde texto del LLM.**

### 3.2 Arquitectura (nuevo paquete `com.kinplatform.kin.conversation`)

```
                         ┌───────────────────────────────────────────┐
                         │       ConversationOrchestrator (POJO)      │
                         │  fachada única del ciclo de conversación   │
                         └──────┬──────────────────────┬──────────────┘
                                │                      │
                  decide en Java│                      │valida en Java
                                ▼                      ▼
              ┌───────────────────────────┐  ┌────────────────────────┐
              │         TurnPolicy        │  │      ResponseGuard     │
              │  decide(ctx, prevDecision)│  │ validate(response,     │
              │        → TurnDirective    │  │        directive)      │
              └─────────────┬─────────────┘  └────────────────────────┘
                            │ lectura de estado
                            ▼
              ┌───────────────────────────┐  ┌────────────────────────┐
              │   ProjectContext (port)   │  │     HistoryWindow      │
              │   ContextRepository       │  │ window(history, max)   │
              └───────────────────────────┘  └────────────────────────┘
                            │ delega ejecución (contrato congelado)
                            ▼
              ┌───────────────────────────────────────────────────────┐
              │  KinMethod.execute/executeStream (Pipeline 10 etapas) │
              │  Analizador→Evaluador→Estratega→Scoring→Recomendaciones│
              │  →Riesgos→Oportunidades→Reporte→Consultor→Eventos     │
              └───────────────────────────────────────────────────────┘
```

### 3.3 Flujo del turno (resumen)

1. `ChatOrchestratorServiceImpl` recibe la petición HTTP/SSE (I/O puro) y delega
   en `ConversationOrchestrator`.
2. `HistoryWindow.window(history, maxMessages)` acota el historial antes de tocar
   el LLM (Java decide el presupuesto; nunca tokenizador de proveedor).
3. `TurnPolicy.decide(projectContext, currentDecision)` produce la
   `TurnDirective` (fase + acción + dimensión + modo + restricciones).
4. La directiva viaja aditivamente (`KinMethodCommand.directive`) al pipeline.
   El pipeline decide la transición real (`ConversationStrategist`) y construye
   el prompt con `PromptRequest.forConversation(context, decision, directive)`.
   En REPORT, `ConsultorStage` selecciona `forReport` (frontera ADR-012 intacta).
5. `ResponseGuard.validate(aiResponse, directive)` emite `ResponseValidation`.
   Si no se acepta, Java decide el fallback (respuesta enlatada + marca
   `accepted=false`; en streaming se marca en contexto, sin romper chunks).
6. Se devuelve un `TurnResult` tipado (proyecto, decisión, directiva, respuesta,
   validación, reporte, eventos) para persistencia y eventos.

---

## 4. Responsabilidades

| Componente | Responsabilidad | Regla |
|---|---|---|
| `ConversationPhase` (enum) | `EXPLORATION`, `REPORTING`, `CLOSED` | Puro, inmutable |
| `CommunicationMode` (enum) | `QUESTION`, `EXPLAIN_REPORT`, `SUMMARY`, `FAREWELL` | Puro, inmutable |
| `TurnConstraints` (record) | `maxLength`, `singleQuestion`, `forbiddenMarkers`; factories `question()` / `reportExplanation()` | Puro, inmutable |
| `ConversationTurn` (record) | Input tipado del turno (proyecto, usuario, mensaje, historial, título, descripción, categoría) | Puro, inmutable |
| `TurnDirective` (record) | `phase`, `action`, `dimension`, `communicationMode`, `constraints` | Puro, inmutable |
| `ResponseValidation` (record) | `accepted`, `issues` | Puro, inmutable |
| `TurnResult` (record) | `projectContext`, `decision`, `directive`, `aiResponse`, `validation`, `consultingReport`, `events` | Puro, inmutable |
| `TurnPolicy` (interface) | Contrato `decide(ProjectContext, ConversationDecision) → TurnDirective` | Dominio (port) |
| `DefaultTurnPolicy` (POJO) | Resolución determinista de fase, modo y restricciones; reusa `ConversationStrategist` para la acción | Dominio puro |
| `ResponseGuard` (clase pura) | `validate(String, TurnDirective) → ResponseValidation`; reglas: vacío, longitud, pregunta única, marcadores prohibidos | Dominio puro |
| `HistoryWindow` (clase pura) | `window(List<Message>, int) → List<Message>`; conserva mensajes más recientes, siempre incluye el mensaje del usuario | Dominio puro |
| `ConversationOrchestrator` (POJO) | Fachada: `orchestrate(ConversationTurn) → TurnResult` y `orchestrateStream(ConversationTurn) → Flux<String>`; compone HistoryWindow + TurnPolicy + KinMethod + ResponseGuard | Dominio puro (sin Spring) |

### 4.1 Frontera de pureza

- **Decisiones de dominio**: fase, modo, restricciones, presupuesto de contexto,
  validación → Java (`kin.conversation`).
- **Comunicación**: texto generado por el LLM, siempre enmarcado por la directiva
  y validado por `ResponseGuard`.
- **I/O**: `ChatOrchestratorServiceImpl` (HTTP/SSE), `ChatService` (persistencia),
  `JpaContextRepository` (durable) → fuera del dominio, como hoy.
- **Prohibición explícita**: el `ResponseGuard` nunca infiere intención ni decisión
  del texto; solo evalúa conformidad de comunicación contra la directiva.

---

## 5. UML

### 5.1 Componentes (nivel paquete)

```
com.kinplatform.kin.conversation
├── ConversationPhase            (enum)
├── CommunicationMode            (enum)
├── TurnConstraints              (record)
├── ConversationTurn             (record)
├── TurnDirective                (record)
├── ResponseValidation           (record)
├── TurnResult                   (record)
├── TurnPolicy                   (interface)
├── DefaultTurnPolicy            (POJO)
├── ResponseGuard                (clase pura)
├── HistoryWindow                (clase pura)
└── ConversationOrchestrator     (POJO)
```

### 5.2 Clases (diagrama de clases)

```
                ┌──────────────────────────────┐
                │    ConversationOrchestrator  │
                ├──────────────────────────────┤
                │ - historyWindow: HistoryWindow│
                │ - turnPolicy: TurnPolicy      │
                │ - kinMethod: KinMethod        │
                │ - guard: ResponseGuard        │
                │ - contextRepository: ContextRepository │
                ├──────────────────────────────┤
                │ + orchestrate(ConversationTurn) → TurnResult │
                │ + orchestrateStream(ConversationTurn) → Flux<String> │
                └──────┬─────────┬─────────┬─────────┬────────┘
                       │         │         │         │
          <<uses>>     │         │         │         │ <<uses>>
                       ▼         ▼         ▼         ▼
              ┌───────────┐ ┌─────────┐ ┌─────────┐ ┌────────────┐
              │TurnPolicy │ │History  │ │Response │ │  KinMethod │
              │  (iface)  │ │ Window  │ │  Guard  │ │(congelado) │
              └─────┬─────┘ └─────────┘ └─────────┘ └────────────┘
                    │ <<implements>>
                    ▼
              ┌─────────────────────────────┐
              │ DefaultTurnPolicy           │
              ├─────────────────────────────┤
              │ + decide(ProjectContext,    │
              │   ConversationDecision)     │
              │   → TurnDirective           │
              └───────┬─────────────────────┘
                      │ <<reusa>>
                      ▼
              ┌─────────────────────────────┐
              │ ConversationStrategist      │  (existente, intacto)
              │ decide → ConversationDecision│
              └─────────────────────────────┘
```

### 5.3 Tipos de datos (records de dominio)

```
TurnDirective(phase: ConversationPhase,
              action: ConversationDecision.Action,
              dimension: AnalyzedDimension | null,
              communicationMode: CommunicationMode,
              constraints: TurnConstraints)

TurnResult(projectContext: ProjectContext,
           decision: ConversationDecision,
           directive: TurnDirective,
           aiResponse: String,
           validation: ResponseValidation,
           consultingReport: ConsultingReport | null,
           events: List<DomainEvent>)

ResponseValidation(accepted: boolean, issues: List<String>)
```

---

## 6. Flujo completo

### 6.1 Flujo CONVERSATION (modo pregunta, fase EXPLORATION)

```
Cliente ──POST /chat──► ChatOrchestratorServiceImpl
                          │
                          ▼
                    ConversationOrchestrator.orchestrate(turn)
                          │ 1. HistoryWindow.window(history, 20)   → history acotado
                          │ 2. ContextRepository.findOrCreate(projectId) → ctx
                          │ 3. TurnPolicy.decide(ctx, ctx.currentDecision()) → directive(EXPLORATION, ASK, QUESTION)
                          │ 4. KinMethodCommand(+directive, history acotado)
                          ▼
                    KinMethod.execute(command)
                          │ Pipeline 10 etapas (motores Java deterministas)
                          │  Estratega: ConversationStrategist.decide → Action.ASK
                          │  Consultor: PromptRequest.forConversation(ctx, decision, directive)
                          │             → PromptAssembler → AIResponder → LLM → aiResponse
                          ▼
                    ResponseGuard.validate(aiResponse, directive) → ResponseValidation
                          │ 6. aiResponse válida (pregunta única, sin marcadores, longitud ok)
                          ▼
                    TurnResult(projectContext, decision, directive, aiResponse, validation, report=null, events)
                          ▼
                    ChatOrchestratorServiceImpl: persistir mensajes + emitir eventos → respuesta HTTP
```

### 6.2 Flujo REPORT (fase REPORTING)

```
Cliente ──POST /chat──► ChatOrchestratorServiceImpl
                          ▼
                    ConversationOrchestrator.orchestrate(turn)
                          │ 1..3 igual; TurnPolicy.decide → directive(REPORTING, REPORT, EXPLAIN_REPORT)
                          │       (en el PRIMER turno que genera el reporte la directiva es la de la
                          │        decisión previa, p. ej. (EXPLORATION, ASK, QUESTION) — ver enmienda M1)
                          ▼
                    KinMethod.execute(command)
                          │ Pipeline: Evaluador→…→ ReportStage (ReportEngine → ConsultingReport)
                          │ Consultor: decision.shouldGenerateReport() → PromptRequest.forReport(consultingReport)  ← frontera ADR-012
                          │            → PromptAssembler → AIResponder → LLM → aiResponse (explicación del reporte)
                          ▼
                    ResponseGuard.validate(aiResponse, directive) → ResponseValidation
                          ▼
                    TurnResult(projectContext, decision, directive, aiResponse, validation, consultingReport, events)
                          ▼
                    ChatOrchestratorServiceImpl: persistir + eventos (ReportGeneratedEvent) → respuesta HTTP
```

> **Enmienda M1 (E7)**: en el turno que genera por primera vez el `ConsultingReport`,
> `TurnPolicy.decide` se ejecuta ANTES del pipeline sobre la decisión previa
> persistida (típicamente `ASK`), por lo que la directiva inicial es
> `(EXPLORATION, ASK, QUESTION)` y no `(REPORTING, REPORT, EXPLAIN_REPORT)`. El
> pipeline produce `REPORT` y `ConsultorStage` usa `forReport` (frontera ADR-012
> intacta). En los turnos posteriores a la generación, la decisión previa es
> `REPORT` y la directiva resulta `(REPORTING, REPORT, EXPLAIN_REPORT)`. Ver ADR-013 §M1.

### 6.3 Flujo STREAM (`/chat/stream`)

```
Cliente ──POST /chat/stream──► ChatOrchestratorServiceImpl
                                  │
                                  ▼
                            ConversationOrchestrator.orchestrateStream(turn) → Flux<String>
                                  │ 1..3: mismo prefijo de decisión Java (HistoryWindow, TurnPolicy, command)
                                  │ 4: KinMethod.executeStream → PipelineContext.aiResponseFlux (Flux<String>)
                                  │ 5: subscription del orquestador → SSE al cliente
                                  │ 6: al completar, ResponseGuard valida la concatenación;
                                  │     si no se acepta → responseValidation marcado en contexto (sin romper chunks)
```

---

## 7. Contratos (interfaces y records nuevos)

### 7.1 `TurnPolicy` (interfaz de dominio)

```java
public interface TurnPolicy {
    TurnDirective decide(ProjectContext context, ConversationDecision previousDecision);
}
```

### 7.2 `ConversationOrchestrator` (fachada)

```java
public class ConversationOrchestrator {
    public TurnResult orchestrate(ConversationTurn turn);
    public Flux<String> orchestrateStream(ConversationTurn turn);
}
```

### 7.3 `ResponseGuard` (reglas deterministas)

```java
public final class ResponseGuard {
    public ResponseValidation validate(String response, TurnDirective directive);
}
```

Reglas (orden de evaluación):
1. **Vacío/blank** → issue `response.empty`.
2. **Longitud** > `directive.constraints().maxLength()` → issue `response.too_long`.
3. **Pregunta única**: si `constraints.singleQuestion()`, contar `?` > 1 → issue `response.multiple_questions`.
4. **Marcadores prohibidos**: si la respuesta contiene `forbiddenMarkers`
   (p. ej. `=== CONSULTING REPORT ===`, `## INFORME DE VIABILIDAD`, `Scoring:`) en
   fase no-REPORTING → issue `response.forbidden_marker`.
5. `accepted = issues.isEmpty()`.

### 7.4 `HistoryWindow` (presupuesto de contexto)

```java
public final class HistoryWindow {
    public List<Message> window(List<Message> history, int maxMessages);
}
```

- Conserva los últimos `maxMessages` mensajes (default 20).
- Siempre incluye el mensaje del usuario del turno actual.
- Presupuesto determinista por **número de mensajes** (independiente del
  tokenizador del proveedor).

### 7.5 Cambios aditivos a contratos existentes

```java
// KinMethodCommand (nuevo campo opcional vía overload)
public record KinMethodCommand(
    UUID projectId, UUID userId, String userMessage,
    List<Message> history, String projectTitle,
    String projectDescription, String projectCategory,
    TurnDirective directive          // null en el constructor original
) {
    public KinMethodCommand { ... }  // overload histórico → directive = null
}

// PipelineContext (2 campos aditivos, patrón ADR-011)
private TurnDirective turnDirective;
private ResponseValidation responseValidation;

// PromptAssembler / PromptRequest (overload aditivo de ADR-012)
public static PromptRequest forConversation(ProjectContext context,
                                            ConversationDecision decision,
                                            TurnDirective directive);
```

---

## 8. Roadmap E1…E7

| Etapa | Alcance | Verificación | Estado (E7) |
|---|---|---|---|
| **E1** | Tipos base `kin.conversation`: enums + records (`ConversationPhase`, `CommunicationMode`, `TurnConstraints`, `ConversationTurn`, `TurnDirective`, `ResponseValidation`, `TurnResult`) | Tests unitarios de records/factories | ✅ 8 tipos + validaciones null; 9 clases de test |
| **E2** | `HistoryWindow` (presupuesto por número de mensajes, usuario siempre presente) | Tests unitarios | ✅ default 20, inmutable; `HistoryWindowTest` (17) |
| **E3** | `ResponseGuard` (5 reglas + tolerancia) | Tests unitarios (tabla de casos) | ✅ 5 reglas en orden §7.3; `ResponseGuardTest` (21) |
| **E4** | `TurnPolicy` + `DefaultTurnPolicy` (resolución fase/modo/restricciones, reúso de `ConversationStrategist`) | Tests unitarios | ✅ mapeo exhaustivo 7 acciones → 4 modos; `DefaultTurnPolicyTest` (22) |
| **E5** | `ConversationOrchestrator` (composición + delegación a `KinMethod` + `TurnResult`) | Tests con mocks (`mockConstruction` de `KinMethod`) | ✅ fachada 5 deps, `orchestrate` + `orchestrateStream`; `ConversationOrchestratorTest` (24) + integración (3) |
| **E6** | Integración aditiva: `KinMethodCommand.directive`, `PipelineContext.turnDirective`/`responseValidation`, overload `PromptRequest.forConversation`, `ConsultorStage` consume directiva y aplica guard, beans en `KinConfig` | Tests de stage + `KinMethodTest` (ArgumentCaptor) | ✅ + `ConversationPromptBuilder` (sección DIRECTIVA, enmienda M2); tests aditivos en `kin.ai`/`pipeline.stage` |
| **E7** | Documentación: ADR-013 → Aprobado, actualizar AGENTS.md/BASELINE/GOVERNANCE/CHANGELOG; `./mvnw clean verify` + JaCoCo ≥90 % en `kin.conversation` | 338 tests previos verdes + nuevos | ✅ **FASE 5.6 CERRADA OFICIALMENTE** — 468 tests, BUILD SUCCESS, `kin.conversation` 100 % (738/738) |

---

## 9. Riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| R1: Duplicidad de decisión entre `TurnPolicy` (fase/modo) y `ConversationStrategist` (acción) | Media | Confusión de responsabilidades | Contrato explícito: `TurnPolicy` enmarca la comunicación; `ConversationStrategist` selecciona la transición; documentado en ADR-013 |
| R2: Crecimiento de `PipelineContext` (+2 campos) | Alta | Contexto mutable más grande | Patrón ya sancionado por ADR-011; campos solo lectura para stages |
| R3: Guard en streaming no bloquea chunks (validación posterior) | Media | Fallback limitado en SSE | Aceptado y documentado; el orquestador marca `responseValidation` en contexto |
| R4: Ventana de contexto puede perder información del historial | Media | Pérdida de matices del usuario | `maxMessages` configurable (default 20); usuario siempre presente; KnowledgeEngine (Fase 6) como evolución |
| R5: Cambios aditivos a contratos congelados (`KinMethodCommand`, `PromptRequest`) | Baja | Riesgo de romper baseline | Solo overloads; constructores históricos intactos; 338 tests previos deben seguir verdes sin tocar aserciones |
| R6: Regresión a parseo de texto del LLM | Baja | Violación del principio rector | Prohibición explícita en ADR-013 + sección 4.1; `ResponseGuard` solo evalúa conformidad |

---

## 10. Compatibilidad

| Contrato congelado | Impacto de Fase 5.6 |
|---|---|
| API REST `/chat` y `/chat/stream` | Sin cambios (misma respuesta/serie de eventos) |
| Contratos SSE/eventos (`QuestionGeneratedEvent`, `ReportGeneratedEvent`, `ScoreCalculatedEvent`, `ConversationCompletedEvent`) | Sin cambios de forma |
| `KinMethod.execute` / `executeStream` | Sin cambios de firma; el orquestador lo compone |
| `Pipeline` / `PipelineStage` / 10 etapas | Sin cambios; solo lectura de campos aditivos |
| `EngineRegistry` / `EngineExecutor` / `DomainEngine` / `DeterministicId` | Sin cambios |
| `ReportEngine` / `ConsultingReport` / 10 `SectionAssembler` | Sin cambios |
| `PromptAssembler` / `ConversationPromptBuilder` / `ReportPromptBuilder` / `SectionFormatter` | Sin cambios; solo overload aditivo de `forConversation` |
| `AIResponder` / `AIRequest` / `AiEngineService` / `ProviderRouter` | Sin cambios |
| `ConversationDecision` / `ConversationStrategist` | Sin cambios |
| `ContextRepository` / `JpaContextRepository` / tabla `project_context` | Sin cambios |
| `ChatOrchestratorServiceImpl` | Delegación a `ConversationOrchestrator` (cambio de aplicación, no de contrato) |
| Frontend / auth / CORS | Sin cambios |

---

## 11. Criterios de aceptación

- [x] `ConversationOrchestrator.orchestrate` devuelve un `TurnResult` con
      `directive` tipada (fase + modo + restricciones) en toda ejecución.
- [x] En fase EXPLORATION, `ResponseGuard` valida: respuesta no vacía, longitud
      ≤ máximo, pregunta única y sin marcadores de reporte; ante violación Java
      decide el fallback y `validation.accepted=false`.
- [x] En fase REPORTING, `ConsultorStage` usa `PromptRequest.forReport(consultingReport)`
      (frontera ADR-012 intacta) y `communicationMode=EXPLAIN_REPORT`. *(El turno
      de generación inicial usa la directiva de la decisión previa — enmienda M1.)*
- [x] El historial que ve el LLM está acotado por `HistoryWindow` (nunca el
      historial completo sin ventana).
- [x] Ninguna decisión se parsea del texto del LLM (grep: cero patrones de
      regex-inferencia en `kin.conversation`).
- [x] Los 338 tests existentes permanecen verdes sin modificación de aserciones
      (468 tests en el cierre, 0 fallos).
- [x] Cobertura JaCoCo ≥90 % de instrucciones en `com.kinplatform.kin.conversation`
      (100 %, 738/738).
- [x] ADR-013 aprobado y reflejado en AGENTS.md/BASELINE/GOVERNANCE/CHANGELOG en E7.

---

## 12. Estado del entregable

| Entregable | Estado |
|---|---|
| `kin-docs/FASE5_6_CONVERSATION_ORCHESTRATOR.md` | ✅ Diseño congelado + cierre E7 (este documento) |
| `kin-docs/adr/ADR-013-conversation-orchestrator.md` | ✅ **Aprobado** (con enmiendas M1/M2/M3 de la auditoría) |
| Código `kin.conversation` | ✅ Implementado (E1…E6): `ConversationOrchestrator`, `TurnPolicy`/`DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, 7 tipos de turno |
| Integración aditiva (E6) | ✅ `KinMethodCommand.directive`, `PipelineContext.turnDirective`/`responseValidation`, overload `PromptRequest.forConversation(context, decision, directive)`, `ConsultorStage` consume directiva + guard streaming, `ConversationPromptBuilder` (sección DIRECTIVA), beans en `KinConfig`, `/chat` y `/chat/stream` delegan en el orquestador |
| Documentación del proyecto (AGENTS/BASELINE/GOVERNANCE/CHANGELOG) | ✅ Actualizada en E7 |
| Commit + tag de fase | ✅ Commit de la fase (sin tag — solo si el usuario lo solicita) |

---

## 13. Bitácora

| Fecha | Evento |
|---|---|
| 2026-07-31 | Auditoría de fuentes (KinMethod, ChatOrchestratorServiceImpl, PipelineContext, ConversationDecision, ProjectContext, ConversationStrategist, EventStage, ContextRepository, PromptAssembler, ADR-012, FASE5_5) |
| 2026-07-31 | Redacción de ADR-013 y FASE 5.6 (Etapa 1: Diseño Arquitectónico) |
| 2026-07-31 | E1…E6 implementados: `kin.conversation` (tipos, HistoryWindow, ResponseGuard, DefaultTurnPolicy, ConversationOrchestrator) + integración aditiva (KinMethodCommand, PipelineContext, PromptRequest, ConsultorStage, ConversationPromptBuilder, KinConfig, ChatOrchestratorServiceImpl). `./mvnw clean verify`: 468 tests, BUILD SUCCESS; JaCoCo `kin.conversation` 100 % (738/738) |
| 2026-07-31 | Auditoría independiente de cierre: **FASE 5.6 APROBADA PARA CIERRE** (0 críticos; M1/M2/M3 documentales) |
| 2026-07-31 | E7: ADR-013 → **Aprobado** con enmiendas M1/M2/M3; AGENTS.md, BASELINE_ARCHITECTURE.md, KIN_ARCHITECTURE_GOVERNANCE.md y CHANGELOG.md actualizados; `./mvnw clean verify` final; commit + push. **FASE 5.6 CERRADA OFICIALMENTE** |

---

## 14. Enmiendas de la auditoría de cierre (E7)

La auditoría independiente de la Fase 5.6 (2026-07-31) dictaminó **FASE 5.6
APROBADA PARA CIERRE** (0 hallazgos críticos) e incorporó tres enmiendas
documentales:

- **M1 — Directiva del turno de generación de reporte**: el turno que genera por
  primera vez el `ConsultingReport` usa la directiva derivada de la decisión previa
  pre-pipeline (p. ej. `(EXPLORATION, ASK, QUESTION)`); los turnos posteriores a la
  generación reciben `(REPORTING, REPORT, EXPLAIN_REPORT)`. `ConsultorStage` usa
  `forReport` (frontera ADR-012 intacta) y el guard puede marcar la explicación
  `accepted=false` (artefacto de auditoría). Detalle: ADR-013 §M1 y §6.2.
- **M2 — `ConversationPromptBuilder` modificado aditivamente**: se sanciona el
  cambio aditivo de E6 (sección `## DIRECTIVA DE COMUNICACIÓN` cuando
  `request.directive() != null`); factory original y frontera REPORT intactos.
  Detalle: ADR-013 §M2.
- **M3 — Responsabilidad del `ResponseGuard`**: bloqueante → `ConversationOrchestrator`
  (valida y emite `ResponseValidation` en `TurnResult`); streaming →
  `ConsultorStage.attachStreamGuard` (deja `responseValidation` en contexto sin
  romper chunks). `ConsultorStage` no aplica el guard en bloqueante. Detalle:
  ADR-013 §M3.

### Resultado final de la implementación (E1…E6)

| Verificación | Resultado |
|---|---|
| `./mvnw clean verify` | **BUILD SUCCESS** — 468 tests, 0 fallos, 0 errores, 0 skipped |
| Cobertura JaCoCo `com.kinplatform.kin.conversation*` | **100 %** de instrucciones (738/738) |
| Paquetes de dominio | `kin.ai` 99.7 %, `kin.ai.prompt` 99.7 %, `kin.ai.prompt.formatter` 99.9 %, `kin.reporting*` 99.2 %, `kin.pipeline.stage` 97.2 %, `kin.scoring` 95.1 %, `kin.engine` 99.1 % |
| Tests previos (Fase 5.5) | 338 verdes sin modificación de aserciones → 468 totales |

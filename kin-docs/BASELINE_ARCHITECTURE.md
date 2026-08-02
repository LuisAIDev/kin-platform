# BASELINE ARCHITECTURE — KIN 2.0 Alpha 1 (núcleo inteligente estable)

> **Milestone**: `v2.0.0-alpha1` — 31 de julio de 2026 (release oficial del núcleo inteligente; sobre el milestone original `v2.0.0-alpha.1`)
> **Estado**: `ALPHA STABLE` (enmendado por ADR-006 … ADR-010 — Fases 5.2.1 y 5.3, ADR-011 — Fase 5.4, ADR-012 — Fase 5.5, ADR-013 — Fase 5.6, ADR-014 — Fase 6: External Knowledge Acquisition, y **ADR-015 — Fase 7: Strategic Interview Engine**)
> **Commit**: `89b39b9` — Branch: `main`
> **Release notes**: `kin-docs/releases/KIN_2_0_ALPHA_1.md`
>
> Este documento es el **baseline contractual** del milestone: define qué existe hoy, qué está
> completo, qué contratos NO deben romperse, qué componente es estable/evolutivo/experimental
> y qué decisiones quedan congeladas. Ningún cambio sobre lo marcado como **Estable** procede
> sin una ADR aprobada.

---

## 1. Contexto

KIN es una plataforma full-stack de gestión de proyectos con evaluación asistida por IA.
Este milestone fija la base arquitectónica para las fases futuras: no añade funcionalidad
nueva de negocio, sino que estabiliza la infraestructura de dominio sobre la que se construirá
el resto de la plataforma. La **Fase 5.2.1** (consolidación del runtime) enmienda este baseline
con ADR-006 (pipeline único), ADR-007 (contexto durable), ADR-008 (puerto de IA + prompt) y
ADR-009 (canonización de scoring).

| Capa | Stack |
|------|-------|
| Backend | Spring Boot 3.2.5 / Java 17 — Maven (`mvnw`) |
| Frontend | Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| Base de datos | PostgreSQL (Docker, Flyway, `ddl-auto: none`) / H2 file-based en dev (`ddl-auto: update`, Flyway off) |
| IA | Router de proveedores (DeepSeek/OpenAI/Ollama) con fallback en español (no requiere LLM para dev/test) |

---

## 2. Qué existe hoy (inventario)

### 2.1 Arquitectura general

```
Clean Architecture + DDD Táctico + Pipeline Pattern + Event-Driven
```

| Bounded Context | Paquete | Responsabilidad |
|-----------------|---------|-----------------|
| `auth` | `com.kinplatform.auth` | Registro, login, emisión de JWT |
| `user` | `com.kinplatform.user` | Usuario, roles (FREE, PREMIUM, FACILITADOR, ADMIN) |
| `project` | `com.kinplatform.project` | CRUD + categorías, estado, scoring de viabilidad |
| `chat` | `com.kinplatform.chat` | Historial de mensajes, streaming SSE (`/chat/stream`), orquestación (I/O puro) |
| `ai` | `com.kinplatform.ai` | Adaptadores de IA: `AiEngineService` (implementa `AIResponder`), providers, analizador heurístico |
| `pricing` | `com.kinplatform.pricing` | Planes, Stripe |
| `reporting` | `com.kinplatform.kin.reporting` | Motores de dominio (recomendación, riesgo, oportunidad, reporte) + scoring |
| `engine` | `com.kinplatform.kin.engine` | Infraestructura común de motores (contrato estable) |
| `pipeline` | `com.kinplatform.kin.pipeline` | Pipeline de análisis + stages genéricos |
| `context` | `com.kinplatform.kin.context` | Tipos de dominio de contexto (ProjectContext, evaluación, decisión, `ContextRepository`) |
| `ai` (dominio) | `com.kinplatform.kin.ai` | Puertos/servicios de IA de dominio (`AIResponder`, `AIRequest`, `PromptRequest`/`PromptType`, `PromptAssembler`) |
| `ai.prompt` (dominio) | `com.kinplatform.kin.ai.prompt` | Ensamblado del prompt: `ConversationPromptBuilder`, `ReportPromptBuilder`, 10 `SectionFormatter` (frontera ADR-012: REPORT solo consume `ConsultingReport`) |
| `conversation` (dominio) | `com.kinplatform.kin.conversation` | Ciclo de conversación dirigido (ADR-013): `ConversationOrchestrator` (fachada), `TurnPolicy`/`DefaultTurnPolicy` (directiva en Java), `ResponseGuard` (guardrail), `HistoryWindow` (presupuesto de contexto) + tipos de turno (`ConversationTurn`, `TurnDirective`, `TurnResult`, `ResponseValidation`, `TurnConstraints`, `ConversationPhase`, `CommunicationMode`) |
| `knowledge` (dominio) | `com.kinplatform.kin.knowledge` | Adquisición de conocimiento externo (ADR-014): `KnowledgeEngine` (canonizado, fase KNOWLEDGE/DOMAIN/50), `KnowledgeGateway`, `SourceRegistry`, `SourceValidator`, puerto `KnowledgeSource`, puerto `KnowledgeRepository` (caché TTL), tipos (`KnowledgeRequest/Query/Candidate/Fact/Input/Result`, `SourceValidation`, `SourceTrust`) |
| `knowledge.stage` (dominio) | `com.kinplatform.kin.knowledge.stage` | `KnowledgeStage` — etapa aditiva de pipeline (composición pura sobre `EngineStage`); escribe `PipelineContext.knowledgeResult` |
| `knowledge.adapter` (infraestructura) | `com.kinplatform.ai.knowledge.adapter` | Adaptadores de fuentes de conocimiento: `CompositeKnowledgeSource`, `HttpKnowledgeSourceAdapter`, `PublicApiConnector`, `JdbcKnowledgeSource`, `RagKnowledgeSource`, `DocumentKnowledgeSource` (todos detrás del puerto `KnowledgeSource`/`KnowledgeRepository`; implementan el principio "Java decide. Las fuentes únicamente aportan conocimiento") |
| `interview` (dominio) | `com.kinplatform.kin.interview` | Entrevista estratégica (ADR-015): `InterviewEngine` (canonizado, fase VALIDATION/DOMAIN), `InterviewBlueprint`, `AnswerValidator`, `InterviewState`/`InterviewProgress`, tipos (`InterviewQuestion`, `AnswerRules`, `AnswerValidation`, `InterviewAnswer`, `InterviewContext`, `InterviewRequest`, `InterviewInput`, `InterviewResult`, `InterviewDirective`, `InterviewDecision`), puerto `InterviewRepository` (durable por proyecto) |
| `interview.engine` (dominio) | `com.kinplatform.kin.interview.engine` | `InterviewEngine` (motor canonizado) + `InterviewBlueprint` + `AnswerValidator` — decisión de entrevista 100 % Java |
| `interview.stage` (dominio) | `com.kinplatform.kin.interview.stage` | `InterviewStage` — etapa aditiva de pipeline (composición pura sobre `EngineStage`); construye `InterviewInput`, invoca `InterviewEngine`, escribe `PipelineContext.interviewResult` y persiste el estado vía `InterviewRepository` |
| `interview.adapter` (infraestructura) | `com.kinplatform.ai.interview.adapter` | Adaptador durable de `InterviewRepository` (`JpaInterviewRepository`, `InterviewStateEntity`, `InterviewStateMapper`) en tabla `interview_state` (Flyway V4 prod / `ddl-auto` dev) |

### 2.2 Infraestructura de motores — `kin/engine` (CONTRATO CONGELADO)

| Tipo | Rol | Estable |
|------|-----|---------|
| `DomainEngine<E,R>` | Contrato funcional de motor: `evaluate(E) → R` + `metadata()` | ✅ |
| `EngineInput` | Interfaz **marcadora** (ADR-009); cada motor declara sus campos tipados | ✅ |
| `EngineResult` | Record inmutable, base de resultados (trazabilidad) | ✅ |
| `EngineMetadata` | `name, version, author, phase, type, priority` | ✅ |
| `EnginePhase` | 16 fases: `ANALYSIS, EVALUATION, STRATEGY, CONSULTATION, SCORING, RECOMMENDATION, RISK, OPPORTUNITY, KNOWLEDGE, INNOVATION, COMPETITION, FINANCIAL, MARKET, VALIDATION, REPORTING, EXPLANATION` | ✅ |
| `EngineType` | `DOMAIN, ADAPTER` | ✅ |
| `EngineRegistry` | Auto-descubrimiento via `List<DomainEngine>` + `get(id)` + `all()` | ✅ |
| `EngineExecutor` | Ejecución sequential/conditional/optional (parallel diseñado, no activo) | ✅ |
| `DeterministicId` | IDs deterministas para trazabilidad | ✅ |
| `EngineStage` | Stage genérico de pipeline que delega en motores | ✅ |

### 2.3 Motores de dominio — `kin/reporting` + `kin/scoring`

| Motor | Input | Output | Notas |
|-------|-------|--------|-------|
| `ScoringEngine` | `ScoringInput(ProjectContext, CompletenessEvaluation)` | `ScoreResult` (totalScore, categoryScores, viabilityLabel, strengths, weaknesses) | ✅ canonizado (ADR-009): implementa `DomainEngine` (SCORING/DOMAIN/30); heurística de longitud por reemplazar antes de KIN 2.5 |
| `RecommendationEngine` | `RecommendationInput` | `RecommendationResult` (recomendaciones deduplicadas y priorizadas) | ✅ estable como motor |
| `RiskEngine` | `RiskInput` | `RiskResult` (riesgos, severidad, probabilidad, score, riskLevel) | ✅ estable como motor |
| `RiskAssembler` | Analizadores de riesgo | `RiskResult` consolidado determinista | ✅ estable |
| `OpportunityEngine` | `OpportunityInput` | `OpportunityResult` (oportunidades priorizadas, top) | ✅ estable como motor (Fase 5.3, ADR-010) |
| `OpportunityAssembler` | Analizadores de oportunidad | `Opportunity` consolidada determinista | ✅ estable (Fase 5.3, ADR-010) |
| `ReportEngine` | `ReportInput` (4 resultados ya calculados) | `ConsultingReport` (10 secciones inmutables) | ✅ existente (Fase 5.4, ADR-011): orquestador puro, prioridad 70, fase REPORTING |
| `KnowledgeEngine` | `KnowledgeInput` | `KnowledgeResult` (hechos verificados, fuentes usadas, validaciones, confianza) | ✅ existente (Fase 6, ADR-014): implementa `DomainEngine` (KNOWLEDGE/DOMAIN/50); delega en `KnowledgeGateway`; offline-first (`KnowledgeResult.empty()`) |
| `InterviewEngine` | `InterviewInput` | `InterviewResult` (directiva, estado, completitud, decisión ASK/REPORT) | ✅ existente (Fase 7, ADR-015): implementa `DomainEngine` (VALIDATION/DOMAIN); delega en `InterviewBlueprint` + `AnswerValidator`; decisión de entrevista 100 % Java |

### 2.4 Pipeline — `kin/pipeline`

| Stage | Función |
|-------|---------|
| `Pipeline` | Orquesta la ejecución secuencial de stages |
| `PipelineStage` | Interfaz `name() + supports() + execute()` |
| `AnalyzerStage` | Extrae dimensiones del mensaje → `ProjectContext.update(...)` |
| `EvaluatorStage` | `CompletenessEvaluator` → `CompletenessEvaluation` |
| `StrategistStage` | `ConversationStrategist` → `ConversationDecision` |
| `InterviewStage` | Compone `EngineStage` → `InterviewEngine` (ADR-015); construye `InterviewInput` (proyecto + respuesta del turno + estado previo), invoca el motor y escribe `PipelineContext.interviewResult`; persiste el estado vía `InterviewRepository`; aplica la decisión efectiva (ASK/REPORT) |
| `KnowledgeStage` | Compone `EngineStage` → `KnowledgeEngine` (ADR-014); construye `KnowledgeRequest` desde el `ProjectContext` (topic, dimensiones cubiertas, keywords) y escribe `PipelineContext.knowledgeResult` |
| `ConsultorStage` | Pide la respuesta al puerto `AIResponder` (bloqueante o streaming); si hay entrevista activa con decisión `ASK` usa el prompt conversacional con `## ENTREVISTA ESTRATÉGICA` (ADR-015); si `decision.shouldGenerateReport()` selecciona `PromptRequest.forReport(...)` (lanza si falta el `ConsultingReport`), en caso contrario `forConversation(context, decision, turnDirective)`; en streaming aplica `ResponseGuard` (`attachStreamGuard`) y deja `responseValidation` en contexto (ADR-013) |
| `ScoringStage` | Compone `EngineStage` → `ScoringEngine` (predicado REPORT) |
| `RecommendationStage` | Compone `EngineStage` → `RecommendationEngine` (predicado REPORT) |
| `RiskStage` | Compone `EngineStage` → `RiskEngine` (predicado REPORT) |
| `OpportunityStage` | Compone `EngineStage` → `OpportunityEngine` (predicado REPORT) |
| `ReportStage` | Compone `EngineStage` → `ReportEngine` (predicado: 4 resultados presentes) |
| `EventStage` | Publica eventos según decisión (ASK→Question, REPORT→Report+Score, siempre ConversationCompleted) |
| `PipelineContext` | Flujo de datos mutable entre stages + `engineResults` + flag `streaming` + `aiResponseFlux` + `turnDirective` + `responseValidation` (ADR-013) + `knowledgeResult` (ADR-014) + `interviewResult` (ADR-015) |

El pipeline actual tiene **12 stages**: Analizador → Evaluador → Estratega → **Entrevista** → **Conocimiento** → Scoring → Recomendaciones → Riesgos → Oportunidades → **Reporte** → **Consultor** → Eventos. `InterviewStage` se inserta entre `StrategistStage` y `KnowledgeStage` (ADR-015, integración aditiva patrón ADR-011/014). `ConsultorStage` se reposicionó tras `ReportStage` (ADR-012) para que el LLM reciba el `ConsultingReport` en modo REPORT; en modo CONVERSATION las etapas de análisis se omiten según predicado.

### 2.5 AI — `ai/` y `kin/ai`

| Componente | Rol |
|------------|-----|
| `AIResponder` (puerto, dominio) | `respond(AIRequest) → String` + `respondStream(AIRequest) → Flux<String>` |
| `AIRequest` (dominio) | Record inmutable (historial, mensaje, system prompt) |
| `PromptAssembler` (dominio) | Fachada pura (ADR-012): `assemble(PromptRequest) → String` delega en `ConversationPromptBuilder` o `ReportPromptBuilder` según `PromptType`; overload aditivo `assemble(PromptRequest, InterviewResult)` (ADR-015) propaga el resultado de la entrevista en modo CONVERSATION |
| `ConversationPromptBuilder` / `ReportPromptBuilder` (dominio) | `kin.ai.prompt`: prompt conversacional (contexto mínimo + instrucción estratégica + sección `## DIRECTIVA DE COMUNICACIÓN` cuando hay directiva, ADR-013, + sección `## ENTREVISTA ESTRATÉGICA` cuando hay `InterviewDirective`, ADR-015) / prompt de reporte (10 secciones formateadas por `SectionFormatter` + instrucción fija "Explica, no decidas") |
| `AiEngineService` (adaptador) | Implementa `AIResponder`; enruta a `ProviderRouter` con fallback en español |
| `AIProvider` | Puerto `generateBlocking() + generateStream()` |
| `ProviderRouter` | Enrutamiento OCP-compliant entre proveedores |
| `context.adapter.HeuristicContextAnalyzerAdapter` | Análisis heurístico (regex) — experimental |

### 2.6 Contexto — `kin/context` + adaptador JPA

| Componente | Rol | Estable |
|------------|-----|---------|
| `ProjectContext` | Estado del proyecto (datos por dimensión, cobertura, decisión, contadores) | ✅ API de actualización congelada |
| `ContextRepository` (puerto) | `findOrCreate / find / save / delete` | ✅ nuevo (ADR-007) |
| `ProjectContext.restore(...)` | Factory de dominio para reconstruir contexto persistido | ✅ |
| `JpaContextRepository` / `ProjectContextEntity` | Adaptador JPA durable (JSON en `project_context`) | ✅ nuevo |

### 2.7 Eventos — `kin/event`

| Componente | Rol | Estable |
|------------|-----|---------|
| `DomainEvent` | Interface `type() + aggregateId()` | ✅ |
| `DomainEventBus` | Interface `publish() + subscribe()` | ✅ |
| `InMemoryDomainEventBus` | Implementación en memoria (sin async/persistencia) | Experimental |
| Eventos concretos | `ConversationCompletedEvent`, `ProjectContextUpdatedEvent`, `QuestionGeneratedEvent`, `ReportGeneratedEvent`, `RiskDetectedEvent`, `ScoreCalculatedEvent` | ✅ tipos |

### 2.8 Runtime — `kin/`

| Componente | Rol | Estable |
|------------|-----|---------|
| `KinMethod` | **Punto de entrada único** (ADR-006): `execute` (bloqueante) y `executeStream` (Flux) | ✅ |
| `KinMethodCommand` / `KinMethodResult` | Records de entrada/salida del runtime | ✅ |
| `ScoringStage` | Etapa de scoring (paquete `pipeline.stage`) | ✅ |

### 2.9 Conversation Orchestrator — `kin/conversation`

| Componente | Rol | Estable |
|------------|-----|---------|
| `ConversationOrchestrator` | Fachada de dominio del ciclo de turno (ADR-013): `orchestrate(ConversationTurn) → TurnResult` + `orchestrateStream(ConversationTurn) → Flux<String>`; compone HistoryWindow + TurnPolicy + KinMethod + ResponseGuard + ContextRepository. Sin Spring (POJO). `ChatOrchestratorServiceImpl` delega ambos endpoints | ✅ nuevo |
| `TurnPolicy` / `DefaultTurnPolicy` | Política de turno determinista: decide en Java fase/modo/restricciones desde la decisión previa persistida (pre-pipeline). Mapeo exhaustivo de las 7 acciones | ✅ nuevo |
| `ResponseGuard` | Guardrail de comunicación (ADR-013): vacío, longitud, pregunta única, marcadores prohibidos; `accepted = issues.isEmpty()`. Nunca infiere intención del texto. Responsabilidad: bloqueante → orquestador; streaming → `ConsultorStage` (enmienda M3) | ✅ nuevo |
| `HistoryWindow` | Ventana de contexto por número de mensajes (default 20); el mensaje del usuario del turno siempre se incluye; presupuesto determinista | ✅ nuevo |
| `ConversationPhase` / `CommunicationMode` / `TurnConstraints` / `ConversationTurn` / `TurnDirective` / `ResponseValidation` / `TurnResult` | Tipos puros (records/enums inmutables) del ciclo de conversación | ✅ nuevo |

### 2.10 Strategic Interview Engine — `kin/interview`

| Componente | Rol | Estable |
|------------|-----|---------|
| `InterviewEngine` | Motor canonizado (ADR-015): `DomainEngine<InterviewInput, InterviewResult>`, fase `VALIDATION`/tipo `DOMAIN`; delega en `InterviewBlueprint` + `AnswerValidator`; degrada a `InterviewResult.empty()`; decisión de entrevista 100 % Java | ✅ nuevo |
| `InterviewBlueprint` | Plan determinista de la entrevista: secuencia de `InterviewQuestion` por `AnalyzedDimension` (orden, obligatoriedad, `AnswerRules`, follow-ups); `next(state)`/`isComplete(state)` | ✅ nuevo |
| `AnswerValidator` | Validación determinista: vacío, longitud mínima, formato, keywords mínimas; aceptar/rechazar/refinar con motivo; nunca consulta al LLM | ✅ nuevo |
| `InterviewState` / `InterviewProgress` / `InterviewAnswer` / `InterviewContext` | Progreso inmutable por proyecto + progreso computable + respuesta del turno + vista de contexto | ✅ nuevo |
| `InterviewInput` / `InterviewResult` / `InterviewDirective` / `InterviewDecision` | Records inmutables de entrada/salida del motor (decisión ASK/REPORT) | ✅ nuevo |
| `InterviewRepository` (puerto) | `findOrCreate(UUID projectId) → InterviewState` + `save(InterviewState)`; persistencia durable por proyecto | ✅ nuevo |
| `InterviewStage` | Etapa aditiva de pipeline (composición pura sobre `EngineStage`); escribe `PipelineContext.interviewResult` y persiste el estado; gating de REPORT por completitud | ✅ nuevo |

El principio rector de la entrevista (ADR-015):

> **Java decide. El LLM únicamente formula preguntas.**
> Java decide qué información falta, en qué orden, si una respuesta es válida y cuándo la
> entrevista está completa; el LLM solo formula la pregunta dictada por `InterviewDirective` y
> comunica el resultado del análisis.

---

## 3. Qué está completo (criterio de aceptación del milestone)

- [x] **Fase 4.0** — Proveedor IA + capa de contexto de dominio.
- [x] **Fase 5.0** — `RecommendationEngine` (ADR-003).
- [x] **Fase 5.1** — `RiskEngine` + `RiskAssembler` (ADR-004).
- [x] **Fase 5.2** — Infraestructura común de motores (ADR-005): `kin/engine`, `EngineStage`, `KinConfig`.
- [x] **Fase 5.2.1** — Consolidación del runtime (ADR-006 … ADR-009): pipeline único para `/chat` y `/chat/stream`, `ContextRepository` JPA durable, `AIResponder`/`PromptAssembler`, scoring canonizado.
- [x] **Fase 5.3** — `OpportunityEngine` (ADR-010): 8 analizadores auto-descubiertos, `OpportunityStage` (9ª etapa).
- [x] **Fase 5.4** — `ReportEngine` (ADR-011): orquestador puro del `ConsultingReport` (10 secciones), `ReportStage` (10ª etapa), ID determinista, cobertura ≥90%.
- [x] **Fase 5.5** — `PromptAssembler` (ADR-012): fachada pura + `ConversationPromptBuilder` + `ReportPromptBuilder` + 10 `SectionFormatter`; `ConsultorStage` reposicionado tras `ReportStage`.
- [x] **Fase 5.6** — `ConversationOrchestrator` (ADR-013): `kin.conversation` (orquestador, `TurnPolicy`/`DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, 7 tipos de turno); directiva en Java pre-pipeline; integración aditiva (`KinMethodCommand.directive`, `PipelineContext.turnDirective`/`responseValidation`, overload `PromptRequest.forConversation`, `ConversationPromptBuilder` DIRECTIVA, `ConsultorStage` guard streaming); `/chat` y `/chat/stream` delegan en el orquestador.
- [x] **Fase 6** — External Knowledge Acquisition (ADR-014): `kin.knowledge` (motor `KnowledgeEngine`, `KnowledgeGateway`, `SourceRegistry`, `SourceValidator`, puertos `KnowledgeSource`/`KnowledgeRepository`, tipos) + `ai.knowledge.adapter` (6 adaptadores: Composite/Http/PublicApi/Jdbc/Rag/Document) + integración aditiva (`KnowledgeStage` + `PipelineContext.knowledgeResult`). ADR-014 **Aprobada**.
- [x] **Fase 7** — Strategic Interview Engine (ADR-015): `kin.interview` (motor `InterviewEngine`, `InterviewBlueprint`, `AnswerValidator`, tipos, puerto `InterviewRepository`) + `ai.interview.adapter` (`JpaInterviewRepository`) + integración aditiva (`InterviewStage` + `PipelineContext.interviewResult` + sección `## ENTREVISTA ESTRATÉGICA`). ADR-015 **Aprobada**. **Fase 7 cerrada oficialmente**.
- [x] Motores, inputs y results canonizados bajo el contrato `DomainEngine`.
- [x] **902 tests verdes** (`./mvnw clean verify`), BUILD SUCCESS, 0 fallos, 0 errores, 0 skipped.
- [x] **Cobertura de dominio ≥ 90 %**: `kin.engine` 99.1 %, `kin.ai` 99.7 %, `kin.ai.prompt` 95.9 %, `kin.ai.prompt.formatter` 99.9 %, `kin.reporting*` 99.2 %, `kin.reporting.report` 99–100 %, `kin.reporting.opportunity` 100 %, `kin.reporting.risk` 99.5 %, `kin.scoring` 95.1 %, **`kin.conversation` 100 %**, **`kin.knowledge` 100 %** (engine 99.47 %, stage 100 %), **`ai.knowledge.adapter` 100 %**, **`kin.interview` + `ai.interview.adapter` 98.39 %** (engine 95 %, stage 100 %, adapter 100 %).
- [x] **15 ADRs** aprobadas (ADR-001 … ADR-015).
- [x] Documentación por fase (FASE5_0/5_1/5_2/5_2_1/5_3/5_4/5_5/5_6 + FASE6_0 + FASE7) + Gobernanza + AGENTS.md + CHANGELOG + Release Notes.

---

## 4. Contratos que NO deben romperse (API pública congelada)

> Cualquier cambio sobre estos contratos requiere una ADR aprobada.

### 4.1 Contratos de dominio

| Contrato | Contenido |
|----------|-----------|
| `DomainEngine` | `evaluate(E) → R`, `metadata() → EngineMetadata` |
| `EngineInput` / `EngineResult` | Marcador de entradas / base de resultados (ADR-009) |
| `EngineMetadata` | Campos actuales |
| `EnginePhase` | 16 valores actuales (aditivo) |
| `EngineType` | `DOMAIN, ADAPTER` |
| `EngineRegistry` | `get(id)`, `all()`, auto-descubrimiento |
| `EngineExecutor` | API de ejecución (sequential/conditional/optional) |
| `EngineStage` | `name()`, `supports()`, `execute()` |
| `RecommendationEngine` / `RecommendationResult` | Contrato público (input/output) |
| `RiskEngine` / `RiskResult` | Contrato público (input/output) |
| `OpportunityEngine` / `OpportunityResult` | Contrato público (input/output; ADR-010) |
| `ReportEngine` / `ConsultingReport` | Contrato público (input/output; ADR-011) |
| `ReportStage` | Stage de pipeline (composición pura sobre `EngineStage`; ADR-011) |
| `ScoringEngine` / `ScoreResult` | Contrato público (input/output; ADR-009) |
| `KinMethod` | Fachada única `execute(KinMethodCommand) → KinMethodResult` + `executeStream(KinMethodCommand) → Flux<String>` |
| `ContextRepository` | `findOrCreate / find / save / delete` (ADR-007) |
| `AIResponder` | `respond(AIRequest)` + `respondStream(AIRequest)` (ADR-008) |
| `ConversationDecision.Action` | `ASK, REPORT, RECOMMEND, VALIDATE, SUMMARIZE, STOP, ESCALATE` |
| `AnalyzedDimension` | 14 dimensiones (se pueden agregar, no eliminar) |
| `DomainEvent` / `DomainEventBus` | `type()`, `aggregateId()`, `publish()`, `subscribe()` |
| `AIProvider` | `generateBlocking()`, `generateStream()` |
| `PipelineStage` / `Pipeline` | `name()`, `supports()`, `execute()` + algoritmo de ejecución |
| `ScoringModel` / `ScoreResult` | Records inmutables |
| `PipelineContext` | Acceso a campos + `engineResults` + `streaming` + `aiResponseFlux` + `turnDirective` + `responseValidation` (ADR-013) + `knowledgeResult` (ADR-014) |
| `ConversationOrchestrator` | `orchestrate(ConversationTurn) → TurnResult` + `orchestrateStream(ConversationTurn) → Flux<String>` (ADR-013) |
| `TurnPolicy` / `DefaultTurnPolicy` | `decide(ProjectContext, ConversationDecision) → TurnDirective` (ADR-013) |
| `ResponseGuard` | `validate(String, TurnDirective) → ResponseValidation` (ADR-013) |
| `HistoryWindow` | `window(List<Message>, int) → List<Message>` (ADR-013) |
| `ConversationTurn` / `TurnDirective` / `TurnResult` / `ResponseValidation` / `TurnConstraints` / `ConversationPhase` / `CommunicationMode` | Tipos del ciclo de conversación (ADR-013) |
| `KnowledgeEngine` / `KnowledgeResult` | Motor de conocimiento (ADR-014): `evaluate(KnowledgeInput) → KnowledgeResult` + `metadata()` |
| `KnowledgeGateway` / `SourceRegistry` / `SourceValidator` | Coordinador de adquisición + registro de fuentes + validación determinista (ADR-014) |
| `KnowledgeSource` / `KnowledgeRepository` | Puertos de fuente y de caché/persistencia de conocimiento (ADR-014) |
| `KnowledgeStage` | Stage aditivo de pipeline (composición pura sobre `EngineStage`; ADR-014) |
| `InterviewEngine` / `InterviewResult` | Motor de entrevista estratégica (ADR-015): `evaluate(InterviewInput) → InterviewResult` + `metadata()` |
| `InterviewRepository` | Puerto de persistencia del estado de entrevista (ADR-015): `findOrCreate(UUID) → InterviewState` + `save(InterviewState)` |
| `InterviewStage` | Stage aditivo de pipeline (composición pura sobre `EngineStage`; ADR-015) |

### 4.2 Contratos de aplicación/API

| Contrato | Contenido |
|----------|-----------|
| API REST | Todos los endpoints bajo `/api/v1` |
| Streaming | `/chat/stream` con SSE (eventos `token`/`error`/`done`) |
| `AiEngineService` | Fallback en español garantizado si todos los proveedores fallan |

### 4.3 Contratos de infraestructura

| Contrato | Contenido |
|----------|-----------|
| CORS dual | `CorsConfig.java` + `SecurityConfig.java` (agregar orígenes en ambos) |
| JWT | Filtro `JwtAuthenticationFilter` + `JwtService` (stateless) |
| Env vars | `NEXT_PUBLIC_API_URL`, `.env` (gitignored, copiar de `.env.example`) |
| Migraciones Flyway | V1…V4 (solo prod/PostgreSQL); dev usa `ddl-auto: update` |

---

## 5. Matriz de estabilidad

### 5.1 ESTABLES — no deben cambiar en fases futuras

| Componente | Justificación |
|-----------|---------------|
| `kin.engine.DomainEngine` | Contrato funcional de todos los motores; congelado por ADR-005/ADR-009 |
| `kin.engine.EngineInput` / `EngineResult` | Marcador de entradas / base de resultados |
| `kin.engine.EngineMetadata` | Metadatos completos para descubrimiento y trazabilidad |
| `kin.engine.EnginePhase` (16) / `EngineType` (DOMAIN/ADAPTER) | Enumeraciones del ciclo de motores |
| `kin.engine.EngineRegistry` | Auto-descubrimiento vía `List<DomainEngine>`; 100 % cobertura |
| `kin.engine.EngineExecutor` | Ejecución determinista; 100 % cobertura |
| `kin.engine.DeterministicId` | IDs reproducibles para tests y trazabilidad |
| `kin.pipeline.stage.EngineStage` | Stage genérico de composición; 100 % cobertura |
| `kin.reporting.RecommendationEngine` | Motor de recomendaciones ADR-003; estable |
| `kin.reporting.risk.RiskEngine` / `RiskAssembler` | Motor de riesgos ADR-004; consolidación determinista |
| `kin.reporting.opportunity.OpportunityEngine` / `OpportunityAssembler` | Motor de oportunidades ADR-010; estable |
| `kin.reporting.report.ReportEngine` | Motor de reporte ADR-011; orquestador puro, estable |
| `ReportStage` | Stage de pipeline ADR-011; composición pura sobre `EngineStage` |
| `ConsultingReport` | VO raíz inmutable del reporte, implementa `EngineResult` |
| `RecommendationResult` / `RiskResult` / `OpportunityResult` / `ScoreResult` | Outputs inmutables que implementan `EngineResult` |
| `kin.scoring.ScoringEngine` | Canonizado por ADR-009; implementa `DomainEngine` (SCORING/DOMAIN/30) |
| `kin.knowledge.KnowledgeEngine` / `KnowledgeGateway` / `SourceRegistry` / `SourceValidator` | Motor y coordinadores de conocimiento (ADR-014); 100 % POJO, decisiones 100 % Java |
| `kin.knowledge.stage.KnowledgeStage` | Stage aditivo de pipeline (ADR-014); composición pura sobre `EngineStage` |
| `KinMethod` / `KinMethodCommand` / `KinMethodResult` | Fachada única del runtime (contrato KIN 2.0 / ADR-006) |
| `ContextRepository` / `AIResponder` / `PromptAssembler` | Puertos/servicios de dominio (ADR-007/ADR-008) |
| `PromptRequest` / `PromptType` / `kin.ai.prompt` builders | Contrato de ensamblado de prompt (ADR-012): REPORT consume solo `ConsultingReport` |
| `ConversationDecision` (+`Action`) | Decisión de conversación, enum completo |
| `AnalyzedDimension` | 14 dimensiones (aditivo) |
| `DomainEvent` / `DomainEventBus` | Interfaces base del event-driven |
| `AIProvider` | Puertos `generateBlocking`/`generateStream` |
| `PipelineStage` / `Pipeline` | Interfaces y algoritmo de ejecución |
| `PipelineContext` | Flujo de datos entre stages + `engineResults` + `consultingReport` + streaming + `turnDirective` + `responseValidation` (ADR-013) |
| `ConversationOrchestrator` | Fachada del ciclo de conversación (ADR-013); compone el contrato congelado `KinMethod` |
| `TurnPolicy` / `DefaultTurnPolicy` | Política de turno determinista (ADR-013); decisión 100 % Java |
| `ResponseGuard` | Guardrail de comunicación (ADR-013); nunca infiere intención |
| `HistoryWindow` | Presupuesto de contexto por número de mensajes (ADR-013) |
| `ConversationTurn` / `TurnDirective` / `TurnResult` / `ResponseValidation` / `TurnConstraints` / `ConversationPhase` / `CommunicationMode` | Tipos de dominio del ciclo de conversación (ADR-013) |
| `kin.interview.InterviewEngine` | Motor de entrevista estratégica (ADR-015); implementa `DomainEngine` (VALIDATION/DOMAIN); decisión 100 % Java |
| `kin.interview.InterviewBlueprint` / `AnswerValidator` | Plan determinista + validación determinista de respuestas (ADR-015); nunca consulta al LLM |
| `kin.interview.InterviewRepository` | Puerto de persistencia del estado de entrevista (ADR-015) |
| `kin.interview.stage.InterviewStage` | Stage aditivo de pipeline (ADR-015); composición pura sobre `EngineStage` |

### 5.2 EN EVOLUCIÓN — cambiarán pero manteniendo la API pública

| Componente | Cambios esperados |
|-----------|-------------------|
| `kin.scoring.ScoringEngine` | Reemplazo de heurística de longitud (antes de KIN 2.5) manteniendo `evaluate(...) → ScoreResult` |
| `ProjectContext` | Se pueden agregar métodos de consulta; no cambiar la API de actualización |
| `CompletenessEvaluator` | Refinamiento de thresholds; API se mantiene |
| `ConversationStrategist` | Nuevas estrategias; API `decide(...) → ConversationDecision` se mantiene |
| `InterviewBlueprint` | Nuevas preguntas/dimensiones = nuevos `InterviewQuestion`; el motor no cambia (OCP, ADR-015) |
| `EventStage` | Semántica completa de eventos según flujo real → KIN 2.1 (ya distingue ASK/REPORT) |
| `kin.pipeline` | Error handling, timeout y métricas por stage → KIN 2.1; orden de stages y contratos se mantienen |
| `JpaContextRepository` | Versionado del formato JSON si cambia el estado del dominio |

### 5.3 EXPERIMENTALES — pueden cambiar significativamente

| Componente | Razón |
|-----------|-------|
| `InMemoryDomainEventBus` | Implementación temporal sin async ni persistencia → será reemplazada (KIN 2.4) |
| `HeuristicContextAnalyzerAdapter` | Análisis regex → será reemplazado por NLP/AI (KIN 2.5) |
| `EngineExecutor.parallel` | Diseñado pero no activo; puede cambiar antes de activarse |
| `kin/event` tipos concretos | Pueden agregarse nuevos eventos; los existentes se mantienen |

---

## 6. Decisiones congeladas (Frozen Decisions)

1. **`kin/` es 100 % POJO**: sin Spring, sin JPA, sin imports de `com.kinplatform.*` excepto `kin.*`.
2. **Toda comunicación entre bounded contexts** es por eventos o por puertos.
3. **Value Objects inmutables** (records de Java o copia defensiva). `PipelineContext` es la única excepción (mutable por diseño).
4. **Los Application Services orquestan, no implementan reglas de negocio**.
5. **`ChatOrchestratorServiceImpl` DELEGA en `ConversationOrchestrator`** (que compone `KinMethod`) en ambos flujos (bloqueante y streaming) — cumplido por ADR-006 y ADR-013.
6. **La infraestructura de motores (`kin/engine`) es un contrato congelado** — ADR-005/ADR-009.
7. **`RecommendationEngine`, `RiskEngine` y `OpportunityEngine` son estables** — ADR-003, ADR-004 y ADR-010.
8. **La heurística de longitud en `ScoringEngine` debe reemplazarse antes de KIN 2.5** — regla absoluta #18.
9. **Cobertura de dominio ≥ 90 %** de instrucciones en `kin.reporting`, `kin.engine`, `kin.ai`, `kin.conversation`, `kin.knowledge` y `kin.interview` (JaCoCo).
10. **Dev sin Docker**: H2 file-based + Flyway deshabilitado (`spring.flyway.enabled=false`); PostgreSQL solo en Docker.
11. **El `ProjectContext` es durable vía `ContextRepository`** (puerto de dominio, adaptador JPA) — ADR-007.
12. **El dominio depende de `AIResponder`/`PromptAssembler`, no de `AiEngineService`** — ADR-008.
13. **El prompt REPORT consume solo `ConsultingReport`** (vía `PromptRequest.forReport`); las fuentes crudas (`ProjectContext`, `ScoreResult`, …) están prohibidas en modo REPORT — ADR-012.
14. **Java decide en la conversación** (ADR-013): `TurnPolicy` decide fase/modo/restricciones, `HistoryWindow` decide el presupuesto de contexto y `ResponseGuard` valida la comunicación — nunca se parsean decisiones del texto del LLM. La directiva viaja aditivamente al pipeline (`KinMethodCommand.directive`) y enmarca el prompt (`PromptRequest.forConversation(context, decision, directive)` + sección DIRECTIVA).
15. **Java decide sobre el conocimiento; las fuentes únicamente aportan conocimiento** (ADR-014): la adquisición, validación, selección y caché del conocimiento externo son decisiones deterministas de Java (`KnowledgeGateway` + `SourceValidator` en `kin.knowledge`). El LLM nunca recupera, valida ni decide conocimiento; los adaptadores (`ai.knowledge.adapter`) solo aportan candidatos. Integración aditiva: `KnowledgeStage` + `PipelineContext.knowledgeResult`.
16. **Java decide la entrevista; el LLM únicamente formula preguntas** (ADR-015): el orden, la validación, la completitud y el presupuesto de la entrevista estratégica son decisiones deterministas de Java (`InterviewEngine` + `InterviewBlueprint` + `AnswerValidator` en `kin.interview`). El LLM nunca decide qué preguntar, en qué orden, si una respuesta es válida ni cuándo terminar; solo formula la pregunta dictada por `InterviewDirective`. Gating de REPORT por completitud; persistencia durable vía `InterviewRepository` (tabla `interview_state`). Integración aditiva: `InterviewStage` + `PipelineContext.interviewResult`.

---

## 7. Preparación para la siguiente fase (siguiente hito — KIN 2.1)

> Análisis de preparación. NO implementación.

### 7.1 Estado de partida

La **Fase 7 (Strategic Interview Engine, ADR-015)** quedó **implementada y cerrada oficialmente**:
el runtime está consolidado (`KinMethod` único punto de entrada, bloqueante y streaming), el
contexto es durable (`ContextRepository`), la infraestructura de motores (`DomainEngine`,
`EngineExecutor`, `EngineStage`) es reutilizable, el ciclo de conversación está dirigido
(`ConversationOrchestrator`, ADR-013), la adquisición de conocimiento externo es aditiva al
pipeline (`KnowledgeStage`, ADR-014) y la recolección de información está dirigida por la
entrevista estratégica (`InterviewStage` + `InterviewEngine`, ADR-015, pipeline de 12 etapas). La
siguiente fase puede consumir el `ProjectContext` durable, el turno tipado/directiva (`TurnResult`,
`TurnDirective`), el `KnowledgeResult` y el `InterviewResult` como puntos de extensión, **sin
tocar los contratos estables**.

### 7.2 Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|------------|
| R1 | `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`) | Baja | Incidencia heredada, ajena al dominio; no bloquea el arranque (warnings) |
| R2 | `EventStage` aún no cubre toda la semántica de eventos | Media | Corregir en KIN 2.1; ya distingue ASK/REPORT |
| R3 | Baja cobertura en infraestructura (auth, pricing, project, ai.provider) | Media | El requisito de ≥ 90 % aplica solo al dominio |
| R4 | Dependencia de proveedores LLM externos | Media | El fallback en español garantiza el desarrollo |
| R5 | Script Flyway `V2` no portable a H2 | Baja | Dev usa `ddl-auto: update`; Flyway solo en prod |
| R6 | Crecimiento de `PipelineContext` (riesgo God Class) | Media | Monitoreo continuo; la API de acceso se mantiene |

### 7.3 Bloqueantes

- **Ningún bloqueante** para iniciar la Fase 7.

### 7.4 Dependencias

| Dependencia | Estado | Requerida para |
|-------------|--------|----------------|
| Proveedores LLM (DeepSeek/OpenAI/Ollama) | Opcional (fallback en español) | Pruebas de IA reales |
| PostgreSQL + Docker | No requerida en dev (H2) | Despliegue / validación de migraciones |
| Frontend (`npm install`) | Requerida | E2E y desarrollo UI |
| Backend (`./mvnw`) | Requerida | Tests y arranque |
| Conocimiento externo (adaptadores `ai.knowledge.adapter`) | Opcional (offline-first: `KnowledgeResult.empty()`) | Enriquecimiento real del análisis (config allowlist/red) |

### 7.5 Recomendaciones para la siguiente fase (KIN 2.1)

1. **Prioridad 1**: consumir el `KnowledgeResult` de la Fase 6 en los analizadores de dominio
   (mercado, innovación, financiero, competitivo) vía inputs aditivos, y/o exponer conocimiento
   citado en la comunicación (exige ADR propia por la frontera ADR-012).
2. **Prioridad 2**: consumir `ResponseValidation` (KIN 2.1): definir el fallback (respuesta enlatada / reintento) ante `accepted=false`, hoy artefacto de auditoría.
3. Mantener el requisito de ≥ 90 % de cobertura en `kin.reporting`, `kin.engine`, `kin.ai`, `kin.conversation`, `kin.knowledge` y `kin.interview` al añadir código nuevo.
4. No romper ninguno de los contratos de §4 sin ADR.

---

## 8. Verificación

```bash
cd kin-backend && ./mvnw clean verify       # 902 tests, 0 fallos, BUILD SUCCESS
# Cobertura: kin.conversation 100 %, kin.knowledge 100 % (engine 99.47 %, stage 100 %), ai.knowledge.adapter 100 %, kin.interview+adapter 98.39 % (engine 95 %, stage 100 %), kin.ai 99.7 %, kin.ai.prompt 95.9 %, kin.ai.prompt.formatter 99.9 %, kin.reporting* 99.2 %, kin.engine 99.1 %, kin.scoring 95.1 %
cd kin-backend && ./mvnw spring-boot:run     # arranque dev H2 (project_context + interview_state auto-creados)
git status                                   # working tree clean (tras commit de la fase)
```

---

*Baseline KIN 2.0 Alpha 1 (enmendado por ADR-006 … ADR-015). Cualquier desviación requiere ADR aprobada.*

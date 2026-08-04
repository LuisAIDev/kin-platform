# Changelog

Todos los cambios notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y el versionado del proyecto en [SemVer](https://semver.org/lang/es/).

## [v1.1.0-phase9] - 2026-08-02

**FASE 9 (KIN 2.1 — "Pipeline Estabilizado") completada y cerrada oficialmente.** ADR-017
**Aprobado**, pipeline resiliente (retry/timeout/métricas/error handling), semántica completa de
eventos, consumo real de `ResponseValidation` con `ResponseFallback` e integración end-to-end.
BUILD SUCCESS con **1210 tests** (0 failures, 0 errors, 0 skipped), cobertura de dominio ≥ 90 %
en los paquetes afectados y contratos congelados intactos.

### Added

- Paquete de dominio `com.kinplatform.kin.pipeline.resilience` (ADR-017, POJO puro, sin Spring):
  - `StagePolicy` — política determinista por stage (reintentos máximos, acción ante fallo `FAIL`/`RETRY`/`SKIP`, timeout en ms).
  - `StageRetryPolicy` — estrategia de reintento (máximo, backoff `NONE`/`FIXED`/`EXPONENTIAL`, stages elegibles).
  - `StageTimeoutConfig` — timeout por stage (por defecto y específico) y acción ante timeout.
  - `StageExecutionStats` — estadística individual de ejecución (duración, éxito/fallo, intentos, timeout).
  - `PipelineMetrics` — métricas inmutables del pipeline (implementa `EngineResult`).
  - `PipelineExecutionException` — excepción de dominio (`TIMEOUT`/`RETRY_EXHAUSTED`/`UNEXPECTED`).
  - `PipelineErrorHandler` — clasificador determinista de errores (detección de `TimeoutException`).
- `com.kinplatform.kin.conversation.ResponseFallback` — fallback determinista de respuesta ante
  `accepted=false` (respuesta enlatada en español / reintento acotado).
- **Pipeline resiliente** (E3): `Pipeline` con error handling por stage (retry/fail/skip), timeout
  por stage y métricas — manteniendo la firma congelada `execute(PipelineContext)` y la API de
  `PipelineStage`; getter aditivo `Pipeline.metrics()`.
- **Semántica completa de eventos** (E4): `EventStage` emite `question_generated` (ASK),
  `report_generated` (solo si el reporte fue generado), `score_calculated`, `risk_detected` (por
  cada riesgo) y `conversation_completed` (siempre), en orden determinista y sin duplicados.
- **Consumo de `ResponseValidation`** (E5):
  - `ConversationOrchestrator` (bloqueante): reintento acotado (re-invocando `KinMethod`) y
    respuesta segura al agotarse; nunca lanza ni devuelve null.
  - `ConsultorStage` (streaming): reintento acotado re-suscribiendo el flujo del LLM; no rompe SSE.
  - `KinMethod`: safety net que anexa la respuesta segura si la validación final quedó rechazada.
  - `ChatOrchestratorServiceImpl`: consume el resultado del fallback (log de validación).

### Changed

- Pipeline de 13 etapas **resiliente**: cada stage ejecutado con política (fail-fast por defecto,
  retry solo donde la política lo permite), timeout medido y métricas internas.
- `ConversationOrchestrator`, `ConsultorStage` y `KinMethod` ganan constructores aditivos con
  `ResponseFallback` (compatibilidad total; firmas públicas intactas).
- ADR-017 pasa de **Propuesto** a **Aprobado**; `FASE9_0.md` queda **FINALIZADA**.

### Improved

- Observabilidad: métricas de duración/éxito/fallo/reintentos por stage (internas, sin persistir).
- Robustez de la comunicación: ante una respuesta inválida del LLM, KIN reintenta (acotado) o
  entrega una respuesta segura determinista.
- Semántica de eventos completa según el flujo real de la conversación.

### Fixed

- `ResponseValidation` dejó de ser un artefacto de auditoría: ahora es consumido en bloqueante y
  streaming (fallback/reintento determinista), tal como documentaba BASELINE §7.5 Prioridad 2.
- `RiskDetectedEvent` (existente) ahora se emite realmente por cada riesgo del flujo REPORT.

### Architecture

- Bounded context `com.kinplatform.kin.pipeline.resilience` (dominio POJO, sin Spring/JPA/IA).
- Clean Architecture + DDD + Ports & Adapters respetados; contratos congelados intactos (solo
  aditivos sancionados por ADR-017).
- Principio rector preservado: **Java decide. El LLM únicamente comunica.**

### Testing

- 27 tests nuevos (de 1183 a **1210**): dominio `kin/pipeline/resilience/` (8 clases), tests del
  pipeline (resilience/retry/timeout/metrics/failure), `EventStageSemanticsTest`/
  `EventStageOrderingTest`/`EventStageCompatibilityTest`, fallback (`ConversationOrchestratorFallbackTest`,
  `ConsultorStageFallbackTest`, `KinMethodFallbackTest`, `StreamingFallbackTest`,
  `ResponseFallbackExecutionTest`) e integración end-to-end (`EndToEndPipelineIntegrationTest`,
  `PipelineControllerIntegrationTest`, `PipelineFlowIntegrationTest`).
- `./mvnw clean verify`: **1210 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.pipeline` 93.7 %; `kin.pipeline.stage` 97.4 %; `kin.pipeline.resilience`
  99.7 %; `kin.conversation` 99.2 %; `kin.reporting*` 99.0 %; `kin.enrichment*` 97.7 %.

### Documentation

- ADR-017 (**Aprobado**), `FASE9_0.md` (**FINALIZADA**), `BASELINE_ARCHITECTURE.md` (+ sección KIN 2.1).
- `README.md` y `CHANGELOG.md` actualizados para la release.

### Performance

- Métricas internas por stage (duración, éxito/fallo, reintentos, timeout) capturadas sin persistir
  ni exponer (sin Micrometer ni Actuator); overhead de medición mínimo (`System.nanoTime`).
- Timeout por stage medido de forma síncrona sin infraestructura externa (solo Java 17).
- Default conservador sin reintentos (`maxRetries=0`) para no re-ejecutar el pipeline sin configurar;
  el reintento con backoff (`NONE`/`FIXED`/`EXPONENTIAL`) solo cuando la política lo permite.

### Security

- `ResponseGuard` ahora se consume en bloqueante y streaming: una respuesta inválida del LLM nunca
  se entrega tal cual (reintento acotado o respuesta segura determinista en español).
- El fallback es 100 % Java y determinista: no incorpora contenido del LLM ni infiere intención;
  ninguna credencial ni secreto nuevo se expone.
- Las métricas del pipeline permanecen internas (no se exponen vía REST/Actuator).

### Compatibility

- **Backward compatible**: todos los cambios son aditivos; ningún contrato congelado del baseline
  se modificó (solo aditivos sancionados por ADR-017).
- Constructores de `ConversationOrchestrator`, `ConsultorStage` y `KinMethod` con overload aditivo
  `ResponseFallback`; los constructores históricos conservan su comportamiento.
- 1210 tests verdes sin modificación de aserciones; API REST y contratos de streaming SSE intactos.
- Offline-first preservado: sin reintentos configurados, el pipeline se comporta como antes.

### Known Limitations

- `kin.event` con cobertura baja (61 %) — paquete no sujeto al requisito de dominio; records finos.
- Semántica de reintentos: `StagePolicy.retriesExhausted` (intentos totales ≤ maxRetries+1) y
  `ResponseFallback.shouldRetry` (intento ≤ maxRetries) difieren en un intento; ambas internamente
  consistentes y testeables, documentadas en el dominio.
- `Pipeline.java` al 93.7 %: rutas defensivas (interrupción de backoff, guardas nulas) sin cubrir.
- Retry bloqueante re-invoca `KinMethod` (eventos publicados por intento, acumulados en el
  `TurnResult`) — comportamiento documentado; default conservador sin reintentos.

## [Unreleased]

### Added

- **FASE 10 — Módulo Enterprise (ADR-018, Bounded Context de generación de documentos)**
  implementado y sancionado:
  - `kin.enterprise` (dominio POJO, sin Spring): aggregate `EnterpriseProject` con máquina
    `REQUESTED → RUNNING → COMPLETED | FAILED` y versionado `(projectId, version)`, 20+ value
    objects con invariantes y 8 motores deterministas puros aislados de `EngineRegistry`.
  - Aplicación: `EnterpriseGenerationService`, `EnterpriseGenerationOrchestrator`,
    `EnterpriseExportService/Orchestrator`, `DefaultEnterpriseProjectTrigger`,
    `EnterpriseProjectRequestedListener` (generación asíncrona), `EnterpriseRendererFactory`,
    `ProgressPublishingEnterpriseProjectRepository`.
  - Infraestructura: `ai.enterprise.adapter` (JPA, tablas `enterprise_project`/`enterprise_document`,
    migración `V7__create_enterprise_project.sql`).
  - Exportación: renderers PDF/DOCX/PPTX (JDK puro) + bundle ZIP.
  - REST + OpenAPI: `EnterpriseController` (11 endpoints), `EnterpriseDashboardController`,
    `EnterpriseProgressController` (SSE con heartbeat de 15 s).
  - Frontend: `kin-enterprise-ui` (React 19 + Vite + Vitest, **55 tests**, ~96 % cobertura).
  - **Ciclo automático (M3B)**: la conversación dispara la generación al completar `REPORT` vía
    `EnterpriseProjectTrigger` → `DomainEventBus` → listener → generación asíncrona; beans de
    trigger/listener/executor en `EnterpriseWebConfig` y `KinConfig` inyecta el trigger real al
    `ConversationOrchestrator` (constructor aditivo, `NO_OP_TRIGGER` fuera de producción).

### Changed

- ADR-018 pasa de **Propuesto** a **Aprobado**; `AUDITORIA_ENTERPRISE_M3.md` documenta el estado y
  el roadmap M3 (M3A..M3H).
- `KinConfig.conversationOrchestrator(...)` inyecta `EnterpriseProjectTrigger` (wiring aditivo; sin
  cambios en contratos congelados ni en las firmas `orchestrate/orchestrateStream`).
- `EnterpriseWebConfig` define los beans `enterpriseGenerationExecutor`, `enterpriseProjectTrigger`
  y `enterpriseProjectRequestedListener`.

### Improved

- Enterprise se activa desde el flujo real del pipeline (turno REPORT) con progreso SSE y
  persistencia durable; idempotencia garantizada por versión.

### Fixed

- El ciclo automático dejó de ser código muerto: `DefaultEnterpriseProjectTrigger` y
  `EnterpriseProjectRequestedListener` ahora son beans Spring y el `ConversationOrchestrator` usa el
  trigger real en producción.
- Corrección de arranque en PostgreSQL: `pricing_plans.features` (JSONB) se mapea con
  `@JdbcTypeCode(SqlTypes.JSON)` y se añade la migración `V8__enforce_pricing_plans_features_jsonb.sql`;
  el seed de planes ya no falla con "column features is of type jsonb but expression is of type
  character varying" en Docker Compose.

### Changed

- **M3H (infraestructura de producción) completado**: `kin-database/init.sql` sincronizado con la
  migración V7 (tablas `enterprise_project`/`enterprise_document`, score `@Embedded`); `docker compose
  up --build` verificado (PostgreSQL + Backend + Frontend) con Flyway V1..V8 migrando desde una base
  vacía y el `DataInitializer` sembrando los planes en JSONB. CORS dual validado para el origin del
  frontend (`http://localhost:3000`).

### Planned

FASE 9 (KIN 2.1 — "Pipeline Estabilizado") fue **publicada** en la release **`v1.1.0-phase9`**
(Tag `v1.1.0-phase9` publicado · **Latest Release**); sus novedades están documentadas en su
entrada del changelog. Nada de esta fase queda pendiente de implementación.

FASE 10 (Enterprise, ADR-018): **COMPLETADA**. Roadmap M3 cerrado — M3A (documentación) y M3B
(ciclo automático) completados; M3C (resultados reales del pipeline en la generación), M3D
(Enterprise Score persistido), M3E (EXECUTIVE_REPORT/DOFA + narrativa LLM), M3F/G (integración UI en
kin-frontend y acción de generación desde la UI) y M3H (infraestructura de producción: `init.sql`
sincronizado con V7 y despliegue Docker Compose verificado con Flyway V1..V8 desde cero).

FASE 11 (planeada): reemplazo de la heurística de longitud en `ScoringEngine` (KIN 2.5), EventBus
async (KIN 2.4), provider deduplication (KIN 2.3) y despliegue en producción (Render/Neon).

## [v1.0.0-phase8] - 2026-08-02

**Primera release estable del proyecto.** Cierra oficialmente las Fases 6, 7 y 8
(Knowledge Engine, Strategic Interview Engine y Knowledge-Enhanced Analysis). Estado:
**FASE 8 COMPLETADA** — ADR-016 **Aprobado**, pipeline de 13 etapas, BUILD SUCCESS con
**1049 tests** (0 failures, 0 errors, 0 skipped), cobertura de dominio ≥ 90 % (JaCoCo) y 16 ADRs
aprobadas (ADR-001 … ADR-016).

### Added

- Nuevo paquete de dominio `com.kinplatform.kin.enrichment` (ADR-016, POJO puro, sin Spring):
  - `EnrichmentEngine` — motor canonizado que implementa `DomainEngine<EnrichmentInput, EnrichmentResult>` (fase `ANALYSIS`, tipo `DOMAIN`, prioridad 55); delega en `FactRanker` y degrada a `EnrichmentResult.empty()` (offline-first).
  - `FactRanker` — selección y ponderación determinista de hechos por categoría (mercado, innovación, financiero, competitivo) con score por `SourceTrust`/frescura/cobertura y dedup; nunca consulta al LLM.
  - Tipos puros: `EvidenceCategory`, `EvidenceScore`, `KnowledgeEvidence`, `EvidenceRank`, `EnrichmentInput`, `EnrichmentResult`, puerto `EnrichmentRepository`.
- `com.kinplatform.kin.enrichment.stage.EnrichmentStage` — etapa aditiva de pipeline (composición pura sobre `EngineStage`, patrón ADR-011/014/015): corre entre `KnowledgeStage` y `ScoringStage`, construye `EnrichmentInput` (contexto + `PipelineContext.knowledgeResult`), invoca el motor y escribe `PipelineContext.enrichmentResult`.
- Inputs aditivos a los motores de análisis: `RecommendationInput.withEnrichment(...)`, `RiskInput.withEnrichment(...)`, `OpportunityInput.withEnrichment(...)` y `ReportInput.withEnrichment(...)` — constructores originales intactos.
- Los analizadores de mercado, innovación, financiero y competitivo (Recommendation/Risk/Opportunity) leen los hechos relevantes como evidencia.
- Sección aditiva de fuentes en el reporte (frontera ADR-012 sancionada aditivamente):
  - `SourcesSection` (11.ª sección, `ReportSectionKind.SOURCES`) + `CitedSource` en `ConsultingReport`.
  - `SourcesSectionAssembler` — ensambla las fuentes citadas desde el `EnrichmentResult` (dedupe por `sourceId`, mejor score).
  - `SourcesSectionFormatter` — formatea la sección de fuentes como Markdown ligero.
  - `ReportEngine` orquesta ahora **11** `SectionAssembler` vía `ReportAssemblers`.
- Integración aditiva al pipeline (E6):
  - `PipelineContext` + campo tipado `EnrichmentResult enrichmentResult` (+ getter `enrichmentResult()` + `withEnrichmentResult(...)` + constructor aditivo; constructor de compatibilidad intacto).
  - `RecommendationStage`, `RiskStage`, `OpportunityStage` y `ReportStage` aplican `input.withEnrichment(context.enrichmentResult())` cuando existe.
  - `ReportPromptBuilder` reconoce `SourcesSection` y la formatea con `SourcesSectionFormatter` (búsqueda opcional con fallback; las 10 secciones históricas intactas).
  - Beans en `KinConfig`: `FactRanker`, `EnrichmentEngine`, `EnrichmentStage`, `SourcesSectionFormatter`; `chatPipeline(...)` inserta `EnrichmentStage` entre `KnowledgeStage` y `ScoringStage` (pipeline de 13 etapas).

### Changed

- Pipeline de 12 → **13 etapas**: `Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Enriquecimiento → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.
- `ConsultingReport` de 10 → **11 secciones** (aditivo: la sección de fuentes se omite cuando está vacía, comportamiento idéntico al previo).
- `ReportEngine` coordina 11 `SectionAssembler` sin cambiar su lógica (orquestador puro, ADR-011 intacto).
- ADR-016 pasa de **Propuesto** a **Aprobado**; `FASE8_0.md` queda **FINALIZADA**.
- `BASELINE_ARCHITECTURE.md`, `AGENTS.md`, `KIN_ARCHITECTURE_GOVERNANCE.md` y `README.md` actualizados a FASE 8 COMPLETADA.

### Improved

- El conocimiento externo de la Fase 6 ahora **se capitaliza**: recomendaciones, riesgos y oportunidades se sustentan en hechos verificados.
- **Trazabilidad en la comunicación**: el consultor cita las fuentes ya seleccionadas por Java (`SourcesSection`), respetando la frontera ADR-012.
- Offline-first preservado: sin hechos, `EnrichmentResult.empty()` y el pipeline se comporta exactamente como antes de la Fase 8.

### Fixed

- Defecto crítico de la auditoría E7: `ReportPromptBuilder` no conocía `SourcesSection` y lanzaba `IllegalArgumentException` al formatear un reporte enriquecido. Corregido con integración aditiva de `SourcesSectionFormatter` (+ test de integración de extremo a extremo).

### Architecture

- Bounded context `com.kinplatform.kin.enrichment` (dominio POJO, sin Spring/JPA/IA).
- Clean Architecture + DDD + Ports & Adapters respetados; contratos congelados intactos (solo aditivos sancionados por ADR-016).
- Sin dependencias circulares funcionales; el patrón `stage ↔ pipeline` es el sancionado por ADR-014/015/016.
- Principio rector preservado: **Java decide. El LLM únicamente comunica.**

### Testing

- 147 tests nuevos (de 902 a **1049**): `kin/enrichment/` (tipos, `FactRankerTest`/`FactRankerCategoryTest`/`FactRankerFreshnessTest`, `EnrichmentEngineTest`, `EnrichmentInputTest`, `EnrichmentResultTest`, `EnrichmentRepositoryTest`), `kin/enrichment/stage/` (`EnrichmentStageTest` 11, `EnrichmentStagePipelineTest` 7), `PipelineContextTest` (8), `ReportPromptSourcesIntegrationTest` (1, cierre del defecto E7), `RecommendationInputEnrichmentTest`, `RiskInputEnrichmentTest`, `RiskAnalyzerEnrichmentTest`, `OpportunityInputEnrichmentTest`, `OpportunityAnalyzerEnrichmentTest`, `ReportInputEnrichmentTest`, `ReportEngineSourcesTest`, `SourcesSectionAssemblerTest`, `SourcesSectionTest`, `CitedSourceTest`, `SourcesSectionFormatterTest`.
- `./mvnw clean verify`: **1049 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.enrichment*` **97.65 %**; `kin.reporting*` 98.96 %; `kin.ai.prompt*` 98.64 %; `kin.pipeline*` 96.29 %; `kin.conversation` 100 %; `kin.knowledge` 100 %; `ai.knowledge.adapter` 100 %; `kin.interview` + adapter 98.39 %. Requisito de ≥ 90 % en `kin.enrichment` cumplido.

### Documentation

- ADR-016 (**Aprobada**), `FASE8_0.md` (**FINALIZADA**), `BASELINE_ARCHITECTURE.md`, `AGENTS.md` actualizados.
- Release notes: `kin-docs/releases/v1.0.0-phase8.md`.
- Guía de demostración: `docs/demo/DEMO.md`.
- `README.md` y `CHANGELOG.md` actualizados para la release.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.
- `ResponseValidation` (bloqueante y streaming) es hoy un artefacto de auditoría sin consumidor en producción; el fallback (respuesta enlatada) se define en KIN 2.1.
- Los adaptadores de conocimiento (`ai.knowledge.adapter`) están implementados con mocks (sin red real); el enriquecimiento efectivo requiere configurar la allowlist/fuentes en producción.
- `EnrichmentRepository` (puerto de dominio) sin adaptador de infraestructura todavía (etapa posterior, por diseño ADR-016).

### Fase 7 (Strategic Interview Engine)

Enmienda de `v2.0.0-alpha1` con ADR-015. **Estado: Architecture Stable (enmendado). FASE 7 CERRADA OFICIALMENTE (2026-08-01).**

Principio: **Java decide. El LLM únicamente formula preguntas.** La entrevista estratégica se orquesta en Java (`InterviewEngine`/`InterviewBlueprint`/`AnswerValidator`); el LLM solo recibe el tema de la entrevista vía sección `## ENTREVISTA ESTRATÉGICA` y formula la pregunta sin decidir su contenido.

### Added

- Nuevo paquete de dominio `com.kinplatform.kin.interview` (ADR-015, POJO puro, sin Spring):
  - `InterviewEngine` — motor canonizado que implementa `DomainEngine<InterviewInput, InterviewResult>` (fase `VALIDATION`, tipo `DOMAIN`); orquesta el ciclo pregunta → respuesta → validación → progreso.
  - `InterviewBlueprint` — decide en Java la siguiente pregunta (secuencia por `AnalyzedDimension`, preguntas requeridas/opcionales, follow-ups).
  - `AnswerValidator` — validador determinista (acepta/refina/rechaza la respuesta del usuario; evalúa concretud, número, dimensión, opciones).
  - Tipos puros: `InterviewInput`, `InterviewResult`, `InterviewState`, `InterviewDecision` (`ASK`/`REPORT`), `InterviewDirective`, `InterviewAnswer`, `InterviewContext`, `InterviewProgress`, `InterviewRequest`, `AnswerRules`, `AnswerValidation`, puerto `InterviewRepository`.
- `com.kinplatform.kin.pipeline.stage.InterviewStage` — etapa aditiva de pipeline (composición pura sobre `EngineStage`, patrón ADR-011): corre entre `StrategistStage` y `KnowledgeStage`, construye `InterviewInput`, persiste el estado vía `InterviewRepository` y aplica la decisión efectiva (ASK mientras incompleta / REPORT al completarse).
- Infraestructura `com.kinplatform.ai.interview.adapter` (E5…E6, detrás del puerto `InterviewRepository`): `JpaInterviewRepository`, `InterviewStateEntity`, `InterviewStateJpaRepository`, `InterviewStateMapper` (tabla `interview_state`).
- Migración Flyway `V4__create_interview_state.sql` + tabla en `kin-database/init.sql`.
- Integración aditiva al pipeline (E6):
  - `PipelineContext` + campo `InterviewResult interviewResult` (+ getter/setter) — patrón ADR-011.
  - `PromptRequest.forConversation(context, decision, directive)` + overload aditivo ADR-015; `PromptAssembler.assemble(PromptRequest, InterviewResult)` propaga la entrevista en modo CONVERSATION (REPORT la ignora).
  - `ConversationPromptBuilder.build(request, interviewResult)` + sección `## ENTREVISTA ESTRATÉGICA` (tema + `AnswerRules`) cuando hay un `InterviewDirective` pendiente.
  - `ConsultorStage` selecciona el prompt de conversación con entrevista cuando la entrevista está activa con decisión `ASK`, `forReport(...)` cuando `decision.shouldGenerateReport()` y `forConversation(...)` en otro caso.
  - Beans en `KinConfig`: `InterviewEngine`, `InterviewBlueprint`, `AnswerValidator`, `InterviewRepository` (adaptador JPA), `InterviewStage`; `chatPipeline(...)` inserta `InterviewStage` entre `StrategistStage` y `KnowledgeStage` (pipeline de 12 etapas).
- Documentación: ADR-015 (**Aprobada**), `FASE7_STRATEGIC_INTERVIEW_ENGINE.md` (diseño + bitácora E1…E7 + criterios de aceptación + cierre), `AGENTS.md`, `BASELINE_ARCHITECTURE.md`, `KIN_ARCHITECTURE_GOVERNANCE.md` actualizados.

### Changed

- Pipeline de 11 → **12 etapas**: `Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.
- Mientras la entrevista está incompleta la decisión efectiva es `ASK` (prioridad 9) y las etapas de análisis se omiten; al completarse la decisión es `REPORT` y el reporte se genera en el mismo turno.
- `KIN_ARCHITECTURE_GOVERNANCE.md`: regla de IA §7.2 (nuevas reglas 8 y 9: *Java decide la entrevista; el LLM únicamente formula preguntas*), §1.11 +3 decisiones Java (`InterviewEngine`, `InterviewBlueprint`, `AnswerValidator`), §6 `InterviewEngine` → Existente.
- `BASELINE_ARCHITECTURE.md`: inventario + bounded contexts `kin.interview`/`interview.stage`/`interview.adapter`, +1 motor (`InterviewEngine`), pipeline 12 etapas, contratos congelados + (`InterviewEngine`/`InterviewResult`, `InterviewRepository`, `InterviewStage`), decisión congelada #16, migraciones V1…V4, cobertura actualizada, §7 siguiente hito KIN 2.1.

### Testing

- 227 tests nuevos (de 675 a **902**): dominio `kin/interview/` (tipos/enums/records, `InterviewStateTest`, `InterviewDecisionTest`, `InterviewDirectiveTest`, `AnswerRulesTest`, `AnswerValidationTest`, `InterviewEngineTest`, `InterviewBlueprintTest`, `AnswerValidatorTest`, `InterviewStageTest`, `InterviewStagePipelineTest`), adapter `ai/interview/` (`JpaInterviewRepositoryTest`, `InterviewStateEntityTest`, `InterviewStateMapperTest`), `PromptAssemblerInterviewTest` (3), `ConversationPromptBuilderInterviewTest` (6), `ConversationOrchestratorInterviewIntegrationTest` (6), `ConsultorStageTest` +4 (gating ASK/REPORT de entrevista), `KinMethodTest` + pipeline de 12 etapas.
- `./mvnw clean verify`: **902 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): **`kin.interview` + `ai.interview.adapter` 98.39 %** (engine 95 %, stage 100 %, adapter 100 %); `kin.conversation` 100 %; `kin.knowledge` 100 %; `ai.knowledge.adapter` 100 %; `kin.ai` 99.7 %; `kin.ai.prompt` 95.9 %; `kin.ai.prompt.formatter` 99.9 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %; `kin.scoring` 95.1 %. Requisito de ≥ 90 % en `kin.interview` cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.
- `ResponseValidation` (bloqueante y streaming) es hoy un artefacto de auditoría sin consumidor en producción; el fallback (respuesta enlatada) se define en KIN 2.1.
- Los adaptadores de conocimiento (`ai.knowledge.adapter`) están implementados con mocks (sin red real); el enriquecimiento efectivo del análisis y la exposición de conocimiento citado en la comunicación (frontera ADR-012) son trabajo futuro.
- El módulo `KnowledgeStage`/`InterviewStage` aún no contribuye al pipeline de scoring efectivo (KIN 2.1): la entrevista valida dimensiones pero las etapas de análisis se omiten mientras está activa.

### Fase 6 (External Knowledge Acquisition)

Enmienda de `v2.0.0-alpha1` con ADR-014. **Estado: Architecture Stable (enmendado). FASE 6 CERRADA OFICIALMENTE (2026-07-31).**

### Added

- Nuevo paquete de dominio `com.kinplatform.kin.knowledge` (ADR-014, POJO puro, sin Spring):
  - `KnowledgeEngine` — motor canonizado que implementa `DomainEngine<KnowledgeInput, KnowledgeResult>` (fase `KNOWLEDGE`, tipo `DOMAIN`, prioridad 50); delega en `KnowledgeGateway` y degrada a `KnowledgeResult.empty()` (offline-first).
  - `KnowledgeGateway` — coordinador de la adquisición: deriva `KnowledgeQuery`, consulta `SourceRegistry`, delega la validación en `SourceValidator` y normaliza los `KnowledgeFact` con métricas deterministas (confianza/calidad).
  - `SourceRegistry` — registro de `KnowledgeSource` por auto-descubrimiento (`List<KnowledgeSource>`, patrón `EngineRegistry`); orden preservado, vista inmutable.
  - `SourceValidator` — validador determinista (HTTPS, allowlist de dominios, estado HTTP 2xx, tipo de contenido, formato, frescura TTL, deduplicación por `(sourceId, url)`, `SourceTrust`); stateless y reentrante; nunca consulta al LLM.
  - Tipos puros: `KnowledgeRequest`, `KnowledgeQuery`, `KnowledgeCandidate`, `KnowledgeFact`, `KnowledgeInput`, `KnowledgeResult`, `SourceValidation`, `SourceTrust`, puerto `KnowledgeSource`, puerto `KnowledgeRepository` (caché TTL).
- `com.kinplatform.kin.knowledge.stage.KnowledgeStage` — etapa aditiva de pipeline (composición pura sobre `EngineStage`, patrón ADR-011): construye la `KnowledgeRequest` desde el `ProjectContext` e invoca `KnowledgeEngine`; escribe `PipelineContext.knowledgeResult`.
- Infraestructura `com.kinplatform.ai.knowledge.adapter` (E4…E5, todos detrás del puerto `KnowledgeSource`): `CompositeKnowledgeSource`, `HttpKnowledgeSourceAdapter` (allowlist de hosts, mitigación SSRF), `PublicApiConnector` (OCP), `JdbcKnowledgeSource`, `RagKnowledgeSource`, `DocumentKnowledgeSource`. Mocks en tests (sin red).
- Integración aditiva al pipeline (E6):
  - `PipelineContext` + campo `KnowledgeResult knowledgeResult` (+ getter/setter) — patrón ADR-011.
  - `KinConfig` + beans `SourceValidator`, `SourceRegistry`, `KnowledgeGateway`, `KnowledgeEngine`, `KnowledgeStage`; `chatPipeline(...)` inserta `KnowledgeStage` entre `StrategistStage` y `ScoringStage` (pipeline de 11 etapas).
- Documentación: ADR-014 (**Aprobada**), `FASE6_0_EXTERNAL_KNOWLEDGE.md` (diseño + bitácora E1…E7 + cierre), `AGENTS.md`, `BASELINE_ARCHITECTURE.md`, `KIN_ARCHITECTURE_GOVERNANCE.md` actualizados.

### Changed

- Pipeline de 10 → **11 etapas**: `Analizador → Evaluador → Estratega → Conocimiento → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.
- `KIN_ARCHITECTURE_GOVERNANCE.md`: regla de IA §7.2 (nueva regla 7: *Java decide sobre el conocimiento; las fuentes únicamente aportan conocimiento*), §1.11 +3 decisiones Java (`KnowledgeGateway`, `SourceValidator`, `KnowledgeEngine`), §6.2 `KnowledgeEngine` → Existente.
- `BASELINE_ARCHITECTURE.md`: inventario + bounded contexts `kin.knowledge`/`knowledge.stage`/`knowledge.adapter`, +1 motor, pipeline 11 etapas, contratos congelados +5, decisión congelada #15, cobertura actualizada.

### Testing

- 207 tests nuevos (de 468 a **675**): 22 en `kin/knowledge/` (tipos/enums/records 8, `SourceRegistryTest`/`SourceValidatorTest`/`KnowledgeGatewayTest`/`KnowledgeEngineTest` 10 en `engine/`, `KnowledgeStageTest` + `KnowledgeStagePipelineTest` 4 en `stage/`), 10 en `ai/knowledge/adapter/` (`CompositeKnowledgeSourceTest`, `HttpKnowledgeSourceAdapterTest`, `PublicApiConnectorTest`, `JdbcKnowledgeSourceTest`, `RagKnowledgeSourceTest`, `DocumentKnowledgeSourceTest`, `KnowledgeEngineAdapterIntegrationTest`, `KnowledgeAdapterGatewayPropagationTest`), `KinMethodTest` + pipeline de 11 etapas.
- `./mvnw clean verify`: **675 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): **`kin.knowledge` 100 %** (engine 99.47 %, stage 100 %), **`ai.knowledge.adapter` 100 %**; `kin.conversation` 100 %; `kin.ai` 99.7 %; `kin.ai.prompt` 99.7 %; `kin.ai.prompt.formatter` 99.9 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %; `kin.scoring` 95.1 %. Requisito de ≥ 90 % en `kin.knowledge` cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.
- `ResponseValidation` (bloqueante y streaming) es hoy un artefacto de auditoría sin consumidor en producción; el fallback (respuesta enlatada) se define en KIN 2.1.
- Los adaptadores de conocimiento (`ai.knowledge.adapter`) están implementados con mocks (sin red real); el enriquecimiento efectivo del análisis y la exposición de conocimiento citado en la comunicación (frontera ADR-012) son trabajo futuro.

## [v2.0.0-alpha1] - 2026-07-31

**Primera release estable del núcleo inteligente de KIN.** Estado: `ALPHA STABLE` — Build
reproducible, contratos congelados, arquitectura validada. Cierra las fases 5.4 (ReportEngine),
5.5 (PromptAssembler) y 5.6 (Conversation Orchestrator). Tag: `v2.0.0-alpha1`. Commit: `89b39b9`.

**Principio arquitectónico**: *Java decide. El LLM únicamente comunica.* Pipeline de 10 etapas
(Analyzer → Evaluator → Strategist → Scoring → Recommendation → Risk → Opportunity →
ReportEngine → Consultor → Events) que produce el `ConsultingReport` (10 secciones) con una
conversación dirigida por `ConversationOrchestrator` (ADR-013). 13 ADRs aprobadas (ADR-001 …
ADR-013), 21 paquetes de dominio `com.kinplatform.kin.*`.

### Métricas

- `./mvnw clean verify`: **468 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.conversation*` 100 % (738/738); `kin.ai*` 99.7 %; `kin.ai.prompt`
  99.7 %; `kin.ai.prompt.formatter` 99.9 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %;
  `kin.scoring` 95.1 %. Requisito de dominio ≥ 90 % cumplido.

### Notas

- Release notes oficiales en `kin-docs/releases/KIN_2_0_ALPHA_1.md`.
- `README.md`, `AGENTS.md`, `BASELINE_ARCHITECTURE.md`, `KIN_ARCHITECTURE_GOVERNANCE.md`
  actualizados para la release.
- Documentación de fase agrupada a continuación (5.6 → 5.2.1); el milestone original 4.0–5.2 se
  conserva en su entrada `[v2.0.0-alpha.1] - 2026-07-30`.

### Fase 5.6 (Conversation Orchestrator)

Enmienda de `v2.0.0-alpha.1` con ADR-013. **Estado: Architecture Stable (enmendado). FASE 5.6 CERRADA OFICIALMENTE (2026-07-31).**

### Added

- Nuevo paquete de dominio `com.kinplatform.kin.conversation` (ADR-013, sin Spring):
  - `ConversationOrchestrator` — fachada del ciclo de turno: `orchestrate(ConversationTurn) → TurnResult` y `orchestrateStream(ConversationTurn) → Flux<String>`; compone `HistoryWindow` + `TurnPolicy` + `KinMethod` (contrato congelado) + `ResponseGuard` + `ContextRepository`.
  - `TurnPolicy` / `DefaultTurnPolicy` — política de turno determinista: decide en Java fase/modo/restricciones desde la decisión previa persistida (pre-pipeline); mapeo exhaustivo de las 7 acciones de `ConversationDecision`.
  - `ResponseGuard` — guardrail de comunicación (vacío, longitud, pregunta única, marcadores prohibidos; `accepted = issues.isEmpty()`). Responsabilidad: bloqueante → orquestador; streaming → `ConsultorStage` (enmienda M3).
  - `HistoryWindow` — presupuesto de contexto por número de mensajes (default 20; el mensaje del usuario del turno siempre se incluye).
  - Tipos de turno puros: `ConversationPhase` (`EXPLORATION`, `REPORTING`, `CLOSED`), `CommunicationMode` (`QUESTION`, `EXPLAIN_REPORT`, `SUMMARY`, `FAREWELL`), `TurnConstraints`, `ConversationTurn`, `TurnDirective`, `ResponseValidation`, `TurnResult`.
- Integración aditiva (E6, enmiendas M1/M2/M3):
  - `KinMethodCommand` + campo `TurnDirective directive` (overload; constructor histórico intacto).
  - `PipelineContext` + campos `turnDirective` y `responseValidation` (patrón ADR-011).
  - `PromptRequest.forConversation(context, decision, directive)` (overload; factory ADR-012 intacto; REPORT refuerza `directive=null`).
  - `ConversationPromptBuilder` + sección `## DIRECTIVA DE COMUNICACIÓN` cuando `request.directive() != null` (sancionada por enmienda M2).
  - `ConsultorStage` consume la directiva para enmarcar el prompt; en streaming aplica `ResponseGuard` (`attachStreamGuard`) y deja `responseValidation` en contexto.
  - Beans en `KinConfig`: `DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, `ConversationOrchestrator`.
- `ChatOrchestratorServiceImpl` delega `/chat` y `/chat/stream` en `ConversationOrchestrator` (cambio de aplicación, no de contrato).
- Documentación: ADR-013 (aprobada con enmiendas M1/M2/M3), `FASE5_6_CONVERSATION_ORCHESTRATOR.md` (diseño + cierre E7), `AGENTS.md`, `BASELINE_ARCHITECTURE.md`, `KIN_ARCHITECTURE_GOVERNANCE.md` actualizados.

### Changed

- **M1 (comportamiento aprobado)**: en el primer turno que genera el `ConsultingReport`, la directiva pre-pipeline deriva de la decisión previa (típicamente `(EXPLORATION, ASK, QUESTION)`); los turnos posteriores reciben `(REPORTING, REPORT, EXPLAIN_REPORT)`. `ConsultorStage` usa `PromptRequest.forReport` (frontera ADR-012 intacta).
- **M3 (guard unificado)**: modo bloqueante → `ConversationOrchestrator` emite `TurnResult.validation`; modo streaming → `ConsultorStage.attachStreamGuard` deja `PipelineContext.responseValidation`.
- `KinMethod.prepare` propaga `command.directive()` → `ctx.turnDirective(...)`.
- `KIN_ARCHITECTURE_GOVERNANCE.md` §1.11 y §7 regla 6: `TurnPolicy`, `HistoryWindow`, `ResponseGuard` y `ConversationOrchestrator` como decisiones Java del ciclo conversacional.
- `BASELINE_ARCHITECTURE.md`: inventario +`kin.conversation`, contratos congelados +7 tipos y 4 componentes, decisiones congeladas #14, cobertura actualizada.

### Testing

- 130 tests nuevos (de 338 a 468): 113 en `kin/conversation/` (enums/records 26, `HistoryWindowTest` 17, `ResponseGuardTest` 21, `DefaultTurnPolicyTest` 22, `ConversationOrchestratorTest` 24, integración pipeline 3), `PromptRequestDirectiveTest` (6), `ConversationPromptBuilderDirectiveTest` (3), `KinMethodTest` +2 (propagación directiva), `ConsultorStageTest` +6 (modo REPORT, guard streaming, framing).
- Los 338 tests previos permanecen verdes sin modificación de aserciones.
- `./mvnw clean verify`: **468 tests, 0 fallos, 0 errores, 0 skipped, BUILD SUCCESS**.
- Cobertura (JaCoCo): **`kin.conversation` 100 % (738/738 instrucciones)**; `kin.ai` 99.7 %; `kin.ai.prompt` 99.7 %; `kin.ai.prompt.formatter` 99.9 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %; `kin.scoring` 95.1 %. Requisito de ≥ 90 % en `kin.conversation` cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.
- `ResponseValidation` (bloqueante y streaming) es hoy un artefacto de auditoría sin consumidor en producción; el fallback (respuesta enlatada) se define en KIN 2.1.

### Fase 5.5 (PromptAssembler)

Enmienda de `v2.0.0-alpha.1` con ADR-012. **Estado: Architecture Stable (enmendado).**

### Added

- `PromptRequest` (record con validación por tipo + factories `forConversation`/`forReport`) y `PromptType` (`CONVERSATION`, `REPORT`) en `kin.ai` (ADR-012).
- `PromptAssembler` refactorizado como **fachada pura**: `assemble(PromptRequest) → String` delega en `ConversationPromptBuilder` o `ReportPromptBuilder` según `PromptType`. Sin lógica, reglas, fallback ni formateo.
- `kin.ai.prompt.ConversationPromptBuilder`: prompt conversacional (personalidad + contexto mínimo Título/Categoría/Cobertura + `INSTRUCCIÓN ESTRATÉGICA`), sin ninguna sección de reporte.
- `kin.ai.prompt.ReportPromptBuilder`: prompt de reporte con las 10 secciones del `ConsultingReport` formateadas + instrucción fija "Explica, no decidas" ("No añadas secciones nuevas. No recalcules scores."). Dispatch por clase concreta de `SectionFormatter`, no por `ReportSectionKind`.
- 10 `SectionFormatter` en `kin.ai.prompt.formatter`: `ExecutiveSummary`, `ScoresSection`, `RecommendationsSection`, `RisksSection`, `OpportunitiesSection`, `FinancialSection`, `MarketSection`, `InnovationSection`, `NextStepsSection`, `ReportMetadata` — salida determinista con `Locale.ROOT` (fijo cultural `es-CO`).
- Frontera ADR-012: `PromptRequest.forReport` acepta solo `ConsultingReport`; fuentes crudas prohibidas en modo REPORT.
- Documentación: ADR-012 (aprobada), `FASE5_5_PROMPT_ASSEMBLER.md` (bitácora E1…E7), `AGENTS.md`, `BASELINE_ARCHITECTURE.md` actualizados.

### Changed

- `ConsultorStage` se reposiciona de la 4.ª a la 9.ª etapa (tras `ReportStage`, antes de `EventStage`): el LLM recibe el `ConsultingReport` en modo REPORT. Selecciona `PromptRequest.forReport(...)` cuando `decision.shouldGenerateReport()` (lanza `IllegalStateException` si el reporte falta) y `forConversation(...)` en caso contrario.
- Pipeline de 10 etapas: `Analizador → Evaluador → Estratega → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.
- `RisksSectionFormatter`: severidad sin `/10` (severityScore puede superar 10); `Locale.ROOT` en todos los formatos numéricos.
- `KinConfig` inyecta builders + 10 formatters; `AiEngineService` queda como adaptador puro del port `AIResponder`.

### Testing

- 64 tests nuevos (de 274 a 338): `ReportPromptBuilderTest` (10), `ConversationPromptBuilderTest` (11), 10 `SectionFormatterTest` (38), `PromptAssemblerTest` +3 (8), `ConsultorStageTest` +2 modo REPORT (7).
- `./mvnw clean verify`: **338 tests, 0 fallos, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.ai` 100 %; `kin.ai.prompt` 98.8 %; `kin.ai.prompt.formatter` 99.9 %. Requisito de ≥ 90 % en `kin.ai` cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.

### Fase 5.4 (ReportEngine)

Enmienda de `v2.0.0-alpha.1` con ADR-011. **Estado: Architecture Stable (enmendado).**

### Added

- `kin.reporting.report.ReportEngine`: motor de dominio puro, orquestador del `ConsultingReport` (ADR-011). Prioridad 70, fase REPORTING.
- Modelo completo del reporte: `ConsultingReport` (VO raíz inmutable, 10 secciones, ID determinista via `DeterministicId`), `ReportBuilder` (contrato estricto: `validate()`, nulos, duplicados), `ReportMetadata` (versiones, coverage, confidence, `sectionsIncluded` derivado).
- 9 secciones de contenido: `ExecutiveSummary`, `ScoresSection`, `RecommendationsSection`, `RisksSection`, `OpportunitiesSection`, `FinancialSection`, `MarketSection`, `InnovationSection`, `NextStepsSection`.
- VOs auxiliares: `DimensionCoverage` (proyección de `ProjectContext.isDimensionCovered`), `NextStep` (agregación top-3 rec/riesgos/opps con fuente y prioridad ya existente).
- 10 `SectionAssembler` tipados (patrón coordinador): `ExecutiveSummaryAssembler`, `ScoresSectionAssembler`, `RecommendationsSectionAssembler`, `RisksSectionAssembler`, `OpportunitiesSectionAssembler`, `FinancialSectionAssembler`, `MarketSectionAssembler`, `InnovationSectionAssembler`, `NextStepsSectionAssembler`, `ReportMetadataAssembler` — agrupados en `ReportAssemblers` (record tipado, sin casts).
- `ReportStage` (composición pura sobre `EngineStage`): predicado exige 4 resultados + decisión REPORT; 10ª etapa del pipeline entre `OpportunityStage` y `EventStage`.
- Cambios aditivos a contratos congelados (BASELINE §4.1): `PipelineContext.consultingReport` (campo tipado), `KinMethodResult.consultingReport` (componente record) — compatibilidad hacia atrás.
- Documentación: `FASE5_4_REPORT_ENGINE.md` (diseño completo), ADR-011, release notes en `kin-docs/releases/KIN_2_0_FASE_5_4_REPORT_ENGINE.md`.
- 102 tests nuevos (de 172 a 274): modelo (inmutabilidad, copyOf, empty), builder (validate, nulos, duplicados, ID determinista), assemblers (proyección pura, frontera), engine (orquestación, nulidad→empty), stage (predicado, engineResults), integración pipeline 10 etapas.

### Changed

- `KinConfig.chatPipeline(...)`: agrega `ReportStage` al pipeline (10 stages).
- `KIN_ARCHITECTURE_GOVERNANCE.md` §6.2: `ReportEngine` pasa de "Futuro (KIN 2.2/3.0)" a "Existente (Fase 5.4)".
- `BASELINE_ARCHITECTURE.md`: inventario +1 motor, pipeline 10 stages, contratos + ReportEngine/ConsultingReport/ReportStage, cobertura actualizada.

### Testing

- `./mvnw clean verify`: **274 tests, 0 fallos, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.reporting.report.model` 99 %; `kin.reporting.report.assembler` 100 %; `kin.reporting.report` 100 %; `kin.pipeline.stage` 99 %. Requisito de ≥ 90 % en dominio cumplido (agregado: 98,6 % → 99 %+).

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.

### Fase 5.3 (OpportunityEngine)

### Added

- `kin.reporting.opportunity.OpportunityEngine`: motor de dominio puro que identifica oportunidades de mejora/capitalización del proyecto (ADR-010).
- 8 analizadores auto-descubiertos (patrón coordinador + analizadores + ensamblador, mismo diseño que `RiskEngine`): `Market`, `Innovation`, `Technological`, `Financial`, `Competitive`, `Scalability`, `Automation`, `MonetizationOpportunityAnalyzer` — categorías: mercado, innovación, tecnológicas, financieras, competitivas, escalabilidad, automatización, monetización.
- `Opportunity`, `OpportunityResult`, `OpportunityInput`, `OpportunityModel`, `OpportunityAssembler`, `OpportunityExplanation`, `OpportunityCategory`.
- `OpportunityStage` (composición pura sobre `EngineStage`): pipeline de 9 etapas, entre `RiskStage` y `EventStage`.
- Campo tipado aditivo `PipelineContext.opportunityResult` (mismo patrón que `riskResult`).
- Documentación: `FASE5_3_OPPORTUNITY_ENGINE.md` (auditoría, diseño, UML, contratos), ADR-010, release notes en `kin-docs/releases/KIN_2_0_FASE_5_3_OPPORTUNITY_ENGINE.md`.
- 42 tests nuevos (de 130 a 172).

### Changed

- `KinConfig.chatPipeline(...)`: agrega `OpportunityStage` al pipeline (9 stages).
- `KIN_ARCHITECTURE_GOVERNANCE.md` §6.2: `OpportunityEngine` pasa de "Futuro (KIN 3.0)" a existente.

### Testing

- `./mvnw clean verify`: **172 tests, 0 fallos, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.reporting.opportunity` 100 %; `kin.reporting*` agregado 98,6 %; `kin.reporting` 95,8 % (+ `risk` 99,5 %); `kin.scoring` 98,9 %; `kin.engine` 100 %. Requisito de ≥ 90 % cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.

### Fase 5.2.1 (consolidación del runtime)

Enmienda de `v2.0.0-alpha.1` con ADR-006…ADR-009. **Estado: Architecture Stable (enmendado).**

### Added

- `KinMethod.executeStream(KinMethodCommand) → Flux<String>`: punto de entrada único del runtime para streaming; `ConsultorStage` deja el `Flux` en `PipelineContext.aiResponseFlux`.
- `ContextRepository` (puerto, `kin.context`) + `ProjectContext.restore(...)`: contexto durable.
- Adaptador JPA durable: `JpaContextRepository`, `ProjectContextEntity`, `ProjectContextJpaRepository` (`ai.context.adapter`, tabla `project_context`).
- Puerto `AIResponder` + `AIRequest` + `PromptAssembler` (`kin.ai`).
- `ScoringInput` y canonización de `ScoringEngine`/`ScoreResult`/`ScoringStage` bajo `DomainEngine` (ADR-009).
- Migración Flyway `V3__create_project_context.sql` + tabla en `kin-database/init.sql`.
- ADRs 006 (runtime), 007 (context repository), 008 (AI responder/prompt assembler), 009 (engine canonization).
- Documentación: `FASE5_2_1_RUNTIME_CONSOLIDATION.md` (UML antes/después).
- 28 tests nuevos (de 102 a 130).

### Changed

- `ChatOrchestratorServiceImpl` ahora es I/O puro: ambos endpoints (`/chat` y `/chat/stream`) delegan en `KinMethod`.
- `ConsultorStage` depende de `AIResponder` + `PromptAssembler` (no del servicio concreto).
- `EngineInput` pasa de record a interfaz marcadora.
- `ScoringStage` compone `EngineStage` (elimina requisito `scoreResult() != null`).
- Dev: `application.yml` con `ddl-auto: update` + Flyway deshabilitado; prod (Flyway) sin cambios.
- `SecurityConfig`: `/test/**` requiere rol `ADMIN`.
- `DeepSeekConfig`: ya no loguea el prefijo/longitud de la API key.

### Removed

- `ProjectContextService` (`ai/context/`) y su cableado — el ciclo de vida del contexto pasa a `ContextRepository`.

### Testing

- `./mvnw clean test`: **130 tests, 0 fallos, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.engine` 100 %; `kin.reporting` 95,8 % (+ `kin.reporting.risk` 99,5 %); `kin.scoring` 100 %. Requisito de ≥ 90 % cumplido.

### Known Issues

- Incidencia heredada: `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No bloquea el arranque (warnings). Fuera del alcance de esta fase.
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.

## [v2.0.0-alpha.1] - 2026-07-30

Primer hito oficial de la arquitectura KIN 2.0. **Estado: Architecture Stable**.
Cierra las fases 4.0, 5.0, 5.1 y 5.2. Tag: `v2.0.0-alpha.1`. Commit: `91426e5`.

### Added

- Infraestructura común de motores en `kin/engine`: `DomainEngine`, `EngineInput`, `EngineResult`, `EngineMetadata`, `EnginePhase`, `EngineType`, `EngineRegistry`, `EngineExecutor`, `DeterministicId`.
- `EngineStage`: stage genérico de pipeline que delega en cualquier motor.
- `kin.reporting.RecommendationEngine` con `RecommendationResult` y `RecommendationStage`.
- `kin.reporting.risk.RiskEngine` con `RiskResult`, `RiskStage` y analizadores (`RiskAssembler`, riesgos de proceso, pricing y modelo de negocio).
- `PipelineContext.engineResults`: mapa `engineId → EngineResult` para resultados no canonizados.
- Beans `EngineRegistry` y `EngineExecutor` en `common.config.KinConfig`.
- ADRs: `ADR-003` (recommendation engine), `ADR-004` (risk engine), `ADR-005` (engine infrastructure).
- Documentación: `FASE5_0_RECOMMENDATION_ENGINE.md`, `FASE5_1_RISK_ENGINE.md`, `FASE5_2_CONSOLIDACION_ENGINES.md`, `BASELINE_ARCHITECTURE.md`, release notes en `kin-docs/releases/KIN_2_0_ALPHA_1.md`.
- 41 tests nuevos (de 61 a 102).

### Changed

- `RecommendationStage` y `RiskStage` ahora delegan en `EngineStage`.
- Motores, inputs y results canonizados para implementar el contrato `DomainEngine` / `EngineInput` / `EngineResult`.
- `AGENTS.md` actualizado (paquetes, conteo de tests, quirks).

### Refactored

- `kin/engine` como infraestructura común reutilizable por cualquier motor de dominio.
- `PipelineContext` extendido con `engineResults` genérico.

### Testing

- `./mvnw clean test`: **102 tests, 0 fallos, BUILD SUCCESS**.
- Cobertura (JaCoCo): `kin.engine` 99,1 % instrucciones / 100 % ramas; `kin.reporting` 96,2 %; `kin.reporting.risk` 99,6 % / 98,6 % ramas. Requisito de ≥ 90 % en `kin.reporting` y `kin.engine` cumplido.

### Documentation

- `ARQUITECTURA_BASE_KIN_2.0.md` actualizada a KIN 2.0 Alpha 1 / Architecture Stable.
- `KIN_ARCHITECTURE_GOVERNANCE.md` §6 con el contrato `DomainEngine`.

### Known Issues

- El script `V2__add_viability_scoring_column.sql` (Flyway) no es portable a H2: el arranque dev requiere `spring.flyway.enabled=false`.
- `ChatOrchestratorServiceImpl` aún no usa `KinMethod` en el flujo streaming (KIN 2.1).
- `EventStage` dispara `ConversationCompleted` de forma fija (KIN 2.1).
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
- Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) — fuera del requisito del dominio.

## [1.0.0] - 2026-07-29

Versión previa a la consolidación KIN 2.0 (proyecto heredado). Ver commits anteriores a `6518010`.

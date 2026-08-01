# Changelog

Todos los cambios notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y el versionado del proyecto en [SemVer](https://semver.org/lang/es/).

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

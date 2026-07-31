# Changelog

Todos los cambios notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y el versionado del proyecto en [SemVer](https://semver.org/lang/es/).

## [Unreleased] - Fase 5.3 (OpportunityEngine)

Enmienda de `v2.0.0-alpha.1` con ADR-010. **Estado: Architecture Stable (enmendado).**

### Added

- `kin.reporting.opportunity.OpportunityEngine`: motor de dominio puro que identifica oportunidades de mejora/capitalización del proyecto (ADR-010).
- 8 analizadores auto-descubiertos (patrón coordinador + analizadores + ensamblador, mismo diseño que `RiskEngine`): `Market`, `Innovation`, `Technological`, `Financial`, `Competitive`, `Scalability`, `Automation`, `MonetizationOpportunityAnalyzer` — categorías: mercado, innovación, tecnológicas, financieras, competitivas, escalabilidad, automatización, monetización.
- `Opportunity`, `OpportunityResult`, `OpportunityInput`, `OpportunityModel`, `OpportunityAssembler`, `OpportunityExplanation`, `OpportunityCategory`.
- `OpportunityStage` (composición pura sobre `EngineStage`): pipeline de 9 etapas, entre `RiskStage` y `EventStage`.
- Campo tipado aditivo `PipelineContext.opportunityResult` (mismo patrón que `riskResult`).
- Documentación: `FASE5_3_OPPORTUNITY_ENGINE.md` (auditoría, diseño, UML, contratos), ADR-010.
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

## [Unreleased] - Fase 5.2.1 (consolidación del runtime)

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

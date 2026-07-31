# Changelog

Todos los cambios notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y el versionado del proyecto en [SemVer](https://semver.org/lang/es/).

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

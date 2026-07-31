# KIN 2.0 Alpha 1 — Notas de la Versión

> **Milestone**: Primer hito oficial de la arquitectura KIN 2.0
> **Estado**: `ARCHITECTURE STABLE` (baseline congelado)
> **Fecha**: 30 de julio de 2026
> **Commit**: `91426e5` (`feat: implement KIN 2.0 architecture phases 4-5.2` + `fix: update pricing plans database schema`)
> **Branch**: `main`
> **Tag**: `v2.0.0-alpha.1`
>
> ⚠️ **Nota histórica**: estas notas corresponden al milestone original. La **Fase 5.2.1** (ADR-006…009) enmienda el baseline: streaming consolidado en `KinMethod`, contexto durable (`ContextRepository`), puerto `AIResponder` + `PromptAssembler` y scoring canonizado. La **Fase 5.3** (ADR-010) agrega `OpportunityEngine` al pipeline (9 etapas). Ver `kin-docs/FASE5_2_1_RUNTIME_CONSOLIDATION.md`, `kin-docs/releases/KIN_2_0_FASE_5_3_OPPORTUNITY_ENGINE.md` y `BASELINE_ARCHITECTURE.md`.

---

## 1. Resumen ejecutivo

KIN 2.0 Alpha 1 cierra el ciclo de consolidación arquitectónica iniciado en KIN 2.0 (phases 4.0, 5.0, 5.1 y 5.2) y lo declara **primer hito estable del proyecto**. A partir de este punto:

- La **infraestructura de motores de dominio** (`kin/engine`) es un contrato congelado: `DomainEngine`, `EngineInput`, `EngineResult`, `EngineMetadata`, `EnginePhase`, `EngineType`, `EngineRegistry`, `EngineExecutor`, `DeterministicId` y `EngineStage` no deben romperse.
- **Tres motores operativos**: `ScoringEngine` (viabilidad), `RecommendationEngine` (recomendaciones) y `RiskEngine` (riesgos), expuestos en el pipeline a través de stages genéricos (`EngineStage`).
- **102 tests** en verde (0 fallos) y cobertura de dominio ≥ 90 % de instrucciones en `kin.engine` (99,1 %) y `kin.reporting` (96,2 %).
- El **AI engine** mantiene el fallback en español: se puede desarrollar y probar sin Ollama.

Este milestone NO añade funcionalidad nueva: es el cierre oficial de la base arquitectónica sobre la que se construirá la plataforma.

---

## 2. Contexto y alcance

KIN pasó de un `ChatOrchestratorServiceImpl` monolítico (flujo streaming sin método de dominio) a una arquitectura de **Clean Architecture + DDD Táctico + Pipeline Pattern + Event-Driven**, con un método de dominio único y un pipeline de análisis reutilizable.

Las fases de este milestone:

| Fase | Contenido | Estado |
|------|-----------|--------|
| **4.0** | Provider IA (`AIProvider`, `ProviderRouter`, `DeepSeekProvider`, fallback) y capa de contexto (`ProjectContext`, `CompletenessEvaluator`, `ConversationStrategist`) | Completada |
| **5.0** | `RecommendationEngine` + `RecommendationResult`/`Stage` + ADR-003 | Completada |
| **5.1** | `RiskEngine` + `RiskResult`/`Stage` + analizadores (`RiskAssembler`, riesgos de pricing, proceso, modelo de negocio) + ADR-004 | Completada |
| **5.2** | Infraestructura común de motores: `DomainEngine`, `EngineRegistry`, `EngineExecutor`, `DeterministicId`, `EngineStage` + refactor de motores/inputs/results/stages + `KinConfig` + ADR-005 | Completada |

---

## 3. Lo nuevo en esta versión

### 3.1 Infraestructura de motores (`kin/engine`) — contrato estable

- `DomainEngine<E extends EngineInput, R extends EngineResult>`: contrato funcional `execute(E) → R` con `metadata()`.
- `EngineInput` / `EngineResult`: records inmutables, base de todos los motores.
- `EngineMetadata`: `id`, `name`, `version`, `phase`, `description`, `engineType`, `deterministic` y requisitos de entrada/salida.
- `EnginePhase`: ciclo `EXTRACT → ANALYZE → EVALUATE → RECOMMEND → DECIDE`.
- `EngineType`: `SCORING`, `RECOMMENDATION`, `RISK`, `EVALUATION`.
- `EngineRegistry`: auto-descubrimiento de motores vía `List<DomainEngine>` + `get(id)` + `all()`.
- `EngineExecutor`: ejecución secuencial, condicional y opcional (paralela diseñada, no activa).
- `DeterministicId`: generación de IDs deterministas (diseñada para trazabilidad y pruebas reproducibles).
- `EngineStage`: stage genérico de pipeline que delega en cualquier motor y escribe el resultado en `PipelineContext.engineResults`.

### 3.2 Motores de dominio (`kin/reporting`)

- **`RecommendationEngine`**: evalúa dimensión por dimensión, genera recomendaciones accionables, validaviaciones y resumen. Incluye deduplicación y ordenamiento por prioridad.
- **`RiskEngine`**: detecta riesgos por categoría (proceso, pricing, modelo de negocio) con severidad (`LOW/MEDIUM/HIGH/CRITICAL`) y probabilidad, calcula score de riesgo y expone `riskLevel`.
- **`RiskAssembler`**: consolida los resultados de los analizadores de riesgo en un único `RiskResult` determinista.

### 3.3 Pipeline

- `RecommendationStage` y `RiskStage` ahora delegan en `EngineStage` (composición) con trazabilidad por motor.
- `PipelineContext` incorpora `engineResults` (mapa `engineId → EngineResult`) para resultados no canonizados.
- Los resultados de motores (`RecommendationResult`, `RiskResult`, `ScoreResult`) implementan `EngineResult` y quedan disponibles en el contexto del pipeline.

### 3.4 Configuración

- `KinConfig`: beans de `EngineRegistry` y `EngineExecutor` con auto-descubrimiento de motores.

---

## 4. Calidad y verificación

| Métrica | Valor |
|---------|-------|
| Tests | **102** (`./mvnw clean test`) |
| Fallos | **0** |
| Errores | **0** |
| Skip | **0** |
| Build | **BUILD SUCCESS** |

Cobertura JaCoCo (instrucciones) de los paquetes de dominio del milestone:

| Paquete | Instrucciones | Ramas |
|---------|---------------|-------|
| `kin.engine` | **99,1 %** | **100 %** |
| `kin.reporting` | **96,2 %** | 90,4 % |
| `kin.reporting.risk` | **99,6 %** | **98,6 %** |
| `kin.decision` | 69 % | 25 % |

Requisito de dominio (≥ 90 % en `kin.reporting` y `kin.engine`): **CUMPLIDO**.

---

## 5. Documentación y ADRs

| Artefacto | Descripción |
|-----------|-------------|
| `ADR-001` | Bounded context de reporting |
| `ADR-002` | Pipeline context |
| `ADR-003` | Recommendation engine |
| `ADR-004` | Risk engine |
| `ADR-005` | Infraestructura común de motores |
| `ARQUITECTURA_BASE_KIN_2.0.md` | Arquitectura contractual (Fase 2.0, ahora con estado de milestone) |
| `FASE5_0` / `FASE5_1` / `FASE5_2` | Documentación por fase (diseño, consolidación, motores) |
| `BASELINE_ARCHITECTURE.md` | Baseline oficial del milestone (qué existe, qué está estable, qué está congelado) |
| `AGENTS.md` | Guía de agentes actualizada (paquetes, comandos, tests, quirks) |
| `CHANGELOG.md` | Registro de cambios |

---

## 6. Problemas conocidos

> Nada en esta lista bloquea el milestone; ninguno corresponde al alcance de este hito.
> Los ítems marcados ✅ fueron resueltos por la **Fase 5.2.1**.

1. **Boot con Flyway + H2**: el script `V2__add_viability_scoring_column.sql` falla en H2 (tipo/DDL no portable). El arranque funciona con Flyway deshabilitado (`spring.flyway.enabled=false`). Es un problema de configuración de dev, no de dominio. ✅ **resuelto** en 5.2.1 (dev usa `ddl-auto: update`; Flyway solo en prod).
2. ✅ **`ChatOrchestratorServiceImpl` streaming**: usa `KinMethod.executeStream` desde la Fase 5.2.1 (ADR-006). Ya no es el componente más inestable.
3. ✅ **`EventStage`**: en 5.2.1 distingue la decisión real (ASK → `QuestionGeneratedEvent`; REPORT → `ReportGeneratedEvent` + `ScoreCalculatedEvent`; siempre `ConversationCompletedEvent`). La semántica completa queda para KIN 2.1.
4. **`InMemoryDomainEventBus`**: implementación en memoria, sin async ni persistencia (KIN 2.4).
5. **Heurística de longitud en `ScoringEngine`**: debe reemplazarse antes de KIN 2.5 (regla absoluta #18).
6. **Cobertura general del proyecto**: los paquetes de infraestructura (auth, pricing, project, ai.provider) tienen cobertura baja. El requisito de ≥ 90 % aplica solo a `kin.reporting` y `kin.engine`.

---

## 7. Pendientes fuera de alcance

- Fase 5.3 y siguientes (ver `BASELINE_ARCHITECTURE.md` → Preparación para la siguiente fase).
- ✅ ~~Refactor del streaming a `KinMethod`.~~ — resuelto en la Fase 5.2.1 (ADR-006).
- Pipeline error handling, timeout y métricas.
- Event bus async con persistencia (outbox).
- Report Engine y Knowledge Engine (KIN 3.0).

---

## 8. Verificación de este milestone

```bash
# Tests
cd kin-backend && ./mvnw clean test        # 102 tests, 0 fallos, BUILD SUCCESS

# Cobertura
open target/site/jacoco/index.html          # kin.engine 99,1 %, kin.reporting 96,2 %

# Backend (H2, sin Docker)
cd kin-backend && ./mvnw spring-boot:run    # http://localhost:8080/api/v1

# Frontend
cd kin-frontend && npm install && npm run dev  # http://localhost:3000

# E2E (con backend en perfil test)
cd kin-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test
cd kin-frontend && npx playwright test
```

---

*KIN 2.0 Alpha 1 — primer hito estable. Las APIs marcadas como estables en `BASELINE_ARCHITECTURE.md` no deben modificarse sin una ADR aprobada.*

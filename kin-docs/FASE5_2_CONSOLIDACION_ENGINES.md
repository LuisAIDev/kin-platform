# FASE 5.2 — Consolidación de Engines (Infraestructura Común)

> Fecha: 2026-07-30 · Estado: **Completado** · Alcance: refactor arquitectónico puro de engines. **Sin cambios de comportamiento.** REST, frontend, eventos y contratos públicos intactos.

## 1. OBJETIVO

Eliminar la duplicación entre `RecommendationEngine` y `RiskEngine`, reducir el acoplamiento del pipeline con los engines y dejar la plataforma lista para KIN 3.0 (Knowledge, Opportunity, Innovation, Competition, Financial, Market, Validation, Report) **sin modificar el núcleo al agregar engines**.

Restricciones cumplidas:
- Sin cambios en endpoints REST ni en comportamiento visible del frontend.
- Compatibilidad total con Fases 1–5.1.
- Los 71 tests previos pasan **sin modificación funcional**.
- `RecommendationEngine` y `RiskEngine` producen **exactamente** los mismos resultados.
- `OpportunityEngine` / `ReportEngine` NO se implementan en esta fase.

## 2. AUDITORÍA (DUPLICACIÓN Y ACOPLAMIENTO DETECTADOS)

| # | Duplicación / acoplamiento | Antes | Después |
|---|---|---|---|
| 1 | Generación de id determinista (UUID v3 de `category\|title\|description`) | Inline en `Recommendation` y `Risk` | `DeterministicId.from(...)` compartido |
| 2 | Inputs con la misma forma (projectContext/evaluation/decision/score) | `RecommendationInput` y `RiskInput` sin contrato | Ambos implementan `EngineInput` (tipado fuerte) |
| 3 | Resultados con campos comunes (confidence/explanation/generatedBy/engineVersion) | `RecommendationResult` y `RiskResult` sin contrato | Ambos implementan `EngineResult` + `isEmpty()` |
| 4 | Guarda de nulidad + `empty()` por engine | Duplicada en cada engine | Contrato `DomainEngine` documenta el patrón; cada engine conserva su `empty()` |
| 5 | Metadatos del engine (nombre/versión/prioridad) | Constantes sueltas | `EngineMetadata` (record inmutable) vía `metadata()` |
| 6 | Fórmula de confianza + ensamblado de explicación de riesgos | Duplicada en los 4 `RiskAnalyzer` | `RiskAssembler.build(...)` compartido |
| 7 | Boilerplate de stages (name/supports/execute) | `RecommendationStage` y `RiskStage` casi idénticos | `EngineStage` genérico; ambos stages = composición pura |
| 8 | PipelineContext con campo por engine | 2 campos tipados; crecería con cada engine | + `Map<String, EngineResult> engineResults` (escala a 20+) |
| 9 | Registro/ejecución de engines | Wiring manual en `KinConfig` | `EngineRegistry` (auto-descubrimiento) + `EngineExecutor` |

## 3. COMPONENTES CREADOS (`com.kinplatform.kin.engine`)

| Clase | Tipo | Responsabilidad |
|---|---|---|
| `DomainEngine<E,R>` | Interfaz (contrato único) | `metadata()` + `evaluate(E)`; composición, no clase base |
| `EngineInput` | Interfaz | Entrada común tipada (projectContext/evaluation/decision/score) |
| `EngineResult` | Interfaz | Resultado común (confidence/explanation/generatedBy/engineVersion/isEmpty) |
| `EngineMetadata` | Record inmutable | name, version, author, phase, type, priority, dependencies |
| `EnginePhase` | Enum | 16 fases incl. OPPORTUNITY…EXPLANATION |
| `EngineType` | Enum | DOMAIN / ADAPTER |
| `EngineExecution<R>` | Record inmutable | result, runtimeMs, metadata |
| `EngineRegistry` | Servicio de dominio puro | Auto-descubre `List<DomainEngine>`; find/contains/names/size/allOrdered/byPhase/after |
| `EngineExecutor` | Servicio de dominio puro | execute, executeAll (secuencial por prioridad), executeIf, executeOptional; paralelo diseñado no implementado |
| `DeterministicId` | Utilidad | UUID v3 determinista compartido |

Modificados:
- `RecommendationInput`/`RiskInput` → `implements EngineInput`.
- `RecommendationResult`/`RiskResult` → `implements EngineResult` (+ `isEmpty()`).
- `RecommendationEngine`/`RiskEngine` → `implements DomainEngine` + `metadata()` (prioridades 40/50, preservando el orden actual: recomendaciones antes que riesgos).
- `Recommendation`/`Risk` → usan `DeterministicId.from(...)` (ids idénticos).
- `RecommendationStage`/`RiskStage` → composición pura sobre `EngineStage` (API pública intacta).
- `PipelineContext` → mapa genérico `engineResults`.
- `KinConfig` → beans `EngineRegistry` + `EngineExecutor` con auto-descubrimiento.
- 4 `RiskAnalyzer` → usan `RiskAssembler` (nuevo en `kin.reporting.risk`).

## 4. DIAGRAMA DE CLASES (UML)

```
                            kin.engine
┌────────────────────────────────────────────────────────────────────┐
│  <<interface>> DomainEngine<E extends EngineInput,                 │
│                            R extends EngineResult>                 │
│  ├── metadata(): EngineMetadata                                    │
│  └── evaluate(E): R                                                │
│        ▲                                                           │
│        │ implements                                                │
│  ┌─────┴───────────┐   ┌──────────────┐                            │
│  │ Recommendation  │   │  RiskEngine  │                            │
│  │ Engine          │   │              │                            │
│  │ (fase RECOM.    │   │ (fase RISK,  │                            │
│  │  prioridad 40)  │   │  prioridad 50)│                           │
│  └─────────────────┘   └──────────────┘                            │
│                                                                     │
│  <<interface>> EngineInput           <<interface>> EngineResult     │
│  projectContext/evaluation/          confidence/explanation/        │
│  decision/score                      generatedBy/engineVersion/     │
│        ▲                                     ▲                     │
│  RecommendationInput ──────────┐    ┌───────RecommendationResult    │
│  RiskInput ────────────────────┼───►┼───────RiskResult              │
│                                └────┘                              │
│                                                                     │
│  EngineMetadata ◄── name/version/author/phase/type/priority/deps    │
│  EngineExecution<R> ◄── result + runtimeMs + metadata               │
│  EngineRegistry ◄── List<DomainEngine<?,?>> (auto-descubrimiento)  │
│  EngineExecutor ◄── execute / executeAll / executeIf / executeOptional
│  DeterministicId ◄── UUID v3 de (category|title|description)        │
└────────────────────────────────────────────────────────────────────┘

                            pipeline
┌────────────────────────────────────────────────────────────────────┐
│  EngineStage<E,R> ──composición──► DomainEngine + executor         │
│  ├ name / supports / execute                                       │
│  ├ inputFactory: PipelineContext → E                               │
│  └ resultWriter: (PipelineContext, R) → void (+ engineResults map) │
│        ▲                                                           │
│  ┌─────┴────────────┐    ┌─────────────┐                           │
│  │ Recommendation   │    │  RiskStage  │   (API pública intacta)   │
│  │ Stage            │    │             │                           │
│  └──────────────────┘    └─────────────┘                           │
│                                                                     │
│  PipelineContext: + engineResults: Map<String, EngineResult>       │
└────────────────────────────────────────────────────────────────────┘

                            kin.reporting.risk
┌────────────────────────────────────────────────────────────────────┐
│  RiskAssembler ──► Risk (explicación + confianza compartida)       │
│  BusinessRiskAnalyzer / TechnicalRiskAnalyzer /                    │
│  FinancialRiskAnalyzer / MarketRiskAnalyzer ──► RiskAssembler      │
└────────────────────────────────────────────────────────────────────┘
```

## 5. ANTES / DESPUÉS

### 5.1 Stage (duplicación eliminada)

**Antes** — `RecommendationStage.execute()` y `RiskStage.execute()` repetían el patrón:
```java
var input = new RecommendationInput(ctx.projectContext(), ctx.evaluation(), ctx.decision(), ctx.scoreResult());
var result = recommendationEngine.evaluate(input);
ctx.recommendationResult(result);
return ctx;
```

**Después** — lógica única en `EngineStage.execute()`:
```java
E input = inputFactory.apply(context);
R result = executor.execute(engine, input).result();
resultWriter.accept(context, result);
context.setEngineResult(engine.metadata().name(), result);
return context;
```
`RecommendationStage`/`RiskStage` = composición pura (solo configuran nombre, motor, predicado, fábrica y escritor).

### 5.2 ID (salida idéntica garantizada)

**Antes**: `UUID.nameUUIDFromBytes((category.name() + "|" + title + "|" + description).getBytes(UTF_8))` en `Recommendation` y `Risk`.
**Después**: `DeterministicId.from(category.name(), title, description)` — misma concatenación y algoritmo, mismo UUID.

### 5.3 Analizador (fórmula y explicación compartidas)

**Antes**: `computeConfidence(evaluation)` + `RiskExplanation.of(...)` copiados en cada uno de los 4 `RiskAnalyzer`.
**Después**: `RiskAssembler.build(category, ..., reason, evidence, evaluation, version)` — una sola implementación.

## 6. MÉTRICAS DE REDUCCIÓN (DUPLICACIÓN Y ACOPLAMIENTO)

| Métrica | Antes | Después | Δ |
|---|---|---|---|
| Implementaciones de generación de id | 2 | 1 | −50% |
| Implementaciones de fórmula de confianza de riesgo | 4 | 1 | −75% |
| Implementaciones de ensamblado de explicación de riesgo | 4 | 1 | −75% |
| Boilerplate de stages de engines | 2× ~40 líneas | 1 genérico + configuración | −~60% |
| Contratos de entrada sin interfaz común | 2 | 0 (ambos `EngineInput`) | eliminado |
| Contratos de resultado sin interfaz común | 2 | 0 (ambos `EngineResult`) | eliminado |
| Metadatos de engine duplicados | constantes sueltas | 1 `EngineMetadata` | centralizado |
| Acoplamiento para agregar un engine | stage nuevo + campo en `PipelineContext` + wiring | solo `DomainEngine` bean (Spring auto-registra) | mínimo |
| Cobertura de instrucciones (`kin.engine`, `EngineStage`, `RiskAssembler`) | — | 100% | nueva infraestructura cubierta |
| Cobertura de instrucciones (`kin.reporting.risk`) | 100% | 100% | sin regresión |

## 7. COMPATIBILIDAD CON FASES 1–5.1 (CONFIRMACIÓN)

- `./mvnw test` → **102 tests, 0 fallos** (71 previos sin modificación funcional + 31 nuevos).
- `RecommendationEngine`/`RiskEngine`: mismas entradas → mismos resultados (los tests previos verifican ids, confianzas, explicaciones y orden).
- REST/SSE/frontend/eventos: **sin cambios** (se verifica con la suite existente + compilación limpia).
- `KinMethod`/`KinMethodResult`/`Pipeline`: sin cambios.
- Regla `kin/` sin dependencias del proyecto: la nueva infraestructura solo importa `java.*` y paquetes `kin.*`.

## 8. PREPARACIÓN KIN 3.0

Para agregar un engine en KIN 3.0:
1. Implementar `DomainEngine<XInput, XResult>` con `metadata()` (fase + prioridad).
2. Declarar el bean en `KinConfig` (Spring lo inyecta en `EngineRegistry` automáticamente).
3. Si se integra al pipeline: configurar un `EngineStage` (o la etapa concreta que lo componga). El `PipelineContext` lo persiste en `engineResults` sin campos nuevos.
4. `EngineExecutor` ejecuta por fase/prioridad; `executeIf`/`executeOptional` para ejecución condicional; paralelo queda disponible activándolo en `executeAll` (stateless + VOs inmutables ⇒ seguro).

**No hace falta** modificar `EngineRegistry`, `EngineExecutor`, `PipelineContext`, `Pipeline` ni los stages existentes.

## 9. DOCUMENTOS RELACIONADOS

- ADR-005 (engine-infrastructure) — contrato único de motores.
- ADR-001/002/003/004 — contexto histórico.
- KIN_ARCHITECTURE_GOVERNANCE.md §6 — contrato de engines (actualizado).
- FASE5_CONSOLIDACION_ARQUITECTONICA.md — diseño de referencia (refinado por ADR-005).

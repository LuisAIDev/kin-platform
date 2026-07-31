# ADR-005: Infraestructura común de motores (Engine Infrastructure)

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: La Fase 5.2 detecta duplicación entre `RecommendationEngine` y `RiskEngine` (generación de IDs, inputs y resultados de forma idéntica, guardas de nulidad, metadatos hardcodeados, stages casi idénticos) y alto acoplamiento: cada engine nuevo requiere un `PipelineStage` propio, campos nuevos en `PipelineContext` y wiring en `KinConfig`. El contrato único de la sección 6 de Governance (`XxxEngine.evaluate(XxxInput)` + `empty()` estático) no expresa metadatos ni permite auto-descubrimiento. De cara a KIN 3.0 (Knowledge, Opportunity, Innovation, Competition, Financial, Market, Validation, Report) se necesita un único contrato de motor con registro y ejecución automáticos, sin modificar REST ni el comportamiento visible.

**Decisión**: Introducir la infraestructura común de motores en `com.kinplatform.kin.engine` (dominio puro, sin Spring) con:

- **`DomainEngine<E extends EngineInput, R extends EngineResult>`** — contrato único por composición (interfaz genérica, NO clase base): `metadata()` + `evaluate(E)`. Los engines existentes lo implementan y conservan su forma y su `empty()` estático.
- **`EngineInput`** — contrato de entrada común con tipado fuerte (`projectContext()`, `evaluation()`, `decision()`, `score()`); cada entrada concreta conserva sus métodos tipados.
- **`EngineResult`** — contrato de resultado común (trazabilidad: `confidence()`, `explanation()`, `generatedBy()`, `engineVersion()`, `isEmpty()`); cada resultado concreto conserva su tipo.
- **`EngineMetadata`** — record inmutable (name, version, author, phase, type, priority, dependencies). Recomendación=40, Riesgo=50 (preserva el orden de ejecución actual).
- **`EnginePhase`** (16 fases, incl. OPPORTUNITY…EXPLANATION) y **`EngineType`** (DOMAIN/ADAPTER).
- **`EngineExecution<R>`** — envoltorio inmutable (result, runtimeMs, metadata) para trazabilidad por ejecución.
- **`EngineRegistry`** — auto-descubrimiento: recibe `List<DomainEngine<?,?>>` inyectada por Spring, indexa por nombre. Agregar un engine NUNCA modifica esta clase.
- **`EngineExecutor`** — modelo de ejecución: `execute`, `executeAll` (secuencial por prioridad), `executeIf`, `executeOptional`. El modo **paralelo está diseñado pero NO implementado** (`executeAllParallel` delega en secuencial): los engines son stateless y sus VOs inmutables, por lo que es seguro activarlo sin cambiar firmas.
- **`DeterministicId`** — utilidad compartida de IDs deterministas (UUID v3 de `category|title|description`), elimina la duplicación en `Recommendation`/`Risk` con salida idéntica.
- **`EngineStage<E,R>`** — etapa genérica por composición (nombre, engine, predicado `supports`, fábrica de input, escritor de resultado, executor). `RecommendationStage` y `RiskStage` se refactorizan a composición pura sobre `EngineStage` (API pública intacta).
- **`PipelineContext`** — nuevo mapa genérico `engineResults` (key = nombre del engine) para escalar a 20+ engines; los campos tipados históricos se conservan.
- **`RiskAssembler`** (`kin.reporting.risk`) — elimina la duplicación de explicación + fórmula de confianza en los 4 `RiskAnalyzer`.

**Alternativas consideradas**:
1. *Clase base abstracta `AbstractEngine`* — Rechazada: Governance 1.5 exige composición; las clases base solo para duplicación real y no se necesitan.
2. *Registro manual de engines (método `register`)* — Rechazado: viola OCP; el auto-descubrimiento via `List<DomainEngine>` (mismo patrón que `List<RiskAnalyzer>`/`ProviderRouter`) evita modificar el registro al agregar engines.
3. *Ejecución paralela activa ya* — Rechazada: sin concurrencia todavía; el diseño la documenta como futuro seguro.
4. *Diseño original de FASE5_CONSOLIDACION (`engineName()`/`supportedPhase()` y executor con `buildInput` switch)* — Refinado: se adopta `metadata()` (información completa en un record) y el executor usa fábricas de entrada por inyección, evitando un switch sobre fases que violaría OCP.

**Consecuencias**:
- Positivas: un solo contrato para todo motor; agregar un engine en KIN 3.0 = implementar `DomainEngine` + declarar bean (Spring lo registra solo); eliminación de duplicación (IDs, inputs, resultados, guardas, stages, analizadores); trazabilidad por ejecución (runtime + versión); mapa genérico en `PipelineContext` para escalar; 100% de cobertura de instrucciones en `kin.engine`, `EngineStage` y `RiskAssembler`.
- Negativas: nueva infraestructura en `KinConfig`; la sección 6.1 de Governance queda desactualizada (se actualiza con esta ADR); el paralelo requiere una ADR o fase futura para activarse.

**Regla que modifica**: Sección 6.1 de KIN_ARCHITECTURE_GOVERNANCE.md ("Contrato Único para Todos los Engines") — sustituida por el contrato `DomainEngine` de esta ADR.

**Cumplimiento**: Ningún cambio en REST, SSE, frontend, eventos, `KinMethod`/`KinMethodResult` ni contratos públicos. `RecommendationEngine` y `RiskEngine` producen exactamente los mismos resultados (71 tests previos pasan sin modificación funcional; 102 tests en total). Los beans `EngineRegistry`/`EngineExecutor` se agregan a `KinConfig` con auto-descubrimiento.

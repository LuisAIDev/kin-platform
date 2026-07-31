# ADR-009: Canonización de ScoringEngine + desacoplamiento de EngineInput

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: En KIN 2.0 Alpha 1 el contrato `EngineInput` (ADR-005) era un record con cuatro accesorios (`projectContext()`, `evaluation()`, `decision()`, `score()`). Eso forzaba a que *todo* motor —también los futuros de KIN 3.0 (Market, Competition, Innovation, Financial, Knowledge…)— declarara campos que no necesita, generando acoplamiento y `null`s. Por otro lado, `ScoringEngine` seguía siendo un servicio de dominio *fuera* de la infraestructura común: no implementaba `DomainEngine`, `ScoreResult` no implementaba `EngineResult` y su etapa de pipeline (`ScoringStage`) no delegaba en `EngineStage` como sí hacían `RecommendationStage` y `RiskStage`. Eso impedía que el scoring participara del registry/executor y de la trazabilidad común.

**Decisión**:

1. **`EngineInput` se convierte en interfaz marcadora** (sin métodos). Cada motor declara en su propio record únicamente los campos que consume:
   - `RecommendationInput` / `RiskInput`: conservan sus accesorios tipados.
   - `ScoringInput(projectContext, evaluation)`: nuevo record para el motor de scoring.
2. **`ScoringEngine` implementa `DomainEngine<ScoringInput, ScoreResult>`**:
   - `metadata()` → `EngineMetadata.of("ScoringEngine", model.version(), "KIN Architecture Team", EnginePhase.SCORING, EngineType.DOMAIN, 30)`. Prioridad 30 (scoring corre después de estrategia/consultoría y antes de recomendaciones=40 y riesgo=50).
   - Conserva el método legacy `evaluate(ProjectContext, CompletenessEvaluation) → ScoreResult`.
3. **`ScoreResult` implementa `EngineResult`**: `confidence()` = total/max, `generatedBy()` = `ScoringEngine.GENERATOR_NAME`, `engineVersion()` = `ScoringEngine.ENGINE_VERSION`, `isEmpty()` = `totalScore == 0 && categoryScores.isEmpty()`. Conserva `ScoreResult.empty()` y todos los campos históricos (API pública intacta).
4. **`ScoringStage` compone `EngineStage<ScoringInput, ScoreResult>`** (mismo patrón que `RecommendationStage`/`RiskStage`): predicado `projectContext() != null && evaluation() != null && decision() != null && decision().shouldGenerateReport()`. Se elimina el requisito previo `scoreResult() != null` que sí aplica a rec/risk.

**Alternativas consideradas**:

1. *Mantener `EngineInput` con los 4 accesorios* — Rechazado: acopla a todos los motores futuros; el marcador es aditivo y no rompe a los existentes.
2. *No canonizar `ScoringEngine`* — Rechazado: deja el scoring fuera del registry/executor y con un stage especial, contradiciendo ADR-005.
3. *Hacer `ScoringStage` requerir `scoreResult() != null`* — Rechazado: el scoring es quien produce el `ScoreResult`; exigirlo de entrada es un ciclo.

**Consecuencias**:
- Positivas: el contrato `DomainEngine` cubre a los tres motores operativos; los motores futuros declaran solo lo que consumen; el scoring gana metadatos, trazabilidad y priorización dentro del executor; `EngineStage` se vuelve la única implementación de stage determinista.
- Negativas: `EngineInput` deja de garantizar campos tipados (el tipado lo dan los records concretos); la cobertura de dominio debe ampliarse a `kin.scoring`.

**Regla que modifica**: Contrato `EngineInput` del `BASELINE_ARCHITECTURE.md` (v2.0.0-alpha.1) — de record de 4 accesorios a interfaz marcadora. Se documenta mediante esta ADR.

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend. `RecommendationEngine` y `RiskEngine` no cambian. `ScoringEngine` produce exactamente los mismos resultados que antes (verificado por `ScoringEngineTest`).

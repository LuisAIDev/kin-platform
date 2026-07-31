# ADR-001: Creación del Bounded Context Reporting

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: La Fase 5 introduce motores de dominio (Recommendation, Risk, Opportunity, Report) que generan resultados estructurados y auditables. Estos motores requieren un bounded context propio para no acoplar el análisis (context), la evaluación (scoring) y la decisión (decision) con la generación de contenido accionable. El contexto `reporting` actualmente no existe y no hay un contrato unificado para resultados de motores.

**Decisión**: Crear el bounded context `com.kinplatform.kin.reporting` con:

- `RecommendationCategory`, `ImpactLevel`, `EffortLevel`: enums tipados (sin `Map<String,Object>`).
- `RecommendationExplanation`: explicación auditable (información usada, regla aplicada, motivo).
- `Recommendation`: Value Object inmutable con id determinista (derivado de contenido, no `UUID.randomUUID()`).
- `RecommendationResult`: resultado inmutable del motor (recomendaciones, prioridad, confianza, categoría dominante, explicación, versión).
- `RecommendationModel`: configuración de umbrales (lowScore, highScore, minCoverage).
- `RecommendationInput`: entrada tipada del motor.
- `RecommendationEngine`: Domain Service puro, determinista, sin Spring ni IA.
- `RecommendationStage`: etapa del pipeline que invoca al motor.

Reglas de diseño aplicadas (ARQUITECTURA_BASE_KIN_2.0):
- Motores como Domain Service puros → puerto de entrada, no dependen de infraestructura.
- Resultados como Value Objects inmutables → inmutables, defensivos, reproducibles.
- Java decide, LLM comunica → el motor NO usa prompts, ni LLM, ni heurísticas aleatorias.

**Alternativas consideradas**:
1. *Generar recomendaciones desde el LLM* — Rechazado: viola el principio "Java decide, LLM comunica" y no es reproducible ni auditable.
2. *Recomendaciones como `Map<String,Object>`* — Rechazado: sin tipos, sin contrato, no auditable.
3. *Usar `UUID.randomUUID()` en cada recomendación* — Rechazado: rompe la reproducibilidad determinista; se usa id derivado del contenido.

**Consecuencias**:
- Positivas: resultados tipados y auditables; el motor es 100% testeable sin infraestructura; no acopla con IA.
- Negativas: nuevo paquete a mantener; la integración con el LLM (explicación del resultado) será responsabilidad de etapas futuras (PromptAssembler).

**Regla que modifica**: Ninguna. Es un componente nuevo que cumple la arquitectura base (BC `reporting`).

**Cumplimiento**: Requiere que `RecommendationStage` se registre en el pipeline (`KinConfig`) y que `PipelineContext` exponga el resultado (ver ADR-002).

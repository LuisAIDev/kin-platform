# ADR-002: PipelineContext — Campo para resultados de motores

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: La Fase 5.0 introduce `RecommendationStage`, que debe escribir el resultado del motor en el contexto del pipeline para que las etapas posteriores (EventStage hoy, LlmExplanationStage en el futuro) puedan consumirlo. `PipelineContext` ya sigue este patrón con `scoreResult` (campo tipado + getter/setter). La consolidación futura (FASE5_CONSOLIDACION) separará `PipelineContext` en `ExecutionMetadata` + `PipelineContext` + `ConsultationResult`, pero eso es un refactor posterior NO incluido en Fase 5.0.

**Decisión**: Añadir un campo tipado `RecommendationResult recommendationResult` a `PipelineContext`, con getter/setter, siguiendo exactamente el patrón existente de `scoreResult`. NO se introduce `ConsultationResult` ni el `EngineRegistry` en esta fase (fuera de alcance de Fase 5.0).

**Alternativas consideradas**:
1. *Implementar `ConsultationResult.Builder` ahora* — Rechazado: es el refactor de la consolidación (Fase 5.x), el usuario aprobó únicamente RecommendationEngine en esta fase; agregarlo sería un cambio de arquitectura no autorizado.
2. *Usar `setAttribute("recommendationResult", ...)`* — Rechazado: pierde tipado y rompe el patrón existente.
3. *Campo tipado nuevo* — Aprobado: consistente con `scoreResult`, mínima superficie de cambio, no rompe contratos existentes.

**Consecuencias**:
- Positivas: tipado fuerte, patrón consistente, el campo estará disponible para el refactor futuro a `ConsultationResult`.
- Negativas: cuando se implemente la consolidación, este campo migrará a `ConsultationResult`; cambio mecánico y acotado.

**Regla que modifica**: `kin-backend/src/main/java/com/kinplatform/kin/pipeline/PipelineContext.java` (clase del BC `pipeline`).

**Cumplimiento**: Requiere que `RecommendationStage` lo escriba y que `KinConfig` lo registre (ver ADR-001). No afecta endpoints REST, SSE ni contratos públicos.

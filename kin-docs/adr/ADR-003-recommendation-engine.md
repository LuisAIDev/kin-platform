# ADR-003: RecommendationEngine — Motor de dominio determinista

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: La Fase 5 requiere que KIN genere recomendaciones accionables y auditables a partir de la información que Java ya analizó (contexto, completitud, score). Estas recomendaciones deben ser deterministas, reproducibles, tipadas y sin dependencia del LLM. La Fase 5.0 implementa únicamente este motor (Risk, Opportunity y Report quedan para fases siguientes).

**Decisión**: Implementar `RecommendationEngine` como Domain Service puro con:

- Entrada: `RecommendationInput(ProjectContext, CompletenessEvaluation, ConversationDecision, ScoreResult)`.
- Salida: `RecommendationResult` inmutable.
- Reglas deterministas (sin LLM, sin random, sin prompts):
  1. **Cobertura**: una recomendación por dimensión faltante (`project.missingDimensions()`); prioridad 9 si está en `criticalMissingDimensions`, sino 6; categoría/impacto/esfuerzo mapeados por dimensión.
  2. **Score**: si `totalScore < lowScoreThreshold` (40) → recomienda reforzar la dimensión cubierta con menor puntaje (prioridad 8); si no hay dimensiones con puntaje → recomendación general de recolección de información. Si `totalScore >= highScoreThreshold` (70) y aún faltan dimensiones → recomendación de innovación sostenible (prioridad 5).
  3. **Madurez**: `EARLY` → priorizar validación temprana del cliente (prioridad 7); `MATURE` con dimensiones pendientes → consolidar plan de escalamiento (prioridad 5).
- Orden: prioridad descendente (estable: `Comparator.comparingInt(priority).reversed()`).
- Confianza determinista: `0.15 + 0.35*coverage + 0.25*qualityOfInformation + 0.25*(totalScore/100)`, acotada a [0,1].
- Categoría dominante: la más frecuente (tie-break por orden de aparición = determinista).
- `generatedBy = "RecommendationEngine"`, versión del `RecommendationModel`.
- `Recommendation.id` determinista (UUID v3 derivado de contenido).
- Si la entrada es nula o incompleta → `RecommendationResult.empty()` (sin excepciones).

**Alternativas consideradas**:
1. *Recomendaciones generadas por el LLM* — Rechazado: viola ARQUITECTURA_BASE_KIN_2.0 (Java decide, LLM comunica), no reproducible.
2. *Heurísticas aleatorias / scoring por "sentimiento"* — Rechazado: el usuario exige determinismo y reproducibilidad.
3. *Usar `evaluation.isComplete()` para la regla de innovación* — Rechazado: en el pipeline real `isComplete()` es true al momento del reporte; se usa `project.missingDimensions()` (fuente real de brechas).

**Consecuencias**:
- Positivas: 96.5% de cobertura de instrucciones y 90.4% de ramas en el paquete `reporting` (≥90% requerido); totalmente testeable sin Spring ni Ollama; reproducible (mismas entradas → mismos ids/resultados).
- Negativas: las reglas son heurísticas de negocio fijas en código; ajustar umbrales requiere cambiar `RecommendationModel` (configurado vía `KinConfig`).

**Regla que modifica**: Ninguna sección de Governance; es un componente nuevo permitido por la arquitectura (BC `reporting`, patrón Domain Service).

**Cumplimiento**: Requiere `RecommendationStage` en el pipeline entre `ScoringStage` y `EventStage` (ADR-001, ADR-002). No afecta endpoints REST, SSE, eventos existentes ni contratos públicos.

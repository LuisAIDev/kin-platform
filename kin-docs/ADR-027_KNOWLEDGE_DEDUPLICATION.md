# ADR-027: Knowledge Deduplication Strategy

## Estado
**Aceptado** — 2026-08-25

## Contexto
El KnowledgeEngine adquiere hechos de múltiples fuentes externas (Banco Mundial, DANE, ECB, etc.) y es común que lleguen hechos duplicados o muy similares (mismo indicador, mismo país, mismo año, valores similares). Esto puede distorsionar el scoring y las recomendaciones al sobre-representar cierta información.

## Decisión
Implementar un **DeduplicationEngine** que, como parte del KnowledgeStage (o en una etapa posterior), deduplique los hechos recibidos utilizando estrategias deterministas en cascada:

1. **ExactMatchStrategy** (priority 10): coincidencia exacta de sourceId, category y claim normalizado.
2. **FuzzyMatchStrategy** (priority 20): similitud Jaro-Winkler para texto (umbral configurable, default 0.85) + tolerancia porcentual para números.
3. **SemanticMatchStrategy** (priority 30, stub): similitud semántica vía embeddings (futuro).

El motor produce un **DeduplicationResult** con:
- `uniqueFacts`: lista de hechos únicos (duplicados eliminados).
- `duplicateGroups`: mapa representante → lista de duplicados (para auditoría).
- `metrics`: totalInput, uniqueFacts, duplicatesRemoved, confidence, y métricas por estrategia.

## Integración en Pipeline
Se añade **DeduplicationStage** (priority 55) justo después de **KnowledgeStage** (priority 50) y antes de **EnrichmentStage**. Recibe el `KnowledgeResult` del contexto, lo procesa y guarda el `DeduplicationResult` en `PipelineContext.deduplicationResult`.

## Configuración
```yaml
kin:
  deduplication:
    enabled: true
    fuzzy:
      threshold: 0.85
    strategies:
      - EXACT_MATCH
      - FUZZY_MATCH
      - SEMANTIC
```

Feature flag `kin.deduplication.enabled` (default `true`) para deshabilitar en tests.

## Alternativas Consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Deduplication Engine (elegida)** | Determinística, configurable, auditable, sin LLM | Latencia adicional ms | ✅ |
| **Deduplicación en LLM** | Podría entender semántica mejor | No determinista, costoso, latencia alta | ❌ |
| **Post-procesamiento en LLM** | Flexible | Mismos problemas que arriba | ❌ |
| **Deduplicación solo en ingesta** | Simple | No cubre duplicados entre fuentes distintas en tiempo real | ❌ |

## Consecuencias

**Positivas**:
- Garantiza hechos únicos para scoring y recomendaciones.
- Auditoría completa via `duplicateGroups`.
- Configurable por entorno (feature flag, umbral fuzzy).

**Negativas/Riesgos**:
- Latencia adicional (~ms) en el pipeline.
- Falsos positivos/negativos posibles en fuzzy match (mitigado con umbral conservador 0.85).
- Solo deduplica dentro de la misma fuente (sourceId) por diseño; cross-source requiere estrategia semántica futura.

## Plan de Migración (PRs)

| PR | Alcance |
|----|---------|
| **PR 1** | Motor, estrategias, stage, tests, config, ADR |
| **PR 2** | Integración en KnowledgeStage (opcional), métricas expandidas |
| **PR 3** | Estrategia semántica real (embeddings), cross-source dedup |

## Referencias
- ADR-014 (Knowledge Engine), ADR-016 (Enrichment Engine)
- [Jaro-Winkler distance](https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance)
- [Deduplication in data pipelines](https://martinfowler.com/articles/data-monolith-to-microservices.html#deduplication)
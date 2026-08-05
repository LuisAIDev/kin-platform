# FASE 13.0 — Enterprise Observability & Operational Excellence (Knowledge Engine)

**Estado**: ✅ Implementado (100 % aditivo, desacoplado del dominio)
**Fecha**: 2026-08-05
**Autor**: KIN Architecture Team

> **Alcance**: observabilidad Enterprise del Knowledge Engine v1. Toda la
> instrumentación vive en **infraestructura** (`com.kinplatform.ai.observability`);
> el dominio (`kin.knowledge.*`) permanece POJO puro, sin Spring, sin cambios
> funcionales. ADR-014 y ADR-012 intactos.

---

## 1. Arquitectura

La observabilidad se compone **fuera del dominio** mediante decoradores sobre los
puertos/colaboradores del `KnowledgeOrchestrator` y del `CitationEngine`, y una
fachada de composición `ObservableKnowledgeRuntime` que produce **exactamente el
mismo `KnowledgeResult`** que el gateway (compatibilidad verificada por test).

```
Application layer
  → ObservableKnowledgeRuntime.acquire(request)          [infra, correlación + log + métricas]
       → KnowledgeOrchestrator (composición con colaboradores observados)
            → TimedQueryPlanner        (latencia planner + intención/estrategia/facetas)
            → TimedPolicyEngine        (latencia policy + decisiones + presupuesto)
            → TimedProviderRegistry    (latencia registry + envuelve fuentes)
            → TimedKnowledgeSource     (consultas/latencia/errores/timeouts por ProviderType)
            → TimedCandidateValidator  (latencia validación + candidatos recibidos/descartados)
            → TimedContextRanker       (latencia ranking)
            → TimedContextAssembler    (latencia assembler)
            → TimedKnowledgeRepository (hit/miss/consultas evitadas)
       → TimedCitationEngine           (latencia citation + estilo/entradas)
       → KnowledgeMetrics (Micrometer) · KnowledgeStructuredLog (SLF4J + MDC) · CorrelationContext
```

## 2. Métricas (Micrometer, prefijo `kin.knowledge.*`)

| Grupo | Métrica |
|---|---|
| Ciclo | `kin.knowledge.cycle` (timer, tag `result`) |
| Etapas | `kin.knowledge.stage` (timer, tag `stage`: policy/planner/cache/registry/fetch/validation/ranking/assembler/citation) |
| Planner | `planner.intent` · `planner.strategy` · `planner.query_strategy` · `planner.facets` · `planner.queries` |
| Policy | `policy.decision` (tag `decision`) |
| Caché | `cache_hit` · `cache_miss` · `cache_avoided_queries` · `cache_saved` (ratio = hit/(hit+miss)) |
| Providers (por `ProviderType` abstracto) | `provider.requests` · `provider.errors` · `provider.timeouts` · `provider.latency` · `provider.registry` (tag `type`) |
| Calidad | `quality.candidates_received` · `quality.candidates_discarded` · `quality.sources_accepted` · `quality.sources_rejected` · `quality.average_score` · `quality.average_confidence` |
| Orchestrator | `orchestrator.state` (tag `state`) · `orchestrator.degraded` · `orchestrator.offline_mode` · `orchestrator.graceful_degradation` · `orchestrator.fail_fast` · `orchestrator.budget_exhausted` · `orchestrator.providers_selected` |
| Citation | `citation.bundles` · `citation.style` · `citation.entries` |

## 3. Logging estructurado

`KnowledgeStructuredLog` (SLF4J, logger `kin.observability.knowledge`) emite
eventos JSON con `correlationId`, `requestId`, `traceId`, `durationMs` y
`result`, y propaga los IDs al MDC. **Nunca** registra prompts, secretos,
tokens, credenciales ni datos personales (solo IDs, duraciones, conteos).

## 4. Tracing (OpenTelemetry preparado)

`CorrelationContext` porta `correlationId`/`requestId`/`traceId` por request
(ThreadLocal) y los propaga al MDC. OpenTelemetry se prepara sin acoplar el
dominio: el `traceId` ya se genera y propaga; la exportación OTel se puede
enlazar en el futuro vía Micrometer Tracing/OTel SDK sin tocar el dominio.

## 5. Micrometer / Prometheus

- Micrometer (actuator) ya presente; `micrometer-registry-prometheus` añadido.
- Endpoint `/actuator/prometheus` expuesto (`management.endpoints.web.exposure.include: health, info, metrics, prometheus`).
- Tag global `application: kin-backend` para filtrar por servicio.

## 6. Dashboards recomendados

- **Planner**: intención por tipo, estrategias elegidas, facetas/consultas por turno.
- **Orchestrator**: transiciones de estado, degradaciones, offline/graceful/fail-fast, presupuesto agotado.
- **Cache**: hit vs miss, ratio, consultas evitadas, saved.
- **Providers**: requests, latencia, errores y timeouts por `ProviderType`.
- **Citation**: bundles, estilo usado, entradas por turno.

## 7. Alertas recomendadas

| Alerta | Condición |
|---|---|
| Cache Hit Ratio bajo | `cache_hit/(cache_hit+cache_miss) < 0.5` en ventana 5m |
| Provider Failure alto | `provider.errors/total > 0.1` en 5m |
| Timeout elevado | `provider.timeouts > N` en 5m |
| Offline Mode frecuente | `orchestrator.offline_mode` crece en 5m |
| Latencia excesiva | p95 de `kin.knowledge.cycle` > 2s en 5m |
| Degradación sostenida | `orchestrator.degraded` creciente |

## 8. Rendimiento esperado

Sobrecarga de instrumentación mínima (contadores/timers Micrometer + logs); los
decoradores solo miden y delegan (sin lógica de negocio). La copia defensiva del
contexto y la deduplicación de fetch siguen las observaciones de la Fase 6.

## 9. Riesgos

1. Métricas por etapa dependen de la composición (si se usa el gateway plano,
   no se registran); el `ObservableKnowledgeRuntime` es el punto de entrada
   observado recomendado.
2. Los contadores de calidad se registran en la fachada (cubren fetch y caché);
   si se instrumenta por separado, evitar doble conteo.
3. MDC/ThreadLocal requieren limpieza por request (`CorrelationContext.clear()`).

## 10. Checklist de producción

```
□ BUILD SUCCESS (2165 tests verdes)
□ Dominio sin Spring/HTTP; observabilidad fuera del dominio
□ Cobertura ≥90 % (domain y observability)
□ /actuator/prometheus expuesto
□ Logging estructurado con correlación
□ Métricas por etapa y por ProviderType abstracto
□ Compatibilidad: mismo KnowledgeResult que el gateway (test)
□ ADR-014 / ADR-012 intactos
```

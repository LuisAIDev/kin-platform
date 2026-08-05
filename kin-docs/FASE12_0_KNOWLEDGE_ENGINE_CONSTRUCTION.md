# FASE 12.0 — Knowledge Engine: Construcción, Hardening y Congelación (v1)

**Estado**: **Congelado** — Knowledge Engine v1
**Fecha**: 2026-08-05
**Autor**: KIN Architecture Team

> **Alcance**: documenta la construcción aditiva del Knowledge Engine sobre ADR-014
> (Fases 1–3), el hardening arquitectónico (Fase 4) y la congelación del módulo v1.
> No modifica ADR-014 ni ADR-012: todo es 100 % aditivo, POJO de dominio.

---

## 1. Contexto

ADR-014 congeló el núcleo de adquisición de conocimiento (`KnowledgeEngine`,
`KnowledgeGateway`, `SourceRegistry`, `SourceValidator`, `KnowledgeRepository`,
`KnowledgeStage`). Sobre esa base y las especificaciones Fase 2 (Policy Engine),
Fase 3 (Query Planner) y Fase 5 (Knowledge Orchestrator), se construyeron tres
componentes de dominio adicionales, verificados por fase:

| Fase | Componente | Paquete | Estado |
|---|---|---|---|
| 1 | KnowledgePolicyEngine | `kin.knowledge.policy` | ✅ Implementado |
| 2 | QueryPlanner | `kin.knowledge.planner` | ✅ Implementado |
| 3 | KnowledgeOrchestrator | `kin.knowledge.orchestrator` | ✅ Implementado |
| 4 | Hardening y congelación | — | ✅ Completado |

## 2. Arquitectura aplicada

- **Clean Architecture / Hexagonal / DDD**: los tres paquetes son POJO puro de
  dominio; solo dependen de `kin.knowledge` (base ADR-014) y entre sí de forma
  unidireccional (`policy ← planner ← orchestrator`). Sin Spring, sin HTTP, sin
  infraestructura, sin proveedores concretos (solo `ProviderType` abstractos).
- **SOLID/OCP/DIP**: `IntentRule`, `QualityRule`/`ProviderRule`/`QueryRule`/`CostRule`/
  `ContextRule` y `OrchestrationStrategyPolicy` son estrategias registrables; el
  Orchestrator consume al PolicyEngine únicamente por su interfaz pública.
- **Patrones**: Strategy, Pipeline (IntentAnalyzer→QueryClassifier→StrategySelector→
  PlanGenerator), State (máquina de estados del Orchestrator), Specification
  (composición de políticas) y Value Objects (configs y mapeos declarativos).

## 3. Revisión arquitectónica (hardening)

- **Clean/Hexagonal/DDD**: ✅ verificados; dependencias solo hacia dentro del dominio.
- **SOLID**: ✅ SRP por clase; OCP vía registros/estrategias; DIP vía interfaces.
- **Desviaciones detectadas**: ninguna estructural. Un único uso de `java.net.URI`
  existe en `SourceValidator` (congelado, ADR-014) — ajeno a los paquetes nuevos.
- **Determinismo**: ✅ verificado por tests (`determinismo_mismaEntradaMismo*` en
  planner y orchestrator, y en el PolicyEngine). La única dependencia temporal es
  `MaxAgeQualityRule` (`OffsetDateTime.now()` para frescura), coherente con el
  `SourceValidator` congelado: decisión determinista *por instante de evaluación*.
- **Dependencias**: ✅ sin ciclos (`policy` no conoce a `planner`/`orchestrator`;
  `planner` conoce a `policy`; `orchestrator` conoce a ambos); sin infraestructura.
- **Inmutabilidad**: ✅ todos los modelos son records (inmutables); el **único**
  objeto mutable del módulo es `OrchestrationContext` (interno al Orchestrator).
- **Ownership (un escritor por objeto)**: ✅
  - `KnowledgeRequest`: creado por `KnowledgeStage`/Gateway; leído por Planner/Policy.
  - `QueryPlan`: **único escritor** `QueryPlanner`; leído por el Orchestrator.
  - `KnowledgeCandidate`: creado por adaptadores `KnowledgeSource`; leído por
    Validator/Gateway/Policy.
  - `KnowledgeFact`/`KnowledgeResult`: escritor `KnowledgeGateway`; leídos por el pipeline.
  - `OrchestrationContext`: único escritor `KnowledgeOrchestrator`.

## 4. Revisión del Planner (keywords, prioridades, facetas, fallback)

- Tabla de keywords, implicaciones (`MERCADO→ESTADISTICA`, `REGULATORIA→LEGAL`) y
  orden de facetas verificadas; fallback determinista (`GENERAL` → consulta web).
- **Caso documentado — Panadería Cartagena**: con las reglas aprobadas de la Fase 2
  genera **4 facetas** (REGULATORIA, LEGAL, MERCADO, ESTADISTICA). Podría producir 5
  (con COMPETENCIA) extendiendo la tabla de keywords de la faceta COMPETENCIA con
  términos de localidad (p. ej. "cartagena") o una implicación adicional, pero eso
  modifica el comportamiento aprobado del planner → **NO implementado**; queda como
  propuesta para una futura decisión de hardening/ADR.

## 5. Revisión del Orchestrator (estados, transiciones, degradación)

- **Estados alcanzables**: los 10 estados son alcanzables (IDLE→…→COMPLETED/FAILED).
- **Transiciones defensivas** (definidas pero no ejercidas por los handlers actuales):
  `PLANNING→FAILED`, `VALIDATION→FAILED`, `RANKING→FAILED`, `ASSEMBLING→FAILED`.
  Se conservan como reserva del contrato de la máquina (cualquier estado puede
  fallar); no se consideran bug ni se modifican.
- **Degradación**: verificada para proveedor caído/timeout (excluye el tipo y sigue
  con los disponibles), caché corrupta (degrada a consulta externa), sin Internet,
  presupuesto agotado y sin proveedores — siempre Graceful o Fail Fast según
  estrategia; nunca excepciones sin controlar.

## 6. Cobertura (JaCoCo, re-ejecutada)

| Paquete | Instrucciones | Ramas | Requisito |
|---|---|---|---|
| `kin.knowledge.policy` | 98.6 % | 87.8 % | ≥90 % ✅ |
| `kin.knowledge.planner` | 96.8 % | 84.0 % | ≥90 % ✅ |
| `kin.knowledge.orchestrator` | 95.1 % | 84.3 % | ≥90 % ✅ |

**BUILD SUCCESS** · **2068 tests** verdes (0 fallos, 0 errores, 0 skipped) ·
"All coverage checks have been met".

## 7. Desviaciones y riesgos remanentes

- **Desviaciones**: ninguna estructural. Documentado: panadería = 4 facetas;
  transiciones defensivas del Orchestrator; frescura dependiente del reloj.
- **Riesgos remanentes**:
  1. La integración física (Gateway/Registry/Cache/Repository) **sigue diferida** a
     una fase autorizada posterior.
  2. La calidad real depende de las fuentes; mitigado por `SourceValidator` +
     `SourceTrust` + Quality Policies.
  3. El coste/latencia del ciclo se controla por Cost/Context Policies del Policy
     Engine (activas a nivel de decisión; presupuesto de turno listo).

## 8. Congelación — Knowledge Engine v1

Se congela la **Knowledge Engine v1**: `kin.knowledge.policy`,
`kin.knowledge.planner`, `kin.knowledge.orchestrator`.

> **Todo cambio posterior deberá ser**: estrategia registrable, adaptador,
> configuración (datos), o exigir una **ADR nueva** si toca contratos congelados.

## 9. Recomendaciones para la siguiente fase (Citation Engine) — sin implementar

1. Exige una **ADR aditiva de citación** (frontera ADR-012): el `KnowledgeFact`
   ya porta `SourceMetadata` (origen, URL, fecha, confianza, validación) y
   `ConfidenceScore`, insumos listos para el `CitationEngine`.
2. La integración física del Orchestrator (Registry/Cache/Repository) debería
   aprobarse en la misma ADR o en una fase de integración explícita.
3. Proponer en ADR/hardening la ampliación de keywords del planner (COMPETENCIA) y
   la evaluación de eliminar las transiciones defensivas no ejercidas.

---

## 10. Integración física del Knowledge Engine (runtime)

**Estado**: ✅ Integrado (cableado aditivo sobre ADR-014).

### Flujo resultante

```
KnowledgeStage → KnowledgeEngine → KnowledgeGateway
  → KnowledgeOrchestrator (máquina de estados, decisiones + ejecución delegada)
      → KnowledgePolicyEngine (decisiones)
      → QueryPlanner (plan)
      → Cache lookup (KnowledgeRepository) → HIT → resultado cacheado
      → MISS → ProviderRegistry (ProviderType) → KnowledgeProviders → candidatos
      → SourceValidator (antes del ranking, nunca se omite)
      → ContextRanker (solo resultados validados)
      → ContextAssembler (KnowledgeResult) → KnowledgeRepository.save → KnowledgeResult
  → PipelineContext
```

### Cableado (100 % aditivo, sin ciclos)

- **Puertos en `orchestrator`**: `ProviderRegistry`, `CandidateValidator`,
  `ContextRanker`, `ContextAssembler`, `RankedCandidate`, `KnowledgeOrchestrationResult`.
- **Implementaciones en `engine`** (orchestrator no conoce `engine` → sin ciclo):
  `SourceRegistryAdapter`, `SourceValidatorAdapter`, `DomainContextRanker`,
  `DomainContextAssembler` (replica exacta de las métricas congeladas de confianza
  y explicación del núcleo).
- **`KnowledgeGateway`** (único archivo existente modificado): pasó a ser punto de
  composición que delega el ciclo al orquestador; conserva el contrato público
  `acquire(KnowledgeRequest) → KnowledgeResult` y los constructores existentes.
  `OrchestrationContext`/`KnowledgeOrchestrator` se extendieron aditivamente
  (ejecución por estado + `coordinateWithResult`); `coordinate()` conserva su
  comportamiento original.
- **Cobertura tras integración**: policy 98.6 %, planner 96.8 %, orchestrator 95.0 %
  (≥90 % ✓). **BUILD SUCCESS**, 2093 tests verdes.

---

## 11. Fase 5 — Citation Engine (aditivo, sin tocar ADR-012)

**Estado**: ✅ Implementado en `kin.knowledge.citation` (POJO puro, 100 % aditivo).

Cadena de integración (frontera ADR-012 intacta):

```
KnowledgeResult → CitationEngine → CitationBundle → (futuro) PromptContextBuilder → Prompt
```

- **Componentes**: `CitationEngine` (fachada determinista), `CitationCollector`
  (dedup por `(sourceId,url)`), `CitationPolicy`/`VerifiedCitationPolicy`
  (Specification; nunca cita sin SourceMetadata), `CitationFormatter` + 5
  formatters (Strategy: Inline/Footnote/Appendix/Hidden/Disabled),
  `CitationFormatterRegistry` (Factory, registrable), modelos inmutables
  `CitationEntry`/`CitationDecision`/`CitationMetadata`/`CitationBundle`/
  `CitationResult`.
- **Estilos registrables**: APA/IEEE/MLA/HTML/JSON futuros = nuevas estrategias en
  el registry, sin tocar el motor.
- **Regla de oro**: el `PromptContextBuilder` consumirá únicamente
  `CitationBundle` (referencias formateadas), nunca `KnowledgeFact`/SourceMetadata.
- **Cobertura**: `kin.knowledge.citation` 99.2 % instrucciones (≥90 % ✓).
- **Integración al prompt DIFERIDA**: el cableado del bundle al prompt/reporte
  toca la frontera ADR-012 → exige una **ADR aditiva** (propuesta documentada),
  no se implementa en esta fase.

# ADR-016: Knowledge-Enhanced Analysis — analizadores de dominio que consumen conocimiento externo verificado y lo citan en el reporte

**Estado**: **Propuesto** (Etapas E1–E3 completadas: diseño arquitectónico, modelo de dominio y
ranking determinista implementados. E4…E7 pendientes de implementación)
**Fecha**: 2026-08-01
**Autor**: KIN Architecture Team

> **Alcance**: este ADR propone y congela (una vez aprobada) la arquitectura de la **Fase 8
> (Knowledge-Enhanced Analysis / Enriquecimiento del Análisis con Conocimiento Externo)**,
> documentada en `kin-docs/FASE8_0.md`. En estado **Propuesto**, sus decisiones están sujetas a
> revisión y no producen cambios de contrato. Las etapas E2–E3 (modelo de dominio y ranking)
> están implementadas de forma aditiva, sin modificar contratos congelados. La integración es
> **aditiva** (patrón ADR-011/014/015) y mantiene el principio rector del proyecto:
> **Java decide. El LLM únicamente comunica.**

---

## Problema

Desde la Fase 6 (ADR-014), `KnowledgeStage` produce un `KnowledgeResult` con hechos verificados
(`KnowledgeFact`, con origen, URL, fecha, confianza y validación) y lo deja en
`PipelineContext.knowledgeResult`. Sin embargo, **ningún motor de análisis lo consume**:

| # | Brecha | Evidencia |
|---|--------|-----------|
| 1 | **El conocimiento externo no llega al análisis**: recomendaciones, riesgos y oportunidades se calculan solo con `ProjectContext` + heurísticas, ignorando hechos verificados. | `RecommendationInput`, `RiskInput`, `OpportunityInput` (sin campo de conocimiento); 0 referencias a `KnowledgeResult` en `kin/reporting` |
| 2 | **Sin selección de hechos relevantes en Java**: no existe un mecanismo determinista que decida qué hechos aplican a qué dimensión/categoría de análisis. | `KnowledgeGateway` normaliza y valida, pero no prioriza por relevancia al proyecto |
| 3 | **La trazabilidad se pierde en la comunicación**: la Fase 6 declaró que exponer conocimiento citado en el reporte exigía una ADR propia (frontera ADR-012); hoy la fuente externa nunca se cita. | Nota de cierre ADR-014; `ReportEngine`/10 `SectionAssembler` no incluyen fuentes |
| 4 | **El LLM no puede explicar el porqué verificado**: al comunicar recomendaciones/riesgos/oportunidades, el prompt solo ve el reporte; sin la referencia a hechos verificados el consultor no puede sustentar con datos externos. | `ConsultingReport` (10 secciones, sin sección de fuentes) |
| 5 | **La riqueza de la Fase 6 queda latente**: la inversión en adquisición/validación de conocimiento no se capitaliza en decisiones de análisis. | `KnowledgeEngine` prioridad 50 produce `KnowledgeResult` que ningún analizador consume |

El resultado es un consultor que *sabe* cosas externas (porque las adquirió en Java) pero **no
las usa** para decidir ni puede **citarlas** al comunicar el análisis.

---

## Contexto

KIN 2.0 Alpha 1 (`v2.0.0-alpha1`, ALPHA STABLE) + Fases 6 y 7 están cerradas oficialmente
(ADR-014 y ADR-015 Aprobados). El pipeline actual tiene **12 etapas**:

`Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`

Las Fases 6 y 7 dejaron dos puntos de extensión sin consumir:

- `PipelineContext.knowledgeResult` (`KnowledgeResult`, ADR-014) — **producido y no consumido**.
- `PipelineContext.interviewResult` (`InterviewResult`, ADR-015) — la entrevista estratégica ya
  garantiza un `ProjectContext` completo y validado antes del análisis.

El baseline §7.5 (Prioridad 1) recomienda explícitamente: *"consumir el `KnowledgeResult` de la
Fase 6 en los analizadores de dominio (mercado, innovación, financiero, competitivo) vía inputs
aditivos, y/o exponer conocimiento citado en la comunicación (exige ADR propia por la frontera
ADR-012)"*.

La Fase 8 cierra ese hueco: un **bounded context de enriquecimiento** que (a) selecciona en Java
los hechos relevantes para el proyecto, (b) los inyecta de forma **aditiva** en los inputs de los
motores de análisis, y (c) los expone como **sección de fuentes citadas** en el `ConsultingReport`
— sancionando la frontera ADR-012 de forma aditiva (precedente enmienda M2 ADR-013).

Principio rector (intacto):

> **Java decide. El LLM únicamente comunica.**
> Java decide qué hechos externos son relevantes, cómo ponderan en el análisis y qué fuentes se
> citan; el LLM solo explica el resultado y las fuentes ya seleccionadas.

---

## Objetivo

Convertir a KIN en un **consultor que sustenta su análisis con conocimiento externo verificado**:

1. **Seleccionar** en Java los hechos externos relevantes para el proyecto (`FactRanker`
   determinista), preservando `SourceTrust` y la validación de la Fase 6.
2. **Enriquecer** los motores de análisis (recomendación, riesgo, oportunidad) mediante inputs
   **aditivos** que portan los hechos seleccionados, sin alterar los contratos existentes.
3. **Citar** en el `ConsultingReport` las fuentes verificadas que sustentan el análisis
   (sección aditiva de fuentes), para que el LLM las comunique con trazabilidad.
4. **Preservar** la offline-first: sin hechos, `EnrichmentResult.empty()` y los analizadores se
   comportan exactamente como hoy (compatibilidad total con el comportamiento actual).

**Java será responsable exclusivamente de**: qué hechos son relevantes, su ponderación en cada
categoría de análisis y qué fuentes se citan.

**El LLM únicamente**: comunica el análisis y las fuentes ya seleccionadas por Java. Nunca
recupera, valida ni decide sobre conocimiento.

---

## Decisión

Se introduce un **bounded context de dominio `com.kinplatform.kin.enrichment`** (POJO puro, sin
Spring/JPA/IA) que actúa como puente entre la adquisición de conocimiento (Fase 6) y el análisis
(Fases 5.x/7). Diseño alineado con los patrones ya congelados (`DomainEngine`, coordinadores,
stages aditivos):

1. **Un motor canonizado** `EnrichmentEngine` implementa
   `DomainEngine<EnrichmentInput, EnrichmentResult>` (fase `ANALYSIS`, tipo `DOMAIN`, prioridad
   **55** — después de Knowledge=50 y antes de Opportunity=60). `evaluate(EnrichmentInput)`
   delega en `FactRanker` para seleccionar y ponderar hechos, y degrada a
   `EnrichmentResult.empty()` si no hay hechos o input no viable (offline-first).
2. **`FactRanker` determinista** (Java): dado un `KnowledgeResult` y el `ProjectContext`,
   produce un `EvidenceRank` — asocia cada `KnowledgeFact` a una categoría de análisis
   (mercado, innovación, financiero, competitivo) mediante coincidencia de términos/keywords con
   las dimensiones cubiertas y aplica un score determinista (peso por `SourceTrust`, frescura,
   cobertura semántica y duplicados). Stateless y reentrante. Nunca consulta al LLM.
3. **`EnrichmentResult`** (EngineResult): `ranks` (evidencia seleccionada por categoría),
   `sourcesUsed`, `confidence`, `explanation`, `empty()`. Implementa `EngineResult` para
   integrarse con `EngineRegistry`/`EngineStage`.
4. **Inputs aditivos a los motores de análisis** (patrón overload, precedente
   `PromptRequest.forConversation` ADR-012/013): `RecommendationInput.withEnrichment(...)`,
   `RiskInput.withEnrichment(...)`, `OpportunityInput.withEnrichment(...)` — constructores
   originales intactos; el campo de enriquecimiento es opcional y `null`/vacío conserva el
   comportamiento actual. Los analizadores de mercado, innovación, financiero y competitivo
   **leen** los hechos relevantes y los incorporan como evidencia en sus oportunidades/riesgos/
   recomendaciones (con referencia al `KnowledgeFact`).
5. **Sección aditiva de fuentes en el reporte** (frontera ADR-012 sancionada aditivamente,
   precedente M2 ADR-013): nuevo tipo `SourcesSection` (11.ª sección, `ReportSectionKind.SOURCES`)
   en `ConsultingReport`, con `SourcesSectionAssembler` (lee `ReportInput.enrichment`) y
   `SourcesSectionFormatter` (presenta las fuentes citadas como Markdown ligero). `ReportInput`
   gana un overload aditivo con el `EnrichmentResult`. El `ReportEngine` sigue siendo orquestador
   puro: coordina **11** `SectionAssembler` (10 existentes + 1 nuevo) vía `ReportAssemblers`.
   `PromptAssembler`/`ReportPromptBuilder` consumen únicamente el `ConsultingReport` (frontera
   ADR-012 intacta) — la sección de fuentes viaja dentro del reporte tipado.
6. **Integración aditiva al pipeline**: nueva etapa `EnrichmentStage` (composición pura sobre
   `EngineStage`, patrón `KnowledgeStage`) entre `KnowledgeStage` y `ScoringStage`; escribe
   `PipelineContext.enrichmentResult` (campo tipado aditivo, patrón ADR-011/014/015). `KinConfig`
   cablea `FactRanker`, `EnrichmentEngine`, `EnrichmentStage` y el nuevo `SectionAssembler`/
   `SectionFormatter`.
7. **Sin cambios en contratos congelados**: `KinMethod`, `Pipeline` (algoritmo), `PipelineStage`,
   `ConversationOrchestrator`, `TurnPolicy`/`ResponseGuard`/`HistoryWindow`, `PromptAssembler`
   (solo recibe el reporte), `AIResponder`, `InterviewEngine`, `KnowledgeEngine`/`KnowledgeGateway`
   (consumidos, no modificados), `ProjectContext`, `ConversationDecision`, `kin/engine` y
   `ConsultingReport` (se le **agrega** una sección, no se modifican las existentes).

### Componentes nuevos (`kin.enrichment`)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `EnrichmentEngine` | Clase canonizada (E2 ✅) | Implementa `DomainEngine<EnrichmentInput, EnrichmentResult>`; fase `ANALYSIS`/tipo `DOMAIN`/prioridad 55; delega en `FactRanker`; degrada a `EnrichmentResult.empty()` si no hay hechos |
| `FactRanker` | Clase pura (E2/E3 ✅) | Selección y ponderación determinista de hechos: categoría de análisis por coincidencia de términos con las dimensiones del `ProjectContext`, score por `SourceTrust`/frescura/cobertura, dedup; nunca consulta al LLM |
| `EvidenceCategory` | enum puro (E2 ✅) | Categorías de análisis objetivo (`MARKET`, `INNOVATION`, `FINANCIAL`, `COMPETITIVE`) — refina el diseño original `KnowledgeRelevance` |
| `EvidenceScore` | record puro (E2 ✅) | Score determinista de relevancia de una evidencia para una categoría (valor `[0,1]` + motivo) |
| `KnowledgeEvidence` | record puro (E2 ✅) | Hecho seleccionado (`KnowledgeFact`) + `EvidenceScore` calculado en Java |
| `EvidenceRank` | record puro (E2 ✅) | Categoría + evidencias ordenadas (mayor a menor score) + confianza agregada — refina el `FactRanking` del diseño original |
| `EnrichmentInput` | record (EngineInput, E2 ✅) | `ProjectContext`, `KnowledgeResult`, `Set<EvidenceCategory>` categorías objetivo, umbral `minScore` |
| `EnrichmentResult` | record (EngineResult, E2 ✅) | `ranks`, `sourcesUsed`, `confidence`, `explanation`, `empty()` |
| `EnrichmentRepository` | puerto puro (E2 ✅) | `find(UUID)` / `save(UUID, EnrichmentResult)` + default `findOrEmpty` (offline-first); implementación de infraestructura en etapa posterior |
| `EnrichmentStage` | Stage aditivo (E6 ⏳) | Composición pura sobre `EngineStage`; construye `EnrichmentInput` (contexto + `PipelineContext.knowledgeResult`), invoca `EnrichmentEngine`, escribe `PipelineContext.enrichmentResult` |

### Cambios aditivos propuestos (se sancionarán en E4…E6)

| Contrato | Cambio propuesto | Tipo |
|----------|------------------|------|
| `PipelineContext` | nuevo campo tipado `EnrichmentResult enrichmentResult` (+ getter/setter) | Aditivo (patrón ADR-011/014/015) |
| `RecommendationInput` / `RiskInput` / `OpportunityInput` | overload aditivo `.withEnrichment(EnrichmentResult)`; constructores originales intactos | Aditivo |
| `ReportInput` | overload aditivo con `EnrichmentResult` | Aditivo |
| `ConsultingReport` | nueva sección `SourcesSection` (11.ª, `ReportSectionKind.SOURCES`); secciones existentes intactas | Aditivo (frontera ADR-012 sancionada, precedente M2 ADR-013) |
| `ReportSectionKind` | nuevo valor `SOURCES` | Aditivo |
| `ReportAssemblers` | + `SourcesSectionAssembler` (coordina `ReportEngine` con 11 assemblers) | Aditivo |
| `kin.ai.prompt.formatter` | + `SourcesSectionFormatter` | Aditivo |
| `Analyzer`s (mercado/innovación/financiero/competitivo) | lectura aditiva de hechos relevantes como evidencia | Aditivo |
| `KinConfig` | beans `FactRanker`, `EnrichmentEngine`, `EnrichmentStage`, `SourcesSectionAssembler`, `SourcesSectionFormatter`; `EnrichmentStage` en `chatPipeline(...)` | Cableado |

**Sin cambios**: `KinMethod`, `Pipeline` (algoritmo), `PipelineStage`, `ConversationOrchestrator`,
`TurnPolicy`/`DefaultTurnPolicy`, `ResponseGuard`, `HistoryWindow`, `PromptAssembler`,
`AIResponder`, `ReportEngine` (orquestador puro intacto), `ScoringEngine`,
`RecommendationEngine`/`RiskEngine`/`OpportunityEngine` (solo consumen inputs aditivos),
`KnowledgeEngine`/`KnowledgeGateway`/`SourceRegistry`/`SourceValidator`,
`InterviewEngine`/`InterviewBlueprint`/`AnswerValidator`, `ProjectContext`,
`ConversationDecision`, `kin/engine`.

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Pasar el `KnowledgeResult` crudo al prompt para que el LLM decida qué citar** | Violenta *Java decide*; el LLM seleccionaría y ponderaría conocimiento. La selección es decisión (FactRanker). Rechazada |
| **Modificar los constructores de los inputs de análisis** (agregar campo obligatorio) | Rompe los contratos congelados de input/output. Overload aditivo opcional preserva compatibilidad. Rechazada la variante obligatoria |
| **Acoplar `FactRanker` dentro de `KnowledgeGateway`** | El gateway (Fase 6) es contrato congelado y su responsabilidad es adquirir/validar/normalizar; la relevancia es responsabilidad del análisis. Rechazada |
| **Una etapa nueva en el `Pipeline`** (modificar el algoritmo) | El pipeline es contrato congelado; `EnrichmentStage` se inserta como stage aditivo (patrón ADR-014/015) sin tocar el algoritmo. Rechazada la variante de modificar `Pipeline` |
| **Citar fuentes directamente en la conversación (prompt)** | Toca la frontera ADR-012 más allá de lo sancionado; la cita viaja dentro del `ConsultingReport` (sección de fuentes) y el prompt solo lo formatea. Rechazada la vía de fuentes crudas en prompt |
| **Reescribir los 10 `SectionAssembler`** para inyectar fuentes en cada sección | Cambio invasivo sobre contrato ADR-011; una sección nueva de fuentes es aditiva y suficiente. Rechazada la variante invasiva |

---

## Consecuencias

### Positivas

- **El conocimiento externo de la Fase 6 se capitaliza**: los análisis se sustentan en hechos
  verificados y el reporte los cita con trazabilidad.
- **Java decide la relevancia**: `FactRanker` selecciona, pondera y ordena en Java;
  `SourceTrust` y la validación de la Fase 6 se preservan en el ranking.
- **Aditividad total**: overloads en inputs, una sección nueva en el reporte, un stage nuevo y un
  campo tipado en `PipelineContext` — mismos patrones ADR-011/014/015. Ningún contrato congelado
  se modifica (solo se sancionan aditivos).
- **Compatibilidad offline**: sin hechos, `EnrichmentResult.empty()` y los analizadores se
  comportan exactamente como hoy (backward compatible, tests existentes intactos).
- **Frontera ADR-012 preservada**: el prompt REPORT sigue consumiendo únicamente el
  `ConsultingReport`; la sección de fuentes es parte del VO tipado.
- **OCP**: nuevos criterios de relevancia = nuevos comportamientos de `FactRanker`; nuevas
  fuentes citables = adaptadores (Fase 6); el dominio no cambia.

### Negativas

- **Crecimiento del dominio**: +1 bounded context, +1 motor canonizado, +1 stage, +1 sección en
  el reporte y overloads aditivos en 4 inputs.
- **Complejidad del ranking**: `FactRanker` exige reglas de relevancia calibradas y testeables;
  riesgo de hechos irrelevantes si los términos no coinciden (mitigado con umbral mínimo).
- **Sección de fuentes en el reporte**: amplía el `ConsultingReport` (11 secciones) y añade un
  `SectionAssembler`/`SectionFormatter`.
- **Costo de decisión (pequeño)**: el `EnrichmentStage` añade un paso más al pipeline (solo si
  hay hechos; degrada rápido en vacío).

---

## Integración con fases anteriores

| Fase / ADR | Integración |
|-----------|-------------|
| **Fase 6 / ADR-014 (Knowledge)** | `EnrichmentStage` consume `PipelineContext.knowledgeResult`; `FactRanker` usa `KnowledgeFact`/`SourceTrust` ya validados. `KnowledgeEngine`/`KnowledgeGateway` **no cambian** |
| **Fase 7 / ADR-015 (Interview)** | La entrevista garantiza un `ProjectContext` completo; `FactRanker` mapea los hechos contra las dimensiones cubiertas (mejor relevancia). `InterviewEngine` **no cambia** |
| **Fase 5.4 / ADR-011 (ReportEngine)** | `ConsultingReport` gana la sección de fuentes; `ReportEngine` coordina 11 assemblers sin cambiar su lógica |
| **Fase 5.5 / ADR-012 (PromptAssembler)** | Frontera intacta: el prompt REPORT consume el reporte (ahora con fuentes); `SourcesSectionFormatter` formatea la sección |
| **Fase 5.6 / ADR-013 (Orchestrator)** | `ConversationOrchestrator`/`TurnPolicy`/`ResponseGuard` no cambian; la directiva de turno y el guard siguen en Java |
| **Fases 5.0–5.3 (Recommendation/Risk/Opportunity)** | Los motores consumen inputs aditivos con hechos; sus contratos de input/output originales intactos |

---

## Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Hechos irrelevantes que degradan el análisis | Media | `FactRanker` con umbral mínimo de score y pesos por `SourceTrust`; dedup; tests de relevancia por categoría |
| R2 | Violación accidental de la frontera ADR-012 | Media | La cita viaja en `ConsultingReport` (sección tipada); el prompt solo formatea; tests de frontera REPORT (patrón ADR-012) |
| R3 | Crecimiento de `PipelineContext` (otro campo aditivo) | Baja | Mismo patrón ADR-011/014/015; monitoreo continuo |
| R4 | Romper constructores de inputs de análisis | Alta | Overloads aditivos con constructores originales intactos; tests que usan los constructores actuales siguen verdes |
| R5 | Costo del ranking en cada turno | Baja | `FactRanker` stateless y determinista; degrada a `empty()` sin hechos; caché del `KnowledgeResult` (Fase 6) |
| R6 | Sección de fuentes vacía o ruidosa en el reporte | Baja | Si `EnrichmentResult` está vacío, la sección se omite (assembler devuelve sección vacía o el reporte la marca ausente) |
| R7 | Confianza en fuentes mal ponderada | Media | `SourceTrust` de la Fase 6 es insumo del score; `OFFICIAL_PUBLIC` > `SECONDARY` > `UNVERIFIED`; nunca se pregunta al LLM |

---

## Roadmap E1…E7

> Estado: **E1–E3 COMPLETADAS** — este ADR (estado **Propuesto**) + `FASE8_0.md`. El diseño
> (E1), el modelo de dominio (E2) y el ranking determinista (E3) están implementados. La
> aprobación habilitará E4…E7.

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico: ADR-016 (Propuesto) + documento FASE8_0 (arquitectura, componentes, flujo, integración, roadmap, criterios) | Documentación | ✅ **Completada (2026-08-01)** |
| **E2** | Modelo de dominio `kin.enrichment`: `EvidenceCategory`/`EvidenceScore`/`KnowledgeEvidence`/`EvidenceRank`/`EnrichmentInput`/`EnrichmentResult`/`EnrichmentRepository` + `FactRanker` + `EnrichmentEngine` canonizado (`DomainEngine`, fase `ANALYSIS`, prioridad 55) + tests de dominio | Código de dominio | ✅ **Completada (2026-08-02)** |
| **E3** | `FactRanker` determinista (mapeo por categoría, score por `SourceTrust`/frescura/cobertura, umbral, dedup) + tests por categoría (`FactRankerTest`, `FactRankerCategoryTest`, `FactRankerFreshnessTest`, …) | Ranking y relevancia | ✅ **Completada (2026-08-02)** |
| **E4** | Inputs aditivos (`RecommendationInput`/`RiskInput`/`OpportunityInput`.withEnrichment) + analizadores de mercado/innovación/financiero/competitivo consumen hechos + tests | Enriquecimiento del análisis | Pendiente |
| **E5** | Sección `SourcesSection` (11.ª) en `ConsultingReport` + `SourcesSectionAssembler` + `SourcesSectionFormatter` + `ReportInput` aditivo + tests | Citas en el reporte | Pendiente |
| **E6** | Integración aditiva al pipeline: `EnrichmentStage` + `PipelineContext.enrichmentResult` + cableado en `KinConfig` + tests de integración/frontera | Integración pipeline | Pendiente |
| **E7** | Auditoría de cierre: ADR-016 → **Aprobado**, contratos congelados intactos, `./mvnw clean verify` (BUILD SUCCESS), cobertura `kin.enrichment` ≥ 90 % (JaCoCo), cierre oficial de la Fase 8 | Cierre de fase | Pendiente |

---

## Criterios de aceptación

- [ ] ADR-016 en estado **Aprobado** tras E1…E7 (actualmente **Propuesto**).
- [x] `kin.enrichment` es 100 % POJO (sin Spring/JPA/IA; solo `java.*`/`org.slf4j`).
- [x] `EnrichmentEngine` implementa `DomainEngine` (fase `ANALYSIS`/tipo `DOMAIN`/prioridad 55).
- [x] `FactRanker` es 100 % determinista: selecciona, pondera y ordena hechos en Java; nunca consulta al LLM.
- [ ] Contratos congelados (`KinMethod`, `Pipeline`, `PipelineStage`, `ConversationOrchestrator`, `PromptAssembler`, `AIResponder`, `KnowledgeEngine`, `InterviewEngine`, `ReportEngine`, `ConsultingReport` existente, `kin/engine`, `ProjectContext`, `ConversationDecision`) **sin cambios** — solo aditivos sancionados.
- [ ] Inputs de análisis con overloads aditivos; constructores originales intactos; tests existentes verdes sin modificación de aserciones.
- [ ] Frontera ADR-012 intacta: el prompt REPORT consume solo `ConsultingReport` (con la nueva sección de fuentes).
- [ ] Offline-first: sin hechos, `EnrichmentResult.empty()` y el pipeline se comporta como hoy.
- [ ] `./mvnw clean verify` → **BUILD SUCCESS**; cobertura de dominio ≥ 90 % en `kin.enrichment` (JaCoCo).

---

## Estado

**PROPUESTO** — Etapas E1–E3 de la Fase 8 (diseño arquitectónico, modelo de dominio y ranking
determinista) completadas. Este ADR NO modifica contratos congelados. Requiere aprobación antes
de implementar E4…E7.

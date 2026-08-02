# FASE 8.0 - Knowledge-Enhanced Analysis (Enriquecimiento del Análisis con Conocimiento Externo)

> **Estado**: **E1–E3 COMPLETADAS** (diseño arquitectónico + modelo de dominio + ranking determinista).
> **Base**: KIN 2.0 Alpha 1 (`v2.0.0-alpha1`) - `ALPHA STABLE` + Fases 6 y 7 cerradas oficialmente
> (ADR-014 y ADR-015 Aprobados). Pipeline de 12 etapas.
> **ADR**: 016 (knowledge-enhanced analysis) - **Estado: Propuesto**.
>
> Secuencia oficial del proyecto: Fase 5.6 → Conversation Orchestrator (CERRADA), Fase 6 →
> KnowledgeEngine + RAG (CERRADA), Fase 7 → Strategic Interview Engine (CERRADA), **Fase 8 →
> Knowledge-Enhanced Analysis (E1–E3 — COMPLETADAS; E4 — PENDIENTE)**.

---

## 1. Objetivo

Cerrar el ciclo entre la adquisición de conocimiento externo (Fase 6) y el análisis (Fases
5.x/7): hacer que las recomendaciones, los riesgos, las oportunidades y el `ConsultingReport`
**consuman y citen** el conocimiento externo verificado, manteniendo intacto el principio rector:

> **Java decide. El LLM únicamente comunica.**
> Java decide qué hechos externos son relevantes, cómo ponderan y qué fuentes se citan.

Objetivos concretos:

1. **Seleccionar** en Java los hechos relevantes para el proyecto (`FactRanker` determinista),
   preservando `SourceTrust` y la validación de la Fase 6.
2. **Enriquecer** los motores de análisis (recomendación, riesgo, oportunidad) mediante inputs
   **aditivos** que portan los hechos seleccionados, sin alterar contratos congelados.
3. **Citar** en el `ConsultingReport` las fuentes verificadas que sustentan el análisis
   (11.ª sección `SourcesSection`), para que el LLM las comunique con trazabilidad.
4. **Preservar** la offline-first: sin hechos, `EnrichmentResult.empty()` y el pipeline se
   comporta exactamente como hoy.

---

## 2. Motivación

### 2.1 Brechas detectadas (auditoría de la Fase 8)

| # | Brecha | Impacto |
|---|--------|---------|
| 1 | El `KnowledgeResult` de la Fase 6 es producido por `KnowledgeStage` pero **nunca consumido** por los motores de análisis (0 referencias en `kin/reporting`) | El conocimiento externo verificado no mejora decisiones |
| 2 | Sin selección de hechos relevantes en Java | No se decide qué hechos aplican a qué dimensión/categoría |
| 3 | La trazabilidad no llega a la comunicación | El LLM no puede sustentar el análisis con fuentes (ADR-012 exigía ADR propia para citar) |
| 4 | `ReportInput` (ADR-011) no porta conocimiento | El reporte de 10 secciones ignora las fuentes |
| 5 | La inversión de la Fase 6 queda latente | Adquisición/validación sin consumo en decisiones |

### 2.2 Por qué ahora

El baseline §7.5 (Prioridad 1) recomienda explícitamente consumir `KnowledgeResult` en los
analizadores (mercado, innovación, financiero, competitivo) e **exponer conocimiento citado en la
comunicación (exige ADR propia por la frontera ADR-012)**. Las Fases 6 y 7 ya entregaron los dos
insumos: `PipelineContext.knowledgeResult` (hechos verificados) y un `ProjectContext` completo
vía la entrevista estratégica (ADR-015). La Fase 8 los conecta.

---

## 3. Responsabilidades

| Actor | Responsabilidad |
|-------|-----------------|
| `EnrichmentEngine` | Motor canonizado (`DomainEngine<EnrichmentInput, EnrichmentResult>`, fase `ANALYSIS`/`DOMAIN`/55): selecciona y pondera hechos; degrada a `empty()` si no hay hechos. Decisión 100 % Java |
| `FactRanker` | Selección determinista de hechos relevantes: mapeo por categoría (mercado, innovación, financiero, competitivo), score por `SourceTrust`/frescura/cobertura semántica, dedup, umbral mínimo. Nunca consulta al LLM |
| `EvidenceRank` / `KnowledgeEvidence` / `EvidenceScore` | Hecho seleccionado + categoría objetivo + score de relevancia + `KnowledgeFact` de origen (ranks por categoría) |
| `EnrichmentResult` | Resultado inmutable: `ranks`, `sourcesUsed`, `confidence`, `explanation`, `empty()` |
| `EnrichmentStage` | Etapa aditiva de pipeline (composición pura sobre `EngineStage`): construye `EnrichmentInput` (contexto + `knowledgeResult`), invoca `EnrichmentEngine`, escribe `PipelineContext.enrichmentResult` |
| Motores de análisis | Consumen inputs aditivos `.withEnrichment(...)`; los analizadores de mercado/innovación/financiero/competitivo incorporan hechos como evidencia |
| `ReportEngine` + `SourcesSectionAssembler` | Orquestador puro (11 assemblers) + ensamblador de la sección de fuentes; `ReportInput` porta el `EnrichmentResult` (overload aditivo) |
| `SourcesSectionFormatter` | Formatea la sección de fuentes como Markdown ligero (frontera ADR-012 intacta) |
| LLM | Comunica el análisis y las fuentes ya seleccionadas por Java. No recupera, valida ni decide conocimiento |

---

## 4. Componentes

### 4.1 Componentes nuevos (`com.kinplatform.kin.enrichment` — dominio POJO puro)

| Componente | Naturaleza | Responsabilidad |
|------------|-----------|-----------------|
| `EnrichmentEngine` | Clase canonizada (E2 ✅) | `DomainEngine<EnrichmentInput, EnrichmentResult>`; fase `ANALYSIS`/tipo `DOMAIN`/prioridad 55; delega en `FactRanker`; offline-first |
| `FactRanker` | Clase pura (E2/E3 ✅) | Ranking determinista de hechos por relevancia al proyecto |
| `EvidenceCategory` | enum puro (E2 ✅) | Categorías objetivo (`MARKET`, `INNOVATION`, `FINANCIAL`, `COMPETITIVE`) + umbral mínimo |
| `EvidenceScore` | record puro (E2 ✅) | Score determinista de relevancia `[0,1]` + motivo |
| `KnowledgeEvidence` | record puro (E2 ✅) | Hecho + score (`KnowledgeFact` + `EvidenceScore`) |
| `EvidenceRank` | record puro (E2 ✅) | Categoría + evidencias ordenadas + confianza agregada |
| `EnrichmentInput` | record (EngineInput, E2 ✅) | `ProjectContext`, `KnowledgeResult`, `Set<EvidenceCategory>`, umbral `minScore` |
| `EnrichmentResult` | record (EngineResult, E2 ✅) | `ranks`, `sourcesUsed`, `confidence`, `explanation`, `empty()` |
| `EnrichmentRepository` | puerto puro (E2 ✅) | `find(UUID)` / `save(UUID, EnrichmentResult)` + default `findOrEmpty` (offline-first) |
| `EnrichmentStage` | Stage aditivo (E6 ⏳) | Compone `EngineStage`; escribe `PipelineContext.enrichmentResult` |

### 4.2 Cambios aditivos propuestos (se sancionarán en E4…E6)

| Contrato | Cambio propuesto | Tipo |
|----------|------------------|------|
| `PipelineContext` | campo `EnrichmentResult enrichmentResult` | Aditivo (patrón ADR-011/014/015) |
| `RecommendationInput` / `RiskInput` / `OpportunityInput` | overload `.withEnrichment(EnrichmentResult)` | Aditivo |
| `ReportInput` | overload aditivo con `EnrichmentResult` | Aditivo |
| `ConsultingReport` | 11.ª sección `SourcesSection` | Aditivo |
| `ReportSectionKind` | valor `SOURCES` | Aditivo |
| `ReportAssemblers` | + `SourcesSectionAssembler` | Aditivo |
| `kin.ai.prompt.formatter` | + `SourcesSectionFormatter` | Aditivo |
| Analizadores (mercado/innovación/financiero/competitivo) | lectura aditiva de hechos como evidencia | Aditivo |
| `KinConfig` | beans nuevos + `EnrichmentStage` en `chatPipeline(...)` | Cableado |

**Sin cambios**: `KinMethod`, `Pipeline` (algoritmo), `PipelineStage`, `ConversationOrchestrator`,
`TurnPolicy`/`ResponseGuard`/`HistoryWindow`, `PromptAssembler`, `AIResponder`, `ReportEngine`
(orquestador puro), `KnowledgeEngine`/`KnowledgeGateway`/`SourceRegistry`/`SourceValidator`,
`InterviewEngine`/`InterviewBlueprint`/`AnswerValidator`, `ProjectContext`, `ConversationDecision`,
`kin/engine`.

---

## 5. Diagrama lógico

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                          DOMINIO (kin.enrichment - POJO puro)                    │
│                                                                                │
│  EnrichmentEngine : DomainEngine<EnrichmentInput, EnrichmentResult>            │
│        │                                                                        │
│        └──► FactRanker ──► EvidenceRank (categoría + evidencias + score)       │
│        └──► EvidenceCategory (categorías objetivo + umbral)                  │
│                                                                                │
│  EnrichmentInput / EnrichmentResult / EnrichmentRepository (records/puerto)    │
└──────────────┬─────────────────────────────────────────────────────────────────┘
               │  consumido por
┌──────────────▼─────────────────────────────────────────────────────────────────┐
│                    MOTORES DE ANÁLISIS (aditivo)                                │
│                                                                                │
│  RecommendationInput.withEnrichment / RiskInput.withEnrichment                  │
│  OpportunityInput.withEnrichment  →  analizadores mercado/innovación/           │
│                                       financiero/competitivo (evidencia)       │
└──────────────┬─────────────────────────────────────────────────────────────────┘
               │
┌──────────────▼─────────────────────────────────────────────────────────────────┐
│                    REPORTE (frontera ADR-012 intacta)                           │
│                                                                                │
│  ReportInput.withEnrichment → ReportEngine (11 SectionAssembler)               │
│     → ConsultingReport (10 secciones + SourcesSection)                         │
│     → ReportPromptBuilder → SourcesSectionFormatter → prompt (solo reporte)    │
└──────────────┬─────────────────────────────────────────────────────────────────┘
               │
               ▼
            LLM (comunica análisis + fuentes ya seleccionadas)
```

### Pipeline (12 → 13 etapas, aditivo)

`Analizador → Evaluador → Estratega → Entrevista → Conocimiento → **Enriquecimiento** → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`

`EnrichmentStage` se inserta entre `KnowledgeStage` y `ScoringStage` (patrón ADR-014/015):
lee `PipelineContext.knowledgeResult` + `ProjectContext`, produce `EnrichmentResult` y lo
escribe en `PipelineContext.enrichmentResult`. Sin hechos, el stage degrada a `empty()` sin
costo y el pipeline se comporta como hoy.

---

## 6. Integración con las fases anteriores

| Fase / ADR | Integración |
|-----------|-------------|
| **Fase 6 / ADR-014 (Knowledge)** | `EnrichmentStage` consume `PipelineContext.knowledgeResult`; `FactRanker` usa `KnowledgeFact`/`SourceTrust` ya validados. `KnowledgeEngine`/`KnowledgeGateway` no cambian |
| **Fase 7 / ADR-015 (Interview)** | La entrevista garantiza un `ProjectContext` completo; el ranking mapea contra dimensiones cubiertas. `InterviewEngine` no cambia |
| **Fase 5.4 / ADR-011 (ReportEngine)** | `ConsultingReport` gana la 11.ª sección; `ReportEngine` coordina 11 assemblers sin cambiar su lógica (orquestador puro) |
| **Fase 5.5 / ADR-012 (PromptAssembler)** | Frontera intacta: el prompt REPORT consume solo el reporte (con la nueva sección); `SourcesSectionFormatter` la presenta |
| **Fase 5.6 / ADR-013 (Orchestrator)** | `ConversationOrchestrator`/`TurnPolicy`/`ResponseGuard` no cambian |
| **Fases 5.0–5.3 (Recommendation/Risk/Opportunity)** | Motores consumen inputs aditivos con hechos; contratos originales intactos |
| **Fase 5.2.1 / ADR-006…009 (runtime/scoring)** | `KinMethod` y `ScoringEngine` no cambian; la etapa es aditiva al mismo pipeline |

---

## 7. Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Hechos irrelevantes que degradan el análisis | Media | `FactRanker` con umbral mínimo y pesos por `SourceTrust`; dedup; tests por categoría |
| R2 | Violación accidental de la frontera ADR-012 | Media | La cita viaja en `ConsultingReport` (sección tipada); el prompt solo formatea; tests de frontera |
| R3 | Crecimiento de `PipelineContext` (otro campo aditivo) | Baja | Mismo patrón ADR-011/014/015; monitoreo |
| R4 | Romper constructores de inputs de análisis | Alta | Overloads aditivos; constructores originales intactos; tests existentes sin cambio de aserciones |
| R5 | Costo del ranking por turno | Baja | `FactRanker` stateless; degrada a `empty()` sin hechos; caché de la Fase 6 |
| R6 | Sección de fuentes vacía o ruidosa | Baja | Se omite la sección si `EnrichmentResult` está vacío |
| R7 | Confianza en fuentes mal ponderada | Media | `SourceTrust` insumo del score; `OFFICIAL_PUBLIC` > `SECONDARY` > `UNVERIFIED` |

---

## 8. Roadmap completo E1…E7

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico: ADR-016 (Propuesto) + FASE8_0 (objetivo, motivación, responsabilidades, componentes, diagrama lógico, integración, riesgos, roadmap, criterios de aceptación) | Documentación | ✅ **Completada (2026-08-01)** |
| **E2** | Modelo de dominio `kin.enrichment`: `EvidenceCategory`/`EvidenceScore`/`KnowledgeEvidence`/`EvidenceRank`/`EnrichmentInput`/`EnrichmentResult`/`EnrichmentRepository` + `FactRanker` + `EnrichmentEngine` canonizado (`DomainEngine`, fase `ANALYSIS`, prioridad 55) + tests de dominio | Código de dominio | ✅ **Completada (2026-08-02)** |
| **E3** | `FactRanker` determinista (mapeo por categoría, score por `SourceTrust`/frescura/cobertura, umbral, dedup) + tests por categoría (`FactRankerTest`, `FactRankerCategoryTest`, `FactRankerFreshnessTest`, …) | Ranking y relevancia | ✅ **Completada (2026-08-02)** |
| **E4** | Inputs aditivos (`RecommendationInput`/`RiskInput`/`OpportunityInput`.withEnrichment) + analizadores consumen hechos + tests | Enriquecimiento del análisis | Pendiente |
| **E5** | `SourcesSection` (11.ª) + `SourcesSectionAssembler` + `SourcesSectionFormatter` + `ReportInput` aditivo + tests | Citas en el reporte | Pendiente |
| **E6** | `EnrichmentStage` + `PipelineContext.enrichmentResult` + cableado en `KinConfig` + tests de integración/frontera | Integración pipeline | Pendiente |
| **E7** | Auditoría de cierre: ADR-016 → Aprobado, contratos intactos, `./mvnw clean verify`, cobertura `kin.enrichment` ≥ 90 %, cierre oficial de la Fase 8 | Cierre de fase | Pendiente |

---

## 9. Criterios de aceptación

- [ ] ADR-016 en estado **Aprobado** tras E1…E7 (actualmente **Propuesto**).
- [x] `kin.enrichment` es 100 % POJO (sin Spring/JPA/IA).
- [x] `EnrichmentEngine` implementa `DomainEngine` (fase `ANALYSIS`/tipo `DOMAIN`/prioridad 55).
- [x] `FactRanker` 100 % determinista; nunca consulta al LLM.
- [ ] Contratos congelados sin cambios (solo aditivos sancionados por ADR-016).
- [ ] Inputs de análisis con overloads aditivos; constructores originales intactos.
- [ ] Frontera ADR-012 intacta: prompt REPORT consume solo `ConsultingReport`.
- [ ] Offline-first: sin hechos, `EnrichmentResult.empty()` y el pipeline se comporta como hoy.
- [ ] `./mvnw clean verify` → **BUILD SUCCESS**; cobertura `kin.enrichment` ≥ 90 % (JaCoCo).
- [x] E2–E3 implementados como código de dominio aditivo; **E4…E7 no implementados en las etapas E1–E3**.

---

## 10. Estado del entregable

| Etapa | Estado |
|-------|--------|
| **E1 — Diseño arquitectónico (ADR-016 Propuesto + FASE8_0)** | ✅ **Completada** |
| **E2 — Modelo de dominio `kin.enrichment` + `EnrichmentEngine` + tests** | ✅ **Completada** |
| **E3 — `FactRanker` determinista + tests por categoría** | ✅ **Completada** |
| **E4 — Enriquecimiento del análisis (inputs aditivos + analizadores)** | ⏳ Pendiente |

**FASE 8 — ETAPAS E1–E3 COMPLETADAS (diseño + modelo de dominio + ranking determinista).
E4…E7 PENDIENTES. Ningún contrato congelado de `BASELINE_ARCHITECTURE.md` se modificó.**

*Fase 8 — Knowledge-Enhanced Analysis. Etapas E1–E3 cerradas oficialmente. ADR-016 en estado
Propuesto (diseño de referencia); el dominio `kin.enrichment` se implementó de forma aditiva sin
modificar contratos congelados. E4…E7 requieren la aprobación del ADR.*

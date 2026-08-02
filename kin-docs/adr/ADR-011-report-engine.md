# ADR-011: ReportEngine — orquestador puro del ConsultingReport

**Estado**: Aprobado (Fase 5.4 — ReportEngine implementado E1…E6 y cerrado E7; alineado con el estado oficial del milestone `v2.0.0-alpha1`)
**Fecha**: 2026-07-31
**Autor**: KIN Architecture Team

**Contexto**: Tras las Fases 4 y 5.0–5.3, el runtime consolidado (ADR-006…ADR-009) ejecuta un
pipeline de **9 etapas** que ya produce `ScoreResult`, `RecommendationResult`, `RiskResult` y
`OpportunityResult` en `PipelineContext`. No existe un modelo unificado de reporte de consultoría:
la estructura del informe vive hoy en el prompt del LLM (hallazgo H5 de la auditoría pre-Fase 5.3),
lo que viola el principio "Java decide, LLM comunica" y hace que el reporte no sea tipado, auditable
ni persistible. El diseño previo (`FASE5_DISENO_ARQUITECTONICO.md` §8 y `FASE5_CONSOLIDACION` §6.2)
planificó `ReportEngine` + `ConsultingReport`, pero con un `ReportEngine` que **invocaba a los otros
engines**: eso duplicaría ejecución que el pipeline ya hace (observación M14) y recalcularía
resultados. La Fase 5.4 debe producir `ConsultingReport` como VO inmutable con un `ReportEngine`
que sea **orquestador puro**: sin reglas de negocio y sin recalcular información ya producida.

**Decisión**:

1. **Nuevo subpaquete `com.kinplatform.kin.reporting.report`** (dominio POJO puro, sin Spring/JPA/IA)
   con tres capas internas:
   - `model/` (VO del reporte): `ConsultingReport`, `ReportSection` (marcadora con `sectionName()` +
     `kind()`), `ReportSectionKind` (enum de taxonomía), `ReportBuilder`, `ReportMetadata` y las 9
     secciones de contenido (`ExecutiveSummary`, `ScoresSection`, `RecommendationsSection`,
     `RisksSection`, `OpportunitiesSection`, `FinancialSection`, `MarketSection`, `InnovationSection`,
     `NextStepsSection`) + VOs auxiliares (`DimensionCoverage`, `NextStep`).
   - `assembler/` (10 ensambladores): `ExecutiveSummaryAssembler`, `ScoresSectionAssembler`,
     `RecommendationsSectionAssembler`, `RisksSectionAssembler`, `OpportunitiesSectionAssembler`,
     `FinancialSectionAssembler`, `MarketSectionAssembler`, `InnovationSectionAssembler`,
     `NextStepsSectionAssembler`, `ReportMetadataAssembler`.
   - raíz: `ReportInput`, `ReportModel`, `SectionAssembler` (interfaz), `ReportAssemblers`
     (agrupación tipada), `ReportEngine`.
2. **`ReportEngine implements DomainEngine<ReportInput, ConsultingReport>`**:
   - `metadata()` → `EngineMetadata.of("ReportEngine", model.version(), "KIN Architecture Team",
     EnginePhase.REPORTING, EngineType.DOMAIN, 70)`. Fase `REPORTING` ya existe (16 fases, ordinal 14);
     prioridad **70** (después de Opportunity=60 y antes de los futuros).
   - **Orquestador puro**: recibe los cuatro resultados **ya calculados** en `ReportInput`
     (`score`, `recommendation`, `risk`, `opportunity`). NUNCA invoca `ScoringEngine`,
     `RecommendationEngine`, `RiskEngine` ni `OpportunityEngine`. NUNCA recalcula scores, prioridades,
     confianzas ni niveles. Guarda de nulidad: `input`/`projectContext`/`evaluation`/`score` nulos →
     `ConsultingReport.empty()`.
   - Coordina los 10 `SectionAssembler` (tipados, agrupados en `ReportAssemblers`) y ensambla el
     reporte con `ReportBuilder`.
3. **`ConsultingReport`**: record inmutable con `id` determinista y reproducible
   (`DeterministicId.from(projectId.toString(), "ConsultingReport", reportVersion)` — método existente
   `from(String,String,String)`, **sin dependencia de `OffsetDateTime.now()`** para el identificador;
   `generatedAt` queda solo como metadata). Contiene `projectId`, las 9 secciones y `ReportMetadata`
   (con `architectureVersion`). Inmutabilidad completa: listas y mapas copiados con
   `List.copyOf`/`Map.copyOf` (incl. `categoryScores` de `ScoresSection` y `engineVersions` de
   `ReportMetadata`). Implementa `EngineResult` para integrarse al
   `EngineRegistry`/`EngineExecutor`/`EngineStage`: `confidence()` → `metadata.confidence()`,
   `explanation()` → `executiveSummary.summaryText()`, `generatedBy()` → `metadata.generatedBy()`,
   `engineVersion()` → `metadata.reportVersion()`, `isEmpty()` → ausencia de contenido en las
   secciones. Se construye con `ReportBuilder`.
4. **`ReportSection`**: interfaz con `sectionName()` (única fuente de verdad del nombre) + `kind()`
   (taxonomía vía enum `ReportSectionKind`: GENERAL, EXECUTIVE, SCORING, ANALYTIC, PROJECTION,
   AGGREGATE, METADATA; método default = GENERAL). Las 10 secciones (incluida `ReportMetadata`) la
   implementan. Las secciones de recomendaciones/riesgos/oportunidades **reutilizan** los VOs
   existentes `Recommendation`, `Risk` y `Opportunity` — no se duplican.
5. **`SectionAssembler<T extends ReportSection>`**: contrato `assemble(ReportInput) → T`, stateless.
   Sin `sectionName()` propio (el nombre lo aporta la sección producida; `ReportBuilder.build()`
   deriva `sectionsIncluded` de las secciones ensambladas). Los 10 ensambladores se inyectan
   **tipados** vía el record de agrupación `ReportAssemblers` (sin `List<?>` + casts, sin `switch` de
   dispatch).
6. **`ReportBuilder`** (contrato estricto, independiente del `empty()` de fallback):
   - `create(UUID projectId)` con `projectId` nulo → `IllegalArgumentException`.
   - Cada setter de sección con sección ya asignada → `IllegalStateException` (sin *last-wins*).
   - `validate()` exige las secciones obligatorias (`executiveSummary`, `scores`, `metadata`) y
     `id`/`projectId` no nulos; se invoca dentro de `build()`.
   - `build()`: `generatedAt = OffsetDateTime.now()` (solo metadata); `id` determinista por
     proyecto+versión (`DeterministicId.from(projectId.toString(), "ConsultingReport",
     metadata.reportVersion())`); `sectionsIncluded` derivada de las secciones ensambladas.
7. **`ReportStage`** compone `EngineStage<ReportInput, ConsultingReport>` (mismo patrón que
   `RiskStage`/`OpportunityStage`): predicado `projectContext != null && evaluation != null &&
   decision != null && decision.shouldGenerateReport() && scoreResult != null &&
   recommendationResult != null && riskResult != null && opportunityResult != null`. Se agrega al
   pipeline en `KinConfig.chatPipeline` **después de `OpportunityStage` y antes de `EventStage`**
   (pipeline de **10 etapas**).
8. **`PipelineContext`** (contrato congelado BASELINE §4.1) gana un campo tipado aditivo
   `consultingReport` (getter/setter), mismo patrón que `opportunityResult`. `EngineStage` además
   registra el resultado en `engineResults()` automáticamente (sin cambios en el stage). Aditivo: no
   elimina ni renombra campos → compatibilidad hacia atrás.
9. **`KinMethodResult`** (contrato congelado BASELINE §4.1) gana el componente aditivo
   `consultingReport` (único punto de construcción: `KinMethod.execute`). Junto con `PipelineContext`
   (item 8), son los **dos** contratos congelados de §4.1 que esta ADR modifica de forma aditiva —
   sin eliminar ni renombrar campos y acotado a un punto de construcción.
10. **Sin cambios fuera del modelo**: no se emiten eventos nuevos (el `EventStage` queda intacto; el
    `ReportGeneratedEvent` existente se mantiene), no se tocan REST/SSE/frontend, no se persiste el
    reporte (fases futuras) y no se introducen renderers.
11. **Puertos futuros declarados (no implementados)**: `ReportRenderer`
    (`render(ConsultingReport) → String`/`byte[]`) + `RendererRegistry` (patrón `EngineRegistry`)
    consumirán el `ConsultingReport` **congelado** en la Fase 5.5+ sin modificarlo. Se declaran ahora
    para proteger el contrato, sin abstracción especulativa (YAGNI).
12. **Evolución de `ReportInput` (documentada)**: al llegar el 5º motor de resultados
    (`KnowledgeEngine`, Fase 6 / KIN 3.0), `ReportInput` encapsulará los resultados en un contenedor
    `EngineResults` (accesores tipados para los 4 núcleo + acceso genérico), evitando añadir campos al
    record por cada motor nuevo. Único punto de cambio: la `inputFactory` de `ReportStage`.

**Alternativas consideradas**:

1. *`ReportEngine` que invoca los engines internamente (diseño FASE5_DISENO original)* — Rechazado:
   duplicaría la ejecución del pipeline (observación M14), acoplaría el motor a otros motores y
   violaría el requisito "sin recalcular". Los resultados ya están en `PipelineContext` cuando la
   etapa `Reporte` corre.
2. *`ReportEngine` como Domain Service suelto, sin implementar `DomainEngine`* — Rechazado: pierde
   `EngineRegistry`/`EngineExecutor`/`EngineStage`/`engineResults` y rompe la uniformidad del
   contrato de motores (ADR-005).
3. *Un solo ensamblador gigante o secciones como `Map<String,Object>`* — Rechazado: replica la
   observación M15 (switch/estructura de 200 líneas no configurable) y pierde tipado y auditabilidad.
4. *Auto-descubrimiento `List<SectionAssembler<?>>` con casts* — Rechazado: exige casts sin verificar
   y un dispatch de colocación; la agrupación tipada `ReportAssemblers` es más auditable y testeable.
5. *Secciones Financial/Market/Innovation con reglas de negocio (scoring derivado, proyecciones,
   estimaciones)* — Rechazado: estas secciones **solo proyectan** los valores de dimensión ya
   presentes en `ProjectContext` y su estado de cobertura; cualquier cálculo de mercado/financiero es
   de fases futuras o del LLM (solo explicación).
6. *Generar el reporte (o sus secciones) desde el LLM* — Rechazado: viola "Java decide, LLM comunica"
   y rompe la determinismo/auditabilidad. La Fase 5.5 añadirá únicamente la **explicación** LLM sobre
   el `ConsultingReport` ya calculado.
7. *Incluir renderers (Markdown/HTML/JSON) y persistencia del reporte en esta fase* — Rechazado: fuera
   de alcance; el modelo queda congelado para que renderers (port `ReportRenderer` +
   `RendererRegistry`, diseño FASE5_CONSOLIDACION §4) y persistencia lo consuman sin modificarlo.

**Consecuencias**:
- Positivas: reporte tipado, inmutable, determinista y auditable; integrado al `EngineRegistry` y al
  `EngineStage`; la estructura del informe sale del prompt (prepara la Fase 5.5); reutilización de los
  VOs `Recommendation`/`Risk`/`Opportunity` existentes; extensibilidad OCP (cambiar el comportamiento
  de una sección = cambiar un bean de assembler); el reporte queda listo para renderers/persistencia.
- Negativas: **30 clases + 1 enum** en `kin.reporting.report` (la cobertura ≥90 % del dominio se amplía
  a este subpaquete); `PipelineContext` gana un campo más (mantiene el patrón tipado existente);
  `KinMethodResult` gana un componente (cambio aditivo sobre contrato congelado, acotado a
  `KinMethod`); no se añade UI, persistencia ni renderizado del reporte (fases posteriores).

**Regla que modifica**: `KIN_ARCHITECTURE_GOVERNANCE.md` §6.2 — `ReportEngine` pasa de
"🔮 Futuro (KIN 2.2/3.0)" a "✅ Existente (Fase 5.4)". `BASELINE_ARCHITECTURE.md` §4.1 — dos contratos
congelados ganan componentes aditivos vía esta ADR: `PipelineContext.consultingReport` (campo tipado)
y `KinMethodResult.consultingReport` (componente del record); ambos sin eliminar ni renombrar campos.
Ninguna regla de Governance se elimina.

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend, `DomainEngine`/`EngineStage`/
`EngineExecutor`/`EngineRegistry`, `DeterministicId` (se usa el overload existente
`from(String,String,String)`, sin modificarlo), `ScoringEngine`/`RecommendationEngine`/`RiskEngine`/
`OpportunityEngine`, `ProjectContext` ni contratos de aplicación. `PipelineContext` y `KinMethodResult`
(ambos contratos congelados de BASELINE §4.1) cambian de forma **aditiva**. El pipeline pasa de 9 a
10 etapas con `ReportStage`. Los 172 tests existentes deben seguir en verde (verificación en la Fase
5.4) y el nuevo subpaquete debe cumplir ≥90 % de cobertura de instrucciones en JaCoCo.

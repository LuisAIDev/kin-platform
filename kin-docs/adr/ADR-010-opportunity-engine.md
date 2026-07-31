# ADR-010: OpportunityEngine — motor de oportunidades del proyecto

**Estado**: Aprobado
**Fecha**: 2026-07-31
**Autor**: KIN Architecture Team

**Contexto**: El pipeline consolidado (ADR-005…ADR-009) detecta brechas de dimensiones (scoring),
recomendaciones accionables (RecommendationEngine) y riesgos (RiskEngine), pero no identifica
oportunidades de mejora/capitalización del proyecto. El `CompletenessEvaluation` ya expone
`detectedOpportunities` (señales textuales) sin motor que las consume. La Fase 5.3 debe agregar
`OpportunityEngine` siguiendo exactamente la filosofía de `RiskEngine` (coordinador + analizadores +
ensamblador), sin copiar reglas existentes, sin romper contratos estables y cubriendo las 8 categorías
obligatorias: mercado, innovación, tecnológicas, financieras, competitivas, escalabilidad,
automatización y monetización.

**Decisión**:

1. **Nuevo subpaquete `com.kinplatform.kin.reporting.opportunity`** con dominio POJO puro (sin
   Spring/JPA/IA), espejando la estructura de `kin.reporting.risk`:
   - `OpportunityCategory` enum (8 categorías), `Opportunity`, `OpportunityExplanation`
     (`usedInformation`, `appliedRule`, `reason`, `evidence`), `OpportunityInput`
     (`projectContext`, `evaluation`, `decision`, `score`), `OpportunityResult`,
     `OpportunityModel` (umbrales de prioridad, VO inyectable), `OpportunityAssembler`
     (confianza + explicación compartidas), `OpportunityAnalyzer` (interface:
     `category()`, `analyze(OpportunityInput)`, `version()`), `OpportunityEngine`,
     `Market/Innovation/Technological/Financial/Competitive/Scalability/Automation/MonetizationOpportunityAnalyzer`.
2. **`OpportunityEngine implements DomainEngine<OpportunityInput, OpportunityResult>`**:
   - `metadata()` → `EngineMetadata.of("OpportunityEngine", model.version(), "KIN Architecture Team",
     EnginePhase.OPPORTUNITY, EngineType.DOMAIN, 60)`. Prioridad **60**: el ordinal de fase
     `OPPORTUNITY` ya existe después de `RISK` (50) y antes de `KNOWLEDGE`; el engine corre tras
     riesgos y antes de los futuros.
   - Coordinador sin reglas de negocio: auto-descubre `List<OpportunityAnalyzer>` (inyección
     Spring, mismo patrón que `List<RiskAnalyzer>`), consolida, ordena por `priority` desc +
     categoría, calcula top y agregado. Guarda de nulidad: input/context/evaluation/score nulos →
     `OpportunityResult.empty()`.
   - `Opportunity.create(...)` usa `DeterministicId.from(category.name(), title, description)`.
   - Confianza compartida (patrón `RiskAssembler`): `0.35 + 0.35·coveragePercent +
     0.3·qualityOfInformation`, clamp `[0,1]`.
3. **`OpportunityStage`** compone `EngineStage<OpportunityInput, OpportunityResult>` (como
   `RiskStage`): predicado `projectContext() != null && evaluation() != null && decision() != null
   && decision().shouldGenerateReport() && scoreResult() != null`. Se agrega al pipeline en
   `KinConfig.chatPipeline` **después de `RiskStage` y antes de `EventStage`** (pipeline de 9 etapas).
4. **`PipelineContext`** gana un campo tipado aditivo `opportunityResult` (getter/setter), mismo
   patrón que `riskResult`. `EngineStage` además registra el resultado en `engineResults()`
   automáticamente (sin cambios en el stage).
5. **Detección determinista** (sin IA): (a) dimensiones ausentes de `ProjectContext` mapeadas a
   categorías (p. ej. `MONETIZACION`→`REVENUE_MODEL`, `ESCALABILIDAD`→`SCALABILITY`,
   `MERCADO`→`SECTOR/TARGET_CUSTOMER/PROBLEM`, etc.); (b) señales de
   `CompletenessEvaluation.detectedOpportunities()` convertidas en oportunidades de prioridad alta;
   (c) prioridad 1-10 derivada de `score.viabilityScore`, madurez y señales (fórmula exacta en
   contratos). `ImpactLevel`/`EffortLevel` se reutilizan de `kin.reporting` (no se duplican).

**Alternativas consideradas**:

1. *Motor monolítico con switch de categorías (estilo RecommendationEngine)* — Rechazado: replica la
   observación M15 de la auditoría previa (clases con switch grandes); el patrón coordinador +
   analizadores (RiskEngine) es extensible y deduplicado.
2. *Copiar/parametrizar `RiskAnalyzer`/`RiskAssembler` para oportunidades* — Rechazado: la regla
   de la fase 5.3 prohíbe copiar código de Recommendation/Risk; se reutiliza solo infraestructura
   compartida (`DomainEngine`, `EngineStage`, `DeterministicId`, `ImpactLevel`, `EffortLevel`,
   `EnginePhase.OPPORTUNITY`).
3. *Introducir `ConsultationResult` en esta fase* — Rechazado: es el refactor de consolidación
   planificado (ADR-002); tocaría contratos estables y excede el alcance del motor.
4. *Emitir eventos nuevos (p. ej. OpportunityDetectedEvent)* — Rechazado: el `EventStage` queda
   intacto; el resultado se expone en el contexto, igual que `RiskResult`.

**Consecuencias**:
- Positivas: 8 categorías obligatorias cubiertas con dominio determinista y auditable; el pipeline
  queda con 9 etapas y el `EngineRegistry` auto-descubre el nuevo motor sin cambios; la cobertura de
  dominio de `kin.reporting` se amplía (nuevo subpaquete, umbral ≥90 %); no se rompe REST/SSE/
  eventos/frontend/tests existentes.
- Negativas: `PipelineContext` gana un campo más (mantiene el patrón tipado existente); el alcance
  de cobertura JaCoCo crece; no se añade persistencia ni UI de oportunidades (fases posteriores).

**Regla que modifica**: Ninguna se elimina. Agrega `OpportunityEngine` a la tabla de engines
obligatorios de la governance §6.2 (pasa de "Futuro (KIN 3.0)" a existente) y al BASELINE como
capacidad estable de la Fase 5.3, vía ADR-010.

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend, `KinMethod`/`KinMethodResult`.
`ScoringEngine`, `RecommendationEngine` y `RiskEngine` no cambian. Los 130 tests existentes deben
seguir en verde (verificado en la Fase 5.3 Stage 7).

# ADR-004: RiskEngine — Sistema compuesto de analizadores especializados

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: La Fase 5 requiere que KIN identifique y clasifique los riesgos del proyecto de forma determinista, explicable y sin LLM. La especificación de Fase 5.1 exige explícitamente **no** un motor monolítico, sino un sistema compuesto por analizadores especializados con un contrato común (`RiskAnalyzer`), donde el `RiskEngine` solo coordina y descubre automáticamente los analizadores registrados (sin listas hardcodeadas dentro del motor).

**Decisión**: Implementar el sistema de riesgos en `com.kinplatform.kin.reporting.risk` (dentro del BC Reporting, ADR-001) con:

- **Contrato** `RiskAnalyzer`: `category()`, `analyze(RiskInput) → List<Risk>`, `version()`. Cada analizador evalúa un único tipo de riesgo.
- **Cuatro analizadores** (servicios de dominio puros, deterministas, sin Spring/IA/infra):
  - `BusinessRiskAnalyzer` (BUSINESS): PROBLEM, VALUE_PROPOSITION, OBJECTIVES, SOLUTION.
  - `TechnicalRiskAnalyzer` (TECHNICAL): MVP, SCALABILITY, SOLUTION.
  - `FinancialRiskAnalyzer` (FINANCIAL): REVENUE_MODEL, RESOURCES, score global < 40.
  - `MarketRiskAnalyzer` (MARKET): TARGET_CUSTOMER, COMPETITION, SECTOR.
- **`RiskEngine`**: orquestador stateless. Recibe `List<RiskAnalyzer>` por constructor (auto-descubrimiento vía DI de Spring, mismo patrón que `ProviderRouter`). NO contiene reglas de negocio: solo consolida, ordena (severityScore desc) y calcula métricas agregadas (overallRiskLevel, topRisks, confianza, explicación).
- **Value Objects inmutables**: `Risk`, `RiskExplanation`, `RiskResult`, `RiskLevel`, `RiskCategory`, `RiskModel`, `RiskInput`.
- `Risk` incluye: id determinista, categoría, severidad, probabilidad, impacto, confianza, explicación, reglas aplicadas, dimensión relacionada y `engineVersion`.
- `RiskExplanation` incluye: información utilizada, regla aplicada, motivo y **evidencia**.
- `RiskStage`: etapa de pipeline después de `RecommendationStage`.
- Confianza determinista por riesgo: `0.35 + 0.35*coverage + 0.3*qualityOfInformation`, acotada a [0,1].
- Id de riesgo determinista (UUID v3 de `category|title|description`).
- Entrada nula/incompleta → `RiskResult.empty()` (sin excepciones).

**Alternativas consideradas**:
1. *Motor monolítico con todas las reglas dentro* — Rechazado: la especificación lo prohíbe explícitamente; viola SRP y dificulta extender categorías.
2. *Reglas generadas por LLM* — Rechazado: viola el principio "Java decide, LLM comunica" (ARQUITECTURA_BASE_KIN_2.0) y no es reproducible.
3. *Analizadores sin contrato común (cada uno con su API)* — Rechazado: impide que el engine los coordine uniformemente.
4. *Lista hardcodeada de analizadores dentro del engine* — Rechazado: el engine debe descubrirlos automáticamente (patrón `List<RiskAnalyzer>` inyectado por Spring, como `ProviderRouter` en Fase 4).

**Consecuencias**:
- Positivas: cada categoría de riesgo es extensible añadiendo un nuevo `RiskAnalyzer` (OCP); el engine no se modifica al agregar analizadores; 100% de cobertura de instrucciones en `kin.reporting.risk`; resultados auditables con evidencia.
- Negativas: agrega un nuevo componente de infraestructura a `KinConfig` (beans por analizador); el LLM no participa en la identificación de riesgos (por diseño).

**Regla que modifica**: Ninguna sección de Governance; componente nuevo permitido por la arquitectura base (BC Reporting).

**Cumplimiento**: Requiere `RiskStage` registrado en `KinConfig.chatPipeline` entre `RecommendationStage` y `EventStage`, y campo `riskResult` en `PipelineContext`. No afecta REST, SSE, eventos existentes ni contratos públicos.

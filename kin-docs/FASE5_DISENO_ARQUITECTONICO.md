# FASE 5 — Diseño Arquitectónico

## Motor de Consultoría Empresarial

> Documento de diseño de nivel empresarial. No contiene implementación.
> Aprobado como base para la Fase 5 de KIN 2.0.
> Debe respetar ARQUITECTURA_BASE_KIN_2.0.md y KIN_ARCHITECTURE_GOVERNANCE.md.

---

## ÍNDICE

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Nuevos Bounded Contexts](#2-nuevos-bounded-contexts)
3. [UML Completo del Dominio](#3-uml-completo-del-dominio)
4. [Diagramas de Componentes](#4-diagramas-de-componentes)
5. [Diagramas de Secuencia](#5-diagramas-de-secuencia)
6. [Contratos entre Engines](#6-contratos-entre-engines)
7. [Flujo del Método KIN Actualizado](#7-flujo-del-método-kin-actualizado)
8. [Arquitectura del ReportEngine](#8-arquitectura-del-reportengine)
9. [Arquitectura de Renderers](#9-arquitectura-de-renderers)
10. [Nuevos ADR Necesarios](#10-nuevos-adr-necesarios)
11. [Riesgos Arquitectónicos](#11-riesgos-arquitectónicos)
12. [Estrategia de Pruebas](#12-estrategia-de-pruebas)
13. [Roadmap de Implementación por Bloques](#13-roadmap-de-implementación-por-bloques)
14. [Verificación de Compatibilidad](#14-verificación-de-compatibilidad)

---

## 1. RESUMEN EJECUTIVO

### 1.1 Qué cambia

La Fase 5 introduce cuatro nuevos Engines de dominio que transforman KIN de un sistema conversacional a un Motor de Consultoría Empresarial:

| Engine | Responsabilidad | Datos de entrada | Datos de salida |
|--------|----------------|------------------|-----------------|
| **RecommendationEngine** | Genera recomendaciones accionables basadas en el perfil del proyecto y score | `ProjectContext`, `ScoreResult` | `RecommendationSet` |
| **RiskEngine** | Identifica y clasifica riesgos del proyecto | `ProjectContext` | `RiskAssessment` |
| **OpportunityEngine** | Identifica oportunidades de mejora y crecimiento | `ProjectContext`, `ScoreResult` | `OpportunitySet` |
| **ReportEngine** | Orquesta los otros engines + ScoringEngine y produce `ConsultingReport` | `ProjectContext`, `ScoreResult` | `ConsultingReport` |

### 1.2 Qué NO cambia

- `KinMethod` sigue siendo la fachada principal — su API `execute(KinMethodCommand) → KinMethodResult` NO cambia.
- `Pipeline` NO cambia — los nuevos engines se integran como **nuevos Pipeline Stages** y como servicios invocados desde `ScoringStage` o `ReportStage`.
- `ProjectContext`, `ConversationDecision`, `AnalyzedDimension`, `ScoringEngine`, `ScoreResult` NO cambian su API pública.
- `AIProvider` y `ProviderRouter` NO cambian.
- Todos los endpoints REST existentes se mantienen intactos.

### 1.3 Principio rector de Fase 5

> **Java analiza, evalúa, decide, estructura y puntúa. El LLM solo recibe datos estructurados y los explica como un consultor humano.**

En concreto:

```
┌─────────────────────────────────────────────────────────────┐
│                    JAVA (Dominio)                             │
│  ScoringEngine → score numérico (75/100)                     │
│  RiskEngine → riesgo alto en competencia, riesgo medio en    │
│               financiamiento                                 │
│  RecommendationEngine → recomendar validación temprana con   │
│                          50 clientes potenciales              │
│  OpportunityEngine → oportunidad de expansión a 3 ciudades   │
│  ReportEngine → ConsultingReport estructurado                │
└──────────────────────────┬──────────────────────────────────┘
                           │ datos estructurados
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    LLM (Solo comunicación)                    │
│  Recibe: ConsultingReport + PromptTemplate                   │
│  Produce: "Tu proyecto tiene una viabilidad del 75%..."      │
│  Prohibido: calcular, decidir, evaluar, modificar datos      │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. NUEVOS BOUNDED CONTEXTS

### 2.1 Propuesta de Contextos

La Fase 5 introduce un nuevo Bounded Context y expande el contexto existente de Scoring:

```
ANTES (Fase 4):
┌────────────────────────────────────────────────────────────┐
│  Consulting  │  Conversation  │  AI  │  Scoring  │  Auth   │
└────────────────────────────────────────────────────────────┘

DESPUÉS (Fase 5):
┌────────────────────────────────────────────────────────────┐
│  Consulting  │  Conversation  │  AI  │  Scoring  │  Auth   │
│              │                │      │           │         │
│              │                │      │  Reporting│         │
│              │                │      │  (NUEVO) │         │
└────────────────────────────────────────────────────────────┘
```

**Scoring** se expande para incluir Reporting porque:

- ReportEngine orquesta ScoringEngine + RiskEngine + RecommendationEngine + OpportunityEngine.
- Todos los engines comparten tipos base, principios de diseño, y ubicación en la arquitectura.
- Juntos forman el "Motor de Evaluación". Separarlos prematuramente crearía dependencias circulares.

**Decisión**: Los cuatro engines (Scoring, Risk, Recommendation, Opportunity) y ReportEngine viven en un **nuevo paquete `kin/reporting/`** dentro del dominio.

### 2.2 Mapa de Paquetes Actualizado

```
com.kinplatform.kin/
├── context/          (existente — Conversation BC)
├── decision/         (existente — Consulting BC)
├── pipeline/         (existente — Consulting BC)
├── pipeline/stage/   (existente + NUEVOS STAGES)
├── scoring/          (existente — Scoring BC)
│   ├── ScoringEngine.java
│   ├── ScoringModel.java
│   └── ScoreResult.java
└── reporting/        (NUEVO — Reporting BC)
    ├── ReportEngine.java
    ├── ConsultingReport.java
    ├── RecommendationEngine.java
    ├── RecommendationSet.java
    ├── RiskEngine.java
    ├── RiskAssessment.java
    ├── OpportunityEngine.java
    ├── OpportunitySet.java
    ├── report/
    │   ├── ExecutiveSummary.java
    │   ├── ScoresSection.java
    │   ├── RecommendationsSection.java
    │   ├── RisksSection.java
    │   ├── OpportunitiesSection.java
    │   ├── InnovationSection.java
    │   ├── CompetitionSection.java
    │   ├── FinancialSection.java
    │   ├── MarketSection.java
    │   └── NextStepsSection.java
    └── renderer/
        ├── ReportRenderer.java         (interface — port)
        ├── MarkdownRenderer.java       (infrastructure)
        ├── HtmlRenderer.java           (infrastructure)
        ├── PdfRenderer.java            (infrastructure)
        └── DocxRenderer.java           (infrastructure)
```

### 2.3 Límites del Contexto

| El contexto Reporting **contiene** | El contexto Reporting **NO contiene** |
|------------------------------------|--------------------------------------|
| `ReportEngine` (orquestador) | Lógica de IA (prompts, LLM) |
| `ConsultingReport` (modelo de dominio) | Lógica de persistencia |
| `RecommendationEngine` | Controladores REST |
| `RiskEngine` | DTOs de API |
| `OpportunityEngine` | Lógica de autenticación/roles |
| `ReportRenderer` (port) | Lógica de conversación |
| Renderers (infraestructura) | `ProjectContext`, `Message` |

---

## 3. UML COMPLETO DEL DOMINIO

### 3.1 Modelo de Dominio de Reporting

```
┌────────────────────────────────────────────────────────────────────┐
│                    REPORTING DOMAIN                                  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 ConsultingReport (VO — Aggregate Root)        │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ - projectId: UUID                                             │   │
│  │ - generatedAt: OffsetDateTime                                 │   │
│  │ - reportVersion: String                                       │   │
│  │ - executiveSummary: ExecutiveSummary                          │   │
│  │ - scores: ScoresSection                                       │   │
│  │ - recommendations: RecommendationsSection                     │   │
│  │ - risks: RisksSection                                         │   │
│  │ - opportunities: OpportunitiesSection                         │   │
│  │ - innovation: InnovationSection                               │   │
│  │ - competition: CompetitionSection                             │   │
│  │ - financial: FinancialSection                                 │   │
│  │ - market: MarketSection                                       │   │
│  │ - nextSteps: NextStepsSection                                 │   │
│  │ - metadata: ReportMetadata                                    │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ + withExecutiveSummary(ExecutiveSummary): ConsultingReport    │   │
│  │ + withScores(ScoresSection): ConsultingReport                 │   │
│  │ + withRecommendations(...): ConsultingReport                  │   │
│  │ + withRisks(RisksSection): ConsultingReport                   │   │
│  │ + withOpportunities(...): ConsultingReport                    │   │
│  │ + toBuilder(): ConsultingReportBuilder                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Las secciones se construyen con un Builder inmutable:               │
│                                                                      │
│  ConsultingReport.builder()                                          │
│      .projectId(id)                                                  │
│      .executiveSummary(summary)                                      │
│      .scores(scores)                                                 │
│      .recommendations(recommendations)                               │
│      .risks(risks)                                                   │
│      .opportunities(opportunities)                                   │
│      .innovation(innovation)                                         │
│      .build()                                                        │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  ExecutiveSummary (VO — record)            │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  projectName: String                       │                     │
│  │  projectCategory: String                   │                     │
│  │  overallScore: int                         │                     │
│  │  viabilityLabel: String                    │                     │
│  │  summaryText: String                       │                     │
│  │  keyHighlights: List<String>               │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  ScoresSection (VO — record)               │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  totalScore: int                           │                     │
│  │  maxScore: int                             │                     │
│  │  categoryScores: Map<String, Integer>      │                     │
│  │  viabilityLabel: String                    │                     │
│  │  confidenceLevel: String                   │                     │
│  │  strengths: List<String>                   │                     │
│  │  weaknesses: List<String>                  │                     │
│  │  scoringModelVersion: String               │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  RecommendationsSection (VO — record)      │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  recommendations: List<Recommendation>     │                     │
│  │  priorityOrder: List<UUID>                 │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  Recommendation (VO — record)              │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  id: UUID                                  │                     │
│  │  category: RecommendationCategory          │                     │
│  │  title: String                             │                     │
│  │  description: String                       │                     │
│  │  priority: int (1-10)                      │                     │
│  │  impactLevel: ImpactLevel                  │                     │
│  │  effortLevel: EffortLevel                  │                     │
│  │  relatedDimension: AnalyzedDimension       │                     │
│  │  actionableSteps: List<String>             │                     │
│  │  expectedOutcome: String                   │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  RecommendationCategory (enum)             │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  VALIDATION, MARKETING, FINANCIAL,         │                     │
│  │  PRODUCT, STRATEGY, OPERATIONS,            │                     │
│  │  INNOVATION, TEAM                          │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  ImpactLevel / EffortLevel (enums)         │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  LOW, MEDIUM, HIGH, CRITICAL               │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  RiskAssessment (VO — record)              │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  risks: List<Risk>                         │                     │
│  │  overallRiskLevel: RiskLevel               │                     │
│  │  topRisks: List<Risk>                      │                     │
│  │  mitigationStrategies: List<String>        │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  Risk (VO — record)                        │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  id: UUID                                  │                     │
│  │  category: RiskCategory                    │                     │
│  │  title: String                             │                     │
│  │  description: String                       │                     │
│  │  probability: RiskLevel                    │                     │
│  │  impact: RiskLevel                         │                     │
│  │  combinedScore: int                        │                     │
│  │  mitigation: String                        │                     │
│  │  relatedDimension: AnalyzedDimension       │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  RiskCategory (enum)                       │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  MARKET, FINANCIAL, TECHNICAL,             │                     │
│  │  OPERATIONAL, COMPETITIVE, LEGAL,          │                     │
│  │  TEAM, EXECUTION                           │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  RiskLevel (enum)                          │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  LOW, MEDIUM, HIGH, CRITICAL               │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  OpportunitySet (VO — record)              │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  opportunities: List<Opportunity>           │                     │
│  │  quickWins: List<Opportunity>              │                     │
│  │  strategicOpportunities: List<Opportunity> │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  Opportunity (VO — record)                 │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  id: UUID                                  │                     │
│  │  category: OpportunityCategory             │                     │
│  │  title: String                             │                     │
│  │  description: String                       │                     │
│  │  potentialImpact: ImpactLevel              │                     │
│  │  effortRequired: EffortLevel               │                     │
│  │  timeToImplement: String                   │                     │
│  │  relatedDimension: AnalyzedDimension       │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  OpportunityCategory (enum)                │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  EXPANSION, DIFFERENTIATION,               │                     │
│  │  REVENUE, COST_OPTIMIZATION,               │                     │
│  │  PARTNERSHIP, TECHNOLOGY,                   │                     │
│  │  CUSTOMER_EXPERIENCE, FUNDING              │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ── Secciones adicionales del reporte: ──                           │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  InnovationSection (VO — record)           │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  innovationScore: int (0-100)             │                     │
│  │  innovationLevel: InnovationLevel          │                     │
│  │  innovationDrivers: List<String>           │                     │
│  │  differentiationFactors: List<String>      │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  CompetitionSection (VO — record)          │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  competitionLevel: CompetitionLevel        │                     │
│  │  identifiedCompetitors: List<String>       │                     │
│  │  competitiveAdvantages: List<String>       │                     │
│  │  marketPosition: String                    │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  FinancialSection (VO — record)            │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  estimatedRevenueModel: String             │                     │
│  │  estimatedInitialInvestment: String        │                     │
│  │  breakEvenEstimate: String                 │                     │
│  │  profitabilityPotential: String            │                     │
│  │  fundingRecommendation: String             │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  MarketSection (VO — record)               │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  marketSize: String                        │                     │
│  │  marketTrend: String                       │                     │
│  │  targetAudience: String                    │                     │
│  │  marketReadiness: String                   │                     │
│  │  growthPotential: String                   │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  NextStepsSection (VO — record)            │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  immediateActions: List<String>            │                     │
│  │  shortTermGoals: List<String>              │                     │
│  │  mediumTermGoals: List<String>             │                     │
│  │  validationPlan: List<String>              │                     │
│  │  estimatedTimeline: String                 │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  InnovationLevel / CompetitionLevel (enum) │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  LOW, MODERATE, HIGH, DISRUPTIVE           │                     │
│  │  (COMPETITION: NONE, LOW, MODERATE, HIGH,  │                     │
│  │   SATURATED)                               │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  ┌────────────────────────────────────────────┐                     │
│  │  ReportMetadata (VO — record)              │                     │
│  ├────────────────────────────────────────────┤                     │
│  │  reportVersion: String                     │                     │
│  │  generatedAt: OffsetDateTime               │                     │
│  │  engineVersions: Map<String, String>        │                     │
│  │  coveragePercent: double                   │                     │
│  │  confidenceLevel: double                   │                     │
│  └────────────────────────────────────────────┘                     │
└────────────────────────────────────────────────────────────────────┘
```

### 3.2 Engines del Dominio

```
┌────────────────────────────────────────────────────────────────────┐
│                      DOMAIN ENGINES                                  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  ScoringEngine (existente, REFACTORIZADO)                │      │
│  │  + evaluate(ctx: ProjectContext, eval: CE): ScoreResult  │      │
│  │  ★ Cambio: reemplazar heurística length() por            │      │
│  │    evaluación semántica multidimensional                  │      │
│  │  ★ Cambio: ScoreResult agrega explanation audit trail    │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  RecommendationEngine (NUEVO)                            │      │
│  ├──────────────────────────────────────────────────────────┤      │
│  │  + evaluate(ctx: ProjectContext,                         │      │
│  │             score: ScoreResult): RecommendationSet       │      │
│  │                                                          │      │
│  │  Reglas (Java, 100%):                                    │      │
│  │  - Si revenue model no cubierto → recomendar definir     │      │
│  │  - Si score < 40 → recomendar validación temprana        │      │
│  │  - Si competencia no analizada → recomendar estudio      │      │
│  │  - Si cliente objetivo no definido → recomendar perfil   │      │
│  │  - Priorización por impacto/esfuerzo                     │      │
│  │  - NO usa LLM para generar recomendaciones               │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  RiskEngine (NUEVO)                                      │      │
│  ├──────────────────────────────────────────────────────────┤      │
│  │  + evaluate(ctx: ProjectContext): RiskAssessment         │      │
│  │                                                          │      │
│  │  Reglas (Java, 100%):                                    │      │
│  │  - Sin competencia → riesgo COMPETITIVO alto            │      │
│  │  - Sin modelo de ingresos → riesgo FINANCIERO medio     │      │
│  │  - Sin MVP → riesgo EJECUCIÓN medio                     │      │
│  │  - Sin escalabilidad → riesgo CRECIMIENTO medio         │      │
│  │  - Sin recursos definidos → riesgo OPERACIONAL bajo     │      │
│  │  - Combinación de dimensiones faltantes → score         │      │
│  │  - NO usa LLM para identificar riesgos                  │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  OpportunityEngine (NUEVO)                               │      │
│  ├──────────────────────────────────────────────────────────┤      │
│  │  + evaluate(ctx: ProjectContext,                         │      │
│  │             score: ScoreResult): OpportunitySet          │      │
│  │                                                          │      │
│  │  Reglas (Java, 100%):                                    │      │
│  │  - Con solución definida + alta cobertura → oportunidad  │      │
│  │    de expansión                                          │      │
│  │  - Con propuesta de valor + sin competencia →            │      │
│  │    diferenciación                                        │      │
│  │  - Con cliente objetivo + alta cobertura → oportunidad   │      │
│  │    de revenue                                            │      │
│  │  - Sin escalabilidad → oportunidad de tecnología         │      │
│  │  - Quick wins: baja inversión, alto impacto              │      │
│  │  - NO usa LLM para identificar oportunidades             │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  ReportEngine (NUEVO — orquestador)                      │      │
│  ├──────────────────────────────────────────────────────────┤      │
│  │  + generate(ctx: ProjectContext,                         │      │
│  │             score: ScoreResult): ConsultingReport        │      │
│  │                                                          │      │
│  │  Internamente:                                           │      │
│  │  1. Invoca RecommendationEngine                          │      │
│  │  2. Invoca RiskEngine                                    │      │
│  │  3. Invoca OpportunityEngine                             │      │
│  │  4. Construye cada sección del reporte                   │      │
│  │  5. Ensambla ConsultingReport con Builder                │      │
│  │  6. NO renderiza — solo produce el modelo                │      │
│  │                                                          │      │
│  │  Dependencias:                                           │      │
│  │  - RecommendationEngine                                  │      │
│  │  - RiskEngine                                            │      │
│  │  - OpportunityEngine                                     │      │
│  │  - ScoringEngine (o recibe ScoreResult ya calculado)     │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  IMPORTANTE: Todos los engines son STATELESS.                       │
│  Todos producen records inmutables.                                  │
│  Ninguno depende de infraestructura.                                  │
│  Ninguno depende de otros engines para su funcionamiento individual. │
│  ReportEngine los orquesta pero no es un engine — es un             │
│  Application Service o Domain Service que coordina.                 │
└────────────────────────────────────────────────────────────────────┘
```

### 3.3 Pipeline Stages Actualizados

```
┌────────────────────────────────────────────────────────────────────┐
│              PIPELINE — NUEVOS STAGES (Fase 5)                      │
│                                                                      │
│  Etapas existentes (no cambian):                                     │
│                                                                      │
│  AnalyzerStage  → EvaluatorStage → StrategistStage                  │
│  → ConsultorStage → ScoringStage → EventStage                       │
│                                                                      │
│  Nuevas etapas (se agregan DESPUÉS de ScoringStage):                │
│                                                                      │
│  ScoringStage (existe) → RecommendationStage → RiskStage            │
│  → OpportunityStage → ReportStage → LlmExplanationStage              │
│  → EventStage (modificado)                                           │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  RecommendationStage (NUEVO)                             │      │
│  │  Invoca: RecommendationEngine.evaluate()                  │      │
│  │  Escribe en ctx: recommendationSet: RecommendationSet     │      │
│  │  supports(): true si coverage > 0.3                       │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  RiskStage (NUEVO)                                       │      │
│  │  Invoca: RiskEngine.evaluate()                            │      │
│  │  Escribe en ctx: riskAssessment: RiskAssessment           │      │
│  │  supports(): siempre true                                 │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  OpportunityStage (NUEVO)                                │      │
│  │  Invoca: OpportunityEngine.evaluate()                     │      │
│  │  Escribe en ctx: opportunitySet: OpportunitySet           │      │
│  │  supports(): true si score > 20                           │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  ReportStage (NUEVO)                                     │      │
│  │  Invoca: ReportEngine.generate()                          │      │
│  │  Escribe en ctx: consultingReport: ConsultingReport       │      │
│  │  supports(): true si todas las etapas anteriores se       │      │
│  │              ejecutaron                                    │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  LlmExplanationStage (NUEVO — reemplaza lógica actual    │      │
│  │  en AiEngineService/ConsultorStage)                       │      │
│  │  Recibe: ConsultingReport (estructurado)                  │      │
│  │  Envia al LLM: report data + prompt template             │      │
│  │  Recibe del LLM: explicación en lenguaje natural         │      │
│  │  Escribe en ctx: aiResponse: String (explicación LLM)    │      │
│  │  supports(): true si consultingReport != null             │      │
│  │                                                          │      │
│  │  CRÍTICO: el LLM NO recibe datos crudos — solo recibe   │      │
│  │  el ConsultingReport ya calculado por Java. El prompt    │      │
│  │  le indica: "Explica este reporte como consultor."       │      │
│  └──────────────────────────────────────────────────────────┘      │
└────────────────────────────────────────────────────────────────────┘
```

---

## 4. DIAGRAMAS DE COMPONENTES

### 4.1 Visión General

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          KIN — FASE 5 COMPONENTES                             │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                           DOMAIN (kin/)                                │   │
│  │                                                                         │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │   │
│  │  │  context/     │ │  decision/   │ │  pipeline/   │ │  scoring/    │  │   │
│  │  │  (existente)  │ │  (existente) │ │  (existente) │ │  (existente) │  │   │
│  │  └──────────────┘ └──────────────┘ └──────┬───────┘ └──────┬───────┘  │   │
│  │                                            │                │           │   │
│  │                                            ▼                ▼           │   │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │   │
│  │  │                     reporting/ (NUEVO)                            │  │   │
│  │  │  ┌────────────────────────────────────────────────────────────┐ │  │   │
│  │  │  │  ConsultingReport (VO) + 9 Section VOs                     │ │  │   │
│  │  │  └────────────────────────────────────────────────────────────┘ │  │   │
│  │  │  ┌──────────────┐ ┌──────────┐ ┌────────────────┐            │  │   │
│  │  │  │Recommendation│ │RiskEngine│ │Opportunity     │            │  │   │
│  │  │  │Engine        │ │          │ │Engine          │            │  │   │
│  │  │  └──────────────┘ └──────────┘ └────────────────┘            │  │   │
│  │  │  ┌──────────────┐ ┌──────────────────┐                       │  │   │
│  │  │  │ ReportEngine │ │ ReportRenderer   │                       │  │   │
│  │  │  │ (orquestador)│ │ (interface — port)│                      │  │   │
│  │  │  └──────────────┘ └──────────────────┘                       │  │   │
│  │  └──────────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                        PIPELINE STAGES                                 │   │
│  │                                                                         │   │
│  │  Analyzer → Evaluator → Strategist → Consultor → Scoring                │   │
│  │  → RecommendationStage → RiskStage → OpportunityStage                   │   │
│  │  → ReportStage → LlmExplanationStage → EventStage                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                    APPLICATION                                         │   │
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────┐  │   │
│  │  │ AiEngineService   │ │ ChatOrchestrator │ │ PromptAssembler     │  │   │
│  │  │ (refactorizado)   │ │ (refactorizado)  │ │ (nuevo — extraído   │  │   │
│  │  └──────────────────┘ └──────────────────┘ │  de AiEngineService) │  │   │
│  │                                              └──────────────────────┘  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                    INFRASTRUCTURE                                       │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐        │   │
│  │  │Markdown    │ │HtmlRenderer│ │PdfRenderer  │ │DocxRenderer│        │   │
│  │  │Renderer    │ │            │ │             │ │           │        │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘        │   │
│  │  ┌────────────┐ ┌────────────────┐                                    │   │
│  │  │JsonRenderer│ │DeepSeekProvider│ (existente)                         │   │
│  │  └────────────┘ └────────────────┘                                    │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Diagrama de Dependencias entre Componentes

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ENGINE DEPENDENCY GRAPH                            │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  ProjectContext + ScoreResult + CompletenessEvaluation        │   │
│  │  (datos de entrada compartidos)                               │   │
│  └──────┬────────────┬──────────────┬──────────────┬───────────┘   │
│         │            │              │              │                │
│         ▼            ▼              ▼              ▼                │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌────────────────┐    │
│  │Scoring   │ │Recommen- │ │  RiskEngine  │ │ Opportunity    │    │
│  │Engine    │ │dation    │ │              │ │ Engine         │    │
│  │(existe)  │ │Engine    │ │              │ │                │    │
│  └────┬─────┘ └────┬─────┘ └──────┬───────┘ └───────┬────────┘    │
│       │            │              │                  │             │
│       └────────────┼──────────────┼──────────────────┘             │
│                    ▼              ▼                                 │
│         ┌─────────────────────────────────────┐                    │
│         │           ReportEngine               │                    │
│         │  (orquesta los 4 engines, produce   │                    │
│         │   ConsultingReport)                  │                    │
│         └────────────────┬────────────────────┘                    │
│                          │                                          │
│                          ▼                                          │
│         ┌─────────────────────────────────────┐                    │
│         │         ConsultingReport             │                    │
│         │  (Modelo de dominio — sin renderizar)│                   │
│         └────────────────┬────────────────────┘                    │
│                          │                                          │
│          ┌───────────────┼───────────────┐                        │
│          ▼               ▼               ▼                          │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                     │
│  │Markdown    │ │  Html      │ │    Pdf     │  ...renderers        │
│  │Renderer    │ │ Renderer   │ │  Renderer  │                     │
│  └────────────┘ └────────────┘ └────────────┘                     │
│                                                                       │
│  IMPORTANTE:                                                          │
│  - RecommendationEngine NO depende de RiskEngine                      │
│  - RiskEngine NO depende de OpportunityEngine                        │
│  - OpportunityEngine NO depende de RecommendationEngine               │
│  - ReportEngine depende de los 3 (solo para orquestar)                │
│  - Los renderers NO dependen de ningún engine                        │
│  - Los renderers solo transforman ConsultingReport                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. DIAGRAMAS DE SECUENCIA

### 5.1 Pipeline Completo con Fase 5

```
Usuario              ChatOrchestrator     KinMethod      Pipeline              Engines              LLM
   │                       │                  │             │                     │                  │
   │── ChatRequest ───────▶│                  │             │                     │                  │
   │                       │                  │             │                     │                  │
   │                       │── KinMethodCmd ──▶             │                     │                  │
   │                       │                  │             │                     │                  │
   │                       │                  │── execute ─▶│                     │                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── AnalyzerStage ──▶│                  │
   │                       │                  │             │◀── ProjectContext ─│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── EvaluatorStage ─▶│                  │
   │                       │                  │             │◀── Evaluation ─────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── StrategistStage ─▶│                  │
   │                       │                  │             │◀── Decision ───────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── ConsultorStage ─▶│                  │
   │                       │                  │             │◀── aiResponse ─────│──────────────────│
   │                       │                  │             │                     │                  │
   │                       │                  │             │── ScoringStage ───▶│ ScoringEngine     │
   │                       │                  │             │◀── ScoreResult ────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── RecommStage ────▶│ RecommEngine      │
   │                       │                  │             │◀── RecommSet ──────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── RiskStage ──────▶│ RiskEngine        │
   │                       │                  │             │◀── RiskAssess ─────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── OppStage ───────▶│ OppEngine         │
   │                       │                  │             │◀── OppSet ─────────│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── ReportStage ────▶│ ReportEngine      │
   │                       │                  │             │◀── ConsultingRpt ──│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │             │── LlmExplainStage ─▶│                  │
   │                       │                  │             │◀── explanation ────│──────────────────│
   │                       │                  │             │                     │                  │
   │                       │                  │             │── EventStage ─────▶│                  │
   │                       │                  │             │                     │                  │
   │                       │                  │◀── result ──│                     │                  │
   │                       │                  │             │                     │                  │
   │                       │◀── KinMethodResult             │                     │                  │
   │                       │                  │             │                     │                  │
   │◀── ChatResponse ─────│                   │             │                     │                  │
```

### 5.2 ReportEngine Interno

```
ReportEngine
    │
    ├── 1. RecommendationEngine.evaluate(ctx, score) ──▶ RecommendationSet
    │
    ├── 2. RiskEngine.evaluate(ctx) ────────────────────▶ RiskAssessment
    │
    ├── 3. OpportunityEngine.evaluate(ctx, score) ──────▶ OpportunitySet
    │
    ├── 4. Construye ExecutiveSummary
    │      ← projectName, category, score, keyHighlights
    │
    ├── 5. Construye ScoresSection
    │      ← ScoreResult (totalScore, categoryScores, strengths, weaknesses)
    │
    ├── 6. Construye RecommendationsSection
    │      ← RecommendationSet.recommendations()
    │
    ├── 7. Construye RisksSection
    │      ← RiskAssessment.risks()
    │
    ├── 8. Construye OpportunitiesSection
    │      ← OpportunitySet.opportunities()
    │
    ├── 9. Construye InnovationSection
    │      ← basado en dimensiones cubiertas + score
    │
    ├── 10. Construye CompetitionSection
    │       ← basado en AnalyzedDimension.COMPETITION
    │
    ├── 11. Construye FinancialSection
    │       ← basado en AnalyzedDimension.REVENUE_MODEL + RESOURCES
    │
    ├── 12. Construye MarketSection
    │       ← basado en TARGET_CUSTOMER + SECTOR + CITY
    │
    ├── 13. Construye NextStepsSection
    │       ← prioriza recomendaciones + riesgos
    │
    └── 14. Ensambla ConsultingReport con Builder
           ← ConsultingReport.builder()
                .projectId(projectId)
                .executiveSummary(executiveSummary)
                .scores(scoresSection)
                .recommendations(recommendationsSection)
                .risks(risksSection)
                .opportunities(opportunitiesSection)
                .innovation(innovationSection)
                .competition(competitionSection)
                .financial(financialSection)
                .market(marketSection)
                .nextSteps(nextStepsSection)
                .metadata(reportMetadata)
                .build()
```

### 5.3 LlmExplanationStage Detalle

```
LlmExplanationStage
    │
    ├── 1. Toma ConsultingReport del PipelineContext
    │
    ├── 2. Invoca PromptAssembler.assemble(consultingReport)
    │      ← Produce: systemPrompt + userPrompt
    │      ← System prompt: "Eres un consultor senior. Explica este
    │         reporte de viabilidad como si hablaras con el emprendedor.
    │         NO agregues información nueva. NO evalúes. Solo explica
    │         los datos proporcionados."
    │      ← User prompt: serialización estructurada del reporte
    │
    ├── 3. Invoca ProviderRouter.routeBlocking(history, prompt, systemPrompt)
    │      ← El LLM recibe datos ESTRUCTURADOS, no crudos del dominio
    │
    ├── 4. Recibe explicación en lenguaje natural del LLM
    │
    └── 5. Escribe en ctx.aiResponse(explanation)
```

---

## 6. CONTRATOS ENTRE ENGINES

### 6.1 Contratos de Entrada/Salida

```
┌────────────────────────────────────────────────────────────────────┐
│                    ENGINE CONTRACTS FORMALES                         │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ScoringEngine (existente — refactorizado)                │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  (ProjectContext, CompletenessEvaluation)         │       │
│  │  Output: ScoreResult                                      │       │
│  │  Package: com.kinplatform.kin.scoring                      │       │
│  │  Mutabilidad: stateless                                   │       │
│  │  Excepciones: no lanza (devuelve ScoreResult.empty())     │       │
│  │  Refactor: explanation audit trail agregado a ScoreResult │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  RecommendationEngine                                     │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  (ProjectContext, ScoreResult)                    │       │
│  │  Output: RecommendationSet                                │       │
│  │  Package: com.kinplatform.kin.reporting                    │       │
│  │  Mutabilidad: stateless                                   │       │
│  │  Excepciones: no lanza (devuelve RecommendationSet.empty())│      │
│  │  Reglas: 100% Java, sin LLM                               │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  RiskEngine                                                │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  (ProjectContext)                                 │       │
│  │  Output: RiskAssessment                                   │       │
│  │  Package: com.kinplatform.kin.reporting                    │       │
│  │  Mutabilidad: stateless                                   │       │
│  │  Excepciones: no lanza (devuelve RiskAssessment.empty())   │      │
│  │  Reglas: 100% Java, sin LLM                               │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  OpportunityEngine                                         │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  (ProjectContext, ScoreResult)                    │       │
│  │  Output: OpportunitySet                                   │       │
│  │  Package: com.kinplatform.kin.reporting                    │       │
│  │  Mutabilidad: stateless                                   │       │
│  │  Excepciones: no lanza (devuelve OpportunitySet.empty())   │      │
│  │  Reglas: 100% Java, sin LLM                               │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ReportEngine (orquestador)                               │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  (ProjectContext, ScoreResult)                    │       │
│  │  Output: ConsultingReport                                 │       │
│  │  Package: com.kinplatform.kin.reporting                    │       │
│  │  Mutabilidad: stateless                                   │       │
│  │  Excepciones: no lanza (devuelve ConsultingReport.empty())│       │
│  │  Dependencias: RecommendationEngine, RiskEngine,          │       │
│  │               OpportunityEngine                            │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ReportRenderer (interface — port)                        │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Input:  ConsultingReport                                 │       │
│  │  Output: String (contenido renderizado)                   │       │
│  │  Package: com.kinplatform.kin.reporting.renderer           │       │
│  │  Nota: NO recibe el LLM — renderiza el modelo puro       │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  Renderer Naming Convention:                              │       │
│  │  - MarkdownRenderer implements ReportRenderer              │       │
│  │  - HtmlRenderer implements ReportRenderer                  │       │
│  │  - PdfRenderer implements ReportRenderer                   │       │
│  │  - DocxRenderer implements ReportRenderer                  │       │
│  │  - JsonRenderer implements ReportRenderer                  │       │
│  │  Todos en: com.kinplatform.kin.reporting.renderer          │       │
│  │  (o infraestructura si usan librerías externas)            │       │
│  └──────────────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────────┘
```

### 6.2 Formato de Contratos (Interfaces Java)

```
// ─── RecommendationEngine ───
public class RecommendationEngine {
    public RecommendationEngine(RecommendationModel model) { ... }
    public RecommendationSet evaluate(ProjectContext ctx, ScoreResult score);
    public static RecommendationSet empty();
}

// ─── RiskEngine ───
public class RiskEngine {
    public RiskEngine(RiskModel model) { ... }
    public RiskAssessment evaluate(ProjectContext ctx);
    public static RiskAssessment empty();
}

// ─── OpportunityEngine ───
public class OpportunityEngine {
    public OpportunityEngine(OpportunityModel model) { ... }
    public OpportunitySet evaluate(ProjectContext ctx, ScoreResult score);
    public static OpportunitySet empty();
}

// ─── ReportEngine ───
public class ReportEngine {
    public ReportEngine(
        RecommendationEngine recEngine,
        RiskEngine riskEngine,
        OpportunityEngine oppEngine
    ) { ... }
    public ConsultingReport generate(ProjectContext ctx, ScoreResult score);
    public static ConsultingReport empty();
}

// ─── ReportRenderer (port) ───
public interface ReportRenderer {
    String render(ConsultingReport report);
    String formatName(); // "markdown", "html", "pdf", "docx", "json"
}
```

---

## 7. FLUJO DEL MÉTODO KIN ACTUALIZADO

### 7.1 Pipeline Stages — Orden Final (Fase 5)

```
Pipeline (orden de stages en KinConfig.java):

 1. AnalyzerStage      (existente)
 2. EvaluatorStage     (existente)
 3. StrategistStage    (existente)
 4. ConsultorStage     (existente) — LLM responde pregunta actual
 5. ScoringStage       (existente, refactorizado)
 6. RecommendationStage (NUEVO)
 7. RiskStage          (NUEVO)
 8. OpportunityStage   (NUEVO)
 9. ReportStage        (NUEVO)
10. LlmExplanationStage (NUEVO) — LLM explica el reporte
11. EventStage         (existente, modificado)
```

### 7.2 PipelineContext — Campos Actualizados

```
PipelineContext (Fase 5):

// Campos existentes (sin cambios):
- projectId: UUID
- userId: UUID
- userMessage: String
- history: List<Message>
- projectTitle, projectDescription, projectCategory

// Campos existentes (sin cambios):
- projectContext: ProjectContext
- evaluation: CompletenessEvaluation
- decision: ConversationDecision
- aiResponse: String
- scoreResult: ScoreResult
- events: List<DomainEvent>
- attributes: Map<String,Object>

// NUEVOS CAMPOS Fase 5:
- recommendationSet: RecommendationSet     ← RecommendationStage
- riskAssessment: RiskAssessment           ← RiskStage
- opportunitySet: OpportunitySet           ← OpportunityStage
- consultingReport: ConsultingReport       ← ReportStage

NOTA: Se agregan 4 campos nuevos. Si en Fase 6 se requieren más de 3
adicionales, activar la regla de God Class (sección 7.1 de ARQUITECTURA_BASE).
```

### 7.3 KinMethodResult — Actualizado

```
KinMethodResult (Fase 5):

- projectContext: ProjectContext         (existente)
- evaluation: CompletenessEvaluation     (existente)
- decision: ConversationDecision          (existente)
- aiResponse: String                     (existente — ahora es la explicación LLM)
- scoreResult: ScoreResult               (existente)
- events: List<DomainEvent>              (existente)
- consultingReport: ConsultingReport     (NUEVO — salida completa del reporte)
```

### 7.4 ChatOrchestratorServiceImpl — Refactorizado

```
processMessage() (bloqueante):
  ANTES: KinMethod → KinMethodResult → guarda aiResponse
  DESPUÉS: KinMethod → KinMethodResult → guarda aiResponse (explicación LLM)
                                         → consultingReport disponible para renderizar
                                         → llama renderer si formato específico

processMessageStream() (streaming):
  ANTES: flujo inline sin KinMethod (divergencia arquitectónica)
  DESPUÉS: KinMethod → obtiene consultingReport → PromptAssembler
           → ProviderRouter.routeStream() → SSE tokens
           → al completar: guarda aiResponse + consultingReport (async)

  Esto ELIMINA la divergencia entre flujo bloqueante y streaming.
```

---

## 8. ARQUITECTURA DEL REPORTENGINE

### 8.1 Principios

1. **El ReportEngine no renderiza.** Produce un modelo de dominio (`ConsultingReport`) que representa un reporte de consultoría completo. Los renderers transforman ese modelo a formatos específicos.
2. **El ReportEngine no usa el LLM.** Orquesta engines Java. La explicación en lenguaje natural la hace `LlmExplanationStage` después.
3. **El ReportEngine produce un modelo inmutable.** Usa Builder pattern para construir el `ConsultingReport`.
4. **Cada sección del reporte es un Value Object inmutable.** Pueden existir secciones opcionales (ej: `FinancialSection` puede ser `empty()` si no hay datos financieros).

### 8.2 Estructura Interna

```
ReportEngine
├── RecommendationEngine (dependencia inyectada)
├── RiskEngine (dependencia inyectada)
└── OpportunityEngine (dependencia inyectada)

Método principal:
  generate(ProjectContext ctx, ScoreResult score) → ConsultingReport

Implementación:
  1. Executes RecommendationEngine.evaluate(ctx, score)
  2. Executes RiskEngine.evaluate(ctx)
  3. Executes OpportunityEngine.evaluate(ctx, score)
  4. Builds ConsultingReport from results
```

### 8.3 Builder de ConsultingReport

```java
ConsultingReport report = ConsultingReport.builder()
    .projectId(projectId)
    .generatedAt(OffsetDateTime.now())
    .reportVersion("2.0")
    .executiveSummary(ExecutiveSummary.of(
        projectName,
        projectCategory,
        score.totalScore(),
        score.viabilityLabel(),
        buildSummaryText(score),
        extractKeyHighlights(score, recommendations, opportunities)
    ))
    .scores(ScoresSection.from(score))
    .recommendations(RecommendationsSection.from(recommendationSet))
    .risks(RisksSection.from(riskAssessment))
    .opportunities(OpportunitiesSection.from(opportunitySet))
    .innovation(InnovationSection.from(ctx, score))
    .competition(CompetitionSection.from(ctx))
    .financial(FinancialSection.from(ctx))
    .market(MarketSection.from(ctx))
    .nextSteps(NextStepsSection.from(recommendationSet, riskAssessment))
    .metadata(ReportMetadata.of(
        "2.0",
        OffsetDateTime.now(),
        Map.of(
            "scoring", "v1",
            "recommendation", "v1",
            "risk", "v1",
            "opportunity", "v1"
        ),
        ctx.coverageRatio(),
        evaluation.confidenceScore()
    ))
    .build();
```

Cada `Section.from()` es un factory method estático que recibe los datos necesarios y produce la sección inmutable.

---

## 9. ARQUITECTURA DE RENDERERS

### 9.1 Principios

1. **Los renderers son intercambiables.** Todos implementan `ReportRenderer`.
2. **Los renderers solo transforman, no calculan.** No pueden invocar engines, no pueden decidir qué incluir.
3. **Los renderers son infrastructure.** Pueden usar librerías externas (Flying Saucer para PDF, Apache POI para DOCX).
4. **Renderers responsables de su formato.** MarkdownRenderer produce Markdown. HtmlRenderer produce HTML. etc.

### 9.2 Interfaz

```java
public interface ReportRenderer {
    String render(ConsultingReport report);
    String formatName();
}
```

### 9.3 Implementaciones

| Renderer | Librería | Formato de salida | Prioridad |
|----------|----------|-------------------|-----------|
| `MarkdownRenderer` | Ninguna (strings) | Markdown (.md) | Alta — Fase 5.1 |
| `HtmlRenderer` | Thymeleaf / jMustache | HTML + CSS | Alta — Fase 5.1 |
| `JsonRenderer` | Jackson | JSON (.json) | Alta — Fase 5.1 |
| `PdfRenderer` | Flying Saucer / iText | PDF (.pdf) | Media — Fase 5.2 |
| `DocxRenderer` | Apache POI / docx4j | Word (.docx) | Baja — Fase 5.3 |

### 9.4 Registro de Renderers

```java
// En KinConfig.java:
@Bean
public ReportRenderer markdownRenderer() { return new MarkdownRenderer(); }

@Bean
public ReportRenderer htmlRenderer() { return new HtmlRenderer(); }

@Bean
public ReportRenderer jsonRenderer() { return new JsonRenderer(); }

// El orquestador puede elegir renderer por formato:
// reportRendererRegistry.render("markdown", consultingReport);
```

---

## 10. NUEVOS ADR NECESARIOS

Se requieren los siguientes ADR antes de implementar. Deben crearse en `kin-docs/adr/`.

### ADR-001: Creación del Bounded Context Reporting

**Motivo**: Nuevo paquete `kin/reporting/` con 4 engines + modelo de reporte.
**Contenido**: Justificar por qué los engines viven juntos en lugar de contextos separados.
**Decisión propuesta**: Un solo Bounded Context `reporting` que contiene Recommendation, Risk, Opportunity y ReportEngine.

### ADR-002: PipelineContext — Nuevos campos para engines

**Motivo**: Se agregan 4 campos a `PipelineContext`.
**Contenido**: Evaluar si usar `attributes: Map` vs campos tipados. Decidir campos tipados por type-safety. Documentar el límite de God Class.

### ADR-003: ReportRenderer como Port en el Dominio

**Motivo**: `ReportRenderer` es una interface definida en `kin/reporting/renderer/` (dominio). Sus implementaciones están en infraestructura.
**Contenido**: Validar que cumple DIP. Justificar que el port esté en el dominio porque el modelo (`ConsultingReport`) está en el dominio.

### ADR-004: Eliminación de lógica de negocio del LLM

**Motivo**: El LLM actualmente recibe datos crudos del dominio. Con Fase 5 solo recibe `ConsultingReport` estructurado.
**Contenido**: Impacto en `AiEngineService`, `ConsultorStage`, `LlmExplanationStage`. Roadmap de migración.

### ADR-005: Refactor de ChatOrchestratorServiceImpl — Streaming usa KinMethod

**Motivo**: Eliminar la divergencia entre flujo bloqueante y streaming.
**Contenido**: Cómo el flujo streaming usará KinMethod + SSE callback. Impacto en tests existentes.

---

## 11. RIESGOS ARQUITECTÓNICOS

### 11.1 Matriz de Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| **PipelineContext God Class** por 4 nuevos campos | Media | Alto | Monitorear tamaño. Si excede 20 campos o 120 líneas, migrar a `Map<String,Object>` con typed accessors. |
| **ReportEngine demasiado grande** (orquesta 3 engines + construye 9 secciones) | Media | Alto | Si ReportEngine supera 250 líneas, extraer `SectionBuilder` factories separadas. |
| **Renderers con lógica de presentación** (formateo condicional, omisión de secciones) | Alta | Medio | En code review: verificar que renderers solo usan if/switch para formateo, NO para filtrar datos. |
| **Rendimiento del pipeline** (ahora 11 stages vs 6 originales) | Media | Medio | Cada stage debe medirse. Si el pipeline total excede 5s, considerar async para stages no críticos (Opportunity, Recommendation). |
| **Acoplamiento entre engines vía ProjectContext** (todos leen las mismas dimensiones) | Baja | Medio | Los engines ya son independientes. ProjectContext es su única fuente de datos. Si un engine necesita datos que otro produce, se introduce un nuevo campo en PipelineContext (no dependencia directa). |
| **Complejidad de configuración** (5 nuevos beans en KinConfig) | Baja | Bajo | KinConfig se mantiene ordenado por grupo. Los nuevos engines se agrupan bajo un método `reportingEngines()`. |

### 11.2 Límites de Seguridad

- **4 nuevos campos en PipelineContext.** Si en Fase 6 se agregan 3+ más, refactorizar.
- `ConsultingReport` puede serializarse a JSON para almacenamiento. Puede crecer hasta ~50KB para proyectos complejos.
- Los renderers no deben acceder a la red ni a bases de datos.

---

## 12. ESTRATEGIA DE PRUEBAS

### 12.1 Pirámide de Pruebas

```
         ╱╲
        ╱  ╲           E2E: 2-3 tests
       ╱    ╲          (ChatController → Pipeline completo)
      ╱──────╲
     ╱        ╲        Integration: 10-15 tests
    ╱          ╲       (PipelineContext flujo, Renderer output)
   ╱────────────╲
  ╱              ╲     Unit: 80+ tests
 ╱────────────────╲    (cada engine, cada VO, cada stage)
```

### 12.2 Pruebas Unitarias (80+ tests)

| Componente | Tests | Cobertura objetivo |
|-----------|-------|--------------------|
| `RecommendationEngine` | 15 | 100% reglas de negocio |
| `RiskEngine` | 12 | 100% reglas de negocio |
| `OpportunityEngine` | 12 | 100% reglas de negocio |
| `ReportEngine` | 10 | 100% orquestación |
| `ConsultingReport` + secciones | 8 | Construcción, inmutabilidad |
| `RecommendationStage` | 5 | supports(), execute() |
| `RiskStage` | 5 | supports(), execute() |
| `OpportunityStage` | 5 | supports(), execute() |
| `ReportStage` | 3 | execute() |
| `LlmExplanationStage` | 5 | Prompt assembly, LLM call |
| `MarkdownRenderer` | 5 | Renderizado correcto |
| `HtmlRenderer` | 5 | Renderizado correcto |

### 12.3 Pruebas de Integración (10-15 tests)

| Test | Descripción |
|------|-------------|
| Pipeline completo con Fase 5 | Ejecutar pipeline de 11 stages, verificar ConsultingReport |
| ReportEngine generate() | Proveer ProjectContext + ScoreResult, verificar todas las secciones |
| RecommendationEngine + RiskEngine + OpportunityEngine sin datos | Todos deben devolver `empty()` |
| MarkdownRenderer + HtmlRenderer + JsonRenderer | Mismo ConsultingReport → 3 formatos diferentes |
| LlmExplanationStage sin LLM | Mock de ProviderRouter, verificar prompt |
| PipelineContext con 4 nuevos campos | Verificar que los campos se escriben/leen correctamente |

### 12.4 Pruebas E2E (2-3 tests)

| Test | Descripción |
|------|-------------|
| ChatController → processMessage → Pipeline → ConsultingReport | Flujo completo bloqueante |
| ChatController → processMessageStream → Pipeline → SSE + ConsultingReport | Flujo completo streaming |
| ReportController (nuevo) → GET /projects/{id}/report → ConsultingReport en JSON | Endpoint de reporte |

---

## 13. ROADMAP DE IMPLEMENTACIÓN POR BLOQUES

Cada bloque es independiente y desplegable. El orden está diseñado para minimizar riesgos.

### Bloque 1 — Fundación (Fase 5.0)
**Duración estimada**: 1 sprint (2 semanas)
**Dependencias**: Ninguna (solo arquitectura existente)

- [ ] ADR-001: Reporting Bounded Context
- [ ] ADR-002: PipelineContext nuevos campos
- [ ] Crear `kin/reporting/` package
- [ ] Crear todos los Value Objects de secciones (`ExecutiveSummary`, `ScoresSection`, etc.)
- [ ] Crear `ConsultingReport` con Builder
- [ ] Crear `ReportMetadata`
- [ ] Tests de VO (inmutabilidad, builders, empty())
- [ ] UML actualizado

### Bloque 2 — Engines Atómicos (Fase 5.1)
**Duración**: 1 sprint
**Dependencias**: Bloque 1

- [ ] ADR-003: ReportRenderer port
- [ ] `RecommendationEngine` + `RecommendationSet` + `Recommendation` + `RecommendationCategory`
- [ ] `RiskEngine` + `RiskAssessment` + `Risk` + `RiskCategory`
- [ ] `OpportunityEngine` + `OpportunitySet` + `Opportunity` + `OpportunityCategory`
- [ ] Tests unitarios completos (100% reglas de negocio)
- [ ] `ReportRenderer` interface + `MarkdownRenderer` + `JsonRenderer`

### Bloque 3 — ReportEngine y Orquestación (Fase 5.2)
**Duración**: 1 sprint
**Dependencias**: Bloque 2

- [ ] `ReportEngine` con orquestación de 3 engines
- [ ] Section factories: `ExecutiveSummary.from()`, `ScoresSection.from()`, etc.
- [ ] Pipeline Stage: `RecommendationStage`, `RiskStage`, `OpportunityStage`
- [ ] Pipeline Stage: `ReportStage`
- [ ] Pipeline Stage: `LlmExplanationStage`
- [ ] Tests de integración del pipeline completo
- [ ] `HtmlRenderer`

### Bloque 4 — Refactor y Compatibilidad (Fase 5.3)
**Duración**: 1 sprint
**Dependencias**: Bloque 3

- [ ] ADR-004: Eliminar lógica de negocio del LLM
- [ ] ADR-005: Streaming usa KinMethod
- [ ] Extraer `PromptAssembler` de `AiEngineService`
- [ ] Refactor `ChatOrchestratorServiceImpl` streaming path → usa KinMethod
- [ ] Refactor `ScoringEngine` (reemplazar heurística length, agregar explanation audit trail)
- [ ] Verificar que todos los tests existentes siguen pasando
- [ ] E2E tests

### Bloque 5 — Reportes y APIs (Fase 5.4)
**Duración**: 1 sprint
**Dependencias**: Bloque 4

- [ ] Nuevo endpoint: `GET /projects/{id}/report`
  - Query param: `format=markdown|html|json`
  - Devuelve el `ConsultingReport` renderizado
- [ ] Nuevo endpoint: `GET /projects/{id}/report/model`
  - Devuelve el `ConsultingReport` como JSON (modelo puro)
- [ ] `PdfRenderer` (si aplica)
- [ ] `DocxRenderer` (si aplica)
- [ ] Documentación de API
- [ ] Metrics y observabilidad

### Bloque 6 — Estabilización (Fase 5.5)
**Duración**: 1 sprint
**Dependencias**: Bloque 5

- [ ] Performance testing (pipeline con 11 stages)
- [ ] Optimization si es necesario (parallel stage execution para engines independientes)
- [ ] Documentación técnica completa
- [ ] UML final actualizado
- [ ] ADRs retrospectivos si es necesario
- [ ] Update de ARQUITECTURA_BASE_KIN_2.0.md

---

## 14. VERIFICACIÓN DE COMPATIBILIDAD

### 14.1 Compatibilidad con Fase 1 (Auth + Users)

| Componente | Impacto de Fase 5 | Acción requerida |
|-----------|-------------------|------------------|
| `AuthController`, `AuthServiceImpl` | Ninguno | No tocar |
| `JwtService`, `JwtAuthenticationFilter` | Ninguno | No tocar |
| `User`, `UserRole`, `UserRepository` | Ninguno | No tocar |

✅ **Compatible.**

### 14.2 Compatibilidad con Fase 2 (Chat + Messages)

| Componente | Impacto de Fase 5 | Acción requerida |
|-----------|-------------------|------------------|
| `ChatController` | Nuevo endpoint `/projects/{id}/report` (además de los existentes) | Solo agregar endpoint. Los existentes no se modifican. |
| `ChatService`, `ChatServiceImpl` | Ninguno | No tocar |
| `ChatMessage`, `ChatMessageRepository` | Ninguno | No tocar |
| `ChatOrchestratorServiceImpl` | Refactor interno del flujo streaming para usar KinMethod. API pública no cambia. | Refactor controlado, mismo contrato. |

✅ **Compatible. API REST no cambia.**

### 14.3 Compatibilidad con Fase 3 (Project Context)

| Componente | Impacto de Fase 5 | Acción requerida |
|-----------|-------------------|------------------|
| `ProjectContext` | Los nuevos engines LEEN de `ProjectContext`. No lo modifican. | No tocar `ProjectContext`. |
| `AnalyzedDimension` | Los nuevos engines usan `AnalyzedDimension` para asociar riesgos/recomendaciones a dimensiones. | No tocar. |
| `CompletenessEvaluator` | `ScoringEngine` refactorizado — evaluador no cambia. | No tocar. |
| `ConversationStrategist` | No cambia. | No tocar. |
| `ContextAnalyzerPort` | No cambia. | No tocar. |

✅ **Compatible. ProjectContext es solo lectura para nuevos engines.**

### 14.4 Compatibilidad con Fase 4 (KinMethod + Pipeline)

| Componente | Impacto de Fase 5 | Acción requerida |
|-----------|-------------------|------------------|
| `KinMethod` | API `execute(KinMethodCommand) → KinMethodResult` NO cambia. El pipeline interno tiene más stages. | No tocar la API pública. |
| `KinMethodCommand` | No cambia. | No tocar. |
| `KinMethodResult` | Nuevo campo `consultingReport`. | Agregar campo — es aditivo, no rompe. |
| `Pipeline` | No cambia. | No tocar. |
| `PipelineStage` | Interface no cambia. | No tocar. |
| `PipelineContext` | 4 nuevos campos tipados. | Agregar campos. Los existentes no cambian. |
| `ScoringEngine` | Refactor interno: reemplazar heurística length + agregar audit trail. | Refactor controlado. Output (`ScoreResult`) compatible. |
| `DomainEventBus` | No cambia. | No tocar. |
| `InMemoryDomainEventBus` | No cambia. | No tocar. |

✅ **Compatible. Todos los cambios son aditivos (nuevos campos, nuevos stages).**

### 14.5 Compatibilidad con endpoints SSE existentes

| Endpoint | Impacto | Estado |
|----------|---------|--------|
| `POST /projects/{id}/chat` (bloqueante) | KinMethod ahora produce ConsultingReport internamente. La respuesta `ChatResponse` sigue siendo la misma. | ✅ Sin cambios visibles |
| `POST /projects/{id}/chat/stream` (SSE) | Internamente usará KinMethod. Los eventos SSE (`token`, `done`, `error`) son idénticos. El `done` puede incluir `reportId` adicional. | ✅ Compatible hacia atrás |

### 14.6 Resumen de Compatibilidad

```
┌─────────────────────────────────────────────────────────────┐
│                     VERIFICACIÓN FINAL                        │
│                                                               │
│  Fase 1 (Auth):      ✅ Sin impacto                           │
│  Fase 2 (Chat):      ✅ API no cambia. Refactor interno.      │
│  Fase 3 (Context):   ✅ Solo lectura. Sin cambios.            │
│  Fase 4 (KinMethod): ✅ API estable. Agregados aditivos.      │
│  SSE Streaming:      ✅ Mismos eventos. Compatible.            │
│  Tests existentes:   ✅ No deben romperse (verificar en CI).  │
│                                                               │
│  TOTAL: COMPATIBILIDAD COMPLETA CON FASES 1-4                │
└─────────────────────────────────────────────────────────────┘
```

---

*Documento generado el 30 de julio de 2026.*
*Versión: FASE5-DESIGN-001*
*Basado en: ARQUITECTURA_BASE_KIN_2.0.md (ARCH-001) y KIN_ARCHITECTURE_GOVERNANCE.md (GOV-001)*

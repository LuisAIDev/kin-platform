# KIN 2.0 — Release Notes · Fase 5.3 (OpportunityEngine)

> **Fase de desarrollo**: enmienda de `v2.0.0-alpha.1` (ADR-010)
> **Estado**: `ARCHITECTURE STABLE (enmendado)`
> **Fecha**: 31 de julio de 2026
> **Commit**: `aabd2b3` (`feat(kin): add OpportunityEngine to pipeline (phase 5.3)`)
> **Branch**: `main`
> **Tag**: *(ninguno — las fases intermedias no constituyen hito de versión)*
>
> ⚠️ **Nota**: este documento corresponde a una fase de desarrollo intermedia. El último
> hito oficial sigue siendo `v2.0.0-alpha.1`. El siguiente tag (`v2.0.0-alpha.2` o el que
> corresponda) se generará al completar el bloque arquitectónico previsto
> (OpportunityEngine + ReportEngine + PromptAssembler/LlmExplanation) y alcanzar un nuevo
> estado estable.

---

## 1. Resumen ejecutivo

La Fase 5.3 agrega el cuarto motor de dominio determinista: **`OpportunityEngine`**
(categoría `OPPORTUNITY`, prioridad 60), que identifica oportunidades de mejora y
capitalización del proyecto a partir de la señal ya calculada por
`CompletenessEvaluation.detectedOpportunities`.

- Sigue el patrón canónico `coordinador + analizadores + ensamblador` del `RiskEngine`,
  **por composición** sobre la infraestructura compartida (`kin.engine`), sin copiar reglas.
- **8 analizadores** auto-descubiertos por `EngineRegistry` vía `List<DomainEngine>`
  (mercado, innovación, tecnológicas, financieras, competitivas, escalabilidad,
  automatización, monetización).
- No usa LLM: es un motor puro POJO determinista (mismo modelo que Scoring/Recommendation/Risk).
- El pipeline pasa de **8 a 9 etapas**: `OpportunityStage` entre `RiskStage` y `EventStage`.
- **172 tests** en verde (0 fallos) y dominio `kin.reporting.opportunity` al **100 %** de
  cobertura de líneas.

---

## 2. Contexto y alcance

`v2.0.0-alpha.1` dejó la infraestructura de motores estable con tres motores operativos
(Scoring 30, Recommendation 40, Risk 50). La Fase 5.3 añade el siguiente eslabón del ciclo
de evaluación de viabilidad (ADR-010): después de conocer **qué tan viable** es el proyecto
(scoring), **qué conviene hacer** (recommendation) y **qué riesgos** lo amenazan (risk), el
nuevo motor responde **dónde está la oportunidad**.

### Alcance

- Solo `OpportunityEngine` y su paquete `kin.reporting.opportunity`.
- 8 categorías: mercado, innovación, tecnológicas, financieras, competitivas, escalabilidad,
  automatización, monetización.
- Fuera de alcance: ReportEngine, LlmExplanationEngine, KnowledgeEngine (fases posteriores).

### Fuera de alcance (explícito)

- **ReportEngine / LlmExplanationEngine / KnowledgeEngine**: fases posteriores del bloque
  arquitectónico (próximo milestone).
- **No** se copia código de reglas de `RecommendationEngine` / `RiskEngine`: solo se
  reutiliza infraestructura compartida por composición.
- **No** se rompe REST/SSE/frontend/contratos/ADRs/Baseline.

---

## 3. Lo nuevo en esta versión

### 3.1 Motor de dominio `kin.reporting.opportunity` (ADR-010)

| Tipo | Clase |
|------|-------|
| Categoría | `OpportunityCategory` (enum: MERCADO, INNOVACION, TECNOLOGICAS, FINANCIERAS, COMPETITIVAS, ESCALABILIDAD, AUTOMATIZACION, MONETIZACION) |
| Valor | `Opportunity` (record inmutable: título, categoría, impacto, prioridad, explicación) |
| Explicación | `OpportunityExplanation` (motivo + orientación) |
| Input | `OpportunityInput` (record: `CompletenessEvaluation` + `OpportunityModel`) |
| Resultado | `OpportunityResult` (record: lista ordenada + coverage + quality + summary) |
| Modelo | `OpportunityModel` (umbrales `8/5/"v1"`, parámetros del ensamblador) |
| Ensamblador | `OpportunityAssembler` (confianza compartida `0.35 + 0.35·coverage + 0.3·quality`, prioridad, bonus por señal faltante, `hasSignal`) |
| Contrato | `OpportunityAnalyzer` (`OpportunityAnalyzeResult` con `hasOpportunity`/`details`) |
| Coordinador | `OpportunityEngine` (`execute` determinista: sort por prioridad desc → categoría → título; top 3) |
| Stage | `OpportunityStage` (composición pura sobre `EngineStage`, nombre "Oportunidades") |

- 8 analizadores: `MarketOpportunityAnalyzer`, `InnovationOpportunityAnalyzer`,
  `TechnologicalOpportunityAnalyzer`, `FinancialOpportunityAnalyzer`,
  `CompetitiveOpportunityAnalyzer`, `ScalabilityOpportunityAnalyzer`,
  `AutomationOpportunityAnalyzer`, `MonetizationOpportunityAnalyzer`.

### 3.2 Pipeline

- `KinConfig.chatPipeline(...)`: **9 etapas**
  (analizador → evaluador → estratega → consultor → scoring → recommendation → risk →
  **opportunity** → eventos).
- `PipelineContext.opportunityResult`: campo tipado aditivo (mismo patrón que
  `scoreResult`/`recommendationResult`/`riskResult`).

### 3.3 Configuración

- `KinConfig`: beans `opportunityModel`, 8 analizadores, `opportunityEngine(...)` y
  `opportunityStage`.

---

## 4. Calidad y verificación

| Métrica | Valor |
|---------|-------|
| Tests | **172** (`./mvnw clean verify`) |
| Fallos | **0** |
| Errores | **0** |
| Skip | **0** |
| Build | **BUILD SUCCESS** |

Tests nuevos de la fase: **42** (de 130 a 172) en 4 clases:
`OpportunityEngineTest` (15), `OpportunityResultTest` (13), `OpportunityAssemblerTest` (9),
`OpportunityStageTest` (5).

Cobertura JaCoCo (líneas) de los paquetes de dominio:

| Paquete | Líneas |
|---------|--------|
| `kin.engine` | **100 %** |
| `kin.reporting*` (agregado) | **98,6 %** |
| `kin.reporting.opportunity` | **100 %** |
| `kin.reporting.risk` | 99,5 % |
| `kin.reporting` | 95,8 % |
| `kin.scoring` | 98,9 % |

Requisito de dominio (≥ 90 % en `kin.reporting` y `kin.engine`): **CUMPLIDO**.

---

## 5. Documentación y ADRs

| Artefacto | Descripción |
|-----------|-------------|
| `ADR-010` | `OpportunityEngine` (categoría, prioridad 60, fase OPPORTUNITY, 4 alternativas rechazadas) |
| `FASE5_3_OPPORTUNITY_ENGINE.md` | Fase completa: auditoría, diseño, UML (5 diagramas), contratos, implementación, tests, compatibilidad |
| `BASELINE_ARCHITECTURE.md` | Reporting BC incluye oportunidades; pipeline 9 etapas; decisión congelada #7 con ADR-010; preparación → Fase 5.4 |
| `KIN_ARCHITECTURE_GOVERNANCE.md` §6.2 | `OpportunityEngine` movido de "Futuro" a existente |
| `CHANGELOG.md` | Entrada `[Unreleased] - Fase 5.3` |
| `AGENTS.md` | Paquete `reporting.opportunity`, 172 tests, cobertura |

---

## 6. Problemas conocidos

> Ninguno nuevo introducido por esta fase. Ítems heredados de `v2.0.0-alpha.1` /
> Fase 5.2.1, todos fuera del alcance:

1. `pricing_plans` sin columnas NOT NULL aplicadas en dev (H2, `ddl-auto: update`). No
   bloquea el arranque (warnings).
2. `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
3. Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.
4. Cobertura baja en paquetes de infraestructura (auth, pricing, project, ai.provider) —
   fuera del requisito del dominio.

---

## 7. Pendientes fuera de alcance

- **Fase 5.4**: ReportEngine + KnowledgeEngine / próximos motores del bloque arquitectónico
  (ver `BASELINE_ARCHITECTURE.md` → Preparación para la siguiente fase).
- PromptAssembler / LlmExplanationEngine (explicaciones LLM sobre resultados de dominio).
- Próximo tag de versión: solo al completar el bloque y alcanzar un nuevo estado estable.

---

## 8. Verificación de esta fase

```bash
# Tests + cobertura
cd kin-backend && ./mvnw clean verify        # 172 tests, 0 fallos, BUILD SUCCESS
open target/site/jacoco/index.html           # kin.reporting.opportunity 100 %

# Backend (H2, sin Docker)
cd kin-backend && ./mvnw spring-boot:run     # http://localhost:8080/api/v1

# Frontend
cd kin-frontend && npm install && npm run dev  # http://localhost:3000

# E2E (con backend en perfil test)
cd kin-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test
cd kin-frontend && npx playwright test
```

---

*KIN 2.0 — Fase de desarrollo 5.3. El milestone oficial sigue siendo `v2.0.0-alpha.1`.*

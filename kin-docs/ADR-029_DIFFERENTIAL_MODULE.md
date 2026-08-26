# ADR-029: Módulo de Diagnóstico Diferencial (KIN Health)

## Estado
**Aceptado** — 2026-08-26

## Contexto

KIN Health ya cuenta con el **Triaje Digital** (ADR-028): catálogo de 50 condiciones y 136 relaciones, extracción de síntomas por NLP + fallback determinista, y un `TriageResult` con condiciones candidatas ordenadas por probabilidad. El siguiente paso es un **Diagnóstico Diferencial**: a partir de los síntomas extraídos, elaborar un análisis más profundo que incluya **factores de riesgo**, **pruebas complementarias** para diferenciar condiciones y una **explicación** de por qué se sugieren ciertas condiciones.

Debe integrarse en la plataforma existente **sin romper nada**, siguiendo Clean Architecture + DDD y la filosofía **"Java decide. El LLM únicamente comunica."**

## Decisión

Crear un bounded context `com.kinplatform.kin.health.differential` con:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.differential.domain` | `DifferentialInput`, `DifferentialResult`, `DifferentialItem`, `RiskFactor` (condición, factor, peso), `RecommendedTest` (condición, prueba, descripción), `DifferentialCatalog`, `PatientContext`, `CatalogUpdate`/`CatalogUpdateResult` |
| Motor | `kin.health.differential.engine` | `DifferentialEngine` (implementa `DomainEngine<DifferentialInput, DifferentialResult>`) |
| Etapa | `kin.health.differential.stage` | `DifferentialStage` (implementa `PipelineStage`, aditiva, justo después de `TriageStage`) |
| Puertos | `kin.health.differential.port` | `DifferentialKnowledgeRepository` (load + `applyUpdate`) |
| Adaptadores | `kin.health.differential.adapter` | Entidades JPA + repos Spring Data + `DifferentialKnowledgeAdapter` (KnowledgeSource) + `DifferentialCatalogParser` |
| API | `kin.health.differential.api` | `DifferentialController`, `DifferentialService`, `DifferentialCatalogUpdateService`, DTOs |
| Config | `kin.health.differential.config` | `DifferentialProperties`, `DifferentialConfig` |

### Modelo de datos (Flyway V23)

- `risk_factors(id, condition_id, factor, weight, description)` — PK + UNIQUE(condition_id, factor), `weight ∈ (0,1]`.
- `recommended_tests(id, condition_id, test, description)` — PK + UNIQUE(condition_id, test).

Seed inicial: **25 factores de riesgo** y **22 pruebas recomendadas** para las condiciones más relevantes (gripe, asma, neumonía, infección urinaria, diabetes, hipertensión, angina, gota, artrosis, artritis, bronquitis, sinusitis).

### Algoritmo del motor (determinista)

1. Cada condición candidata del `TriageResult` se convierte en un `DifferentialItem` conservando probabilidad, severidad, urgencia y síntomas coincidentes.
2. **Ajuste por factores de riesgo**: para cada `RiskFactor` del catálogo cuya clave coincida con el `PatientContext`, se modifica la probabilidad con `p' = p + weight·(1-p)` (siempre creciente, acotada a [0,1]).
3. **Pruebas recomendadas**: se adjuntan las pruebas del catálogo para cada condición.
4. **Explicación**: template determinista en Java (síntomas coincidentes + factores de riesgo aplicados).
5. Se ordena por probabilidad descendente y se limita a `kin.health.differential.max-items` (default 5).

El motor **nunca** usa LLM: solo amplía y explica el resultado determinista del triaje.

### Integración en pipeline

`DifferentialStage` (justo después de `TriageStage`, antes de `EvaluatorStage`) se inyecta en `KinConfig.chatPipeline`. La etapa:
- **Soporta** el turno solo si el módulo está habilitado, hay `ProjectContext`, y hay `PipelineContext.triageResult` no vacío.
- Ejecuta el motor y almacena `PipelineContext.differentialResult` (campo aditivo) y el `EngineResult` en `engineResults`.
- Emite `DifferentialPerformedEvent` (implementa `DomainEvent` + `HasUserId`).
- Si el módulo está deshabilitado o no hay triaje, se omite sin alterar el flujo.

### Enriquecimiento del catálogo (KnowledgeEngine)

- `DifferentialKnowledgeAdapter` implementa el puerto `KnowledgeSource`: lee un dataset empaquetado (`data/differential-catalog.json`) u una API médica externa configurada, con degradación elegante al bundle.
- `DifferentialCatalogParser` convierte el JSON estructurado en `CatalogUpdate` resolviendo nombres de condición contra el catálogo de triaje (nunca inserta factores/pruebas huérfanas) con IDs deterministas.
- `DifferentialKnowledgeRepository.applyUpdate` realiza un **upsert idempotente**.
- `DifferentialCatalogUpdateService` integra el KnowledgeEngine; ante fallo total, el catálogo local permanece intacto.

## API REST

| Endpoint | Método | Descripción | Seguridad |
|----------|--------|-------------|-----------|
| `/api/v1/health/differential?consultationId=...` | GET | Diagnóstico diferencial de una consulta de triaje existente del paciente autenticado | JWT |
| `/api/v1/health/differential` | POST | Recibe síntomas directos (+ factores de riesgo) y devuelve el diagnóstico diferencial | JWT |

El `userId` se resuelve **siempre** desde la autenticación (`AuthenticatedUsers.require`), nunca desde el body, garantizando el aislamiento por paciente (una consulta de otro usuario devuelve 404).

## Configuración (feature flags)

```yaml
kin:
  health:
    differential:
      enabled: ${KIN_HEALTH_DIFFERENTIAL_ENABLED:true}
      max-items: ${KIN_HEALTH_DIFFERENTIAL_MAX_ITEMS:5}
      catalog:
        source-id: ${KIN_HEALTH_DIFFERENTIAL_CATALOG_SOURCE_ID:health-differential}
        external-enabled: ${KIN_HEALTH_DIFFERENTIAL_CATALOG_EXTERNAL_ENABLED:false}
        base-url: ${KIN_HEALTH_DIFFERENTIAL_CATALOG_BASE_URL:}
        bundled-resource: ${KIN_HEALTH_DIFFERENTIAL_CATALOG_BUNDLED_RESOURCE:data/differential-catalog.json}
```

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Motor determinista + catálogo (elegida)** | 100 % determinista, sin LLM, extensible, aditivo al triaje | Catálogo inicial limitado | ✅ |
| **Explicación generada por LLM** | Lenguaje natural flexible | No determinista, viola "Java decide", costoso | Futuro (opcional) |
| **Pruebas por reglas manuales en el motor** | Simple | Duplica conocimiento, difícil de ampliar | ❌ |
| **Enriquecimiento vía KnowledgeEngine (elegida)** | Reutiliza validación/adquisición, degradación elegante | Requiere resolver nombres → ids | ✅ |

## Consecuencias
**Positivas**:
- Diagnóstico diferencial determinista y auditable, heredado del triaje.
- Factores de riesgo y pruebas sugeridas por condición (datos estructurados).
- Módulo aislado: etapa aditiva que se omite sin triaje o sin el flag.
- El pipeline existente no se ve afectado (el `DifferentialKnowledgeAdapter` no se registra en el `SourceRegistry` principal).
- Datos de salud confidenciales y aislados por usuario.

**Negativas / límites**:
- El catálogo de factores/pruebas es una muestra y no reemplaza el juicio clínico (disclaimer en la UI y en la respuesta).
- La explicación es por template; si en el futuro se desea texto libre, se delegaría a un LLM opcional sin afectar el cálculo.

### Mejora UX (fase de consolidación)

- La UI del diagnóstico diferencial añade **enlaces a información externa**
  (Wikipedia y MedlinePlus en español) por condición, para que el paciente
  profundice en cada candidato sin depender del template de explicación.
- El cálculo de probabilidades, factores de riesgo y pruebas sigue siendo
  100 % determinista; los enlaces son solo presentación.

## Referencias
- `kin-docs/adr/ADR-028_TRIAGE_MODULE.md` (base: triaje digital)
- `kin-docs/adr/ADR-014-external-knowledge-acquisition.md` (KnowledgeSource, patrón puerto/adaptador)
- `kin-docs/adr/ADR-015-strategic-interview-engine.md` (etapa aditiva + campo aditivo en `PipelineContext`)
- Migración `V23__create_differential_tables.sql`
- Dataset empaquetado `data/differential-catalog.json`

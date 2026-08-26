# ADR-028: Módulo de Triaje Digital (KIN Health)

## Estado
**Aceptado** — 2026-08-26 (base) · **Enmendado** 2026-08-26 (fase profesional: catálogo 50 condiciones, NLP, enriquecimiento desde KnowledgeEngine) · **Enmendado** 2026-08-26 (consolidación: catálogo 100 condiciones, NER V2, validación clínica, HealthDataImporter)

## Contexto

KIN expande la plataforma al sector salud. El primer módulo es un **Triaje Digital**: el paciente introduce sus síntomas y obtiene una lista de posibles condiciones con un score de probabilidad, basado en conocimiento médico estructurado. Es el primero de una serie de funcionalidades de salud (apoyo al diagnóstico, gestión hospitalaria, etc.) y debe integrarse en la plataforma existente **sin romper nada** y siguiendo la filosofía de dominio puro + motores deterministas.

La **fase profesional** escala el módulo base (12 condiciones, extracción por keywords) a un catálogo de **50 condiciones y 100+ relaciones**, extracción de síntomas con **NLP (OpenNLP)** y **enriquecimiento del catálogo desde el KnowledgeEngine existente** con degradación elegante.

Principios rectores aplicados:
- **Java decide. El LLM únicamente comunica.** El cálculo de probabilidades es 100 % determinista, sin IA. El NLP solo extrae síntomas; nunca calcula probabilidades.
- **Clean Architecture + DDD**: el dominio (`kin.health.triage.domain`) no depende de Spring, JPA ni NLP; los adaptadores viven en infraestructura.
- **Aditividad**: la integración al pipeline y a `PipelineContext` es aditiva (patrón ADR-011/014/015).
- **Offline-first / degradación elegante**: si el KnowledgeEngine o la fuente externa fallan, el catálogo local (bundle empaquetado CIE-10/WHO) sigue disponible y el pipeline no se rompe.

## Decisión

Crear un bounded context `com.kinplatform.kin.health.triage` con:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.triage.domain` | `Symptom` (+ `aliases`), `Condition`, `SymptomConditionRelation` (peso 0..1 + `required`), enums `Severity`/`Urgency`, `TriageCatalog`, `TriageInput`, `TriageResult`, `SymptomExtractor` (puerto), `KeywordSymptomExtractor`, `SymptomNormalizer`, `CatalogUpdate`, `CatalogUpdateResult` |
| Motor | `kin.health.triage.engine` | `TriageEngine` (implementa `DomainEngine<TriageInput, TriageResult>`) |
| Etapa | `kin.health.triage.stage` | `TriageStage` (implementa `PipelineStage`, aditiva) |
| Puertos | `kin.health.triage.port` | `TriageKnowledgeRepository` (load + `applyUpdate`), `TriageConsultationRepository` |
| Adaptadores | `kin.health.triage.adapter` | Entidades JPA + repos Spring Data + mappers + `OpenNLPSymptomExtractor` (NLP) + `HealthKnowledgeAdapter` (KnowledgeSource médico) + `HealthCatalogParser` |
| API | `kin.health.triage.api` | `TriageController`, `TriageService`, `TriageCatalogUpdateService`, `TriageCatalogAdminController`, DTOs |
| Config | `kin.health.triage.config` | `TriageProperties`, `TriageConfig` |

### Modelo de datos (Flyway V21 + V22)

- `symptoms(id, name, description, icd_code, aliases JSONB)` — V22 añade `aliases` para la normalización NLP.
- `conditions(id, name, description, icd_code, severity, urgency, recommendation)` con `CHECK` en `severity`/`urgency`.
- `symptom_condition_relations(symptom_id, condition_id, weight, required)` — PK compuesta, `weight ∈ (0,1]`.
- `triage_consultations(id, user_id, symptoms JSONB, results JSONB, created_at)` — historial auditable por paciente.

Seed: **50 condiciones** y **100+ relaciones** (V21 + V22) con CIE-10; cada condición con como mucho UN síntoma `required` (el más patognomónico). Los síntomas incluyen `aliases` (p. ej. `cefalea` → `dolor de cabeza`, `tos seca` → `tos`).

### Algoritmo del motor (determinista, inalterado)

1. Una condición **solo es candidata** si están presentes todos sus síntomas `required`.
2. `rawScore(c) = Σ weight(s)` sobre los síntomas presentes.
3. Se descartan condiciones sin síntomas coincidentes (`rawScore == 0`).
4. `probability(c) = rawScore(c) / Σ rawScore` sobre las candidatas (distribución que suma 1).
5. Orden descendente por probabilidad y límite a `kin.health.triage.max-conditions` (default 5).

El NLP **nunca** participa en el cálculo: solo amplía los candidatos de síntomas extraídos del texto libre.

### Extracción de síntomas (NLP + normalización)

- `SymptomExtractor` es un **puerto de dominio**; `KeywordSymptomExtractor` es su implementación determinista pura (fallback siempre disponible).
- `OpenNLPSymptomExtractor` (infraestructura) usa `WhitespaceTokenizer` + `DictionaryNameFinder` de **OpenNLP 2.3.1** con un diccionario construido del catálogo (nombres + aliases). No requiere descarga de modelos; degrada con gracia a keywords ante cualquier fallo o catálogo vacío.
- `SymptomNormalizer` (dominio puro) normaliza términos (acentos, puntuación) y resuelve aliases al nombre canónico.
- Criterio de aceptación: *"tengo tos seca, fiebre de 38° y dolor muscular"* → `[tos, fiebre, dolor muscular]`.

### Enriquecimiento del catálogo desde el KnowledgeEngine

- `HealthKnowledgeAdapter` implementa el puerto `KnowledgeSource` del KnowledgeEngine: por defecto lee un **dataset empaquetado** (`data/triage-catalog-extended.json`, CIE-10/WHO) y, si el operador habilita `external-enabled` + `base-url`, consulta una API médica externa (PubMed/WHO) con `HttpClient` (offline-first, ante error degrada al bundle).
- `HealthCatalogParser` (infra, Jackson) convierte el contenido JSON estructurado de candidatos/facts en `CatalogUpdate` con **IDs deterministas** (`DeterministicId`), haciendo el upsert idempotente.
- `TriageKnowledgeRepository.applyUpdate(CatalogUpdate)` realiza el **upsert idempotente** (nunca duplica).
- `TriageCatalogUpdateService` integra el KnowledgeEngine: primero intenta `knowledgeEngine.evaluate(...)`; si produce datos estructurados los aplica; si el motor falla o está vacío, **degradación elegante** al `HealthKnowledgeAdapter`; ante fallo total devuelve un resultado vacío y el catálogo local permanece intacto.
- Endpoint administrativo `POST /api/v1/admin/health/triage/catalog/update` (rol ADMIN) fuerza la actualización bajo demanda.

### Integración en pipeline

`TriageStage` (después del Analizador) se inyecta en `KinConfig.chatPipeline`. La etapa:
- **Soporta** el turno solo si el módulo está habilitado, hay `ProjectContext`, mensaje no vacío y la categoría del proyecto es `SALUD` (gate barato).
- Extrae síntomas con el `SymptomExtractor` inyectado (NLP con fallback keywords); si no hay síntomas reconocidos, **se omite**.
- Ejecuta el motor y almacena `PipelineContext.triageResult` (campo aditivo) y el `EngineResult` en `engineResults`.
- Emite `TriagePerformedEvent` (implementa `DomainEvent` + `HasUserId`).

## API REST

| Endpoint | Método | Descripción | Seguridad |
|----------|--------|-------------|-----------|
| `/api/v1/health/triage` | POST | Consulta de triaje (`{ "symptoms": [...] }`) → condiciones con probabilidad, severidad, urgencia, recomendación + `disclaimer` | JWT (roles FREE/PREMIUM/FACILITADOR/PATIENT/ADMIN) |
| `/api/v1/health/triage/symptoms` | GET | Catálogo de síntomas para el formulario | JWT |
| `/api/v1/health/triage/history` | GET | Historial del paciente autenticado (aislamiento por `userId` desde la autenticación) | JWT |
| `/api/v1/admin/health/triage/catalog/update` | POST | Fuerza la actualización del catálogo desde fuentes externas (fase profesional) | ADMIN |

El `userId` se resuelve **siempre** desde la autenticación (`AuthenticatedUsers.require`), nunca desde el body, garantizando que el paciente solo vea su propio historial.

## Configuración (feature flags)

```yaml
kin:
  health:
    triage:
      enabled: ${KIN_HEALTH_TRIAGE_ENABLED:true}
      max-conditions: ${KIN_HEALTH_TRIAGE_MAX_CONDITIONS:5}
      data-source: ${KIN_HEALTH_TRIAGE_DATA_SOURCE:db}
      nlp-enabled: ${KIN_HEALTH_TRIAGE_NLP_ENABLED:true}
      catalog:
        source-id: ${KIN_HEALTH_TRIAGE_CATALOG_SOURCE_ID:health-catalog}
        source-name: ${KIN_HEALTH_TRIAGE_CATALOG_SOURCE_NAME:Health Catalog}
        external-enabled: ${KIN_HEALTH_TRIAGE_CATALOG_EXTERNAL_ENABLED:false}
        base-url: ${KIN_HEALTH_TRIAGE_CATALOG_BASE_URL:}
        bundled-resource: ${KIN_HEALTH_TRIAGE_CATALOG_BUNDLED_RESOURCE:data/triage-catalog-extended.json}
```

Con `enabled=false` el `TriageStage` no soporta ningún turno y `TriageService` lanza `TriageDisabledException` (404) en la API.

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Motor determinista propio (elegida)** | 100 % determinista, testeable, sin LLM, extensible | Requiere catálogo curado | ✅ |
| **NLP con OpenNLP + fallback keywords (elegida)** | Detecta sinónimos/frases; determinista sin modelos; fallback garantizado | Requiere aliases en el catálogo | ✅ |
| **NLP con LLM** | Lenguaje natural flexible | No determinista, viola "Java decide", costoso | ❌ |
| **Enriquecimiento vía KnowledgeEngine (elegida)** | Reutiliza validación/adquisición existente; degradación elegante | Requiere parser de contenido estructurado | ✅ |
| **Enriquecimiento con red directa en el pipeline** | Simple | Rompe offline-first, arriesga el pipeline | ❌ |
| **Bayesiana con priors médicos** | Más precisa clínicamente | Requiere datos clínicos reales y validación | Futuro |

## Consecuencias

**Positivas**:
- Cálculo determinista y auditable (pesos + síntomas coincidentes en cada resultado).
- Catálogo profesional: **50 condiciones y 100+ relaciones** con aliases CIE-10/WHO.
- NLP de extracción sin modelos descargados y con fallback determinista; el motor nunca usa NLP.
- Enriquecimiento idempotente y offline-first: la actualización bajo demanda no rompe el catálogo local.
- Módulo aislado: el pipeline existente no se ve afectado (etapa aditiva con gate por categoría `SALUD`; el `HealthKnowledgeAdapter` no se registra en el `SourceRegistry` principal).
- Datos de salud confidenciales y aislados por usuario.

**Negativas / límites**:
- El catálogo sigue siendo una muestra y no reemplaza el juicio clínico (se declara explícitamente en la UI).
- `triage_consultations` guarda datos sensibles: cualquier operación futura debe mantener el aislamiento por `userId`.
- La fuente externa real (PubMed/WHO) requiere configuración explícita del operador; el enriquecimiento automático por defecto usa el bundle empaquetado.

## Consolidación del catálogo (fase 2)

### Catálogo 100+ condiciones

- **Flyway V26** amplía el catálogo a **100 condiciones**, **95 síntomas** y **~253 relaciones**, añadiendo la columna `validation_status` a `conditions`.
- `Condition` (dominio) y `ConditionEntity`/mapper incluyen `ValidationStatus` (`PENDING`/`REVIEWED`/`APPROVED`/`REJECTED`).

### HealthDataImporter

- `HealthDataImporter` (infraestructura) integra el `KnowledgeEngine` para adquirir condiciones/síntomas desde fuentes externas, normaliza y deduplica con IDs deterministas y aplica la actualización al repositorio JPA con estado `PENDING`.
- Endpoint admin `POST /api/v1/admin/health/catalog/import-from-external`.
- Feature flag `kin.health.triage.auto-update` (default `false`): la importación ocurre bajo demanda.

### NER V2

- `OpenNLPSymptomExtractorV2` (infra) mejora la extracción con **negaciones** ("no tengo…"), **parafraseo** ("me duele la cabeza" → "dolor de cabeza") y **mediciones** ("fiebre de 38°"), manteniendo el fallback determinista a keywords.
- Criterio de aceptación verificado: **≥10 % de mejora de F1 sobre keyword matching** en un corpus de textos clínicos en español (`OpenNLPSymptomExtractorV2Test`).

### Informe de cobertura

- `CoverageReportService` genera el informe de cobertura de síntomas por condición; `GET /api/v1/admin/health/catalog/coverage` lo expone. Condiciones con <2 síntomas se marcan como insuficientes.

### Validación clínica

- Proceso documentado en `kin-docs/GUIA_VALIDACION_CLINICA.md`: importación → informe de cobertura → revisión por profesional → `APPROVED`/`REJECTED`.

### Mejoras UX

- Triaje: el paciente confirma/edita los síntomas seleccionados antes de analizar (multiselect + búsqueda).
- Diagnóstico diferencial: enlaces externos (Wikipedia, MedlinePlus) por condición.
- Dashboard: gráfico de evolución de consultas por mes.

## Referencias
- `kin-docs/adr/ADR-005-engine-infrastructure.md` (motor `DomainEngine`)
- `kin-docs/adr/ADR-014-external-knowledge-acquisition.md` (patrón puerto/adaptador + etapa aditiva + KnowledgeSource)
- `kin-docs/adr/ADR-015-strategic-interview-engine.md` (etapa aditiva + campo aditivo en `PipelineContext`)
- Migraciones `V21__create_triage_tables.sql`, `V22__expand_triage_catalog.sql` y `V26__consolidate_triage_catalog.sql`
- Dataset empaquetado `data/triage-catalog-extended.json`
- `kin-docs/GUIA_VALIDACION_CLINICA.md` (proceso de validación clínica)

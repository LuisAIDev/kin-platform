# FASE 6.0 - Adquisición de Conocimiento Externo (Cerrada oficialmente)

> **Estado**: **IMPLEMENTADA Y CERRADA OFICIALMENTE (E1…E7)** — commit `d5f912c` + enmienda E7
> (documentación de cierre, commit final de la fase)
> **Base**: KIN 2.0 Alpha 1 (`v2.0.0-alpha1`) - `ALPHA STABLE` (release oficial, commit `89b39b9`)
> **ADRs**: 014 (external knowledge acquisition) - **Estado: Aprobado** (decisión congelada)
>
> Secuencia oficial del proyecto: Fase 5.6 → Conversation Orchestrator, **Fase 6 → KnowledgeEngine + RAG (CERRADA)**,
> Fase 7 → siguiente hito.
>
> Este documento entrega el **diseño completo e implementado** de la Fase 6: congela la
> arquitectura de adquisición de conocimiento externo, documenta la bitácora E1…E7 y el cierre
> oficial de la fase.

---

## 1. Objetivo

Convertir a KIN en un **consultor capaz de enriquecer sus análisis utilizando información
externa verificable**, manteniendo intacto el principio fundamental del proyecto:

> **Java decide. El LLM únicamente comunica.**
> La IA nunca calculará. La IA nunca decidirá. Toda decisión continuará siendo tomada por Java.

Objetivos concretos:

1. **Adquirir** conocimiento externo (APIs, bases de datos, fuentes públicas, documentos e
   Internet) de forma desacoplada del LLM.
2. **Validar** cada fuente en Java con reglas deterministas (protocolo, allowlist, estado,
   frescura, formato, confianza) y descartar lo que no pasa.
3. **Normalizar** el conocimiento en hechos inmutables (`KnowledgeFact`) con trazabilidad
   (origen, URL, fecha, confianza).
4. **Enriquecer** los analizadores de dominio (mercado, innovación, financiero, competitivo,
   etc.) para mejorar recomendaciones, riesgos, oportunidades y el `ConsultingReport`.
5. **Preservar** los contratos congelados: la integración al pipeline es aditiva (patrón
   ADR-011) y no se tocan `KinMethod`, `Pipeline`, `ConsultorStage`, `PromptAssembler`,
   `AIResponder`, `ConversationOrchestrator` ni `ReportEngine`.

---

## 2. Auditoría del estado actual

### 2.1 Estado del repositorio (base → al cierre de la fase)

| Ítem | Estado (base E1) | Estado (cierre E7) |
|------|--------|--------|
| Branch | `main` @ `d5f912c` (release oficial KIN 2.0 Alpha 1) | `main` (enmienda E7 de la Fase 6) |
| Tags | `v2.0.0-alpha.1`, `v2.0.0-alpha1` | Sin tag nuevo (fase de documentación) |
| Working tree | Limpio | Limpio (tras commit E7) |
| ADRs | ADR-001 … ADR-013 presentes (13/13) | ADR-001 … ADR-014 presentes (14/14) — ADR-014 **Aprobado** |
| `BASELINE_ARCHITECTURE.md` | Baseline contractual actualizado (release) | + bounded context `kin.knowledge`, pipeline de 11 etapas, `KnowledgeStage`/`KnowledgeEngine` |
| `KIN_ARCHITECTURE_GOVERNANCE.md` | Gobernanza enmendada por ADR-013 | Enmendada por ADR-014 (regla de IA + decisión congelada #15) |
| `AGENTS.md` | Guía actualizada a la release `v2.0.0-alpha1` | Actualizada a la Fase 6 (paquete `kin.knowledge`, 675 tests) |
| `CHANGELOG.md` | Entrada `[v2.0.0-alpha1] - 2026-07-31` | + entrada FASE 6 External Knowledge Acquisition |
| `README.md` | Estado actual actualizado a la release | Sin cambios en E7 (fuera de alcance) |
| `kin-docs/releases/KIN_2_0_ALPHA_1.md` | Release notes oficiales del núcleo | Sin cambios en E7 (fuera de alcance) |
| Build | `./mvnw clean verify` → **BUILD SUCCESS, 468 tests** | **BUILD SUCCESS, 675 tests, 0 fallos, 0 errores, 0 skipped** |
| Cobertura JaCoCo | `kin.conversation*` **100 %**; `kin.ai*` 99.7 %; `kin.reporting*` 99.2 %; `kin.engine` 99.1 %; `kin.scoring` 95.1 % | + `kin.knowledge` 100 %; `kin.knowledge.engine` 99.47 %; `kin.knowledge.stage` 100 %; `ai.knowledge.adapter` 100 % |
| Contratos congelados | Intactos (ningún archivo `.java` modificado en esta etapa) | Intactos (integración aditiva; solo `PipelineContext` + `KinConfig` tocados de forma aditiva) |

### 2.2 Estado del pipeline (base → final)

Pipeline base de 10 etapas (`KinMethod` → orden oficial):

`Analyzer → Evaluator → Strategist → Scoring → Recommendation → Risk → Opportunity → ReportEngine → Consultor → Events`

**Pipeline final de 11 etapas** (Fase 6, E6 — integración aditiva):

`Analyzer → Evaluator → Strategist → **Knowledge → Scoring** → Recommendation → Risk → Opportunity → ReportEngine → Consultor → Events`

El análisis consume el `ProjectContext` (dimensiones extraídas de la conversación + heurísticas
internas) **y** el conocimiento externo verificado (`KnowledgeResult` producido por
`KnowledgeStage`). La integración es aditiva: `KnowledgeStage` se inserta entre `StrategistStage`
y `ScoringStage`, y el resultado viaja en `PipelineContext.knowledgeResult` (patrón ADR-011).

### 2.3 Brechas detectadas

| # | Brecha | Impacto |
|---|--------|---------|
| 1 | Sin canal de conocimiento externo | Análisis limitado al contexto interno del proyecto |
| 2 | Sin validación de fuentes | No hay forma de distinguir dato verificado de referencia no fiable |
| 3 | Sin trazabilidad de orígenes | No se puede citar ni auditar de dónde salió un dato |
| 4 | Sin caché de conocimiento | Una futura consulta externa por turno sería lenta y costosa |
| 5 | Sin motor canonizado de conocimiento | La infraestructura `DomainEngine` está lista pero sin uso para conocimiento |

---

## 3. Diseño arquitectónico

### 3.1 Principio rector

> **Java obtiene el conocimiento. Java lo valida. Java decide qué hechos se usan.**
> El LLM únicamente comunica sobre el análisis resultante.

Reglas derivadas (vinculantes):

1. La recuperación, el filtrado, la validación y la selección de conocimiento son **decisiones
   de Java** (deterministas y testeables).
2. El LLM **nunca** recupera, valida ni decide sobre conocimiento.
3. El LLM **nunca** recibe fuentes crudas; recibe únicamente el prompt ensamblado de hoy
   (ADR-012). Exponer conocimiento citado en la comunicación es un cambio aditivo futuro que
   requiere una ADR propia.
4. Ninguna llamada externa se ejecuta dentro del dominio: el dominio define puertos
   (`KnowledgeSource`, `KnowledgeRepository`) y la infraestructura los implementa.
5. Sin red, el sistema degrada con gracia: `KnowledgeResult.empty()` con motivos, pipeline
   operativo (offline-first).

### 3.2 Arquitectura (nuevo bounded context `com.kinplatform.kin.knowledge`)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      DOMINIO  (kin.knowledge - POJO puro)                  │
│                                                                          │
│  KnowledgeGateway ──► SourceRegistry ──► KnowledgeSource  (puerto)        │
│        │                    │                                              │
│        │                    └────► KnowledgeCandidate                       │
│        ├──► SourceValidator ──► SourceValidation / SourceTrust              │
│        ├──► KnowledgeFact (normalización)                                  │
│        └──► KnowledgeRepository (puerto de caché, TTL)                     │
│                                                                          │
│  KnowledgeEngine : DomainEngine<KnowledgeInput, KnowledgeResult>          │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │  (implementaciones en infraestructura)
┌───────────────────────────────▼──────────────────────────────────────────┐
│                    INFRAESTRUCTURA  (adaptadores)                          │
│                                                                          │
│  HttpKnowledgeSourceAdapter  (APIs / Internet, cliente HTTP)              │
│  PublicApiConnector          (fuentes públicas, OCP-compliant)            │
│  JdbcKnowledgeSource         (base de datos de conocimiento / caché)      │
│  RagKnowledgeSource          (índice vectorial, recuperación en Java)     │
│  DocumentKnowledgeSource     (ingestión de documentos)                    │
│  KnowledgeRepositoryAdapter  (caché persistente con TTL)                  │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │  (cableado en KinConfig)
                      Pipeline existente (aditivo, E6)
```

### 3.3 Límites de la Fase 6

| Límite | Definición |
|--------|-----------|
| Fuentes | Solo fuentes en **allowlist** configurada (dominios/protocolos permitidos). **No** scraping web generalizado |
| Red | Solo adaptadores de infraestructura; timeouts, rate limit y retry acotado. **No** llamadas en tests (mocks) |
| Datos | No se almacena PII ni contenido del proyecto fuera del `KnowledgeRepository`; los hechos son públicos y verificados |
| Bases de datos | No se modifican las tablas del core (`project_context`, etc.). La caché de conocimiento es un almacén separado (futura migración Flyway) |
| Contratos | `KinMethod`, `Pipeline`, `PipelineStage`, `ConsultorStage`, `PromptAssembler`, `AIResponder`, `ConversationOrchestrator`, `ReportEngine`, `ConsultingReport`, `kin/engine` y `ConversationDecision` **no cambian** |
| Presupuesto | Límite de consultas por turno y TTL de caché para evitar llamadas repetidas y costos |
| LLM | El LLM no recupera, no valida, no decide; no recibe fuentes crudas |

---

## 4. Responsabilidades

### 4.1 Frontera de pureza

`kin.knowledge` respeta la **Frontera de pureza** del proyecto (gobernanza §1.1 y ADR-001):

- Sin anotaciones Spring/JPA.
- Sin DTOs, sin lógica de persistencia, sin lógica de serialización.
- Únicas dependencias permitidas: `java.*` y `org.slf4j`.
- Sin imports de `com.kinplatform.*` distintos de `kin.*` (p. ej. `kin.engine`, `kin.context`).

### 4.2 Responsabilidades por componente

| Componente | Responsabilidad |
|------------|-----------------|
| `KnowledgeGateway` | Orquesta la adquisición: recibe `KnowledgeRequest`, consulta fuentes registradas, valida candidatos, normaliza hechos y produce `KnowledgeResult`. Decide en Java qué hechos entran |
| `KnowledgeEngine` | Motor canonizado (ADR-005): `evaluate(KnowledgeInput) → KnowledgeResult`; fase `KNOWLEDGE`, tipo `DOMAIN` |
| `KnowledgeSource` | Puerto: `fetch(KnowledgeQuery) → List<KnowledgeCandidate>`. Sin lógica de negocio |
| `SourceRegistry` | Registro de `KnowledgeSource` disponibles (auto-descubrimiento vía `List<KnowledgeSource>`, patrón `EngineRegistry`) |
| `SourceValidator` | Reglas deterministas: HTTPS, allowlist, estado HTTP, formato, frescura (TTL), dedup, `SourceTrust` |
| `KnowledgeRepository` | Puerto de caché/persistencia de hechos verificados con TTL |
| `KnowledgeStage` (E6) | Etapa aditiva de pipeline: compone `KnowledgeEngine` (patrón `EngineStage`), escribe `PipelineContext.knowledgeResult` |

---

## 5. UML (diseño)

### 5.1 Componentes (nivel paquete)

| Paquete | Rol |
|---------|-----|
| `com.kinplatform.kin.knowledge` | Dominio: tipos (`KnowledgeRequest/Query/Candidate/Fact/Input/Result`, `SourceValidation`, `SourceTrust`, `KnowledgeSource`, `KnowledgeRepository`) |
| `com.kinplatform.kin.knowledge.engine` | Dominio: `KnowledgeEngine` (canonizado), `KnowledgeGateway`, `SourceRegistry`, `SourceValidator` |
| `com.kinplatform.kin.knowledge.stage` | `KnowledgeStage` (E6, aditivo; composición pura sobre `EngineStage`) |
| `com.kinplatform.ai.knowledge.adapter` | Adaptadores de infraestructura: `CompositeKnowledgeSource`, `HttpKnowledgeSourceAdapter`, `PublicApiConnector`, `JdbcKnowledgeSource`, `RagKnowledgeSource`, `DocumentKnowledgeSource` |
| `com.kinplatform.common.config` | `KinConfig`: cableado de beans (`SourceValidator`, `SourceRegistry`, `KnowledgeGateway`, `KnowledgeEngine`, `KnowledgeStage`) |

### 5.2 Clases (diagrama de clases)

```
KnowledgeGateway
  + acquire(KnowledgeRequest): KnowledgeResult
  - validate(candidate): SourceValidation      (delega en SourceValidator)
  - normalize(candidate): KnowledgeFact
  - cache/read de KnowledgeRepository (TTL)

KnowledgeEngine implements DomainEngine<KnowledgeInput, KnowledgeResult>
  + evaluate(input): KnowledgeResult
  + metadata(): EngineMetadata                 (fase KNOWLEDGE, tipo DOMAIN)

KnowledgeSource  <<puerto>>
  + fetch(query: KnowledgeQuery): List<KnowledgeCandidate>

SourceRegistry
  + register(KnowledgeSource)
  + all(): List<KnowledgeSource>

SourceValidator
  + validate(candidate): SourceValidation      (HTTPS, allowlist, estado, TTL, dedup, trust)

KnowledgeRepository  <<puerto>>
  + find(query): Optional<KnowledgeResult>
  + save(facts, ttl): void
```

### 5.3 Tipos de datos (records/enums de dominio)

| Tipo | Contenido |
|------|-----------|
| `KnowledgeRequest` | `topic`, dimensiones objetivo, `keywords`, límite de resultados, ventana de tiempo |
| `KnowledgeQuery` | Proyección de `KnowledgeRequest` para una fuente |
| `KnowledgeCandidate` | `content`, `sourceId`, `sourceName`, `url`, `publishedAt`, `contentType`, `meta` |
| `SourceValidation` | `accepted`, `reasons`, `trust` (`SourceTrust`) |
| `SourceTrust` | `OFFICIAL_PUBLIC`, `SECONDARY`, `UNVERIFIED` |
| `KnowledgeFact` | `claim`, `sourceId`, `url`, `publishedAt`, `trust`, `category`, `deterministicId` |
| `KnowledgeInput` | Implementa `EngineInput` |
| `KnowledgeResult` | Implementa `EngineResult`: `facts`, `sourcesUsed`, `validations`, `empty()`, `metadata` |

---

## 6. Flujo completo: cómo Java obtiene conocimiento

### 6.1 APIs externas

1. `KnowledgeGateway.acquire(...)` construye un `KnowledgeQuery` por fuente registrada.
2. El adaptador `HttpKnowledgeSourceAdapter` (infraestructura) implementa `KnowledgeSource.fetch`
   y llama a la API con el cliente HTTP inyectado (timeout, rate limit, retry acotado).
3. El resultado crudo se envuelve en `KnowledgeCandidate` (el dominio no ve la red).
4. `SourceValidator` valida el candidato; si no pasa, `SourceValidation.accepted=false` con el
   motivo y se descarta.

### 6.2 Bases de datos

1. El dominio consulta `KnowledgeRepository` (puerto) antes de ir a la red: si hay hechos
   frescos en caché, se devuelven (sin llamadas externas).
2. Un adaptador `JdbcKnowledgeSource` permite leer fuentes de conocimiento externas (almacén
   separado del core) a través del mismo puerto `KnowledgeSource`.
3. La caché persistente (`KnowledgeRepositoryAdapter`) usa un almacén propio con TTL; **no**
   modifica `project_context` ni ninguna tabla existente.

### 6.3 Fuentes públicas

1. `PublicApiConnector` agrupa adaptadores por fuente oficial (estadísticas, marcos
   regulatorios, referencias sectoriales), todos implementando `KnowledgeSource` (OCP).
2. Cada conector es **auto-descubierto** por `SourceRegistry` vía `List<KnowledgeSource>`
   (mismo patrón que `EngineRegistry`).
3. Las fuentes fuera de la **allowlist** configurada se rechazan en `SourceValidator`.

### 6.4 RAG (recuperación aumentada dirigida por Java)

1. `RagKnowledgeSource` recibe `KnowledgeQuery` del `KnowledgeGateway` y consulta el índice
   vectorial.
2. El adaptador devuelve **candidatos** (`KnowledgeCandidate`) con score de similitud; la
   **selección** (umbral determinista, dedup, confianza) la ejecuta `SourceValidator` en Java.
3. El LLM no participa en la recuperación ni en la selección de fragmentos.

### 6.5 Documentos

1. `DocumentKnowledgeSource` (infra) ingiere documentos (p. ej. PDF/ODT) con un parser de
   infraestructura.
2. Produce `KnowledgeCandidate`s normalizados; el dominio solo consume el puerto.

### 6.6 Internet

1. Todo tráfico pasa por `HttpKnowledgeSourceAdapter` (infra), configurado con allowlist de
   hosts (mitigación SSRF), HTTPS obligatorio, timeouts y rate limit.
2. Fallback offline: si la red no está disponible, `KnowledgeGateway` devuelve
   `KnowledgeResult.empty()` con motivos y el pipeline sigue operando.

### 6.7 Validación de fuentes

`SourceValidator` aplica reglas **deterministas en Java** en este orden:

1. **Protocolo**: HTTPS obligatorio.
2. **Allowlist**: el dominio/host está en la lista permitida (config por entorno).
3. **Estado**: código HTTP 2xx (si aplica), `contentType` permitido.
4. **Frescura**: `publishedAt` dentro de la ventana TTL configurada.
5. **Formato**: estructura válida y parseable (JSON/texto estructurado).
6. **Deduplicación**: no repetir `(sourceId, url)`.
7. **Confianza**: `SourceTrust` derivado del tipo de fuente (oficial pública > secundaria > no
   verificada).

Una fuente que no pasa se descarta con `SourceValidation.rejected` y su motivo. **Nunca se
pregunta al LLM si una fuente es válida.**

### 6.8 Desacople del LLM (resumen)

| Capa | Conocimiento | LLM |
|------|--------------|-----|
| Adquisición | `KnowledgeGateway` (Java) | No participa |
| Validación | `SourceValidator` (Java) | No participa |
| Selección | `SourceValidator` + `KnowledgeGateway` (Java) | No participa |
| Enriquecimiento | Analizadores de dominio (Java) | No participa |
| Comunicación | Prompt ensamblado actual (ADR-012) | Recibe solo el prompt de hoy |

---

## 7. Contratos (interfaces y records nuevos — implementados)

| Contrato | Firma (implementada) |
|----------|----------|
| `KnowledgeSource` | `List<KnowledgeCandidate> fetch(KnowledgeQuery query)` |
| `KnowledgeGateway` | `KnowledgeResult acquire(KnowledgeRequest request)` |
| `KnowledgeEngine` | `evaluate(KnowledgeInput) → KnowledgeResult` + `metadata()` (fase KNOWLEDGE, tipo DOMAIN, prioridad 50) |
| `SourceValidator` | `SourceValidation validate(KnowledgeCandidate candidate)` + `List<SourceValidation> validateAll(List<KnowledgeCandidate>)` |
| `SourceRegistry` | `register(KnowledgeSource)`, `all()`, `size()`, `isEmpty()`, `empty()` |
| `KnowledgeRepository` | `Optional<KnowledgeResult> find(KnowledgeQuery)` + `save(KnowledgeResult, Duration ttl)` |
| `KnowledgeStage` (E6) | Stage aditivo: compone `EngineStage`, escribe `PipelineContext.knowledgeResult` |

### Cambios aditivos a contratos existentes (implementados en E6)

| Contrato | Cambio | Tipo |
|----------|--------|------|
| `PipelineContext` | nuevo campo `KnowledgeResult knowledgeResult` (+ getter/setter) | Aditivo (patrón ADR-011) |
| `KinConfig` | beans `SourceValidator`, `SourceRegistry`, `KnowledgeGateway`, `KnowledgeEngine`, `KnowledgeStage`; `KnowledgeStage` insertado en `chatPipeline(...)` | Cableado |

**Sin cambios** a: `KinMethod`, `Pipeline`, `PipelineStage`, `ConsultorStage`,
`PromptAssembler`, `AIResponder`, `ConversationOrchestrator`, `ReportEngine`,
`ConsultingReport`, `kin/engine`, `ProjectContext`, `ConversationDecision`.

---

## 8. Roadmap E1…E7 — Bitácora de implementación

| Etapa | Contenido | Entregable | Estado |
|-------|-----------|-----------|--------|
| **E1** | Diseño arquitectónico (ADR-014 + este documento) | ✅ Documentación | ✅ **Completado (2026-07-31)** |
| **E2** | Modelo de dominio `kin.knowledge`: tipos (`KnowledgeRequest/Query/Candidate/Fact/Input/Result`, `SourceValidation`, `SourceTrust`) + `KnowledgeGateway` + `SourceRegistry` + `KnowledgeEngine` canonizado (`DomainEngine`, fase KNOWLEDGE) + tests de dominio | Código de dominio | ✅ **Completado (E2)** |
| **E3** | `SourceValidator` determinista (HTTPS, allowlist, TTL, dedup, trust) + puerto `KnowledgeRepository` + caché en memoria con TTL + tests | Validación y caché | ✅ **Completado (E3)** |
| **E4** | Adaptador HTTP base `HttpKnowledgeSourceAdapter` + `PublicApiConnector` (OCP) + `CompositeKnowledgeSource` con timeout/retry/rate-limit/fallback + tests con mocks (sin red) | Conectores API/Internet | ✅ **Completado (E4)** |
| **E5** | `JdbcKnowledgeSource` + `RagKnowledgeSource` (índice vectorial, recuperación en Java) + `DocumentKnowledgeSource` (parser de documentos) + tests con mocks | Conectores BD/RAG/documentos | ✅ **Completado (E5)** |
| **E6** | Integración aditiva al pipeline: `KnowledgeStage` + `PipelineContext.knowledgeResult` + cableado en `KinConfig` + actualización de `BASELINE`, `GOVERNANCE`, `AGENTS`, `CHANGELOG` | Integración pipeline | ✅ **Completado (E6)** |
| **E7** | Auditoría de cierre: ADR-014 **Aprobada**, validación de contratos congelados intactos, `./mvnw clean verify` (**675 tests, BUILD SUCCESS**), cobertura `kin.knowledge` ≥ 90 % (**100 %**), cierre oficial de la Fase 6 | Cierre de fase | ✅ **Completado (E7)** |

> **Bitácora**: E1…E7 se ejecutaron bajo instrucciones explícitas posteriores, respetando las
> prohibiciones de la fase (sin llamadas reales de red en tests — mocks; sin scraping
> generalizado; allowlist de fuentes; sin tocar contratos congelados; el LLM nunca recupera,
> valida ni decide conocimiento). El cierre (E7) es **solo documentación**: no se añadió
> funcionalidad fuera de lo ya implementado en E2…E6.

---

## 9. Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Dependencia de APIs externas (disponibilidad, rate limits) | Media | Timeouts, retry acotado, caché TTL, fallback offline (`KnowledgeResult.empty()`) |
| R2 | Calidad/confiabilidad de fuentes | Media | `SourceValidator` + `SourceTrust`; allowlist de dominios |
| R3 | Latencia por consulta externa por turno | Media | Caché `KnowledgeRepository` + presupuesto de consultas |
| R4 | SSRF / seguridad de red | Alta | HTTPS obligatorio + allowlist de hosts (el adaptador nunca resuelve dominios arbitrarios) |
| R5 | Complejidad del RAG | Media | Recuperación dirigida por Java; adaptador aislado detrás del puerto `KnowledgeSource` |
| R6 | Crecimiento de `PipelineContext` (otro campo aditivo) | Baja | Mismo patrón ADR-011; monitoreo continuo |
| R7 | Violación accidental de la frontera ADR-012 si se expone conocimiento al prompt | Media | El diseño no modifica `PromptAssembler`; exponer fuentes en la comunicación exige una ADR propia |

---

## 10. Compatibilidad con ADR-001…ADR-013

| ADR | Compatibilidad |
|-----|----------------|
| ADR-001 (reporting BC) | ✅ El conocimiento enriquece los motores de reporting; no cambia el bounded context |
| ADR-002 (pipeline context) | ✅ `PipelineContext.knowledgeResult` es un campo aditivo (mismo patrón que ADR-011) |
| ADR-003 / 004 / 010 (recommendation/risk/opportunity) | ✅ Los analizadores pueden consumir hechos vía inputs aditivos; contratos intactos |
| ADR-005 (engine infrastructure) | ✅ `KnowledgeEngine` implementa `DomainEngine`; sin cambios en `kin/engine` |
| ADR-006 (runtime) | ✅ `KinMethod` no cambia; `KnowledgeStage` se inserta en el mismo pipeline |
| ADR-007 (context repository) | ✅ `KnowledgeRepository` es un puerto nuevo y separado; `project_context` intacto |
| ADR-008 (AI responder) | ✅ `AIResponder` no cambia; el conocimiento no pasa por el port de IA |
| ADR-009 (engine canonization) | ✅ `KnowledgeEngine` se canoniza bajo `DomainEngine` desde el diseño |
| ADR-011 (report engine) | ✅ `ReportEngine`/`ConsultingReport` no cambian; los hechos alimentan analizadores, no el VO del reporte |
| ADR-012 (prompt assembler) | ✅ `PromptAssembler` y la frontera REPORT no cambian; el LLM recibe el mismo prompt |
| ADR-013 (conversation orchestrator) | ✅ `ConversationOrchestrator` no cambia; la directiva y el turno siguen siendo Java |
| ADR-010 (opportunity engine) | ✅ Los analizadores de oportunidad (mercado, innovación) son consumidores naturales de hechos verificados |

---

## 11. Criterios de aceptación

- [x] ADR-014 aprobada (**Aprobado** al cierre de la fase).
- [x] `kin.knowledge` es 100 % POJO: sin Spring, sin JPA, sin dependencias del proyecto excepto `kin.*`.
- [x] Contratos congelados (`KinMethod`, `Pipeline`, `PipelineStage`, `ConsultorStage`,
      `PromptAssembler`, `AIResponder`, `ConversationOrchestrator`, `ReportEngine`,
      `ConsultingReport`, `kin/engine`, `ProjectContext`, `ConversationDecision`) **sin cambios**.
- [x] Integración al pipeline **aditiva** (patrón ADR-011): `PipelineContext.knowledgeResult` + `KnowledgeStage`.
- [x] `./mvnw clean verify` → **BUILD SUCCESS**; **675 tests verdes** (0 fallos, 0 errores, 0 skipped).
- [x] Cobertura de dominio ≥ 90 % (JaCoCo) en `kin.knowledge` (**100 %**; `kin.knowledge.engine` 99.47 %).
- [x] `SourceValidator` 100 % determinista; ninguna validación delega en el LLM.
- [x] Offline-first: sin red, `KnowledgeResult.empty()` y el pipeline sigue operando.
- [x] Sin llamadas de red en tests (mocks); sin scraping generalizado; allowlist de fuentes.
- [x] Principio *Java decide / LLM comunica* intacto: el LLM nunca recupera, valida ni decide conocimiento.

---

## 12. Estado del entregable

| Etapa | Estado |
|-------|--------|
| **E1 — Diseño arquitectónico (ADR-014 + este documento)** | ✅ **Completado** |
| E2 — Modelo de dominio | ✅ **Completado** |
| E3 — Validación y caché | ✅ **Completado** |
| E4 — Conectores API/Internet | ✅ **Completado** |
| E5 — Conectores BD/RAG/documentos | ✅ **Completado** |
| E6 — Integración pipeline | ✅ **Completado** |
| E7 — Auditoría de cierre | ✅ **Completado** |

**FASE 6 CERRADA OFICIALMENTE (2026-07-31). ADR-014 APROBADA.**

**Entregables de la Fase 6**:
- `kin-docs/adr/ADR-014-external-knowledge-acquisition.md` (aprobada)
- `kin-docs/FASE6_0_EXTERNAL_KNOWLEDGE.md` (diseño + bitácora E1…E7 + cierre)
- Dominio: `com.kinplatform.kin.knowledge` (`*.knowledge`, `*.knowledge.engine`, `*.knowledge.stage`)
- Infraestructura: `com.kinplatform.ai.knowledge.adapter` (6 adaptadores)
- Integración: `PipelineContext.knowledgeResult` (aditivo) + `KnowledgeStage` en `chatPipeline(...)`

**Implementado (E2…E6)**: modelo de dominio, motor canonizado, gateway, registro, validador,
puerto de caché, 6 adaptadores de infraestructura, integración aditiva al pipeline y tests.
**Cierre (E7)**: solo documentación — ADR-014 a Aprobado, enmiendas en `BASELINE`,
`GOVERNANCE`, `AGENTS`, `CHANGELOG`, auditoría final (675 tests, BUILD SUCCESS, cobertura
`kin.knowledge` ≥ 90 %). **No se implementó funcionalidad fuera de E7.**

*Fase 6 — adquisición de conocimiento externo cerrada oficialmente. Ningún contrato congelado de
`BASELINE_ARCHITECTURE.md` se modifica sin una ADR aprobada.*

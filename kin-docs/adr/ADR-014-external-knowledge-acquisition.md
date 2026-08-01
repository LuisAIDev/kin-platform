# ADR-014: Adquisición de Conocimiento Externo — KnowledgeGateway que enriquece el análisis en Java y mantiene el LLM desacoplado

**Estado**: **Aprobado** (implementación completa E1…E7 — Fase 6 cerrada oficialmente)
**Fecha**: 2026-07-31
**Autor**: KIN Architecture Team

> **Alcance**: este ADR define y congela la arquitectura de adquisición de conocimiento externo
> de la Fase 6 (`FASE6_0_EXTERNAL_KNOWLEDGE.md`). **Implementado y verificado (E1…E7)**: la
> aprobación inicial (Etapa 1) habilitó la implementación por etapas (E2…E7); al cierre de la
> fase la decisión queda **congelada** y su estado pasa a **Aprobado**. No modifica ningún
> contrato congelado: la integración al pipeline es **aditiva** (patrón ADR-011) y el LLM
> permanece desacoplado.

---

## Contexto

KIN 2.0 Alpha 1 (`v2.0.0-alpha1`) ha liberado el **núcleo inteligente**: un pipeline de dominio
de 10 etapas que produce un `ConsultingReport` (10 secciones) a partir del `ProjectContext`,
con motores deterministas canonizados bajo `DomainEngine` (ADR-005/009), un runtime único
`KinMethod` (ADR-006), contexto durable `ContextRepository` (ADR-007), IA por puertos
`AIResponder`/`PromptAssembler` (ADR-008/012) y un ciclo conversacional dirigido por
`ConversationOrchestrator` (ADR-013).

El principio rector del proyecto es:

> **Java decide. El LLM únicamente comunica.**

Hoy todo el conocimiento que consume el análisis proviene **exclusivamente** del
`ProjectContext`: las dimensiones extraídas de la conversación y las heurísticas internas.
El consultor no puede contrastar el proyecto contra información externa verificable
(estadísticas de mercado, marcos regulatorios, comparables, referencias sectoriales), lo que
limita la calidad de recomendaciones, riesgos, oportunidades y del reporte.

La Fase 6 convierte a KIN en un **consultor que enriquece sus análisis con conocimiento externo
verificable**, preservando el principio: **la adquisición, validación y selección de
conocimiento son decisiones de Java**; el LLM únicamente comunica sobre el resultado analizado.

### Restricciones impuestas al diseño (cumplidas en E1…E7)

1. No se modifica ninguno de los contratos congelados: `KinMethod`, `Pipeline`, `PipelineStage`,
   `ConsultorStage`, `PromptAssembler`, `AIResponder`, `ConversationOrchestrator`,
   `ReportEngine`, `ConsultingReport` ni la infraestructura `kin/engine`.
2. No se agregan dependencias nuevas (sin librerías externas, sin cliente HTTP real).
3. Conectores, HTTP, RAG, búsquedas, bases de datos nuevas e IA adicional se implementan como
   **adaptadores de infraestructura** detrás de puertos de dominio; el dominio
   (`com.kinplatform.kin.knowledge`) es 100 % POJO y nunca toca la red.
4. El LLM nunca recupera, valida ni decide sobre conocimiento: solo recibe el prompt
   ensamblado que ya consume hoy. **"Java decide. Las fuentes únicamente aportan conocimiento."**

---

## Decisión

Se introduce un **bounded context de dominio `com.kinplatform.kin.knowledge`** (POJO puro, sin
Spring, sin JPA) que actúa como **KnowledgeGateway** de la plataforma. El diseño sigue el patrón
ya congelado de los motores de dominio (ADR-005) y de los analizadores coordinadores
(ADR-004/010/011):

1. **Un motor canonizado** `KnowledgeEngine` implementa `DomainEngine<KnowledgeInput,
   KnowledgeResult>` (fase `KNOWLEDGE`, tipo `DOMAIN`, prioridad 50). El conocimiento externo se
   adquiere, valida y normaliza **en Java** y se representa como hechos inmutables
   (`KnowledgeFact`). `evaluate(KnowledgeInput)` delega en `KnowledgeGateway.acquire(...)` y
   degrada a `KnowledgeResult.empty()` si el input o el gateway no están disponibles
   (offline-first).
2. **Puertos de fuente** (`KnowledgeSource`) definidos en el dominio; las conexiones reales
   (APIs HTTP, bases de datos, fuentes públicas, RAG, documentos, Internet) son **adaptadores
   de infraestructura** que implementan esos puertos — el dominio nunca toca la red. En
   `kin.knowledge.engine` viven `KnowledgeGateway` (coordinador: deriva `KnowledgeQuery`,
   consulta `SourceRegistry`, delega la validación en `SourceValidator` y normaliza los hechos
   con métricas deterministas de confianza/calidad) y `SourceRegistry` (registro por
   auto-descubrimiento vía `List<KnowledgeSource>`, patrón `EngineRegistry`, orden de registro
   preservado, vista inmutable).
3. **`SourceValidator` determinista** (Java): protocolo HTTPS, allowlist de dominios, estado
   HTTP (2xx), tipo de contenido, formato (contenido no vacío), frescura (TTL por ventana),
   deduplicación por `(sourceId, url)` y nivel de confianza (`SourceTrust`). Una fuente que no
   pasa se descarta con su motivo (`SourceValidation.rejected(...)`); **nunca se le pregunta al
   LLM**. Stateless y reentrante: la deduplicación vive dentro de `validateAll(...)`. Config
   inyectada por constructor (allowlist, `maxAge`, content types); `SourceValidator.strict()`
   es el default offline-first.
4. **`KnowledgeRepository`** (puerto de persistencia/caché): `find(KnowledgeQuery) → Optional
   <KnowledgeResult>` + `save(KnowledgeResult, Duration ttl)`. Los hechos verificados se cachean
   con TTL para no repetir llamadas a la red en cada turno; el adaptador vive en infraestructura
   (en memoria o persistente; no modifica `project_context`).
5. **Integración aditiva al pipeline** (implementada en E6): nueva etapa `KnowledgeStage` entre
   `StrategistStage` y `ScoringStage`, y un campo tipado aditivo
   `PipelineContext.knowledgeResult` (mismo patrón sancionado por ADR-011). `KnowledgeStage` es
   composición pura sobre `EngineStage` (patrón `ScoringStage`/`OpportunityStage`/`ReportStage`):
   lee el `ProjectContext`, construye la `KnowledgeRequest` (topic, dimensiones cubiertas,
   keywords, límite y ventana de tiempo) e invoca `KnowledgeEngine`. El conocimiento enriquece
   los **analizadores de dominio en Java** (p. ej. mercado, innovación, financiero,
   competitivo); **no se modifica** `ConsultorStage`, `PromptAssembler` ni `AIResponder`.
6. **El LLM permanece desacoplado**: recibe exactamente el mismo prompt de hoy. La exposición
   de conocimiento citado en la comunicación (p. ej. una sección de fuentes en el reporte o una
   cita en la conversación) es un cambio **aditivo futuro** que exigirá una ADR propia por
   tocar la frontera ADR-012; no forma parte de esta decisión.
7. **Infraestructura** (E4…E5): `com.kinplatform.ai.knowledge.adapter` implementa los puertos.
   `CompositeKnowledgeSource` (compone múltiples `KnowledgeSource` en orden determinista),
   `HttpKnowledgeSourceAdapter` (APIs/Internet, configuración de timeout/retry/rate-limit y
   allowlist para mitigar SSRF), `PublicApiConnector` (fuentes públicas, OCP-compliant),
   `JdbcKnowledgeSource` (base de datos de conocimiento/caché persistente),
   `RagKnowledgeSource` (índice vectorial, recuperación en Java) y `DocumentKnowledgeSource`
   (ingestión de documentos). Todos devuelven candidatos crudos; la decisión siempre es del
   dominio. Cableado en `KinConfig`.

### Componentes nuevos (`kin.knowledge`)

| Componente | Rol | Estable |
|------------|-----|---------|
| `KnowledgeEngine` | Motor canonizado (ADR-005/009): `evaluate(KnowledgeInput) → KnowledgeResult`, fase `KNOWLEDGE`, tipo `DOMAIN`, prioridad 50 | ✅ Implementado |
| `KnowledgeGateway` | Fachada de dominio de la adquisición: deriva `KnowledgeQuery`, compone `SourceRegistry` + `SourceValidator` + normalización en `KnowledgeFact`s con métricas deterministas (confianza/calidad) | ✅ Implementado |
| `KnowledgeSource` | Puerto de fuente de conocimiento: `fetch(KnowledgeQuery) → List<KnowledgeCandidate>` | ✅ Implementado |
| `SourceRegistry` | Registro de `KnowledgeSource` disponibles (auto-descubrimiento por `List<KnowledgeSource>`, patrón `EngineRegistry`; orden preservado, vista inmutable) | ✅ Implementado |
| `SourceValidator` | Reglas deterministas de validación de candidatos → `SourceValidation` (HTTPS, allowlist, estado, formato, frescura, dedup, `SourceTrust`) | ✅ Implementado |
| `KnowledgeRepository` | Puerto de caché/persistencia de hechos verificados (TTL): `find(KnowledgeQuery)` + `save(KnowledgeResult, Duration)` | ✅ Implementado |
| `KnowledgeRequest` / `KnowledgeQuery` / `KnowledgeCandidate` / `KnowledgeFact` / `KnowledgeInput` / `KnowledgeResult` / `SourceValidation` / `SourceTrust` | Records/enums inmutables del ciclo de conocimiento | ✅ Implementado |
| `KnowledgeStage` | Etapa aditiva de pipeline (composición pura sobre `EngineStage`) entre `StrategistStage` y `ScoringStage`; escribe `PipelineContext.knowledgeResult` | ✅ Implementado |

Los **adaptadores** (infraestructura, `com.kinplatform.ai.knowledge.adapter`) implementados en
E4…E5: `HttpKnowledgeSourceAdapter` (APIs/Internet con allowlist de hosts y mitigación SSRF),
`PublicApiConnector` (fuentes públicas, OCP-compliant), `JdbcKnowledgeSource` (base de datos de
conocimiento/caché persistente), `RagKnowledgeSource` (índice vectorial) y
`DocumentKnowledgeSource` (ingestión de documentos), además de `CompositeKnowledgeSource`
(composición de fuentes en orden determinista). Todos implementan el puerto `KnowledgeSource`
(o el puerto `KnowledgeRepository`); el cableado vive en `KinConfig`.

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Pasar el contexto crudo al LLM** para que "recupere" conocimiento | Violenta el principio *Java decide / LLM comunica*; el LLM no valida fuentes ni calcula. Rechazada |
| **RAG asistido por el LLM** (el LLM decide qué recuperar) | La recuperación, el scoring y la selección de fragmentos son decisiones; se ejecutan en Java. El `RagKnowledgeSource` solo devuelve candidatos. Rechazada la variante LLM-dirigida |
| **Conectores acoplados al dominio** (HTTP en `kin.knowledge`) | Rompe la pureza del dominio (frontera ADR-001/1.1 de gobernanza). Rechazada: puertos en dominio, adaptadores en infraestructura |
| **Ampliar el `ProjectContext`** para guardar conocimiento externo | Mezcla estado del proyecto con datos externos verificados; el contexto ya es contrato congelado. Rechazada: caché propia en `KnowledgeRepository` |
| **Búsqueda en vivo sin caché en cada turno** | Latencia, rate limits y costos; sin presupuesto de consultas. Rechazada: cache con TTL y límites |

---

## Consecuencias

### Positivas

- El análisis se enriquece con información externa verificable **sin tocar el pipeline
  congelado** (integración aditiva, patrón ADR-011).
- El principio *Java decide* se extiende a la capa de conocimiento: adquisición, validación,
  selección y caché son 100 % deterministas en el dominio.
- OCP: cada nueva fuente es un adaptador que implementa `KnowledgeSource`; el dominio no cambia.
- Offline-first: sin red (fallback), el motor devuelve `KnowledgeResult.empty()` con motivos y el
  pipeline sigue operando — desarrollo y tests sin conectividad.
- Trazabilidad: cada `KnowledgeFact` lleva origen, URL, fecha, confianza y validación
  (`provenance`), auditable en el reporte.

### Negativas

- Complejidad adicional: un motor, dos puertos nuevos, validación y caché.
- Dependencias externas (APIs públicas) introducen variabilidad y rate limits; mitigado con
  allowlist, timeouts, retry acotado y fallback.
- El beneficio real depende de la calidad de las fuentes; mitigado con `SourceValidator` y
  `SourceTrust`.
- Requiere decisión futura (ADR propia) si se desea citar fuentes dentro del prompt/reporte
  (frontera ADR-012).

---

## Regla que modifica

**ADR aditivo** (implementado en E1…E7): amplía el inventario de la arquitectura con un nuevo
bounded context `kin.knowledge` (+ paquete de infraestructura `ai.knowledge.adapter`) y su
integración aditiva al pipeline (`KnowledgeStage` + `PipelineContext.knowledgeResult`), **sin
modificar** contratos congelados ni las decisiones congeladas #1–#14 de
`BASELINE_ARCHITECTURE.md`. La gobernanza se enmienda para declarar que **Java decide sobre el
conocimiento: las fuentes únicamente aportan conocimiento** (regla de IA §7.2 y decisión
congelada #15).

## Cumplimiento

- **Etapa 1 (este ADR)**: solo documentación (diseño arquitectónico). Sin código.
- **Implementación E2…E7**: aprobada tras la Etapa 1 y ejecutada por etapas. Cierre oficial de
  la Fase 6 con: `kin.knowledge` 100 % POJO, adaptadores 100 % en infraestructura, integración
  aditiva al pipeline, cobertura de dominio ≥ 90 % (JaCoCo), BUILD SUCCESS, **675 tests verdes**
  (0 fallos, 0 errores, 0 skipped) y contratos congelados intactos.
- Criterio de cierre de la fase cumplido: `kin.knowledge` 100 % POJO, cobertura de dominio
  ≥ 90 % (JaCoCo), integración aditiva, BUILD SUCCESS y contratos congelados intactos.
- **Estado del ADR**: **Aprobado** — decisión congelada. Cualquier cambio futuro (p. ej. exponer
  conocimiento citado en la comunicación, frontera ADR-012) exige una ADR propia.

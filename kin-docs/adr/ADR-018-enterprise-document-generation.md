# ADR-018: Bounded Context Enterprise — Generación y exportación de documentos de negocio (Fase 10)

**Estado**: **Aprobado** (Fase 10 — Milestones M2A…M2J implementados y, con este ADR, sancionados;
M3A documentación y M3B activación del ciclo automático forman parte del roadmap M3)
**Fecha**: 2026-08-03
**Autor**: KIN Architecture Team

> **Alcance**: este ADR sanciona el **Bounded Context Enterprise** (Fase 10): un nuevo contexto
> de dominio que genera, versiona, persiste y exporta documentos de negocio de un proyecto KIN
> (lean canvas, plan de mercado, plan financiero, hoja de ruta, matriz de riesgos, KPIs y plan de
> innovación) en formato PDF/DOCX/PPTX, consumiendo el `ProjectContext` durable y los resultados
> del pipeline. También sanciona el **cableado del ciclo automático** (M3B): la conversación
> dispara la generación vía evento de dominio cuando el pipeline completa `REPORT`. Todos los
> cambios son **aditivos** y no modifican ningún contrato congelado de `BASELINE_ARCHITECTURE.md`
> (§4). Principio rector intacto: **Java decide. El LLM únicamente comunica.**

---

## Problema

La plataforma produce un `ConsultingReport` (Fase 5.4) pero no una documentación empresarial
ejecutable (planes, hoja de ruta, KPIs, matriz de riesgos) en formatos exportables. La Fase 10
implementó ese módulo — `kin.enterprise` + `ai.enterprise.adapter` + `kin-enterprise-ui` — pero
con dos brechas críticas:

| # | Brecha | Evidencia |
|---|--------|-----------|
| 1 | **El módulo Enterprise no está sancionado por ADR ni documentado** (no existe en `BASELINE`, `README` ni `CHANGELOG`; la migración Flyway V7 no está documentada). | `kin-docs/` no contiene ninguna referencia a Enterprise/Fase 10; `BASELINE_ARCHITECTURE.md` §4.3 lista "migraciones V1…V4". |
| 2 | **El ciclo automático es código muerto en producción**: `DefaultEnterpriseProjectTrigger` y `EnterpriseProjectRequestedListener` no son beans Spring y `KinConfig` construye el `ConversationOrchestrator` con el constructor de 5 argumentos → `NO_OP_TRIGGER`. Enterprise nunca se dispara desde la conversación. | `KinConfig.java:571-579`, `ConversationOrchestrator.java:52,60`, `DefaultEnterpriseProjectTrigger.java:33`, `EnterpriseProjectRequestedListener.java:34`. |

---

## Contexto

KIN 2.0 Alpha 1 + Fases 5–9 (ADR-006…017) están cerradas. La Fase 10 implementó el módulo
Enterprise en 11 commits (`f9c3b7a`…`410fb8b`) con los milestones:

`M2A Contract Hardening → M2B Aggregate Root → M2C Value Objects → M2D Deterministic Engines →
M2E Generation Service → M2F Event Integration → M2G JPA Persistence → M2H Export Infrastructure →
M2I REST + OpenAPI → M2J Dashboard + SSE + React UI`

El módulo está **aislado** de los contratos congelados: los 8 motores deterministas NUNCA se
registran como `DomainEngine` (decisión de aislamiento del `package-info` de `engine`), no se
modificó `EngineRegistry`, `KinMethod`, `Pipeline` ni las firmas públicas de
`ConversationOrchestrator` (la integración es aditiva vía constructor sobrecargado). El flujo
documentado del pipeline sigue intacto (13 etapas).

El punto de integración aditiva ya existe en el dominio: `ConversationOrchestrator` invoca
`enterpriseTrigger.request(projectId)` cuando `decision.action() == REPORT` y hay
`consultingReport` (`ConversationOrchestrator.java:268-285`), y porta el constructor aditivo de 6
argumentos con `EnterpriseProjectTrigger` (`:84-92`). Lo que falta es exclusivamente el **cableado
de composición** (composition root) — que este ADR sanciona en M3B.

---

## Decisión

### 1. Bounded Context Enterprise

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `com.kinplatform.kin.enterprise` | Aggregate `EnterpriseProject` (máquina `REQUESTED → RUNNING → COMPLETED \| FAILED`, versión `(projectId, version)`), VO con invariantes, 8 motores deterministas puros (sin I/O), eventos `EnterpriseProjectRequested/Generated/Failed`, puertos `EnterpriseProjectRepository` y `DocumentRenderer`, `EnterpriseProjectTrigger` |
| Aplicación | `...enterprise.application` | `EnterpriseGenerationService`, `EnterpriseGenerationOrchestrator`, `EnterpriseExportService/Orchestrator`, `DefaultEnterpriseProjectTrigger`, `EnterpriseProjectRequestedListener`, `EnterpriseRendererFactory`, `ProgressPublishingEnterpriseProjectRepository` |
| Web | `...enterprise.web` | `EnterpriseController` (11 endpoints), `EnterpriseDashboardController`, `EnterpriseProgressController` (SSE), DTOs, `EnterpriseWebConfig` |
| Infraestructura | `com.kinplatform.ai.enterprise.adapter` | `EnterpriseProjectRepositoryAdapter` (JPA), entidades `enterprise_project`/`enterprise_document`, mappers |
| Frontend | `kin-enterprise-ui` (Vite + React) | Dashboard de estado/versiones/documentos, exportación y SSE |

### 2. Aislamiento de los motores Enterprise

Los 8 motores (`DefaultMarketEngine`, `DefaultFinancialPlanEngine`, `DefaultRoadmapEngine`,
`DefaultKpiEngine`, `DefaultRiskPlanEngine`, `DefaultInnovationEngine`,
`DefaultBusinessModelEngine`, `DefaultEnterpriseScoreEngine`) **no implementan `DomainEngine` ni se
registran en `EngineRegistry`**. Son motores de negocio del BC Enterprise, no etapas del pipeline:
`EngineRegistry` (contrato congelado §4.1) queda intacto y no los descubre. Esta decisión se
refleja ya en `EnterpriseWebConfig.java:17-26`.

### 3. Puertos

- `EnterpriseProjectRepository` — `findLatestVersion / findByVersion / findAllVersions / save`.
- `DocumentRenderer` — renderizado binario PDF/DOCX/PPTX (JDK puro, sin librerías).
- `EnterpriseProjectTrigger` — puerto de emisión que la capa de conversación consume
  (`request(UUID)`, no bloqueante, idempotente).

### 4. Cableado del ciclo automático (M3B, sancionado por este ADR)

**Objetivo**: `Conversación → Pipeline → REPORT → EnterpriseProjectTrigger → DomainEventBus →
EnterpriseProjectRequestedListener → EnterpriseGenerationOrchestrator → EnterpriseGenerationService
→ Repository → EnterpriseProjectGenerated`.

Implementación **mínima y aditiva**:

```
┌────────────────────────────┐        ┌──────────────────────────────┐
│ ConversationOrchestrator   │        │ EnterpriseWebConfig (beans)  │
│  (contrato congelado,      │        │                              │
│   constructor aditivo 6 args)──► enterpriseTrigger (DefaultEnterpriseProjectTrigger)
│                            │        │   │                          │
│  turno REPORT + reporte    │        │   ▼                          │
│  ──► trigger.request(id)   │        │ DomainEventBus (existente)   │
└────────────────────────────┘        │   │                          │
                                      │   ▼                          │
                                      │ enterpriseProjectRequestedListener
                                      │   │ executor (dedicado)      │
                                      │   ▼                          │
                                      │ EnterpriseGenerationOrchestrator
                                      │   │                          │
                                      │   ▼                          │
                                      │ EnterpriseGenerationService  │
                                      │   ▼                          │
                                      │ Repository (JPA, @Primary    │
                                      │  decorado con progreso SSE)  │
                                      │   ▼                          │
                                      │ EnterpriseProjectGenerated   │
                                      └──────────────────────────────┘
```

1. **`EnterpriseWebConfig`** (composition root del BC) define tres beans nuevos:
   - `enterpriseGenerationExecutor()` → `Executor` dedicado (pool acotado, threads daemon) para la
     generación asíncrona. No hay consumidores del `applicationTaskExecutor` auto-configurado en el
     proyecto (`@Async`/`@EnableAsync` no se usan), por lo que la definición de un bean `Executor`
     no altera ningún comportamiento existente.
   - `enterpriseProjectTrigger(repository, eventBus)` → `DefaultEnterpriseProjectTrigger`.
   - `enterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, executor)` →
     `EnterpriseProjectRequestedListener` (se suscribe al bus en su constructor).
2. **`KinConfig.conversationOrchestrator(...)`** pasa a inyectar `EnterpriseProjectTrigger` y usa el
   constructor aditivo de 6 argumentos ya existente (`ConversationOrchestrator.java:84-92`). El
   `ResponseFallback` por defecto es idéntico al del constructor de 5 argumentos → comportamiento
   previo intacto cuando no hay trigger.

**Sin cambios**: aggregate, motores, contratos, eventos, `Pipeline`, `EngineRegistry`, firmas
`orchestrate`/`orchestrateStream`, REST/SSE. La idempotencia ya está garantizada por
`EnterpriseGenerationService.generateRequested` (una versión por `(projectId, version)`) y por el
propio trigger (no publica si hay generación en vuelo).

### 5. Persistencia

Migración `V7__create_enterprise_project.sql` (tablas `enterprise_project` y `enterprise_document`)
documentada por este ADR; sincronización de `kin-database/init.sql` y despliegue → M3H.

### 6. Fuera de alcance de este ADR (roadmap M3, milestones posteriores)

- M3C: resultados reales del pipeline en la generación (hoy los 4 resultados llegan `empty()`).
- M3D: persistencia/expuesto del `EnterpriseScore` (hoy se calcula y se descarta).
- M3E: generación de `EXECUTIVE_REPORT`/`DOFA` y narrativa LLM vía `AIResponder` (requerirá
  extensión aditiva de `PromptType`, preservando la frontera ADR-012).
- M3F/G: integración de `kin-enterprise-ui` con `kin-frontend` y acción de generación desde la UI.
- M3H: infraestructura de producción (CORS, Docker, `init.sql`).

> **Nota M3C (implementado)**: los resultados reales del pipeline ya fluyen al BC Enterprise. El
> runtime (`KinMethod`, constructor aditivo) publica los 4 resultados deterministas del turno
> `REPORT` en el puerto `EnterprisePipelineResultStore` (`InMemory...`, correlación por
> `projectId`, consumo único) y `EnterpriseProjectRequestedListener` los fusiona en la
> `EnterpriseGenerationRequest`. Sin resultados (generación manual vía REST o sin turno previo) el
> flujo conserva el modo offline-first. El punto de captura es el runtime, no la conversación:
> `ConversationOrchestrator`, eventos, `Pipeline`, motores y contratos congelados no se modifican.

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Registrar los 8 motores Enterprise como `DomainEngine`** | Rompería el aislamiento y haría que `EngineRegistry` (contrato congelado) los descubriera como etapas del pipeline; los motores Enterprise no son etapas. Aislamiento deliberado. |
| **Mover la capa web fuera de `kin.enterprise.web`** (a `common/config` + `dto`) | El usuario exige no mover paquetes; la excepción a la regla "kin.* POJO puro" se sanciona explícitamente por este ADR para la capa web del BC Enterprise. |
| **Activar el ciclo vía polling o cron** | Viola el principio event-driven del proyecto (§1.8 gobernanza) y duplicaría el flujo por eventos ya diseñado. |
| **Disparar la generación síncrona dentro del turno** | El diseño exige que la conversación termine igual que hoy; la generación es asíncrona (listener + executor). |
| **Añadir un nuevo `Executor` gestionado por Spring** (`ThreadPoolTaskExecutor`) | Ningún consumidor actual del auto-configurado; un pool acotado simple con daemon threads es suficiente y no añade dependencias. |

---

## Consecuencias

### Positivas

- **Enterprise se activa desde el flujo real**: un turno REPORT genera el proyecto empresarial
  automáticamente, con progreso SSE y persistencia durable.
- **Cero cambios en contratos congelados**: solo wiring aditivo (beans + constructor sobrecargado
  existente).
- **Composición explícita**: los beans del BC viven en su composition root (`EnterpriseWebConfig`),
  el dominio sigue siendo POJO puro.
- **Aislamiento preservado**: `EngineRegistry`/`Pipeline` no conocen el módulo Enterprise.
- **Compatibilidad total**: los 1786 tests existentes siguen verdes sin modificar aserciones.

### Negativas

- **La generación automática ejecuta trabajo asíncrono real** en el backend (8 motores + 7
  renderizados + persistencia): riesgo de concurrencia gestionado por idempotencia y pool acotado.
- **Una lectura extra de repositorio** (`findLatestVersion`) en el hilo de la conversación al
  completar un REPORT (lectura ligera `@Transactional(readOnly=true)`).

---

## Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Doble disparo (`POST /generate` + evento) | Media | Idempotencia por `(projectId, version)`: `generateRequested` devuelve la versión existente; el trigger no publica si hay vuelo |
| R2 | Concurrencia en la generación asíncrona | Media | Pool acotado dedicado (`enterpriseGenerationExecutor`), threads daemon; cada `save`/`find` es su propia transacción JPA |
| R3 | Bean `Executor` que desactive el auto-configurado de Spring | Baja | Ningún consumidor del `applicationTaskExecutor`; verificado por grep (`@Async`/`@EnableAsync` no existen) |
| R4 | Fallos silenciosos del listener | Media | `EnterpriseProjectRequestedListener` no propaga: los fallos quedan en el aggregate `FAILED` y en `EnterpriseProjectFailed` (comportamiento pre-existente) |
| R5 | Romper el contexto Spring al añadir beans | Media | Tests de wiring herméticos (factory methods de las configs) + `./mvnw clean verify` completo |

---

## Roadmap M3 (roadmap completo en `kin-docs/AUDITORIA_ENTERPRISE_M3.md`)

| Milestone | Contenido | Estado (este ADR) |
|-----------|-----------|-------------------|
| **M3A** | ADR-018 + documentación (README, CHANGELOG, diagramas) | ✅ **Completada en este ADR** |
| **M3B** | Activación del ciclo automático (beans + wiring) | ✅ **Implementado con este ADR** |
| **M3C** | Resultados reales del pipeline en la generación | ✅ **Implementado (runtime → `EnterprisePipelineResultStore` → listener)** |
| **M3D** | Enterprise Score persistido y expuesto | ✅ **Implementado (aggregate → `@Embedded` JPA → dashboard/REST)** |
| **M3E** | EXECUTIVE_REPORT/DOFA + narrativa LLM | ✅ **Implementado (AIResponder → `EnterpriseNarrativeGenerator` → 9 documentos)** |
| **M3F** | Integración UI en kin-frontend | ✅ **Implementado (ruta `/dashboard/projects/[id]/enterprise`, standalone eliminado)** |
| **M3G** | Acción de generación desde la UI | ✅ **Implementado (botón Generar + POST async + SSE + auto-refresh)** |
| M3H | Infraestructura de producción | ⏳ Pendiente |

---

## Criterios de aceptación (M3A + M3B)

- [x] ADR-018 en estado **Aprobado**; README y CHANGELOG reflejan la Fase 10 y el módulo Enterprise.
- [x] `DefaultEnterpriseProjectTrigger` y `EnterpriseProjectRequestedListener` registrados como beans
      Spring en `EnterpriseWebConfig` con un `Executor` dedicado.
- [x] `KinConfig.conversationOrchestrator` inyecta el `EnterpriseProjectTrigger` real (constructor
      aditivo de 6 argumentos); el `NO_OP_TRIGGER` deja de usarse en producción.
- [x] Un turno REPORT completo dispara `EnterpriseProjectRequested` → listener → generación
      asíncrona → `EnterpriseProjectGenerated`, sin doble generación (idempotencia).
- [x] Ningún contrato congelado modificado: `Pipeline`, `EngineRegistry`, `KinMethod`, aggregate,
      eventos, motores y firmas `orchestrate/orchestrateStream` intactos.
- [x] `./mvnw clean verify` → **BUILD SUCCESS** con **1786+ tests** (0 failures/errors) y cobertura
      de dominio ≥ 90 % en los paquetes afectados (JaCoCo).

---

## Estado

**APROBADO** — El BC Enterprise (Fase 10) queda sancionado y documentado, y el ciclo automático
(M3B) queda implementado con cableado puramente aditivo. Este ADR **NO modifica contratos
congelados**. La Fase 10 queda **oficialmente sancionada** y el roadmap M3 continúa con M3C.

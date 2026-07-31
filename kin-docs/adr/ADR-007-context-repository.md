# ADR-007: ContextRepository (puerto) + adaptador JPA durable

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: En KIN 2.0 Alpha 1 el `ProjectContext` de cada conversación vivía en `ProjectContextService` (un `ConcurrentHashMap<UUID, ProjectContext>` en `ai/context/`). Consecuencias: el contexto se perdía al reiniciar el backend, no era trazable ni auditable, y el pipeline dependía de una infraestructura concreta (violando la dirección de dependencias de la arquitectura limpia). El pipeline (vía `KinMethod`) necesita un estado durable del proyecto para la fase 5.3 (Knowledge, Opportunity, Report) y para ofrecer contexto real entre sesiones.

**Decisión**: Introducir el puerto `ContextRepository` en el dominio (`com.kinplatform.kin.context`) y un adaptador JPA durable:

- **Puerto** `ContextRepository`: `findOrCreate(projectId, title, description, category)`, `find(projectId)`, `save(projectId, context)`, `delete(projectId)`.
- **`ProjectContext.restore(data, covered, decision, exchangeCount, reportGenerated)`**: factory de dominio que reconstruye el contexto persistido sin pasar por `update(...)` (que incrementa el contador de intercambios) ni por `fromProject(...)` (que siembra datos).
- **Adaptador JPA** (`com.kinplatform.ai.context.adapter`):
  - `ProjectContextEntity` — entidad 1:1 con `projects.id` (`project_id` PK, `context_data` TEXT, `updated_at`).
  - `ProjectContextJpaRepository` — Spring Data.
  - `JpaContextRepository` — serializa el estado del dominio como JSON (DTO privado `ProjectContextData`) mediante `ObjectMapper`; el formato de persistencia no acopla el dominio.
- **Esquema**: migración Flyway `V3__create_project_context.sql` para producción (PostgreSQL) y tabla `project_context` agregada a `kin-database/init.sql`. En dev (H2) se usa `ddl-auto: update`, por lo que la tabla se auto-crea sin Flyway.

`ProjectContextService` y su cableado (`ContextAnalyzerPort` en el service) se eliminan; el ciclo de vida del contexto queda bajo el repositorio.

**Alternativas consideradas**:

1. *Adapter en memoria (mantener `ConcurrentHashMap` detrás del puerto)* — Rechazado: el objetivo declarado de la fase es "dejar el contexto preparado para persistencia real"; un adaptador en memoria solo cambia la forma, no la sustancia.
2. *Persistir el objeto de dominio directamente (JPA en `kin.context`)* — Rechazado: viola la regla congelada nº 1 ("kin/ es 100 % POJO"); la entidad y la serialización son responsabilidad del adaptador.
3. *No persistir y reconstruir desde el historial de mensajes* — Rechazado: el historial no captura decisiones, dimensiones cubiertas ni contadores; duplicaría el estado.

**Consecuencias**:
- Positivas: contexto durable entre reinicios; el pipeline depende de una abstracción; aditivo para futuros adaptadores (Redis, Postgres nativo, etc.) sin tocar el dominio; FK con `ON DELETE CASCADE` al eliminar un proyecto.
- Negativas: nueva tabla (migración en prod, DDL automático en dev); la serialización JSON debe versionarse si cambia el estado del dominio.

**Regla que modifica**: Inventario del `BASELINE_ARCHITECTURE.md` (v2.0.0-alpha.1) — `ProjectContextService` se elimina; se agrega el puerto `ContextRepository` y el adaptador JPA.

**Cumplimiento**: Sin cambios en REST, SSE, eventos ni contratos de dominio públicos. El `ProjectContext` conserva su API de actualización y consulta.

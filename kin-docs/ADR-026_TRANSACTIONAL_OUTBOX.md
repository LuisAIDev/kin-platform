# ADR-026: Transactional Outbox Pattern para Domain Events

## Estado
**Aceptado** — 2026-08-25

## Contexto
KIN Platform utiliza un bus de eventos en memoria (`InMemoryDomainEventBus`) para desacoplar
la escritura de eventos de dominio de su procesamiento. Eventos como `ReportGeneratedEvent`,
`KnowledgeAcquiredEvent` o `ConversationCompletedEvent` se publican dentro de la misma
transacción que el caso de uso (ej. `KinMethod.execute()`).

**Problema**: Si el listener falla o la infraestructura de mensajería (futuro Kafka/Pulsar)
está caída, el evento se pierde. No hay reintento ni persistencia duradera. Además,
el patrón actual obliga a que todos los listeners sean síncronos y rápidos.

**Requisitos**:
1. **Atomicidad**: El evento debe persistirse en la misma transacción que el caso de uso.
2. **Durabilidad**: El evento debe sobrevivir a reinicios/crashes del backend.
3. **Reintento**: Fallos transitorios en la publicación deben reintentarse automáticamente.
4. **Observabilidad**: Métricas de latencia, pendientes, fallos, dead-letter.
5. **Feature flag**: Deshabilitable para tests que no usan BD.

## Decisión
Implementar el **Transactional Outbox Pattern**:

1. **Tabla `domain_event_outbox`** (PostgreSQL) con campos: `id`, `aggregate_id`, `event_type` (FQCN),
   `payload` (JSONB), `metadata` (JSONB), `status` (PENDING/PUBLISHED/FAILED/DEAD_LETTER),
   `retry_count`, `created_at`, `published_at`, `last_error`.

2. **Puerto `OutboxEventPublisher`** (dominio puro, sin Spring):
   - `publish(DomainEvent event)`: valida `null` y `aggregateId`, lanza excepciones claras.
   - Se invoca **dentro** de la transacción del caso de uso.

3. **Adaptador `TransactionalOutboxEventPublisher`** (infraestructura):
   - Usa `JdbcTemplate` + `ObjectMapper` (con `JavaTimeModule`).
   - Verifica `TransactionSynchronizationManager.isActualTransactionActive()`.
   - Serializa evento completo (polimórfico) a JSONB.
   - Respeta `kin.outbox.enabled` (default `true`).

4. **Relé (`OutboxRelay`)** — *Implementado en PR 2*:
   - Clase `OutboxRelay` en `com.kinplatform.kin.infrastructure.outbox`.
   - Polling programado con `@Scheduled(fixedDelayString = "${kin.outbox.relay.poll-interval-ms:2000}")`.
   - Procesamiento por lotes usando `SELECT ... FOR UPDATE SKIP LOCKED ORDER BY created_at LIMIT ?`.
   - Reintentos con backoff exponencial (configurable via `kin.outbox.relay.backoff-base-ms`).
   - Dead-letter tras `maxRetries` (configurable via `kin.outbox.relay.max-retries`, default 5).
   - Métricas Micrometer:
     - `kin.outbox.published` (Counter)
     - `kin.outbox.failed` (Counter)
     - `kin.outbox.dead_letter` (Counter)
     - `kin.outbox.pending` (Gauge)
     - `kin.outbox.relay.duration` (Timer)
   - Logging estructurado con `correlationId` y `userId` extraídos del metadata.
   - Feature flag `kin.outbox.relay.enabled` (default `true`).
   - Configuración via `OutboxRelayProperties` (prefijo `kin.outbox.relay`).

## Alternativas Consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Outbox Table (elegida)** | Atomicidad transaccional, simple, sin dependencias externas | Polling latency (ms-seg), requiere tabla | ✅ |
| **Kafka Transactional Producer** | Nativo, exactly-once | Complejidad operacional, overkill para escala actual | ❌ |
| **Event Sourcing completo** | Auditoría total, replay | Cambio masivo de arquitectura, curva de aprendizaje | ❌ |
| **Dual Write (DB + Bus)** | Simple | **No atómico** — race condition, pérdida de eventos | ❌ |

## Consecuencias

**Positivas**:
- Garantía **at-least-once** sin perder eventos.
- Listeners pueden ser lentos/fallar sin bloquear la transacción del usuario.
- Base para migración futura a Kafka (cambiar solo el relé).
- Tests de integración pueden verificar persistencia atómica.

**Negativas/Riesgos**:
- Latencia de publicación = `poll-interval` (default 2s). Aceptable para eventos internos.
- Duplicados posibles (at-least-once). Listeners deben ser idempotentes (usar `event.id`/`correlationId`).
- Tabla crece si relé falla. Índice parcial `WHERE status='PENDING'` mitiga scans.

## Integración en Pipeline (PR 3)

La integración en el pipeline se realizó mediante:

1. **`KinMethod`** (`com.kinplatform.kin.KinMethod`):
   - Inyección de `OutboxEventPublisher` (opcional, `null` si deshabilitado).
   - Método `publish(List<DomainEvent>)` modificado para:
     - Publicar en outbox transaccional vía `OutboxEventPublisher.publish(event)` (at-least-once).
     - Si `outboxEventPublisher` es `null` (feature flag `kin.outbox.enabled=false`), fallback a `eventBus.publish()` directo.
     - Para eventos que requieren entrega síncrona inmediata (SSE), también publicar en `DomainEventBus` tras guardar en outbox. Actualmente: `ReportGeneratedEvent`.
   - Constructores actualizados para inyectar `OutboxEventPublisher` (opcional, default `null`).

2. **`KinConfig`** (`com.kinplatform.common.config.KinConfig`):
   - Bean `kinMethod` actualizado para inyectar `OutboxEventPublisher`.

3. **Behavior**:
   - Con `kin.outbox.enabled=true` (default): eventos se guardan en tabla `domain_event_outbox` dentro de la misma transacción del caso de uso. El relé los procesa asíncronamente.
   - Con `kin.outbox.enabled=false`: fallback a publicación directa en `DomainEventBus` (comportamiento legacy).
   - Eventos `ReportGeneratedEvent` se publican también en `DomainEventBus` para mantener la entrega síncrona a SSE sin latencia.

## Plan de Migración (PRs)

| PR | Alcance |
|----|---------|
| **PR 1** | Tabla, Puerto, Adaptador JdbcTemplate, Tests unitarios, Config, ADR |
| **PR 2 (completado)** | `OutboxRelay` (scheduled, `FOR UPDATE SKIP LOCKED`, métricas, reintentos, dead-letter) |
| **PR 3 (completado)** | Integración en `KinMethod` (reemplazar `DomainEventBus` directo por `OutboxEventPublisher`) |
| **PR 4 (completado)** | Idempotencia en listeners, DLQ Admin API, Dead Letter Queue UI |
| **PR 5 (completado)** | UI DLQ Frontend + E2E Playwright, idempotencia extendida |

| **PR 4 (completado)** | Idempotencia en listeners, DLQ Admin API, Dead Letter Queue UI |
106: 
107: ## Idempotencia y Dead Letter Queue (PR 4)
108: 
109: La semántica *at-least-once* del outbox implica que los listeners pueden recibir eventos duplicados.
109: Para evitar procesamiento duplicado, se implementa idempotencia a nivel de listener:
110: 
110: 1. **Tabla `processed_events`** (V20): almacena `event_id` (hash o correlationId), `aggregate_id`, `event_type`, `processed_at`.
111: 2. **`IdempotencyService`** (`com.kinplatform.kin.eventbus.IdempotencyService`):
111:    - `tryMarkProcessed(eventId, aggregateId, eventType)`: verifica e inserta atómicamente.
112:    - Listeners invocan `tryMarkProcessed` antes de procesar; si retorna `false`, el evento ya fue procesado.
112: 3. **Listener con idempotencia** (`EnterpriseProjectRequestedListener`):
112:    - Inyecta `IdempotencyService` (opcional).
113:    - Usa `correlationId` del evento o genera uno basado en `projectId + version`.
114:    - Ignora duplicados silenciosamente (log DEBUG).
115: 
115: **Dead Letter Queue (DLQ) — Endpoints Admin:**
116: 
116: | Endpoint | Método | Descripción |
117: |----------|--------|-------------|
118: | `/admin/outbox/dead-letter` | GET | Listar eventos DEAD_LETTER (paginado, filtros `aggregateId`, `eventType`). |
119: | `/admin/outbox/dead-letter/{id}/retry` | POST | Reencolar: status → PENDING, `retry_count` = 0. |
120: | `/admin/outbox/dead-letter/{id}/delete` | POST | Eliminar evento específico. |
120: | `/admin/outbox/dead-letter` | DELETE | Eliminar todos (requiere `confirm=true`). |
121: | `/admin/outbox/dead-letter/stats` | GET | Estadísticas: total, por tipo, top agregados. |
121: 
122: - Seguridad: `@PreAuthorize("hasRole('ADMIN')")`.
122: - Solo en perfiles `prod`/`staging` (deshabilitado en `test` por defecto).
123: - Métricas: `kin.outbox.dead_letter` (Counter), `kin.outbox.failed` (Counter).
123: 
123: ## UI Dead Letter Queue y Pruebas E2E (PR 5)

Se implementa una interfaz de usuario completa para la gestión de la Dead Letter Queue en `/admin/outbox/dead-letter`:

**Página DLQ (`/dashboard/admin/outbox/dead-letter`):**
- Listado paginado con columnas: id, aggregate_id, event_type, created_at, last_error, retry_count.
- Botón "Reencolar" (POST `/admin/outbox/dead-letter/{id}/retry`): reenvía evento a PENDING.
- Botón "Eliminar" por evento (DELETE `/admin/outbox/dead-letter/{id}`).
- Botón "Eliminar todos" con doble confirmación (DELETE `/admin/outbox/dead-letter`).
- Filtros por `event_type` y `aggregate_id`.
- Paginación configurable (default 50 por página).
- Protección ADMIN: `@PreAuthorize("hasRole('ADMIN')")`, middleware de autenticación en proxy.ts.

**Pruebas E2E (Playwright):**
- `admin-dlq.spec.ts`: login admin, navegar a DLQ, reencolar, eliminar, verificar permisos.
- `admin-permissions.spec.ts`: acceso denegado sin rol ADMIN.

**Extensión de idempotencia (opcional):**
- `IdempotencyService` extendido a otros listeners críticos (`ReportGeneratedEvent` listener).
- Eventos incluyen `correlationId` para trazabilidad.

## Referencias

- [Transactional Outbox Pattern (Microsoft)](https://learn.microsoft.com/en-us/azure/architecture/reference-architectures/event-driven/transactional-outbox)
- [Outbox Pattern — Event Driven Architecture (Confluent)](https://www.confluent.io/blog/transactional-outbox-pattern-event-driven-microservices/)
- ADR-014 (Knowledge Engine), ADR-018 (Enterprise Bounded Context)
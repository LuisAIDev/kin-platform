# ADR-033: Seguimiento de pacientes (planes, tareas y evolución) — KIN Health

## Estado
**Aceptado** — 2026-08-30

## Contexto

KIN Health ya cuenta con la **Relación médico-paciente con estados** (ADR-031/V30), los
**permisos granulares por relación** (Área 5, `RelationshipAccessValidator`) y las
**notificaciones en la plataforma** (badges). El siguiente paso de la hoja de ruta KIN Salud 2.0
es el **Seguimiento (Área 9)**: que el médico cree planes de seguimiento, asigne tareas, registre
la evolución del paciente y se generen recordatorios automáticos con entrega real dentro de la
plataforma.

Principios aplicados (intactos):
- **Java decide. El LLM únicamente comunica.** Todo el cálculo (vencimientos, recordatorios,
  alertas de evolución) es determinista en Java.
- **Aditividad**: nuevos bounded context (`kin.health.followup`), tablas nuevas (V31) y una etapa
  de seguimiento sin tocar contratos congelados.
- **Permisos por relación**: toda operación sobre datos de un paciente exige relación `ACTIVE`
  (Área 5).

## Decisión

Crear un bounded context `com.kinplatform.kin.health.followup` (Clean Architecture + DDD):

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.followup.domain` | `FollowUpPlan` (título, descripción, ventana, frecuencia `DAILY/WEEKLY/MONTHLY`, estado `ACTIVE/PAUSED/COMPLETED`), `FollowUpTask` (descripción, vencimiento, estado `PENDING/COMPLETED/OVERDUE`, `reminderSent` para deduplicar recordatorios), `PatientEvolution` (síntomas, `vitals` JSONB flexible, adherencia, notas), `FollowUpPlanWithTasks` |
| Puertos | `kin.health.followup.port` | `FollowUpPlanRepository`, `FollowUpTaskRepository`, `PatientEvolutionRepository` |
| Adaptadores | `kin.health.followup.adapter` | Entidades JPA + repos Spring Data + mappers (tablas `follow_up_plans`/`follow_up_tasks`/`patient_evolutions`, Flyway V31; `vitals` como JSONB con ObjectMapper, patrón `patient_profiles`) |
| Servicio | `kin.health.followup.api` | `FollowUpService` |
| Config | `kin.health.followup.config` | `FollowUpProperties` (`enabled`, `reminder-days-before`, `evolution-alert-days`) + `FollowUpConfig` (con `@EnableScheduling`) |
| Scheduler | `kin.health.followup.infrastructure` | `FollowUpReminderScheduler` (diario 08:00) |
| API | `kin.health.followup.api` | `FollowUpController` |

### Servicio (`FollowUpService`)

- `createPlan(physicianId, patientId, title, description, frequency, startDate, endDate)`:
  valida relación `ACTIVE`, crea el plan y las **tareas iniciales** según la frecuencia
  (DAILY → 7 días, WEEKLY → 4 semanas, MONTHLY → 3 meses). Emite `FollowUpPlanCreatedEvent`.
- `addTask(physicianId, planId, description, dueDate)`: solo el médico dueño del plan, con
  relación `ACTIVE`. Emite `FollowUpTaskAddedEvent`.
- `completeTask(taskId, userId)`: el paciente o el médico implicado (validado por
  `plan.involves(userId)`); el médico exige relación `ACTIVE`. Emite `FollowUpTaskCompletedEvent`.
- `recordEvolution(physicianId, patientId, symptoms, vitals, medicationAdherence, notes)`:
  valida relación `ACTIVE`. Emite `PatientEvolutionRecordedEvent`.
- Consultas: `listPlansForPatient` (médico, relación ACTIVE), `listActivePlansForPatient`
  (paciente), `listOverdueTasks` (médico), `getPatientEvolution` (médico),
  `getOwnEvolution` (paciente), `pendingTaskCountForPatient` (notificaciones).

Los eventos se publican de forma **transaccional** vía `OutboxEventPublisher` (fallback al bus en
memoria), mismo patrón que `RelationshipService`.

### Recordatorios automáticos (scheduler)

`FollowUpReminderScheduler` (diario, `@Scheduled(cron = "0 0 8 * * *")`):
1. **Marca vencidas**: `PENDING` con `dueDate < now` → `OVERDUE`.
2. **Crea recordatorios dentro de la plataforma**: tareas `PENDING` con `reminderSent=false` que
   vencen en la ventana `[now, now + reminder-days-before]` → inserta un `Reminder` (reutiliza la
   tabla/dominio `reminders` del dashboard, tipo `GENERAL`) y marca `reminderSent=true`
   (deduplicación).
3. **Alerta de evolución sin registrar**: por cada médico con planes `ACTIVE`, si un paciente no
   registra evolución en `evolution-alert-days` días y no existe una alerta
   `EVOLUTION_STALE` pendiente, crea una `ClinicalAlert` (severidad `MEDIA`, tipo
   `EVOLUTION_STALE` — nuevo valor aditivo del enum) que aparece en el panel del médico.

**Nota**: `FollowUpConfig` habilita `@EnableScheduling`, que como efecto secundario activa el
`OutboxRelay` (ADR-026, ya definido con `@Scheduled` pero sin programación a nivel de aplicación).

### Seguridad

- `SecurityConfig`: `/health/followup/**` → `PATIENT`/`PHYSICIAN`/`ADMIN`.
- Las operaciones sobre un paciente exigen relación `ACTIVE` (`RelationshipAccessValidator`).
- Las operaciones de médico exigen rol `PHYSICIAN`/`ADMIN` (403 en otro caso).
- El paciente opera solo sobre sus propios datos (userId del JWT).

### API REST

**Médico**:
- `POST /api/v1/health/followup/plans` — crear plan.
- `POST /api/v1/health/followup/plans/{planId}/tasks` — añadir tarea.
- `GET /api/v1/health/followup/patients/{patientId}/plans` — planes de un paciente.
- `POST /api/v1/health/followup/patients/{patientId}/evolution` — registrar evolución.
- `GET /api/v1/health/followup/patients/{patientId}/evolution` — historial de evolución.
- `GET /api/v1/health/followup/tasks/overdue` — tareas vencidas.

**Paciente**:
- `GET /api/v1/health/followup/plans/active` — planes activos.
- `POST /api/v1/health/followup/tasks/{taskId}/complete` — completar tarea.
- `GET /api/v1/health/followup/evolution` — propio historial (solo lectura).

### Notificaciones

`NotificationCountsService` suma dos contadores: `pendingTasks` (paciente) y `overdueTasks`
(médico), alimentando los badges del sidebar de "Seguimiento".

## Alternativas consideradas

| Opción | Decisión |
|--------|----------|
| **Recordatorio en la creación de la tarea** (listener por evento) | ❌ Se usa el scheduler diario con `reminder_sent` (deduplicación robusta, sin duplicados por reintentos) |
| **Evolución con campos fijos por especialidad** | ❌ Se usa `vitals` como JSONB flexible (configurable por especialidad sin migraciones) |
| **Tabla `reminders` nueva para seguimiento** | ❌ Se reutiliza la tabla/dominio `reminders` existente (tipo `GENERAL`) |

## Consecuencias

**Positivas**: seguimiento completo con permisos por relación; recordatorios y alertas
deterministas dentro de la plataforma; badges actualizados; extensible (nueva especialidad = más
campos en `vitals`, sin código).

**Negativas**: nuevo BC (+3 tablas, +3 puertos, +3 adaptadores, servicio, scheduler y controlador);
`@EnableScheduling` activa también el `OutboxRelay` (comportamiento deseado/previsto); el scheduler
diario añade un job programado.

## Referencias

- `kin-docs/adr/ADR-031_PHYSICIAN_PORTAL.md` (relación médico-paciente y permisos)
- `kin-docs/adr/ADR-026_TRANSACTIONAL_OUTBOX.md` (publicación de eventos)
- Migración `V31__create_followup_tables.sql`

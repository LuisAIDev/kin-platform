# ADR-034: Agenda y disponibilidad de médicos — KIN Health

## Estado
**Aceptado** — 2026-08-30

## Contexto

KIN Health ya cuenta con la **relación médico-paciente con estados** (ADR-031/V30), los
**permisos granulares** (Área 5), el **seguimiento** (ADR-033) y las **notificaciones** en la
plataforma. El siguiente paso de la hoja de ruta KIN Salud 2.0 es la **Agenda real (Área 10)**:
disponibilidad semanal del médico, gestión de slots, reserva de citas con validación de
traslapes y recordatorios automáticos.

Principios aplicados (intactos):
- **Java decide. El LLM únicamente comunica.** Todo el cálculo (slots, traslapes, recordatorios)
  es determinista en Java.
- **Aditividad**: nuevo bounded context (`kin.health.scheduling`) y **ampliación aditiva de la
  tabla `appointments` existente** (V32) para mantener la compatibilidad con el flujo de
  telemedicina.
- **Permisos por relación**: toda operación sobre un paciente exige relación `ACTIVE` (Área 5).

## Decisión

Crear un bounded context `com.kinplatform.kin.health.scheduling` (Clean Architecture + DDD):

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.scheduling.domain` | `PhysicianAvailability` (día de la semana, `startTime`/`endTime`, `slotDurationMinutes`, `active`) |
| Puertos | `kin.health.scheduling.port` | `PhysicianAvailabilityRepository` |
| Adaptadores | `kin.health.scheduling.adapter` | Entidad JPA + Spring Data + adaptador (tabla `physician_availabilities`, V32) |
| Servicio | `kin.health.scheduling.api` | `SchedulingService` |
| Config | `kin.health.scheduling.config` | `SchedulingProperties` + `SchedulingConfig` (la programación ya la habilita `FollowUpConfig`, ADR-033) |
| Scheduler | `kin.health.scheduling.infrastructure` | `AppointmentReminderScheduler` (diario 08:00) |
| API | `kin.health.scheduling.api` | `SchedulingController` |

**Cita (`Appointment`)**: se **amplía la entidad/dominio de telemedicina existente** (misma tabla
`appointments`) de forma aditiva con `durationMinutes`, `rescheduledFrom`, `cancellationReason`,
`availabilitySlotId` y `reminderSent`, y el estado `REPROGRAMADA`. El factory de 7 argumentos
original se conserva (compatibilidad).

### Servicio (`SchedulingService`)

**Disponibilidad**:
- `setAvailability(physicianId, dayOfWeek, start, end, slotDuration, active)`: upsert por
  día; emite `AvailabilityUpdatedEvent`.
- `getAvailability(physicianId)` / `removeAvailability(physicianId, availabilityId)`.

**Slots**:
- `getAvailableSlots(physicianId, date)`: genera los slots del día según la duración y
  **excluye los ocupados** por citas abiertas (`PENDIENTE`/`CONFIRMADA`).
- `getAvailableSlotsForPatient(patientId, physicianId, date)`: idem validando relación `ACTIVE`.

**Citas** (todas validan relación `ACTIVE` y **traslapes**):
- `requestAppointment` (→ `PENDIENTE`, emite `AppointmentRequestedEvent`).
- `confirmAppointment` (médico dueño, → `CONFIRMADA`, emite `AppointmentConfirmedEvent`).
- `cancelAppointment` (paciente o médico, con motivo, emite `AppointmentCanceledEvent`).
- `rescheduleAppointment`: valida el nuevo slot, marca la original `REPROGRAMADA` y crea la nueva
  con `rescheduledFrom` (emite `AppointmentRescheduledEvent`).
- `completeAppointment` (→ `COMPLETADA`).
- `upcomingAppointments` / `appointmentHistory` (por rol, aislamiento por `userId`).

### Recordatorios automáticos (`AppointmentReminderScheduler`)

Diario (08:00):
1. **Citas confirmadas en las próximas 48 h** → crea un `Reminder` dentro de la plataforma
   (reutiliza la tabla `reminders`) y envía un correo al paciente
   (`EmailSender.sendAppointmentReminderEmail`); deduplicado con `reminder_sent`.
2. **Citas `PENDIENTE` sin confirmar tras `confirmation-reminder-hours`** → recuerda al médico
   (crea un `Reminder` deduplicado por marcador `[id]`).

### Seguridad

- `SecurityConfig`: `/health/scheduling/**` → `PATIENT`/`PHYSICIAN`/`ADMIN`.
- Operaciones sobre un paciente exigen relación `ACTIVE`; el paciente solo ve sus citas
  (userId del JWT); el médico solo gestiona sus citas y pacientes activos.

### API REST

**Médico**: `POST /health/scheduling/availability`, `GET .../availability`,
`DELETE .../availability/{id}`, `GET .../patients/{patientId}/slots?date=`,
`PUT .../appointments/{id}/confirm|complete`.

**Paciente**: `GET .../physicians/{physicianId}/slots?date=`,
`POST .../appointments/request`, `PUT .../appointments/{id}/cancel|reschedule`.

**Ambos**: `GET .../appointments/upcoming|history`.

## Alternativas consideradas

| Opción | Decisión |
|--------|----------|
| **Tabla de citas nueva para agenda** | ❌ Se amplía la tabla `appointments` existente (V32) para mantener compatibilidad con telemedicina |
| **Estado `RESCHEDULED` en inglés** | ❌ Se usa `REPROGRAMADA` (idioma de los estados existentes de `AppointmentStatus`) |
| **Validación de traslapes por rango fijo** | ❌ Se consulta una ventana ±4 h y se valida el solapamiento real en Java |

## Consecuencias

**Positivas**: agenda real con disponibilidad semanal, slots y validación de traslapes;
recordatorios automáticos (plataforma + correo) con deduplicación; badges actualizados;
compatibilidad total con el flujo de citas de telemedicina.

**Negativas**: ampliación de la entidad `Appointment` (nuevos campos) y de la tabla
`appointments` (migración V32); nuevo BC con 2 tablas; scheduler diario adicional.

## Referencias

- `kin-docs/adr/ADR-032_TELEMEDICINE.md` (citas de telemedicina)
- `kin-docs/adr/ADR-031_PHYSICIAN_PORTAL.md` (permisos por relación, Área 5)
- Migración `V32__extend_appointments_and_availability.sql`

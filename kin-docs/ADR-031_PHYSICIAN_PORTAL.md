# ADR-031: Portal para Médicos (KIN Health)

## Estado
**Aceptado** — 2026-08-26

## Contexto

KIN Health cuenta con el **Triaje Digital** (ADR-028), el **Diagnóstico Diferencial** (ADR-029) y el **Dashboard del Paciente** (ADR-030). El siguiente paso es extender la plataforma a los **profesionales de la salud**: un portal donde el médico vea sus pacientes asignados, el historial de triajes y diagnósticos, un resumen clínico y alertas automáticas cuando un paciente presenta un triaje de **alta urgencia** (p. ej. síntomas de infarto).

Debe integrarse sin romper los módulos existentes, siguiendo Clean Architecture + DDD, la filosofía **"Java decide. El LLM únicamente comunica."** y con **aislamiento estricto** (un médico solo ve pacientes asignados a él).

## Decisión

Crear un bounded context `com.kinplatform.kin.health.physician` con:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.physician.domain` | `PatientSummary`, `ClinicalAlert` (tipo/severidad/estado), `PhysicianPatientAssignment` |
| Puertos | `kin.health.physician.port` | `PhysicianPatientRepository`, `ClinicalAlertRepository` |
| Adaptadores | `kin.health.physician.adapter` | Entidades JPA + repos + `JpaPhysicianPatientRepository` + `JpaClinicalAlertRepository` |
| API | `kin.health.physician.api` | `PhysicianController`, `PhysicianAdminController`, `PhysicianService`, DTOs |
| Config | `kin.health.physician.config` | `PhysicianProperties`, `PhysicianConfig`, `ClinicalAlertEventListener` |

### Modelo de datos (Flyway V25)

- `physician_patient_assignments(physician_id, patient_id, assigned_at)` — clave compuesta, FK a `users`.
- `clinical_alerts(id, patient_id, physician_id, type, severity, message, status, created_at, acknowledged_at)` — con CHECK en `severity`/`status`.

### Generación de alertas (determinista)

- `TriagePerformedEvent` (ADR-028) se extiende aditivamente con `maxUrgency` y `conditionNames`.
- `ClinicalAlertEventListener` (config) se suscribe al `DomainEventBus` y, cuando `maxUrgency == ALTA`, invoca `PhysicianService.createHighUrgencyAlerts(patientId, symptoms, conditions)`.
- La alerta se crea para **cada médico asignado** al paciente. Java decide la generación; el LLM nunca participa.
- El médico puede marcar una alerta como atendida (`POST /alerts/{alertId}/acknowledge` → `ACKNOWLEDGED`).

### Servicio de aplicación (PhysicianService)

- `listPatients(physicianId, pageable)`: pacientes asignados con resumen (nombre, condiciones activas, último triaje, alertas activas).
- `patientSummary(physicianId, patientId)` / `patientHistory(...)`: solo si el paciente está asignado (**aislamiento estricto**; 404 en caso contrario).
- `activeAlerts(physicianId)`, `acknowledgeAlert(physicianId, alertId)`.
- `assignPatient(physicianId, patientId)`: MVP, asignación manual por administrador.
- `createHighUrgencyAlerts(...)`: llamada por el listener.

### Asignación de pacientes

- MVP: endpoint ADMIN `POST /api/v1/admin/health/physician/assign`. Futuro: automática por región/especialidad.

### Seguridad y cumplimiento

- Rol `PHYSICIAN` añadido a `UserRole`. Endpoints `/health/physician/**` restringidos a `PHYSICIAN`/`ADMIN`.
- Aislamiento estricto por `userId` (desde la autenticación, nunca del body).
- Logs de auditoría de accesos en los endpoints del controlador.
- **Consentimiento del paciente**: documentado como requisito para producción (el MVP asume pacientes de la plataforma); ver "Consecuencias".

## API REST

| Endpoint | Método | Descripción | Seguridad |
|----------|--------|-------------|-----------|
| `/api/v1/health/physician/patients` | GET | Pacientes asignados al médico (paginado) | JWT (PHYSICIAN/ADMIN) |
| `/api/v1/health/physician/patients/{patientId}/summary` | GET | Resumen clínico del paciente | JWT (PHYSICIAN/ADMIN) |
| `/api/v1/health/physician/patients/{patientId}/history` | GET | Historial de triajes del paciente | JWT (PHYSICIAN/ADMIN) |
| `/api/v1/health/physician/alerts` | GET | Alertas activas del médico | JWT (PHYSICIAN/ADMIN) |
| `/api/v1/health/physician/alerts/{alertId}/acknowledge` | POST | Marcar alerta como atendida | JWT (PHYSICIAN/ADMIN) |
| `/api/v1/admin/health/physician/assign` | POST | Asignar paciente a médico | ADMIN |

## Configuración (feature flags)

```yaml
kin:
  health:
    physician:
      enabled: ${KIN_HEALTH_PHYSICIAN_ENABLED:true}
```

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Servicio + listener de eventos (elegida)** | Alertas deterministas vía DomainEvents, desacoplado | Requiere extender el evento de triaje | ✅ |
| **Alertas por polling del dashboard** | Simple | Acopla y añade latencia | ❌ |
| **Resumen clínico duplicado en el portal** | Aislado | Duplica lógica del dashboard | ❌ |
| **Asignación automática por especialidad** | Escalable | Fuera del MVP | Futuro |

## Consecuencias

**Positivas**:
- Portal completo para médicos con pacientes asignados y resumen clínico.
- Alertas automáticas y deterministas de alta urgencia (infarto, etc.).
- Aislamiento estricto y datos confidenciales.
- `TriagePerformedEvent` extiende su contrato aditivamente (constructor de compatibilidad preservado).
- Módulo aislado y aditivo: no modifica los módulos de triaje/diferencial/dashboard.

**Negativas / límites**:
- La asignación es manual (MVP); el consentimiento del paciente no se modela todavía (requisito de producción).
- No hay mensajería médico-paciente implementada (estructura futura).
- La información clínica del portal no reemplaza la decisión médica (disclaimer en la UI).

## Referencias
- `kin-docs/adr/ADR-028_TRIAGE_MODULE.md` (evento `TriagePerformedEvent`)
- `kin-docs/adr/ADR-029_DIFFERENTIAL_MODULE.md` y `ADR-030_HEALTH_DASHBOARD.md` (módulos reutilizados)
- `kin-docs/adr/ADR-015-strategic-interview-engine.md` (patrón de capas aditivas)
- Migración `V25__create_physician_tables.sql`

---

## Enmienda — Ciclo de vida de la relación médico-paciente (V30)

**Estado**: Aprobado (extiende ADR-031 de forma aditiva).

### Contexto

La asignación original era **binaria y forzada por ADMIN** (`physician_patient_assignments`
con `physician_id`, `patient_id`, `assigned_at`): no había estados, invitación ni
consentimiento del paciente. La hoja de ruta KIN Salud 2.0 identificó esta relación como la
**dependencia crítica** que bloquea mensajería avanzada, agenda real, documentos compartidos y
permisos granulares.

### Decisión

1. **Ciclo de vida con estados** (Flyway `V30__evolve_physician_patient_assignments.sql`):
   - Columnas nuevas: `status` (`PENDING/ACTIVE/SUSPENDED/ENDED`, CHECK), `invited_by` (FK
     `users`, ON DELETE SET NULL), `invited_at`, `accepted_at`, `ended_at`, `ended_reason`.
   - Índices por `(physician_id, status)` y `(patient_id, status)`.
   - **Backfill**: las filas existentes (ADMIN/piloto) quedan `ACTIVE` — no bloquea el piloto.
2. **Dominio** (`kin.health.physician.domain`): `RelationshipStatus` enum; `PhysicianPatientAssignment`
   ampliado (estado, invitación, fechas y motivo) con transiciones inmutables
   (`accepted`, `ended`, `suspended`, `reactivated`). `of(...)` conserva la asignación
   directa `ACTIVE` (ADMIN/piloto intactos); `invitation(...)` crea `PENDING`.
3. **Puerto/adaptador**: `PhysicianPatientRepository` ampliado (búsquedas por estado,
   invitación pendiente, duplicados). **`isAssigned` solo es `true` para relaciones `ACTIVE`**:
   mensajería, citas y acceso clínico requieren relación activa. Las relaciones existentes ya
   son `ACTIVE` (backfill), por lo que **ningún flujo existente se rompe**.
4. **Servicio** `RelationshipService`: `invitePatient` (verifica médico PHYSICIAN, paciente
   PATIENT por email, evita duplicados ACTIVE/PENDING), `acceptInvitation` (→ ACTIVE con
   `accepted_at`), `rejectInvitation` (→ ENDED con `ended_reason=REJECTED_BY_PATIENT`),
   `pendingInvitationsForPatient`.
5. **Permisos granulares por estado (Área 5)**: componente centralizado
   `access.RelationshipAccessValidator` (`hasActiveRelationship`,
   `requireActiveRelationship` direccional y `requireActiveRelationshipBetween` simétrico).
   **Solo las relaciones `ACTIVE` permiten acceso clínico**; `PENDING`, `SUSPENDED` y `ENDED`
   lanzan `RelationshipNotActiveException` (403, mensaje claro). Lo aplican `PhysicianService`
   (resumen, historial y `acknowledgeAlert`) y `TelemedicineService` (mensajes y citas). Es un
   bean inyectable y expone el método booleano para uso declarativo (`@PreAuthorize`) si se
   habilita method security. Se eliminan las excepciones binarias antiguas
   (`PhysicianPatientNotFoundException`, `TelemedicineAssignmentException`) en favor de la
   nueva excepción central.
5. **Eventos de dominio**: `PatientInvitedEvent` y `RelationshipAcceptedEvent` publicados de
   forma **transaccional** vía `OutboxEventPublisher` (fallback al bus en memoria) para
   futuras notificaciones y auditoría.
6. **Notificación por correo** (`PatientInvitedEventListener`): al recibir `PatientInvitedEvent`
   se envía al paciente un correo con el nombre/especialidad del médico, el mensaje opcional y
   el enlace a su panel de invitaciones (`<FRONTEND_BASE_URL>/dashboard/patient/invitations`).
   La entrega es **asíncrona respecto a la API** (el outbox relay la despacha en su hilo de
   polling); los fallos de SMTP se registran con `log.error` sin romper la invitación. Extiende
   `EmailSender` con `sendInvitationEmail(...)` (implementado en `SmtpEmailSender` y
   `LoggingEmailSender`). Feature flag `kin.health.physician.invitation-email-enabled`.
6. **API REST**:
   - `POST /api/v1/health/physician/patients/invite` (PHYSICIAN): `{ patientEmail, message }`.
   - `GET /api/v1/health/physician/patients?status=ACTIVE|PENDING|ALL` (PHYSICIAN): la lista
     muestra el estado; los pendientes solo exponen identidad (sin datos clínicos).
   - `GET /api/v1/health/patient/relationships/pending` (PATIENT): invitaciones con nombre y
     especialidad del médico.
   - `POST /api/v1/health/patient/relationships/accept` y `/reject` (PATIENT): aislamiento
     estricto por `userId` del JWT.
7. **Seguridad**: `SecurityConfig` restringe `/health/patient/**` a `PATIENT`/`ADMIN`.
   Feature flag `kin.health.physician.invite-enabled` (default `true`).
8. **Frontend**: botón "Invitar paciente" + modal en el Portal Médico; filtro por estado
   (Activos/Pendientes/Todos) con badge de estado en la lista; nueva página del paciente
   "Invitaciones" (`/dashboard/patient/invitations`) con aceptar/rechazar; ítem en el sidebar.

### Compatibilidad

- **Aditivo**: no se elimina la asignación por ADMIN ni el piloto (ambos siguen creando
  `ACTIVE` directas).
- **Sin regresión**: `isAssigned` (ACTIVE) mantiene el comportamiento de mensajería/citas
  para las relaciones existentes (backfill ACTIVE).
- Las excepciones del flujo (`PatientNotRegisteredException`, `InvitationNotFoundException`,
  `DuplicateRelationshipException`, `PhysicianNotFoundException`) extienden
  `ResponseStatusException` para que `GlobalExceptionHandler` preserve el código HTTP (404/409).

### Verificación

- Unit: `RelationshipServiceTest` (13 casos: invitación, duplicados, aceptación, rechazo,
  pendientes, feature flag). `PhysicianControllerTest` ampliado (invite, filtro status).
  Nuevo `PatientRelationshipControllerTest` (MockMvc).
- Unit del correo: `PatientInvitedEventListenerTest` (9 casos: envío con datos del médico y
  enlace, mensaje opcional, saludo con email si no hay nombre, flags, fallo SMTP sin romper).
- Unit de permisos: `RelationshipAccessValidatorTest` (11 casos: ACTIVE/PENDING/SUSPENDED/ENDED
  y direcciones simétricas). `PhysicianServiceTest`/`TelemedicineServiceTest` cubren el
  rechazo con relación pendiente/finalizada.
- Integración (Testcontainers): `PhysicianPatientRelationshipIntegrationTest` verifica la
  migración V30 (columnas + CHECK), el flujo completo sobre HTTP real con JWT y el
  aislamiento entre pacientes.
- Frontend: `InvitePatientModal.test.tsx`, `InvitationsList.test.tsx`,
  `PatientList.test.tsx` (badge PENDING), `PatientDetailView.test.tsx`.


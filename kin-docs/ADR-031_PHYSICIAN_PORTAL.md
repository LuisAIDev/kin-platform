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

# ADR-035: Auditoría de accesos a datos de salud — KIN Health

## Estado
**Aceptado** — 2026-08-30

## Contexto

KIN Health gestiona datos sensibles (historial, mensajería, citas, seguimiento, agenda) y la hoja
de ruta KIN Salud 2.0 exige **auditoría de accesos (Área 12)**: registrar quién accedió a qué
dato de salud de qué paciente y cuándo, permitir consultas filtradas (usuario, fecha, acción) y
dar **transparencia al paciente** (quiénes accedieron a sus datos).

Principios aplicados (intactos):
- **Java decide. El LLM únicamente comunica.** El registro es determinista en Java.
- **Aditividad**: nuevo bounded context (`kin.health.audit`) y tabla nueva (V33), sin tocar
  contratos congelados.
- **Asíncrono y no intrusivo**: los logs se guardan vía evento de dominio (outbox) para no
  bloquear la operación principal; la auditoría nunca rompe el flujo.

## Decisión

Crear un bounded context `com.kinplatform.kin.health.audit` (Clean Architecture + DDD):

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.audit.domain` | `AuditLog` (id, userId, `AuditAction`, `AuditResourceType`, resourceId, patientId, timestamp, ipAddress, userAgent, `details` JSON) |
| Puertos | `kin.health.audit.port` | `AuditLogRepository` (save, findByUserId/PatientId/date-range, findByActionAndResource, search) |
| Adaptadores | `kin.health.audit.adapter` | Entidad JPA + Spring Data + adaptador (tabla `audit_logs`, V33; `details` JSONB) |
| Servicio | `kin.health.audit.api` | `AuditService` (publica `AuditLogEvent` por outbox) |
| Eventos | `kin.health.audit.event` | `AuditLogEvent` + `AuditLogEventListener` (persiste de forma asíncrona) |
| API | `kin.health.audit.api` | `AuditAdminController` (`/admin/health/audit/logs`) y `AuditPatientController` (`/health/audit/my-logs`) |

### Enfoque híbrido (explícito, no AOP)

Se inyecta `AuditService` en los servicios de salud y se llama explícitamente en los puntos clave
(más control y menos efectos secundarios que un `@Aspect`):

- **PhysicianService**: `VIEW_SUMMARY` (resumen), `VIEW_HISTORY` (historial), `ACKNOWLEDGE_ALERT`.
- **TelemedicineService**: `SEND_MESSAGE`, `READ_MESSAGES`, `REQUEST_APPOINTMENT`, y
  `CONFIRM_/CANCEL_/COMPLETE_APPOINTMENT` según el estado.
- **FollowUpService**: `CREATE_PLAN`, `ADD_TASK`, `COMPLETE_TASK`, `RECORD_EVOLUTION`,
  `VIEW_EVOLUTION`.
- **SchedulingService**: `REQUEST_/CONFIRM_/CANCEL_/RESCHEDULE_/COMPLETE_APPOINTMENT`.

### Persistencia asíncrona

`AuditService.logAccess(...)` publica `AuditLogEvent` de forma **transaccional** vía
`OutboxEventPublisher` (fallback al bus en memoria). El `AuditLogEventListener` (suscrito al bus,
despachado por el `OutboxRelay`) guarda el `AuditLog` inmutable. Los fallos se registran y nunca
rompen el flujo principal. IP y user-agent se capturan de la petición actual si están disponibles.

### Seguridad

- `SecurityConfig`: `/admin/health/audit/**` → ADMIN (regla `/admin/**` existente);
  `/health/audit/**` → PATIENT/ADMIN.
- Solo el ADMIN consulta todos los logs; el paciente solo ve **sus propios** logs
  (aislamiento por userId del JWT).

### API REST

- `GET /api/v1/admin/health/audit/logs?userId=&patientId=&action=&startDate=&endDate=&page=&size=` (ADMIN).
- `GET /api/v1/health/audit/my-logs?startDate=&endDate=&page=&size=` (PATIENT).

### Configuración

```yaml
kin:
  health:
    audit:
      enabled: ${KIN_HEALTH_AUDIT_ENABLED:true}
      retention-days: ${KIN_HEALTH_AUDIT_RETENTION_DAYS:365}
```

## Alternativas consideradas

| Opción | Decisión |
|--------|----------|
| **AOP (`@Aspect`) sobre todos los servicios de salud** | ❌ Riesgo de logs innecesarios y efectos secundarios; se usa inyección explícita de `AuditService` en los puntos clave |
| **Guardar el log síncrono en el servicio** | ❌ Bloquea la operación; se publica `AuditLogEvent` vía outbox (asíncrono) |

## Consecuencias

**Positivas**: trazabilidad completa de accesos; transparencia al paciente; consultas filtradas
para ADMIN; inmutable y no intrusivo (asíncrono, no rompe flujos).

**Negativas**: nuevo BC (+1 tabla, +3 consultas de paginación); llamadas explícitas de auditoría
en 4 servicios (mantenimiento ligero).

## Referencias

- Migración `V33__create_audit_logs.sql`
- `kin-docs/adr/ADR-026_TRANSACTIONAL_OUTBOX.md` (entrega asíncrona de eventos)
- `kin-docs/adr/ADR-031_PHYSICIAN_PORTAL.md` (permisos por relación)

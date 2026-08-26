# ADR-032: Telemedicina (KIN Health)

## Estado
**Aceptado** — 2026-08-26

## Contexto

KIN Health cuenta con **Triaje** (ADR-028), **Diagnóstico Diferencial** (ADR-029), **Dashboard del Paciente** (ADR-030) y **Portal para Médicos** (ADR-031). El siguiente paso es la **telemedicina básica**: comunicación asíncrona segura entre pacientes y sus médicos asignados (mensajería + solicitud y gestión de citas), con notificaciones básicas dentro de la plataforma.

Debe integrarse sin romper los módulos existentes, siguiendo Clean Architecture + DDD y el aislamiento estricto (un mensaje/cita solo involucra al paciente y a su médico asignado).

## Decisión

Crear un bounded context `com.kinplatform.kin.health.telemedicine` con:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.telemedicine.domain` | `Message` (emisor, receptor, conversación, contenido, leído), `Appointment` (paciente, médico, fecha/hora, motivo, estado) |
| Puertos | `kin.health.telemedicine.port` | `MessageRepository`, `AppointmentRepository` |
| Adaptadores | `kin.health.telemedicine.adapter` | Entidades JPA + repos + `JpaMessageRepository` (contenido cifrado en reposo) + `JpaAppointmentRepository` |
| API | `kin.health.telemedicine.api` | `TelemedicineService`, `TelemedicineController`, DTOs |
| Config | `kin.health.telemedicine.config` | `TelemedicineProperties`, `TelemedicineConfig`, `ContentCipher` |

### Modelo de datos (Flyway V27)

- `messages(id, sender_id, receiver_id, conversation_id, content TEXT cifrado, is_read, created_at)`.
- `appointments(id, patient_id, physician_id, scheduled_at, reason, status, created_at)` con CHECK en `status` (`PENDIENTE`/`CONFIRMADA`/`CANCELADA`/`COMPLETADA`).

### Mensajería

- `conversationId` es **determinista y simétrico** (`conversationIdOf(a,b)` ordena el par): ambos lados del hilo ven la misma conversación.
- Enviar un mensaje requiere que el médico esté asignado al paciente (`PhysicianPatientRepository.isAssigned`, ADR-031) en cualquier dirección.
- El contenido se **cifra en reposo** con AES/GCM (`ContentCipher`, infraestructura) usando una clave de configuración (`kin.health.telemedicine.crypto-secret`).
- `MessageResponse.mine` indica si el mensaje es del usuario autenticado (el frontend no conoce el userId; el backend lo resuelve del JWT).

### Citas

- `requestAppointment(patientId, physicianId, scheduledAt, reason)`: crea la cita en `PENDIENTE` (requiere asignación).
- `updateAppointmentStatus(physicianId, appointmentId, status)`: solo el médico de la cita puede confirmar/rechazar/completar.
- `listAppointments(userId, asPhysician)`: filtra por rol (paciente o médico).

### Notificaciones

- `GET /unread` devuelve el contador de mensajes no leídos del usuario autenticado.
- El frontend usa **polling** (15 s en el chat, 30 s en la lista) para refrescar; sin WebSockets en esta fase.

### Seguridad y aislamiento

- Los mensajes solo son visibles para emisor/receptor (`belongsTo`/validación de conversación).
- Las citas solo para el paciente y su médico asignado.
- Contenido sensible cifrado en reposo; roles `PATIENT`/`PHYSICIAN` reutilizados.

## API REST

| Endpoint | Método | Descripción | Seguridad |
|----------|--------|-------------|-----------|
| `/api/v1/health/telemedicine/conversations` | GET | Conversaciones del usuario (con nombre, último mensaje, no leídos) | JWT |
| `/api/v1/health/telemedicine/messages?with=...` | GET | Mensajes de la conversación (marca leídos) | JWT |
| `/api/v1/health/telemedicine/messages` | POST | Enviar mensaje (requiere asignación) | JWT |
| `/api/v1/health/telemedicine/unread` | GET | Contador de mensajes no leídos | JWT |
| `/api/v1/health/telemedicine/appointments` | POST | Solicitar cita (requiere asignación) | JWT |
| `/api/v1/health/telemedicine/appointments/{id}/status` | PUT | Médico actualiza estado de la cita | JWT (PHYSICIAN) |
| `/api/v1/health/telemedicine/appointments` | GET | Lista de citas (filtro por rol) | JWT |

## Configuración (feature flags)

```yaml
kin:
  health:
    telemedicine:
      enabled: ${KIN_HEALTH_TELEMEDICINE_ENABLED:true}
      crypto-secret: ${KIN_HEALTH_TELEMEDICINE_CRYPTO_SECRET:kin-telemedicine-dev-key}
```

> En producción DEBE configurarse `crypto-secret` con un secreto propio.

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Mensajería por polling (elegida)** | Simple, sin WebSockets, suficiente para MVP | Latencia de hasta 30 s | ✅ |
| **WebSockets/SSE push** | Instantáneo | Mayor complejidad de infraestructura | Futuro |
| **Contenido en claro en BD** | Simple | No cumple cifrado en reposo | ❌ |
| **Reutilizar `PhysicianPatientRepository` (elegida)** | Cero duplicación de validación de asignación | Acopla a ADR-031 | ✅ |

## Consecuencias

**Positivas**:
- Comunicación paciente↔médico segura (cifrado en reposo, aislamiento estricto).
- Solicitud y gestión de citas determinista.
- Validación de asignación reutilizada del portal de médicos.
- Módulo aislado y aditivo; no modifica los contratos de ADR-028/029/030/031.

**Negativas / límites**:
- Sin WebSockets (polling); latencia de hasta 30 s en notificaciones.
- Las notificaciones son solo contadores dentro de la plataforma (sin push externo).
- La mensajería depende de la asignación médico-paciente (MVP).

## Referencias
- `kin-docs/adr/ADR-031_PHYSICIAN_PORTAL.md` (`PhysicianPatientRepository`)
- `kin-docs/adr/ADR-030_HEALTH_DASHBOARD.md` (patrón de capas y aislamiento)
- Migración `V27__create_telemedicine_tables.sql`

# Guía de Telemedicina — KIN Health

## Propósito

Comunicación asíncrona segura entre pacientes y sus médicos asignados, y gestión
de citas (ADR-032). Esta guía explica el flujo de uso para pacientes y médicos.

## Requisitos previos

- El paciente debe tener asignado un médico (endpoint admin
  `POST /api/v1/admin/health/physician/assign`).
- Roles: `PATIENT` (solicita citas, envía mensajes) y `PHYSICIAN`
  (responde, confirma/rechaza citas).

## Mensajería

1. El paciente entra a **Mensajes** (`/dashboard/patient/messages`).
2. Se listan las conversaciones con su médico (nombre, último mensaje, no
   leídos). Al abrir una conversación, los mensajes entrantes se marcan como
   leídos.
3. Se envía un mensaje escribiendo en el campo inferior y pulsando **Enviar**
   (o `Enter`).
4. El médico ve la conversación en `/dashboard/physician/messages` y responde
   de igual forma.

Reglas:
- Solo se puede conversar con un médico asignado al paciente (y viceversa).
- Los mensajes solo son visibles para emisor y receptor.
- El contenido se cifra en reposo (AES/GCM) en la base de datos.

### Notificaciones de no leídos

- `GET /api/v1/health/telemedicine/unread` devuelve el contador de mensajes no
  leídos del usuario autenticado.
- El frontend actualiza el contador por **polling** (15 s en el chat, 30 s en la
  lista de conversaciones).

## Citas

### Paciente

1. Entrar a **Citas** (`/dashboard/patient/appointments`).
2. En **Solicitar cita**, elegir fecha, hora y motivo, y pulsar **Solicitar
   cita**.
3. La cita queda en estado `PENDIENTE`. El paciente ve sus citas en **Mis
   citas**.

### Médico

1. Entrar a **Citas** (`/dashboard/physician/appointments`).
2. En **Mis citas** se ven las solicitudes `PENDIENTE`.
3. Pulsar **Confirmar** o **Rechazar**. El estado cambia a `CONFIRMADA` o
   `CANCELADA`. Una cita `CONFIRMADA` puede marcarse `COMPLETADA` (misma
   acción de actualización).

### Estados

| Estado | Descripción |
|--------|-------------|
| `PENDIENTE` | Solicitud creada, esperando confirmación del médico. |
| `CONFIRMADA` | Aceptada por el médico. |
| `CANCELADA` | Rechazada/cancelada. |
| `COMPLETADA` | Cita realizada. |

## API útil

```bash
# Enviar mensaje
curl -X POST http://localhost:8080/api/v1/health/telemedicine/messages \
  -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
  -d '{"receiverId": "<id>", "content": "Hola doctor"}'

# Mensajes de una conversación (marca leídos)
curl "http://localhost:8080/api/v1/health/telemedicine/messages?with=<id>" \
  -H "Authorization: Bearer <JWT>"

# No leídos
curl http://localhost:8080/api/v1/health/telemedicine/unread -H "Authorization: Bearer <JWT>"

# Solicitar cita
curl -X POST http://localhost:8080/api/v1/health/telemedicine/appointments \
  -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
  -d '{"physicianId": "<id>", "scheduledAt": "2026-09-01T10:00:00Z", "reason": "Control"}'

# Confirmar/rechazar cita (médico)
curl -X PUT http://localhost:8080/api/v1/health/telemedicine/appointments/<id>/status \
  -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMADA"}'
```

## Consideraciones de seguridad

- El `userId` siempre se resuelve desde el JWT (nunca del body).
- Mensajería y citas exigen asignación médico-paciente válida.
- Contenido cifrado en reposo; en producción configurar
  `KIN_HEALTH_TELEMEDICINE_CRYPTO_SECRET`.
- No hay WebSockets: las notificaciones se actualizan por polling.

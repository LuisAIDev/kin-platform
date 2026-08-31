# ADR-038: Motor de Automatizaciones — KIN Health

## Estado
**Aceptado** — 2026-09-16

## Contexto
KIN Health necesita un motor de automatizaciones (Área 14) que permita a médicos y administradores definir reglas condicionales del tipo "SI ocurre X, ENTONCES haz Y". Estas reglas deben ser deterministas, escritas en Java y ejecutarse automáticamente cuando ocurren eventos de dominio (triajes, citas, tareas, documentos, etc.). El objetivo es reducir la carga administrativa y asegurar respuestas consistentes a situaciones clínicas recurrentes.

Principios aplicados:
- **Java decide. El LLM únicamente comunica.** La lógica de activación y las acciones son 100% Java, sin IA para la toma de decisiones.
- **Aditividad**: nuevo bounded context `kin.health.automation` y tablas nuevas (V35), sin tocar contratos congelados.
- **Auditoría**: toda creación, modificación y ejecución de reglas queda registrada (Área 12).
- **Outbox**: eventos de dominio se procesan vía `OutboxEventPublisher` para ejecución asíncrona ordenada.
- **Seguridad por relación**: las reglas son por médico (médico crea su propia regla) o globales (ADMIN). El médico solo puede editar sus propias reglas.

## Decisión
Crear un bounded context `com.kinplatform.kin.health.automation` con la siguiente arquitectura de capas:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.automation.domain` | `AutomationRule`, `RuleExecutionLog`, `TriggerEvent`, `ActionType` |
| Puertos | `kin.health.automation.port` | `AutomationRuleRepository`, `RuleExecutionLogRepository` |
| Adaptadores | `kin.health.automation.adapter` | `AutomationRuleEntity`, `RuleExecutionLogEntity`, `JpaAutomationRuleRepository`, `JpaRuleExecutionLogRepository` |
| Configuración | `kin.health.automation.config` | `AutomationProperties` (`kin.health.automation.*`) |
| Servicio | `kin.health.automation.service` | `AutomationService`, `RuleEngine` |
| API REST | `kin.health.automation.api` | `AutomationController` |
| Integración | - | `RelationshipAccessValidator`, `AuditService`, `OutboxEventPublisher`, listeners de eventos |

### Modelo de dominio

```
TriggerEvent: TRIAGE_PERFORMED, APPOINTMENT_CONFIRMED, TASK_COMPLETED, 
               DOCUMENT_UPLOADED, APPOINTMENT_REMINDER_SENT, etc.

ActionType: CREATE_ALERT, SEND_NOTIFICATION, SEND_EMAIL, CREATE_TASK, etc.

AutomationRule (record):
  - UUID id
  - String name
  - String description
  - TriggerEvent triggerEvent
  - String conditions (JSONB: {"field": "urgency", "operator": "EQ", "value": "HIGH"})
  - ActionType action
  - String actionParams (JSONB: {"alertType": "URGENT", "messageTemplate": "..."})
  - boolean enabled
  - UUID createdBy
  - OffsetDateTime createdAt
  - OffsetDateTime updatedAt

RuleExecutionLog:
  - UUID id
  - UUID ruleId
  - UUID eventId (correlationId del evento que disparó la regla)
  - OffsetDateTime triggeredAt
  - boolean executed
  - String error (opcional)
  - String details (JSONB, opcional)
```

### API REST

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| `POST` | `/api/v1/health/automation/rules` | PHYSICIAN, ADMIN | Crear nueva regla |
| `GET` | `/api/v1/health/automation/rules` | PHYSICIAN, ADMIN | Listar reglas (por médico o globales) |
| `PUT` | `/api/v1/health/automation/rules/{id}` | PHYSICIAN, ADMIN | Actualizar regla |
| `PUT` | `/api/v1/health/automation/rules/{id}/toggle` | PHYSICIAN, ADMIN | Activar/Desactivar regla |
| `DELETE` | `/api/v1/health/automation/rules/{id}` | PHYSICIAN, ADMIN | Eliminar regla (soft delete) |
| `GET` | `/api/v1/admin/health/automation/logs` | ADMIN | Ver logs de ejecución |

### Motor de ejecución (RuleEngine)

El `RuleEngine` evalúa reglas cuando ocurre un evento:

1. **Obtener reglas**: `ruleRepository.findByTriggerEventAndEnabled(event, true)`
2. **Evaluar condiciones**: Para cada regla, evaluar el JSON `conditions` contra el `eventPayload` usando lógica determinista:
   - Sintaxis de condiciones: `{"field": "urgency", "operator": "EQ", "value": "HIGH"}`
   - Operadores soportados: `EQ` (igual), `NE` (no igual), `GT` (mayor que), `GTE` (mayor o igual), `LT` (menor que), `LTE` (menor o igual)
   - Los campos disponibles dependen del evento (para TRIAGE_PERFORMED: urgency, severity, symptoms, etc.)
3. **Ejecutar acción**: Si todas las condiciones se cumplen, ejecutar la acción:
   - `CREATE_ALERT`: `AlertService.createAlert(alertType, message, patientId, physicianId)`
   - `SEND_NOTIFICATION`: `NotificationService.send(to, message, type)`
   - `SEND_EMAIL`: `EmailSender.send(to, subject, template, context)`
   - `CREATE_TASK`: `FollowUpService.addTask(patientId, physicianId, title, description, recurrence)`
4. **Registrar ejecución**: Guardar en `RuleExecutionLog` (regla, evento, éxito/fallo, marca de tiempo).

### Guardrails (guardrails)

- **Siempre Java**: las condiciones y acciones son definidas y ejecutadas por Java, sin intervención de IA en la lógica.
- **Validación de condiciones**: las condiciones solo pueden usar campos disponibles en el evento de dominio y operadores seguros (EQ, NE, GT, GTE, LT, LTE).
- **Acciones seguras**: solo se permiten acciones de creación de alertas, notificaciones, correos y tareas. No se permiten acciones que modifiquen datos clínicos ni eliminen información.
- **Auditoría obligatoria**: cada creación, activación/desactivación y ejecución de regla debe registrarse con `AuditService`.
- **Máximo de reglas por médico**: opcional límite configurables (ej. 50 reglas por médico) para evitar sobrecarga.
- **Prioridad de reglas**: si múltiples reglas se disparan para el mismo evento, se ejecutan en orden de `createdAt` (las más antiguas primero) o se puede configurar prioridad explícita.

### Consecuencias

**Positivas**:
- Reducción de carga administrativa: respuestas automáticas a situaciones recurrentes.
- Consistencia: todas las respuestas ante un evento siguen la misma regla definida.
- Trazabilidad completa: cada ejecución queda registrada en `rule_execution_logs`.
- Flexibilidad: las condiciones JSON permiten configurar escenarios variados sin código.
- Auditoría: cumplimiento con regulaciones de salud (quién creó qué regla y cuándo se ejecutó).
- Seguro: la IA no decide; solo se usan operadores seguros y acciones limitadas.

**Negativas**:
- Nuevo bounded context (+2 tablas Flyway V35, +6 services, +2 controllers).
- Las condiciones requieren definición cuidadosa para cubrir casos de uso esperados.
- El motor de evaluación de condiciones JSON agrega complejidad al código.
- El listener de eventos debe gestionar correctamente la ejecución asíncrona sin bloquear.
- Requiere pruebas exhaustivas para asegurar que las condiciones se evalúan correctamente en todos los escenarios.

### Referencias

- `kin-docs/ADR-037_AI_ASSIST.md` (patrón de bounded context y auditoría)
- `kin-docs/ADR-036_CLINICAL_DOCUMENTS.md` (pattern de migración Flyway y configuración)
- `kin-docs/ADR-026_TRANSACTIONAL_OUTBOX.md` (entrega asíncrona de eventos)
- `kin-docs/ADR-035_AUDIT.md` (formato de auditoría)
- Eventos de dominio: `TriagePerformedEvent`, `AppointmentConfirmedEvent`, `FollowUpTaskAddedEvent`, `DocumentUploadedEvent`
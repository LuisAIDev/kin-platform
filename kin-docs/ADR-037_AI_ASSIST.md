# ADR-037: IA Asistencial — KIN Health

## Estado
**Aceptado** — 2026-08-30

## Contexto
KIN Health necesita un módulo de **IA Asistencial** (Área 13) que proporcione asistencia inteligente a médicos y pacientes en escenarios clínicos comunes. El módulo debe ofrecer cinco tipos de asistencia asistida por IA:
- **RESUMEN**: generar resúmenes concisos del historial del paciente
- **ORGANIZAR**: agrupar síntomas por categorías clínicas
- **PREPARAR CONSULTA**: generar preguntas para el médico basadas en el historial
- **EXPLICAR DIAGNÓSTICO**: explicar hallazgos médicos en lenguaje sencillo al paciente
- **REDACCION MENSAJE**: redactar mensajes para el paciente basados en recomendaciones médicas

El objetivo es mejorar la eficiencia clínica sin reemplazar el juicio médico. La IA nunca genera diagnósticos, probabilidades, riesgos ni recomendaciones — solo presenta u organiza datos estructurados.

## Decisión
Crear un bounded context `com.kinplatform.kin.health.aiassist` con la siguiente arquitectura de capas:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.aiassist.domain` | `AIAssistRequest` (record), `AIAssistType` enum |
| Puertos | `kin.health.aiassist.port` | `AIAssistRepository`, `AIProviderPort` |
| Adaptadores | `kin.health.aiassist.adapter` | `AIAssistEntity` (JPA), `JpaAIAssistRepository`, `DeepSeekAIProvider` |
| Configuración | `kin.health.aiassist.config` | `AIAssistProperties` (`kin.health.aiassist.*`) |
| Servicio | `kin.health.aiassist.service` | `AIAssistService` (5 métodos de asistencia) |
| API REST | `kin.health.aiassist.api` | `AIAssistController` (`/api/v1/health/aiassist/*`) |
| Integración | - | `RelationshipAccessValidator`, `AuditService`, `AIResponder` |

### Modelo de dominio

```
AIAssistType: SUMMARY, ORGANIZE, PREPARE, EXPLAIN, DRAFT
AIAssistRequest (record):
  - UUID id
  - AIAssistType type
  - String inputData (datos estructurados de entrada)
  - String response (respuesta de la IA)
  - OffsetDateTime timestamp
  - UUID userId (médico que solicitó)
  - UUID patientId
  - String context (descripción del contexto)
```

### Reglas de negocio

1. **Acceso por relación activa**: Toda operación requiere relación `ACTIVE` entre médico y paciente (validado por `RelationshipAccessValidator`).
2. **IA nunca diagnostica**: La IA solo resume, organiza, explica o redacta basándose en datos existentes. No se generan nuevos datos clínicos.
3. **Auditoría obligatoria**: Toda operación de IA debe registrarse con `AuditService` (acción, recurso tipo, IDs).
4. **Modo deshabilitado**: Cuando `kin.health.aiassist.enabled=false`, el servicio lanza `AIAssistDisabledException`.
5. **Acceso paciente**: Los pacientes solo pueden acceder a su propio historial de IA (`/my/history`).
6. **Acceso médico**: Los médicos pueden acceder al historial de sus pacientes asignados.

### API REST

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| `POST` | `/api/v1/health/aiassist/patients/{patientId}/summary` | PHYSICIAN | Generar resumen del historial |
| `POST` | `/api/v1/health/aiassist/patients/{patientId}/consultation-prep}` | PHYSICIAN | Preparar preguntas para consulta |
| `POST` | `/api/v1/health/aiassist/patients/{patientId}/draft-message` | PHYSICIAN | Redactar mensaje para paciente |
| `POST` | `/api/v1/health/aiassist/differential-explain` | PHYSICIAN | Explicar diagnóstico diferencial |
| `GET` | `/api/v1/health/aiassist/patients/{patientId}/history` | PHYSICIAN, PATIENT | Historial de operaciones IA |
| `GET` | `/api/v1/health/aiassist/my/history` | PATIENT | Mi propio historial de IA |

### Guardrails (guardrails)

- **RelationshipAccessValidator**: Valida que el médico tenga relación ACTIVE con el paciente antes de cualquier operación.
- **AIAssistDisabledException**: Lanzado cuando el módulo está deshabilitado en properties.
- **AuditService**: Cada operación debe auditarse antes y después (o alrededor) de la ejecución.
- **Límites de tokens/temperatura**: Configurados en `AIAssistProperties` (maxTokens default 500, temperature default 0.2).
- **Modelo configurable**: `model` property define qué modelo DeepSeek usar (default: `deepseek-v4-flash`).
- **Salida solo lectura**: La IA nunca puede modificar el historial clínico, solo generar textos de apoyo.

### Consecuencias

**Positivas**:
- Mejor eficiencia clínica: resúmenes y preguntas generados automáticamente.
- Consistencia en la presentación de información: prompts estandarizados.
- Trazabilidad completa: cada operación de IA queda registrada en auditoría.
- Acceso controlado por relaciones médico-paciente.
- Flexibilidad: modelo y parámetros configurables vía properties.
- Fondo off-line-first: los tests pueden mockear el proveedor de IA sin necesidad de LLM real.
- Cumple ADR-012 (REPORT consume solo ConsultingReport) y ADR-013 (directivas de comunicación).

**Negativas**:
- Nuevo bounded context (+1 tabla Flyway V**, +5 services, +2 controllers).
- La dependencia de la IA introduce latencia (timeout 60s en stage Consultor).
- Los prompts deben ser cuidadosamente diseñados para evitar salidas no deseadas.
- El médico debe revisar y validar siempre la salida de la IA.
- Requiere configuración de API key de DeepSeek en entorno producción.

### Referencias

- `kin-docs/ADR-036_CLINICAL_DOCUMENTS.md` ( patrón de bounded context y auditoría)
- `kin-docs/ADR-035_AUDIT.md` (formato de eventos de auditoría)
- `kin-docs/ADR-031_PHYSICIAN_PORTAL.md` (permisos por relación ACTIVE)
- `kin-docs/ADR-026_TRANSACTIONAL_OUTBOX.md` (entrega asíncrona de eventos)
- `kin-docs/ADR-012_REPORT.md` (frontera REPORT consume ConsultingReport)
- `kin-docs/ADR-013_CONVERSATION_ORCHESTRATOR.md` (directivas de comunicación)
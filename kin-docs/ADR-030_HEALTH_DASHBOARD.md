# ADR-030: Dashboard de Salud del Paciente (KIN Health)

## Estado
**Aceptado** — 2026-08-26

## Contexto

KIN Health cuenta con **Triaje Digital** (ADR-028) y **Diagnóstico Diferencial** (ADR-029). El paciente necesita un **panel centralizado** que consolide su experiencia: resumen de salud, historial de consultas, perfil con factores de riesgo gestionados por el paciente (que el `DifferentialEngine` consume) y un **plan de cuidado personalizado** determinista.

Debe integrarse sin romper los módulos existentes, siguiendo Clean Architecture + DDD y la filosofía **"Java decide. El LLM únicamente comunica."**

## Decisión

Crear un bounded context `com.kinplatform.kin.health.dashboard` con:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.dashboard.domain` | `PatientProfile` (factores de riesgo + condiciones crónicas), `HealthSummary` (estadísticas agregadas), `CarePlan`, `Reminder`, `CareRecommendationRegistry` (plantillas deterministas) |
| Puertos | `kin.health.dashboard.port` | `DashboardRepository` (perfil + recordatorios) |
| Adaptadores | `kin.health.dashboard.adapter` | Entidades JPA + repos + `JpaDashboardRepository` (JSONB) |
| API | `kin.health.dashboard.api` | `DashboardController`, `DashboardService`, DTOs |
| Config | `kin.health.dashboard.config` | `DashboardProperties`, `DashboardConfig` |

### Modelo de datos (Flyway V24)

- `patient_profiles(user_id, profile_data JSONB, updated_at)` — perfil con factores de riesgo y condiciones crónicas.
- `reminders(id, user_id, type, title, scheduled_at, active, created_at)` — recordatorios base (CITA/MEDICACION/GENERAL) para futura agenda.

### Servicio de aplicación (DashboardService)

- **Resumen** (`summary`): total de consultas, condiciones más frecuentes (top N), fecha del último triaje, recordatorios activos y total de diagnósticos diferenciales (consultas con ≥2 condiciones candidatas).
- **Historial** (`history`): lista cronológica paginada de consultas de triaje vía `TriageConsultationRepository.findByUserId(userId, pageable)`.
- **Detalle** (`consultationDetail`): recupera una consulta del paciente autenticado (404 si pertenece a otro usuario).
- **Perfil** (`profile`/`updateProfile`): guarda/carga factores de riesgo y condiciones crónicas; los factores son consumidos por el `DifferentialEngine` (ADR-029) en futuras consultas.
- **Plan de cuidado** (`carePlan`): condiciones identificadas = crónicas del perfil + condiciones más frecuentes del historial; genera recomendaciones con `CareRecommendationRegistry` (plantillas deterministas en Java, sin LLM).
- **Recordatorios** (`reminders`/`createReminder`): estructura base para citas/medicación.

### Lógica de negocio (determinista)

- `CareRecommendationRegistry.defaults()`: mapa condición → recomendación con prioridad (ALTA/MEDIA/BAJA) para hipertensión, diabetes, asma, gripe, neumonía, migraña, etc. `planFor(conditions)` deduplica por consejo y combina múltiples condiciones.
- El resumen y el plan se calculan **solo** a partir de datos persistidos (repositorios de triaje y dashboard); nunca del LLM.

### Integración

- `DashboardController` (JWT, aislamiento por `userId` desde la autenticación): `GET /summary`, `GET /history`, `GET /history/{consultationId}`, `GET/PUT /profile`, `GET /care-plan`, `POST/GET /reminders`.
- Se reutiliza el `TriageConsultationRepository` existente (se añade la variante paginada `findByUserId(userId, pageable)`).

## API REST

| Endpoint | Método | Descripción | Seguridad |
|----------|--------|-------------|-----------|
| `/api/v1/health/dashboard/summary` | GET | Resumen de salud (consultas, condiciones frecuentes, último triaje, recordatorios) | JWT |
| `/api/v1/health/dashboard/history` | GET | Historial paginado de consultas de triaje | JWT |
| `/api/v1/health/dashboard/history/{consultationId}` | GET | Detalle de una consulta del paciente | JWT |
| `/api/v1/health/dashboard/profile` | GET/PUT | Leer/actualizar factores de riesgo y condiciones crónicas | JWT |
| `/api/v1/health/dashboard/care-plan` | GET | Plan de cuidado personalizado | JWT |
| `/api/v1/health/dashboard/reminders` | GET/POST | Listar/crear recordatorios base | JWT |

El `userId` se resuelve **siempre** desde la autenticación (`AuthenticatedUsers.require`), nunca desde el body, garantizando el aislamiento por paciente.

## Configuración (feature flags)

```yaml
kin:
  health:
    dashboard:
      enabled: ${KIN_HEALTH_DASHBOARD_ENABLED:true}
      top-conditions: ${KIN_HEALTH_DASHBOARD_TOP_CONDITIONS:3}
```

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Decisión |
|--------|----------|-------------|----------|
| **Servicio + plantillas deterministas (elegida)** | 100 % determinista, sin LLM, extensible | Plantillas limitadas al registro | ✅ |
| **Plan generado por LLM** | Lenguaje natural flexible | No determinista, viola "Java decide", costoso | ❌ |
| **Resumen por agregación en SQL** | Rápido | Acopla el dashboard a la BD, duplica lógica | ❌ |
| **Reutilizar repositorios de dominio (elegida)** | Cero duplicación, aditivo | Requiere paginación en el puerto existente | ✅ |

## Consecuencias

**Positivas**:
- Panel centralizado con resumen, historial, perfil y plan de cuidado.
- Factores de riesgo del paciente reutilizados por el `DifferentialEngine`.
- Plan de cuidado determinista, combinable y deduplicado.
- Módulo aislado y aditivo: no modifica los contratos de triaje/differential (solo añade paginación al puerto).
- Datos de salud confidenciales y aislados por usuario.

**Negativas / límites**:
- El plan de cuidado usa plantillas (limitadas al `CareRecommendationRegistry`); ampliable en el futuro.
- Los recordatorios son una estructura base sin notificaciones.
- La información no reemplaza el juicio clínico (disclaimer en la UI).

## Referencias
- `kin-docs/adr/ADR-028_TRIAGE_MODULE.md` (triaje, repositorio de consultas)
- `kin-docs/adr/ADR-029_DIFFERENTIAL_MODULE.md` (factores de riesgo → `DifferentialEngine`)
- `kin-docs/adr/ADR-015-strategic-interview-engine.md` (patrón de capas aditivas)
- Migración `V24__create_patient_profiles.sql`

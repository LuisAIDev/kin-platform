# Guía de Monitoreo — KIN Platform

## Endpoints de observabilidad

| Endpoint | Descripción |
|----------|-------------|
| `/api/v1/actuator/health` | Health agregado (UP/DOWN) |
| `/api/v1/actuator/health/healthModules` | Estado de los módulos de salud (triaje, diferencial, dashboard, médico, telemedicina) + caché |
| `/api/v1/actuator/metrics` | Métricas Micrometer (Prometheus compatible) |
| `/api/v1/actuator/prometheus` | Formato Prometheus |
| `/api/v1/admin/health/pilot/metrics` | Métricas anonimizadas del piloto (KPIs) |

## Métricas del piloto

Endpoint `GET /api/v1/admin/health/pilot/metrics` (rol ADMIN). Devuelve un
informe **anonimizado** (sin correos ni nombres):

| Campo | Descripción | Umbral de alerta |
|-------|-------------|------------------|
| `totalPatients` | Pacientes del piloto (asignados) | — |
| `patientsWithTriage` | Pacientes con al menos un triaje | < 90 % del total |
| `triageCompletionRate` | % de pacientes que completaron el triaje | < 0.8 |
| `totalTriages` | Volumen de triajes | — |
| `totalMessages` | Volumen de mensajes de telemedicina | — |
| `totalAppointments` | Volumen de citas | — |
| `avgPhysicianResponseMinutes` | Tiempo medio de respuesta del médico (proxy: lapso entre mensaje del paciente y primera respuesta del médico) | > 240 min |
| `pendingAlerts` | Alertas ALTA sin reconocer | > 0 en 15 min |

Los KPIs del piloto y el procedimiento de arranque están documentados en
`kin-docs/BRIEF_PILOTO.md`.

## Métricas de salud (fase de producción)

| Métrica | Descripción | Alerta sugerida |
|---------|-------------|-----------------|
| `kin.health.triages.total` | Total de triajes | Ninguna |
| `kin.health.triages.high_urgency` | Triajes con urgencia máxima ALTA | **> 0 pendientes de revisión en 15 min** (portal médico) |
| `kin.health.differentials.total` | Total de diagnósticos diferenciales | Ninguna |
| `kin.health.telemedicine.messages.total` | Total de mensajes de telemedicina | Ninguna |
| `kin.health.telemedicine.appointments.total` | Total de citas de telemedicina | Ninguna |
| `kin.outbox.pending` | Eventos pendientes del outbox | **> 100 durante 5 min** |
| `kin.outbox.dead_letter` | Eventos en DLQ | **> 0** |
| `http.server.requests` | Latencia/estado de peticiones HTTP | p99 > 2 s, 5xx > 1 % |

## Umbrales de alerta recomendados

| Señal | Umbral | Severidad |
|-------|--------|-----------|
| Health `DOWN` | - | Crítica |
| Error rate HTTP 5xx | > 1 % en 5 min | Alta |
| p99 latencia | > 2 s en 5 min | Media |
| Triajes ALTA sin revisar | > 0 en 15 min | Alta |
| Tiempo de respuesta médico (piloto) | > 4 h (240 min) | Media |
| Tasa de finalización de triaje (piloto) | < 80 % | Media |
| Outbox pending | > 100 en 5 min | Media |
| DLQ | > 0 | Alta |
| CPU backend | > 80 % en 10 min | Media |
| Memoria backend | > 85 % en 10 min | Media |

## Logs estructurados

- Perfiles `prod`/`render`: JSON vía `LogstashEncoder` con `service`, `correlationId`, `traceId`, `spanId`, `userId`.
- Cada petición lleva `correlationId` (filtro `CorrelationIdFilter`).
- Búsqueda recomendada: correlacionar por `correlationId` ante errores.

## Caché del catálogo

- `spring.cache.type=simple` (memoria) por defecto; `redis` + `kin.cache.redis.enabled=true` para Redis.
- Caché `kin.triage.catalog` (catálogo de condiciones/síntomas); se invalida al
  actualizar/importar el catálogo.

## Check de readiness

- `/actuator/health/readiness` (DB + readinessState).
- `/actuator/health/liveness` (livenessState).

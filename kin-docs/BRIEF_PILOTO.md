# Brief del Piloto Clínico — KIN Health

> Documento de planificación para el equipo. Define objetivos, alcance,
> indicadores de éxito y el procedimiento de arranque del piloto.

## 1. Contexto

KIN Health implementa un flujo clínico asistido: triaje digital determinista
(ADR-028), diagnóstico diferencial (ADR-029), dashboard de paciente (ADR-030),
portal médico (ADR-031) y telemedicina (ADR-032). Antes de la apertura general,
se valida el producto con un grupo reducido de usuarios reales.

## 2. Objetivos

1. Validar la **usabilidad** de Mi Salud y el Portal Médico.
2. Validar la **calidad clínica** del triaje y del diagnóstico diferencial en
   casos reales.
3. Validar el **flujo de comunicación** (alertas, mensajería, citas).
4. Recopilar feedback estructurado para la siguiente iteración.

## 3. Alcance y tamaño

| Dimensión | Valor |
|-----------|-------|
| Pacientes | 10–20 |
| Médicos | 2–3 |
| Duración | 4 semanas (extensible) |
| Categoría de proyecto | Salud (`SALUD`) |

Cada paciente se asigna a **un** médico responsable (tabla
`physician_patient_assignments`).

## 4. Indicadores de éxito (KPIs)

| KPI | Fuente | Objetivo |
|-----|--------|----------|
| Tasa de finalización del triaje | `GET /admin/health/pilot/metrics` | > 80 % |
| Tiempo medio de respuesta del médico | `avgPhysicianResponseMinutes` | < 4 h |
| % de triajes ALTA revisados | alertas acknowledge | 100 % |
| Feedback recibido | formulario piloto | ≥ 1 por participante |
| Pacientes con al menos 1 triaje | `patientsWithTriage` | ≥ 90 % |

## 5. Procedimiento de arranque

### 5.1 Pre-requisitos

- Backend desplegado en producción/ensayo (Render/Neon) con migraciones
  V1..V28 aplicadas y feature flags de salud activos.
- Frontend desplegado (Vercel) con `NEXT_PUBLIC_FEEDBACK_URL` configurado.
- SMTP configurado (`APP_MAIL_ENABLED=true`) para el envío de credenciales.

### 5.2 Crear el grupo piloto

Un **administrador** ejecuta una sola llamada (idempotente; los usuarios
existentes se actualizan a rol/marca verificada):

```bash
curl -X POST https://<backend>/api/v1/admin/health/pilot/setup \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "patients": ["ana@kin.com", "luis@kin.com"],
    "physicians": ["dra.garcia@kin.com"],
    "password": "Temporal-2026!",
    "assignments": [
      { "patientEmail": "ana@kin.com", "physicianEmail": "dra.garcia@kin.com" },
      { "patientEmail": "luis@kin.com", "physicianEmail": "dra.garcia@kin.com" }
    ]
  }'
```

Respuesta: `{ "usersCreated": 3, "assignmentsCreated": 2 }`.

- **Idempotencia**: si un correo ya existe, se actualiza el rol y se marca
  `emailVerified`/`active` en vez de duplicarse.
- **Asignaciones**: se crean vía `PhysicianService.assignPatient`; las
  asignaciones con correos desconocidos se omiten (no fallan).

### 5.3 Entrega a participantes

- Enviar `GUIA_USUARIO_PILOTO.md` a cada participante.
- Firmar y archivar `MODELO_CONSENTIMIENTO.md` antes de activar el acceso.

## 6. Monitoreo semanal

1. Consultar `GET /admin/health/pilot/metrics` (métricas anonimizadas).
2. Revisar los KPIs de la sección 4 y las alertas de `GUIA_MONITOREO.md`.
3. Analizar el feedback del formulario y agruparlo por tema.
4. Emitir un mini-informe con hallazgos y decisiones.

## 7. Criterios de salida del piloto

- KPIs de la sección 4 cumplidos durante 2 semanas consecutivas.
- Sin errores bloqueantes (categoría "Alta" sin workaround) en la última semana.
- Decisión go/no-go para la apertura general.

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Baja participación | Recordatorios semanales y feedback fácil (1 clic) |
| Triajes incorrectos | Validación clínica por los médicos del piloto; ajuste de pesos/catálogo |
| Datos sensibles | Cifrado en reposo, mínima exposición, métricas anonimizadas |
| Saturación del médico | Máx. 10 pacientes por médico; umbral de tiempo de respuesta |

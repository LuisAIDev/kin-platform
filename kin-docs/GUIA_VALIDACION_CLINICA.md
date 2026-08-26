# Guía de Validación Clínica — KIN Health

## Propósito

El catálogo de triaje (ADR-028) se amplía de forma continua con fuentes externas
(WHO, PubMed, CIE-10, datasets estructurados). Todo dato importado entra con
estado `PENDING` y **debe pasar revisión por un profesional de la salud antes de
usarse en producción** (estado `APPROVED`). Esta guía documenta el proceso.

## Estados de validación

| Estado | Significado |
|--------|-------------|
| `PENDING` | Importado automáticamente o recién creado; pendiente de revisión. |
| `REVIEWED` | Revisado por un profesional de la salud; aún no aprobado para producción. |
| `APPROVED` | Validado y apto para uso en producción. |
| `REJECTED` | Rechazado; no debe usarse para el triaje. |

## Flujo recomendado

1. **Importación**: un administrador dispara
   `POST /api/v1/admin/health/catalog/import-from-external` (o se habilita
   `kin.health.triage.auto-update=true`). Las condiciones entran como `PENDING`
   y las relaciones se deduplican por ID determinista.
2. **Informe de cobertura**: `GET /api/v1/admin/health/catalog/coverage`
   muestra por condición cuántos síntomas tiene asociados. Las condiciones con
   **menos de 2 síntomas** se marcan como insuficientes y requieren ampliación.
3. **Revisión clínica**: para cada condición importada:
   - Verificar la **fuente** (CIE-10, WHO, guía clínica) y la exactitud del
     `icd_code`.
   - Validar la **severidad** (`LEVE`/`MODERADO`/`GRAVE`) y la **urgencia**
     (`BAJA`/`MEDIA`/`ALTA`).
   - Validar los **pesos** de cada relación síntoma-condición (0..1) y la
     bandera `required` (máximo UN síntoma obligatorio, el más patognomónico).
   - Validar la **recomendación** breve de apoyo.
4. **Aprobación**: pasar la condición a `APPROVED`. Las condiciones con datos
   dudosos pasan a `REJECTED` o `REVIEWED` para revisión posterior.

> Nota: el cambio de estado requiere SQL directo o una futura herramienta
> administrativa (actualmente se gestiona vía migraciones y datos de seed).

## Reglas de calidad mínimas

- Cada condición debe tener **al menos 2 síntomas** asociados (cobertura
  adecuada). El informe de cobertura marca las insuficientes.
- Una condición con `REJECTED` **no debe** aparecer en los resultados de triaje
  ni de diagnóstico diferencial (el motor ignora condiciones rechazadas).
- Las condiciones `PENDING`/`REVIEWED` pueden usarse en entornos de prueba pero
  se recomienda filtrarlas en producción hasta su aprobación.

## Consideraciones de consentimiento

El portal para médicos (ADR-031) opera sobre pacientes de la plataforma con su
consentimiento. Para producción se debe documentar y registrar el consentimiento
explícito del paciente antes de compartir su historial con un profesional.

## Herramientas de soporte

- `POST /api/v1/admin/health/catalog/import-from-external` — importar desde
  fuentes externas (KnowledgeEngine + bundle).
- `GET /api/v1/admin/health/catalog/coverage` — informe de cobertura por
  condición.
- `POST /api/v1/admin/health/triage/catalog/update` — actualización del
  catálogo (legacy del módulo de triaje).

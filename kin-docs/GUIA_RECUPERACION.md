# Guía de Recuperación — KIN Platform

## Principios

- **Migraciones idempotentes**: V1..V28 se aplican una sola vez; las nuevas
  migraciones siempre son aditivas y `IF NOT EXISTS`/`ON CONFLICT DO NOTHING`.
- **Rollback de código** = volver a un commit anterior y redesplegar. No se
  revierten migraciones en producción; se corrige hacia delante.
- **Outbox**: entrega at-least-once. Si el relé falla, los eventos quedan
  `PENDING` y se reintentan; tras `max-retries` pasan a `DEAD_LETTER`.

## Procedimientos

### 1. Deploy fallido (app no arranca)

1. Revisar logs de arranque (migraciones Flyway, beans).
2. Si la migración falló: corregir en una **nueva** migración (V29+), no editar
   V1..V28 (rompería el checksum de Flyway en bases ya migradas).
3. Redesplegar el commit anterior mientras se corrige.

### 2. Degradación del backend (502/tiempos de espera)

1. Verificar `GET /api/v1/actuator/health` y `/health/healthModules`.
2. Revisar métricas: p99, error rate, outbox pending, pool de conexiones.
3. Escalar el Web Service en Render (más instancias/memoria).
4. Si Redis está habilitado y caído: el backend degrada offline-first
   (catálogo se recalcula en memoria; sin fallo).

### 3. Base de datos degradada (Neon)

1. Revisar `readiness` (incluye `db`).
2. Los módulos de salud dependen de la BD: triaje, dashboard, médico y
   telemedicina requerirán la base disponible.
3. Si la base está parcialmente caída: aumentar recursos en Neon; los
   reintentos de Hikari (`connection-timeout: 20000`) amortiguan picos.

### 4. Eventos atascados (outbox)

1. `kin.outbox.pending > 0` sostenido: revisar logs del `OutboxRelay`.
2. Si un evento falla siempre: pasa a `DEAD_LETTER` (DLQ) tras `max-retries`.
3. Reintento manual de DLQ: `POST /api/v1/admin/outbox/dead-letter/{id}/retry`
   (ver `OutboxAdminController`).

### 5. Rotación de secretos

1. **JWT_SECRET**: rotar en todos los entornos a la vez; invalidará sesiones
   activas (logout masivo). Programar en ventana de mantenimiento.
2. **Telemedicina crypto-secret**: soporta **rotación progresiva** — pasar una
   lista de claves separadas por comas en
   `KIN_HEALTH_TELEMEDICINE_CRYPTO_SECRET` (nueva primera, anteriores después).
   Los mensajes antiguos se descifran probando todas las claves; los nuevos se
   cifran con la primera (activa). Cuando todos los mensajes estén re-cifrados
   con la nueva clave, retirar las antiguas.

### 6. Restauración de datos

1. Neon ofrece puntos de restauración. Restaurar a un timestamp anterior.
2. Tras restaurar: verificar consistencia de `flyway_schema_history` (si se
   restauró a una versión anterior, las migraciones V… posteriores se aplicarán
   de nuevo; son idempotentes).
3. Los datos de salud (triage_consultations, messages, patient_profiles) son
   confidenciales: asegurar que las restauraciones solo ocurran en el entorno
   correcto y con acceso restringido.

### 7. Reset de base local (dev)

- `scripts/reset-dev-db.ps1` (exige confirmación `RESET`); solo base local.
- No usar en producción.

## Checklist de incidente

- [ ] Confirmar salud (`/actuator/health`).
- [ ] Confirmar módulos de salud (`/actuator/health/healthModules`).
- [ ] Capturar `correlationId` de las peticiones fallidas en los logs JSON.
- [ ] Verificar métricas (p99, 5xx, outbox pending, pool).
- [ ] Determinar si es degradación (escale) o error (corrija código).
- [ ] Documentar la causa raíz y la corrección en el CHANGELOG.

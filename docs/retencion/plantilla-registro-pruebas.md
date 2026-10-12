# Plantilla de Registro de Pruebas de Recuperación — Kin Medical

**Instrucciones**: Completar una fila por cada prueba de restauración ejecutada.  
**Ubicación**: `docs/retencion/pruebas-recuperacion/registro-YYYY-MM-DD.md`

---

## Registro de Pruebas

| Fecha | Escenario | Backup Usado | RTO Real | RPO Real | Éxito | Observaciones |
|-------|-----------|--------------|----------|----------|-------|---------------|
| 2026-10-11T10:00:00-05:00 | DR-02 (Corrupción DB) | kin_backup_20261010_030000.dump | 2h 15m | 23h 45m | SÍ | Restauración en staging exitosa. Verificación de 5 tablas críticas OK. |
| 2026-04-15T14:30:00-05:00 | DR-05 (Migración) | kin_backup_20260414_030000.dump | 1h 40m | 24h 00m | SÍ | Migración a servidor nuevo completada. DNS actualizado sin downtime adicional. |
| 2026-01-20T09:00:00-05:00 | DR-03 (Borrado accidental) | kin_backup_20260119_030000.dump | 3h 10m | 23h 30m | PARCIAL | Tabla `audit_logs` no restaurada completamente (FK constraint). Requiere revisar orden de restauración en `restore-postgres.sh`. |

---

## Guía de Relleno

| Columna | Qué poner | Ejemplo |
|---------|-----------|---------|
| **Fecha** | ISO 8601 con zona horaria | `2026-10-11T10:00:00-05:00` |
| **Escenario** | Código DR-01 a DR-05 | `DR-02` |
| **Backup Usado** | Nombre exacto del archivo `.dump` | `kin_backup_20261010_030000.dump` |
| **RTO Real** | Tiempo cronómetro: detección → healthcheck UP | `2h 15m` |
| **RPO Real** | Datos perdidos: hora incidente - hora backup | `23h 45m` |
| **Éxito** | `SÍ` / `NO` / `PARCIAL` | `SÍ` |
| **Observaciones** | Detalles técnicos, errores, lecciones, acciones correctivas | `Verificación de 5 tablas críticas OK.` |

---

## Criterios de Éxito

| Resultado | Definición |
|-----------|------------|
| **SÍ** | Todas las verificaciones de la sección 6 pasan en el primer intento |
| **PARCIAL** | Healthcheck UP pero alguna verificación funcional falla (ej: RIPS no genera, paciente no visible) |
| **NO** | Healthcheck DOWN, o restauración falla, o pérdida de datos > RPO |

---

## Acciones Post-Prueba

| Si resultado | Acción obligatoria |
|--------------|-------------------|
| **SÍ** | Archivar registro. Actualizar procedimiento si hay mejoras. |
| **PARCIAL** | Documentar fallo. Crear ticket de mejora. Re-ejecutar prueba en ≤ 30 días. |
| **NO** | Escalar a Soporte KIN Nivel 2. Análisis de causa raíz (RCA) en ≤ 72h. Re-ejecutar prueba en ≤ 15 días. |

---

## Historial de Cambios a esta Plantilla

| Versión | Fecha | Autor | Cambio |
|---------|-------|-------|--------|
| 1.0 | 2026-10-11 | Equipo KIN | Versión inicial |
# Política de Retención y Backup — Kin Medical On-Premise

## Requisitos Legales (Colombia)

- **Resolución 839/2017** (ex 839/1999): Retención de Historia Clínica por **15 años**.
- **Ley 1581/2012** (Protección de Datos Personales): Principio de limitación temporal.
- **Resolución 1995/1999** (Historia Clínica): Disponibilidad e integridad.

## Estrategia de Backup

### 1. Backup Diario (Incremental/Completo con pg_dump -Fc)
- **Frecuencia**: Diaria a las 03:00 AM
- **Formato**: Custom (-Fc) — comprimido, permite restauración paralela y selectiva
- **Retención local**: 30 días (configurable via `RETENTION_DAYS`)
- **Almacenamiento**: `/var/backups/kin/` (montar en disco separado/NAS)

### 2. Backup Semanal Completo
- **Frecuencia**: Domingos a las 04:00 AM
- **Mismo script**, misma retención

### 3. Backup Mensual (Archivo Largo Plazo)
- **Frecuencia**: Primer día de mes a las 05:00 AM
- **Destino**: Almacenamiento externo (S3 compatible, Azure Blob, NAS remoto)
- **Retención**: 15 años (cumplimiento Res. 839/2017)
- **Cifrado**: AES-256 en reposo (recomendado)

## Procedimiento de Restauración

### Restauración Parcial (Tabla/Esquema)
```bash
pg_restore -d $DATABASE_URL --table=pacientes backup.dump
```

### Restauración Completa (Desastre)
```bash
./scripts/restore-postgres.sh /var/backups/kin/kin_backup_20260111_030000.dump
```

### Verificación de Integridad (Pre-restore)
```bash
pg_restore --list backup.dump | head -20
```

## Monitoreo y Alertas

- **Log centralizado**: `/var/log/kin-backup.log`
- **Métricas clave**:
  - Tamaño del backup (crecimiento inesperado = alerta)
  - Duración del backup (> 2h = alerta)
  - Fallos de pg_dump/pg_restore = alerta crítica
- **Recomendado**: Prometheus + Alertmanager + Grafana

## Responsabilidades

| Rol | Responsabilidad |
|-----|----------------|
| IPS (Cliente) | Proveer infraestructura, definir política de retención, validar restores periódicos |
| KIN (Proveedor) | Entregar scripts, documentar procedimientos, dar soporte técnico |

## Validación Periódica (Recomendado)

| Frecuencia | Acción |
|------------|--------|
| Mensual | Restaurar backup en ambiente de staging y validar integridad de datos |
| Trimestral | Simular desastre completo (RTO/RPO) |
| Anual | Auditoría de cumplimiento Res. 839/2017 |
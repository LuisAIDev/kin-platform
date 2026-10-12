#!/bin/bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kin}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
DATABASE_URL="${DATABASE_URL:?DATABASE_URL no definida}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/kin_backup_$TIMESTAMP.dump"

echo "[$(date)] Iniciando backup de KIN..."
pg_dump "$DATABASE_URL" -Fc -f "$BACKUP_FILE"
echo "[$(date)] Backup completado: $BACKUP_FILE"

# Verificar integridad
pg_restore --list "$BACKUP_FILE" > /dev/null
echo "[$(date)] Integridad verificada"

# Rotación: eliminar backups > RETENTION_DAYS
find "$BACKUP_DIR" -name "kin_backup_*.dump" -mtime +$RETENTION_DAYS -delete
echo "[$(date)] Rotación aplicada (> $RETENTION_DAYS días)"
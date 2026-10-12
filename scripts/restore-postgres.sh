#!/bin/bash
set -euo pipefail

BACKUP_FILE="${1:?Debe especificar el archivo de backup}"
DATABASE_URL="${DATABASE_URL:?DATABASE_URL no definida}"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Archivo no encontrado: $BACKUP_FILE"
    exit 1
fi

echo "ADVERTENCIA: Esto reemplazará los datos actuales de $DATABASE_URL"
read -p "¿Continuar? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Cancelado"
    exit 0
fi

echo "[$(date)] Iniciando restauración desde $BACKUP_FILE..."
pg_restore --clean --if-exists --no-owner -d "$DATABASE_URL" "$BACKUP_FILE"
echo "[$(date)] Restauración completada"
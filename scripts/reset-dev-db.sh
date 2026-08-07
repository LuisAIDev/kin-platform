#!/usr/bin/env bash
# ============================================================
# KIN Platform — Reset de base de datos H2 local (Linux/macOS)
#
# Detiene el backend si está corriendo, elimina la base H2
# persistente (data/kindb.mv.db) y vuelve a arrancar Spring Boot.
#
# Cuándo usarlo:
#   - Aparecen errores de ALTER TABLE al arrancar, p. ej.
#     "JdbcSQLIntegrityConstraintViolationException: NULL not allowed
#     for column SUPPORT_LEVEL" (o similar) tras cambiar el modelo JPA.
#   - El esquema H2 quedó obsoleto respecto a las entidades.
#
# Por qué: en dev Hibernate usa ddl-auto: update sobre una base H2
# persistente. Si una entidad añade una columna NOT NULL y la tabla ya
# tiene filas, H2 rechaza el ALTER. La solución es regenerar la base
# desde las entidades (nunca modificar entidades para adaptarlas).
#
# Uso:
#   bash scripts/reset-dev-db.sh
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/kin-backend"
DB_FILE="$BACKEND_DIR/data/kindb.mv.db"
TRACE_FILE="$BACKEND_DIR/data/kindb.trace.db"
PORT="${PORT:-8080}"

echo "=== KIN — Reset de base H2 local (dev) ==="

# 1. Detener la aplicación si está corriendo (puerto configurado)
if command -v lsof >/dev/null 2>&1; then
  PID="$(lsof -ti tcp:"$PORT" || true)"
elif command -v fuser >/dev/null 2>&1; then
  PID="$(fuser "$PORT"/tcp 2>/dev/null || true)"
else
  PID=""
fi

if [ -n "${PID:-}" ]; then
  echo "[1/4] Backend detectado en el puerto $PORT (PID $PID). Deteniendo..."
  kill -9 $PID 2>/dev/null || true
  sleep 2
else
  echo "[1/4] No hay backend corriendo en el puerto $PORT."
fi

# Limpieza extra: procesos Java/Maven de este proyecto con spring-boot:run
pkill -f "$BACKEND_DIR.*spring-boot:run" 2>/dev/null || true
sleep 1

# 2. Eliminar la base H2 persistente
if [ -f "$DB_FILE" ]; then
  echo "[2/4] Eliminando base obsoleta: $DB_FILE"
  rm -f "$DB_FILE"
  rm -f "$TRACE_FILE"
  echo "      Base eliminada. Hibernate la recreará desde cero al arrancar."
else
  echo "[2/4] No existe base local ($DB_FILE). Se creará una nueva al arrancar."
  rm -f "$TRACE_FILE"
fi

# 3. Verificar que el wrapper Maven existe
if [ ! -f "$BACKEND_DIR/mvnw" ]; then
  echo "ERROR: No se encontró $BACKEND_DIR/mvnw" >&2
  exit 1
fi

# 4. Arrancar Spring Boot de nuevo
echo "[3/4] Arrancando Spring Boot..."
echo "[4/4] Backend disponible en http://localhost:8080/api/v1 (Ctrl+C para detener)."
echo ""

cd "$BACKEND_DIR"
./mvnw spring-boot:run

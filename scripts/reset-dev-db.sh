#!/usr/bin/env bash
# ============================================================
# KIN Platform — Reset de base de datos PostgreSQL local (Linux/macOS)
#
# Detiene el backend si está corriendo, ELIMINA el esquema public de la
# base de desarrollo (PostgreSQL, perfil 'dev') y vuelve a arrancar Spring
# Boot. Flyway recrea V1..V11 desde cero en el siguiente arranque.
#
# IMPORTANTE: operación DESTRUCTIVA (DROP SCHEMA public CASCADE). Requiere
# confirmación explícita (escribir RESET) y solo está soportada para la
# base local (localhost). Nunca ejecuta el reset sobre una base remota.
#
# Uso:
#   bash scripts/reset-dev-db.sh
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/kin-backend"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
PORT="${PORT:-8080}"

echo "=== KIN — Reset de base PostgreSQL local (dev) ==="

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

# 2. Determinar la base de desarrollo
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="kin_platform"
DB_USER="kin_admin"
if [ -n "${DATABASE_URL:-}" ] && [[ "$DATABASE_URL" =~ jdbc:postgresql://([^:/]+):([0-9]+)/([^?]+) ]]; then
  DB_HOST="${BASH_REMATCH[1]}"
  DB_PORT="${BASH_REMATCH[2]}"
  DB_NAME="${BASH_REMATCH[3]}"
fi
if [ -n "${DATABASE_USER:-}" ]; then
  DB_USER="$DATABASE_USER"
fi

# Solo se resetea la base local (no se ejecuta un DROP remoto automático).
if [[ "$DB_HOST" != "localhost" && "$DB_HOST" != "127.0.0.1" && "$DB_HOST" != "::1" ]]; then
  echo "ERROR: El reset destructivo solo está soportado para la base DEV local." >&2
  echo "  DATABASE_URL apunta a $DB_HOST — no se ejecuta el reset automáticamente." >&2
  exit 1
fi

# 3. Confirmación explícita (operación destructiva, nunca automática)
echo "[2/4] ADVERTENCIA: se ELIMINARÁN TODOS LOS DATOS del esquema public de"
echo "      $DB_HOST:$DB_PORT/$DB_NAME"
read -r -p "      Escribe RESET para confirmar: " confirm
if [ "$confirm" != "RESET" ]; then
  echo "      Cancelado. No se modificó la base."
  exit 0
fi

# 4. Eliminar el esquema (vía psql del contenedor postgres-db de Compose)
echo "[3/4] Eliminando esquema public de $DB_NAME ..."
PG_ARGS=()
if [ -n "${DATABASE_PASSWORD:-}" ]; then
  PG_ARGS=(-e "PGPASSWORD=$DATABASE_PASSWORD")
fi
docker compose -f "$COMPOSE_FILE" exec -T "${PG_ARGS[@]}" postgres-db \
  psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
echo "      Esquema public eliminado. Flyway recreará V1..V11 en el siguiente arranque."

# 5. Verificar que el wrapper Maven existe
if [ ! -f "$BACKEND_DIR/mvnw" ]; then
  echo "ERROR: No se encontró $BACKEND_DIR/mvnw" >&2
  exit 1
fi

# 6. Arrancar Spring Boot de nuevo (perfil dev)
echo "[4/4] Arrancando Spring Boot (perfil 'dev')..."
echo "Backend disponible en http://localhost:8080/api/v1 (Ctrl+C para detener)."
echo ""

cd "$BACKEND_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

#!/usr/bin/env bash
# ============================================================
# KIN Platform — Arranque del backend con guard FAIL-FAST (Linux/macOS)
#
# Nunca lanza una segunda instancia de Spring Boot. Antes de
# ejecutar spring-boot:run comprueba:
#   1. java con KinApplication (app ya arrancada)
#   2. Maven Wrapper (mvnw spring-boot:run en curso)
#   3. Puerto 8080 escuchando
# Si cualquiera existe, NO arranca otro backend, muestra
# "Backend ya está ejecutándose." y deja que se reutilice.
#
# Dev usa PostgreSQL (perfil 'dev', en vez de H2 - H2-2). Antes de arrancar
# comprueba la base de datos:
#   - DATABASE_PASSWORD debe estar definida (env o .env).
#   - PostgreSQL debe responder en DATABASE_URL (por defecto localhost:5432).
# Si la base no está disponible, informa el problema y DETIENE (sin bucle).
#
# Uso:
#   bash scripts/start-dev-backend.sh
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/kin-backend"
PORT="${PORT:-8080}"

declare -a ALREADY=()

# 1. ¿Java con KinApplication ya en marcha?
if command -v pgrep >/dev/null 2>&1 && pgrep -f "KinApplication" >/dev/null 2>&1; then
  ALREADY+=("java KinApplication")
fi

# 2. ¿Maven Wrapper lanzando spring-boot:run?
if command -v pgrep >/dev/null 2>&1 && pgrep -f "MavenWrapperMain.*spring-boot:run" >/dev/null 2>&1; then
  ALREADY+=("Maven Wrapper")
fi

# 3. ¿Puerto en escucha?
PID=""
if command -v lsof >/dev/null 2>&1; then
  PID="$(lsof -ti tcp:"$PORT" || true)"
elif command -v fuser >/dev/null 2>&1; then
  PID="$(fuser "$PORT"/tcp 2>/dev/null || true)"
fi

if [ -n "${PID:-}" ]; then
  ALREADY+=("puerto $PORT")
fi

if [ "${#ALREADY[@]}" -gt 0 ]; then
  echo "Backend ya está ejecutándose: $(IFS=', '; echo "${ALREADY[*]}")."
  echo "No se inicia otra instancia. Reutilízalo en http://localhost:$PORT/api/v1"
  exit 0
fi

if [ ! -f "$BACKEND_DIR/mvnw" ]; then
  echo "ERROR: No se encontró $BACKEND_DIR/mvnw" >&2
  exit 1
fi

# --- Chequeo de base PostgreSQL (perfil dev) ---
DB_HOST="localhost"
DB_PORT="5432"
if [ -n "${DATABASE_URL:-}" ] && [[ "$DATABASE_URL" =~ jdbc:postgresql://([^:/]+):([0-9]+)/ ]]; then
  DB_HOST="${BASH_REMATCH[1]}"
  DB_PORT="${BASH_REMATCH[2]}"
fi

if [ -z "${DATABASE_PASSWORD:-}" ]; then
  echo "ERROR: DATABASE_PASSWORD no está definida." >&2
  echo "  El perfil dev usa PostgreSQL (no H2). Define en .env o en el entorno:" >&2
  echo "    DATABASE_URL=jdbc:postgresql://localhost:5432/kin_platform" >&2
  echo "    DATABASE_USER=kin_admin" >&2
  echo "    DATABASE_PASSWORD=<igual que POSTGRES_PASSWORD del docker-compose.yml>" >&2
  exit 1
fi

if ! timeout 3 bash -c "cat < /dev/null > /dev/tcp/$DB_HOST/$DB_PORT" 2>/dev/null; then
  echo "ERROR: PostgreSQL no responde en $DB_HOST:$DB_PORT." >&2
  echo "  Levanta la base de desarrollo:" >&2
  echo "    docker compose up -d postgres-db   (usa POSTGRES_PASSWORD del .env)" >&2
  echo "  El backend no se inicia sin base disponible." >&2
  exit 1
fi

echo "PostgreSQL detectado en $DB_HOST:$DB_PORT. Arrancando backend con perfil 'dev'..."
echo "Backend disponible en http://localhost:$PORT/api/v1 (Ctrl+C para detener)."
echo ""

cd "$BACKEND_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

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

echo "No hay backend en marcha. Arrancando UNA instancia de Spring Boot..."
echo "Backend disponible en http://localhost:$PORT/api/v1 (Ctrl+C para detener)."
echo ""

cd "$BACKEND_DIR"
./mvnw spring-boot:run

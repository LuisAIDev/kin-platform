#!/usr/bin/env bash
# ============================================================
# test-smtp.sh — Prueba de SMTP desde el entorno de despliegue
# (consola de Render Shell o local).
#
# Uso:
#   scripts/test-smtp.sh [destinatario]
#
# Lee MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD /
# MAIL_FROM desde el entorno (Render las inyecta). El destinatario
# se toma del argumento o de MAIL_DIAGNOSTIC_TO.
# ============================================================
set -euo pipefail

MAIL_HOST="${MAIL_HOST:?MAIL_HOST no definido en el entorno}"
MAIL_PORT="${MAIL_PORT:-587}"
MAIL_USERNAME="${MAIL_USERNAME:?MAIL_USERNAME no definido en el entorno}"
MAIL_PASSWORD="${MAIL_PASSWORD:?MAIL_PASSWORD no definido en el entorno}"
MAIL_FROM="${MAIL_FROM:?MAIL_FROM no definido en el entorno}"

TEST_TO="${1:-${MAIL_DIAGNOSTIC_TO:-}}"
if [ -z "${TEST_TO}" ]; then
  echo "Uso: scripts/test-smtp.sh <destinatario> (o define MAIL_DIAGNOSTIC_TO)" >&2
  exit 1
fi

echo "Probando SMTP ${MAIL_HOST}:${MAIL_PORT} desde=${MAIL_FROM} a=${TEST_TO}"

MSG="$(mktemp)"
trap 'rm -f "$MSG"' EXIT
cat > "$MSG" <<EOF
From: KIN Platform <${MAIL_FROM}>
To: <${TEST_TO}>
Subject: KIN — prueba SMTP (test-smtp.sh)
Date: $(date -R)

Correo de prueba del script test-smtp.sh de KIN.
Si lo recibes, el envío SMTP funciona correctamente.
EOF

if [ "${MAIL_PORT}" = "465" ]; then
  URL="smtps://${MAIL_HOST}:${MAIL_PORT}"
  EXTRA_ARGS=()
else
  URL="smtp://${MAIL_HOST}:${MAIL_PORT}"
  EXTRA_ARGS=(--ssl-reqd)
fi

curl --url "$URL" \
  "${EXTRA_ARGS[@]}" \
  --mail-from "${MAIL_FROM}" \
  --mail-rcpt "${TEST_TO}" \
  --user "${MAIL_USERNAME}:${MAIL_PASSWORD}" \
  --upload-file "$MSG"

echo
echo "SMTP OK: el servidor aceptó el mensaje."

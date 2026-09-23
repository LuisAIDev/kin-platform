#!/usr/bin/env bash
# Smoke test para KIN Billing (Dia 9)
# Uso: bash kin-backend/smoke-test.sh
BASE_URL="https://kin-backend-lwmy.onrender.com/api/v1"
FRONT_URL="https://kin-platform.com"

echo "=== Smoke Test KIN Billing ==="

# 1. Health check (publico)
echo "[1] Health check..."
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/actuator/health"
# Esperado: 200

# 2. Frontend (publico)
echo "[2] Frontend..."
curl -s -o /dev/null -w "%{http_code}\n" "$FRONT_URL/dashboard/physician"
# Esperado: 200 (o redirect a login)

# 3. Endpoints billing (requieren JWT; 401/403 si no hay token es esperado)
echo "[3] Billing protegido (sin token -> 401/403 esperado)..."
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/billing/contracts"
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/billing/dashboard/kpis"

# 4. Verificar schema version en Neon (ejecutar via flyway:info o SQL)
echo "[4] Flyway schema version (manual):"
echo "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
# Esperado: V60 success=true

echo "=== Smoke Test completo ==="

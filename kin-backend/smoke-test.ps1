# Smoke test para KIN Billing (Dia 9)
# Uso: pwsh kin-backend/smoke-test.ps1
$BaseUrl = "https://kin-backend-lwmy.onrender.com/api/v1"
$FrontUrl = "https://kin-platform.com"

Write-Output "=== Smoke Test KIN Billing ==="

Write-Output "[1] Health check..."
try { (Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 30).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }

Write-Output "[2] Frontend..."
try { (Invoke-WebRequest -Uri "$FrontUrl/dashboard/physician" -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 30).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }

Write-Output "[3] Billing protegido (sin token -> 401/403 esperado)..."
foreach ($path in @("/billing/contracts", "/billing/dashboard/kpis")) {
    try { (Invoke-WebRequest -Uri "$BaseUrl$path" -UseBasicParsing -TimeoutSec 30).StatusCode }
    catch { $_.Exception.Response.StatusCode.value__ }
}

Write-Output "[4] Flyway schema version (manual):"
Write-Output "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

Write-Output "=== Smoke Test completo ==="

# ============================================================
# KIN Platform — Arranque del backend con guard FAIL-FAST (Windows)
#
# Nunca lanza una segunda instancia de Spring Boot. Antes de
# ejecutar spring-boot:run comprueba:
#   1. java.exe con KinApplication (app ya arrancada)
#   2. Maven Wrapper (mvnw spring-boot:run en curso)
#   3. Puerto 8080 escuchando
# Si cualquiera existe, NO arranca otro backend, muestra
# "Backend ya está ejecutándose." y deja que se reutilice.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File scripts/start-dev-backend.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "kin-backend"
$mvnw = Join-Path $backend "mvnw.cmd"
$port = 8080

$already = @()

# 1. ¿Java con KinApplication ya en marcha?
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "KinApplication" } |
    ForEach-Object { $already += "java KinApplication (PID $($_.ProcessId))" }

# 2. ¿Maven Wrapper lanzando spring-boot:run?
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "MavenWrapperMain" -and $_.CommandLine -match "spring-boot:run" } |
    ForEach-Object { $already += "Maven Wrapper (PID $($_.ProcessId))" }

# 3. ¿Puerto en escucha?
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $pidToKeep = $listener | Select-Object -First 1 -ExpandProperty OwningProcess
    $already += "puerto $port (PID $pidToKeep)"
}

if ($already.Count -gt 0) {
    Write-Host "Backend ya esta ejecutandose: $($already -join ', ')." -ForegroundColor Yellow
    Write-Host "No se inicia otra instancia. Reutilizalo en http://localhost:$port/api/v1" -ForegroundColor Green
    exit 0
}

if (-not (Test-Path -LiteralPath $mvnw)) {
    Write-Error "No se encontro $mvnw"
}

Write-Host "No hay backend en marcha. Arrancando UNA instancia de Spring Boot..." -ForegroundColor Cyan
Write-Host "Backend disponible en http://localhost:$port/api/v1 (Ctrl+C para detener)." -ForegroundColor Green
Write-Host ""

Push-Location $backend
try {
    & $mvnw "spring-boot:run"
} finally {
    Pop-Location
}

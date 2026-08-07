# ============================================================
# KIN Platform - Arranque del backend con guard FAIL-FAST (Windows)
#
# Nunca lanza una segunda instancia de Spring Boot. Antes de
# ejecutar spring-boot:run comprueba:
#   1. java.exe con KinApplication (app ya arrancada)
#   2. Maven Wrapper (mvnw spring-boot:run en curso)
#   3. Puerto 8080 escuchando
# Si cualquiera existe, NO arranca otro backend, muestra
# "Backend ya est� ejecut�ndose." y deja que se reutilice.
#
# Dev usa PostgreSQL (perfil 'dev', en vez de H2 - H2-2). Antes de arrancar
# comprueba la base de datos:
#   - DATABASE_PASSWORD debe estar definida (env o .env).
#   - PostgreSQL debe responder en DATABASE_URL (por defecto localhost:5432).
# Si la base no est� disponible, informa el problema y DETIENE (sin bucle).
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

# 1. �Java con KinApplication ya en marcha?
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "KinApplication" } |
    ForEach-Object { $already += "java KinApplication (PID $($_.ProcessId))" }

# 2. �Maven Wrapper lanzando spring-boot:run?
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "MavenWrapperMain" -and $_.CommandLine -match "spring-boot:run" } |
    ForEach-Object { $already += "Maven Wrapper (PID $($_.ProcessId))" }

# 3. �Puerto en escucha?
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

# --- Chequeo de base PostgreSQL (perfil dev) -------------------------------
$dbHost = "localhost"
$dbPort = 5432
if ($env:DATABASE_URL -and $env:DATABASE_URL -match 'jdbc:postgresql://([^:/]+):(\d+)/') {
    $dbHost = $matches[1]
    $dbPort = [int]$matches[2]
}

if (-not $env:DATABASE_PASSWORD) {
    Write-Host "ERROR: DATABASE_PASSWORD no esta definida." -ForegroundColor Red
    Write-Host "  El perfil dev usa PostgreSQL (no H2). Define en .env o en el entorno:" -ForegroundColor Yellow
    Write-Host "    DATABASE_URL=jdbc:postgresql://localhost:5432/kin_platform" -ForegroundColor Yellow
    Write-Host "    DATABASE_USER=kin_admin" -ForegroundColor Yellow
    Write-Host "    DATABASE_PASSWORD=<igual que POSTGRES_PASSWORD del docker-compose.yml>" -ForegroundColor Yellow
    exit 1
}

$tcpOk = $false
$client = New-Object System.Net.Sockets.TcpClient
try {
    $task = $client.ConnectAsync($dbHost, $dbPort)
    $tcpOk = $task.Wait(3000) -and $client.Connected
}
catch {
    $tcpOk = $false
}
finally {
    $client.Dispose()
}

if (-not $tcpOk) {
    Write-Host "ERROR: PostgreSQL no responde en $dbHost`:$dbPort." -ForegroundColor Red
    Write-Host "  Levanta la base de desarrollo:" -ForegroundColor Yellow
    Write-Host "    docker compose up -d postgres-db   (usa POSTGRES_PASSWORD del .env)" -ForegroundColor Yellow
    Write-Host "  El backend no se inicia sin base disponible." -ForegroundColor Yellow
    exit 1
}

Write-Host "PostgreSQL detectado en $dbHost`:$dbPort. Arrancando backend con perfil 'dev'..." -ForegroundColor Cyan
Write-Host "Backend disponible en http://localhost:$port/api/v1 (Ctrl+C para detener)." -ForegroundColor Green
Write-Host ""

Push-Location $backend
try {
    & $mvnw "spring-boot:run" "-Dspring-boot.run.profiles=dev"
}
finally {
    Pop-Location
}

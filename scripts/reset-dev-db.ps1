# ============================================================
# KIN Platform - Reset de base de datos PostgreSQL local (Windows)
#
# Detiene el backend si est� corriendo, ELIMINA el esquema public de la
# base de desarrollo (PostgreSQL, perfil 'dev') y vuelve a arrancar Spring
# Boot. Flyway recrea V1..V11 desde cero en el siguiente arranque.
#
# IMPORTANTE: operaci�n DESTRUCTIVA (DROP SCHEMA public CASCADE). Requiere
# confirmaci�n expl�cita (escribir RESET) y solo est� soportada para la
# base local (localhost). Nunca ejecuta el reset sobre una base remota.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File scripts/reset-dev-db.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "kin-backend"
$mvnw = Join-Path $backend "mvnw.cmd"
$compose = Join-Path $root "docker-compose.yml"
$port = 8080

Write-Host "=== KIN - Reset de base PostgreSQL local (dev) ===" -ForegroundColor Cyan

# 1. Detener la aplicaci�n si est� corriendo (puerto 8080 o proceso mvnw)
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $pidToStop = $listener | Select-Object -First 1 -ExpandProperty OwningProcess
    Write-Host "[1/4] Backend detectado en el puerto $port (PID $pidToStop). Deteniendo..." -ForegroundColor Yellow
    Stop-Process -Id $pidToStop -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
} else {
    Write-Host "[1/4] No hay backend corriendo en el puerto $port." -ForegroundColor Green
}

# Limpieza extra: procesos Maven wrapper hu�rfanos de este proyecto
Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='cmd.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match [regex]::Escape($backend) -and $_.CommandLine -match "spring-boot:run" } |
    ForEach-Object {
        Write-Host "      Deteniendo proceso relacionado (PID $($_.ProcessId))." -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
Start-Sleep -Seconds 1

# 2. Determinar la base de desarrollo
$dbHost = "localhost"
$dbPort = 5432
$dbName = "kin_platform"
$dbUser = "kin_admin"
if ($env:DATABASE_URL -and $env:DATABASE_URL -match 'jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') {
    $dbHost = $matches[1]
    $dbPort = [int]$matches[2]
    $dbName = $matches[3]
}
if ($env:DATABASE_USER) {
    $dbUser = $env:DATABASE_USER
}

# Solo se resetea la base local (no se ejecuta un DROP remoto autom�tico).
if ($dbHost -notin @("localhost", "127.0.0.1", "::1")) {
    Write-Host "ERROR: El reset destructivo solo est� soportado para la base DEV local." -ForegroundColor Red
    Write-Host "  DATABASE_URL apunta a $dbHost — no se ejecuta el reset autom�ticamente." -ForegroundColor Yellow
    exit 1
}

# 3. Confirmaci�n expl�cita (operaci�n destructiva, nunca autom�tica)
Write-Host "[2/4] ADVERTENCIA: se ELIMINAR�N TODOS LOS DATOS del esquema public de" -ForegroundColor Red
Write-Host "      $dbHost`:$dbPort/$dbName" -ForegroundColor Red
$confirm = Read-Host "      Escribe RESET para confirmar"
if ($confirm -ne "RESET") {
    Write-Host "      Cancelado. No se modific� la base." -ForegroundColor Yellow
    exit 0
}

# 4. Eliminar el esquema (v�a psql del contenedor postgres-db de Compose)
Write-Host "[3/4] Eliminando esquema public de $dbName ..." -ForegroundColor Yellow
$pgPasswordArg = @()
if ($env:DATABASE_PASSWORD) {
    $pgPasswordArg = @("-e", "PGPASSWORD=$($env:DATABASE_PASSWORD)")
}
docker compose -f $compose exec -T @pgPasswordArg postgres-db psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1 -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo al resetear la base. Revisa que el servicio postgres-db est� levantado (docker compose up -d postgres-db)."
}

Write-Host "      Esquema public eliminado. Flyway recrear� V1..V11 en el siguiente arranque." -ForegroundColor Green

# 5. Verificar que el wrapper Maven existe
if (-not (Test-Path -LiteralPath $mvnw)) {
    Write-Error "No se encontro $mvnw"
}

# 6. Arrancar Spring Boot de nuevo (perfil dev)
Write-Host "[4/4] Arrancando Spring Boot (perfil 'dev')..." -ForegroundColor Cyan
Write-Host "Backend disponible en http://localhost:$port/api/v1 (Ctrl+C para detener)." -ForegroundColor Green
Write-Host ""

Push-Location $backend
try {
    & $mvnw "spring-boot:run" "-Dspring-boot.run.profiles=dev"
}
finally {
    Pop-Location
}

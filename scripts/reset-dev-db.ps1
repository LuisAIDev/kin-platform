# ============================================================
# KIN Platform — Reset de base de datos H2 local (Windows)
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
#   powershell -ExecutionPolicy Bypass -File scripts/reset-dev-db.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "kin-backend"
$dbFile = Join-Path $backend "data\kindb.mv.db"
$mvnw = Join-Path $backend "mvnw.cmd"
$port = 8080

Write-Host "=== KIN — Reset de base H2 local (dev) ===" -ForegroundColor Cyan

# 1. Detener la aplicación si está corriendo (puerto 8080 o proceso mvnw)
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $pidToStop = $listener | Select-Object -First 1 -ExpandProperty OwningProcess
    Write-Host "[1/4] Backend detectado en el puerto $port (PID $pidToStop). Deteniendo..." -ForegroundColor Yellow
    Stop-Process -Id $pidToStop -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
} else {
    Write-Host "[1/4] No hay backend corriendo en el puerto $port." -ForegroundColor Green
}

# Limpieza extra: procesos Maven wrapper huérfanos de este proyecto
Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='cmd.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match [regex]::Escape($backend) -and $_.CommandLine -match "spring-boot:run" } |
    ForEach-Object {
        Write-Host "      Deteniendo proceso relacionado (PID $($_.ProcessId))." -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
Start-Sleep -Seconds 1

# 2. Eliminar la base H2 persistente (kindb.mv.db + kindb.trace.db)
$traceFile = Join-Path $backend "data\kindb.trace.db"
if (Test-Path -LiteralPath $dbFile) {
    Write-Host "[2/4] Eliminando base obsoleta: $dbFile" -ForegroundColor Yellow
    Remove-Item -LiteralPath $dbFile -Force
    Remove-Item -LiteralPath $traceFile -Force -ErrorAction SilentlyContinue
    Write-Host "      Base eliminada. Hibernate la recreara desde cero al arrancar." -ForegroundColor Green
} else {
    Write-Host "[2/4] No existe base local ($dbFile). Se creara una nueva al arrancar." -ForegroundColor Green
    Remove-Item -LiteralPath $traceFile -Force -ErrorAction SilentlyContinue
}

# 3. Verificar que el wrapper Maven existe
if (-not (Test-Path -LiteralPath $mvnw)) {
    Write-Error "No se encontro $mvnw"
}

# 4. Arrancar Spring Boot de nuevo
Write-Host "[3/4] Arrancando Spring Boot..." -ForegroundColor Cyan
Write-Host "[4/4] Backend disponible en http://localhost:8080/api/v1 (Ctrl+C para detener)." -ForegroundColor Green
Write-Host ""

Push-Location $backend
try {
    & $mvnw "spring-boot:run"
} finally {
    Pop-Location
}

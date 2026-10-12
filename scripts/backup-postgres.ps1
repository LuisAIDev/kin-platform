#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Backup de PostgreSQL para Kin Medical On-Premise.
.DESCRIPTION
    Genera un backup comprimido de la base de datos usando pg_dump,
    verifica su integridad y aplica retención configurable.
.PARAMETER BackupDir
    Directorio de destino (default: C:\Backups\kin)
.PARAMETER RetentionDays
    Días a retener backups (default: 30)
.PARAMETER DatabaseUrl
    Connection string de PostgreSQL (requerido, o via $env:DATABASE_URL)
.EXAMPLE
    .\backup-postgres.ps1 -DatabaseUrl "postgresql://user:pass@localhost:5432/kin"
.EXAMPLE
    $env:DATABASE_URL="postgresql://user:pass@localhost:5432/kin"; .\backup-postgres.ps1
#>

param(
    [string]$BackupDir = "C:\Backups\kin",
    [int]$RetentionDays = 30,
    [string]$DatabaseUrl = $env:DATABASE_URL
)

$ErrorActionPreference = "Stop"

if (-not $DatabaseUrl) {
    Write-Error "DATABASE_URL no definida. Pásala como parámetro o variable de entorno."
    exit 1
}

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $BackupDir "kin_backup_$timestamp.dump"

Write-Host "[$(Get-Date)] Iniciando backup de KIN..."
& pg_dump $DatabaseUrl -Fc -f $backupFile
Write-Host "[$(Get-Date)] Backup completado: $backupFile"

# Verificar integridad
& pg_restore --list $backupFile > $null
Write-Host "[$(Get-Date)] Integridad verificada"

# Rotación: eliminar backups > RetentionDays
$cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem $BackupDir -Filter "kin_backup_*.dump" | Where-Object { $_.LastWriteTime -lt $cutoff } | Remove-Item -Force
Write-Host "[$(Get-Date)] Rotación aplicada (> $RetentionDays días)"
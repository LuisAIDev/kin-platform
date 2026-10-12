#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Restauración de PostgreSQL para Kin Medical On-Premise.
.DESCRIPTION
    Restaura un backup generado con backup-postgres.ps1 usando pg_restore.
.PARAMETER BackupFile
    Ruta al archivo .dump a restaurar (requerido).
.PARAMETER DatabaseUrl
    Connection string de PostgreSQL (requerido, o via $env:DATABASE_URL).
.PARAMETER Force
    Omite confirmación interactiva (útil para automatización).
.EXAMPLE
    .\restore-postgres.ps1 -BackupFile "C:\Backups\kin\kin_backup_20260111_030000.dump" -DatabaseUrl "postgresql://user:pass@localhost:5432/kin"
.EXAMPLE
    .\restore-postgres.ps1 -BackupFile "kin_backup_20260111_030000.dump" -Force
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$DatabaseUrl = $env:DATABASE_URL,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if (-not $DatabaseUrl) {
    Write-Error "DATABASE_URL no definida. Pásala como parámetro o variable de entorno."
    exit 1
}

if (-not (Test-Path $BackupFile)) {
    Write-Error "Archivo no encontrado: $BackupFile"
    exit 1
}

if (-not $Force) {
    Write-Warning "ADVERTENCIA: Esto reemplazará los datos actuales de $DatabaseUrl"
    $confirm = Read-Host "¿Continuar? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Host "Cancelado"
        exit 0
    }
}

Write-Host "[$(Get-Date)] Iniciando restauración desde $BackupFile..."
& pg_restore --clean --if-exists --no-owner -d $DatabaseUrl $BackupFile
Write-Host "[$(Get-Date)] Restauración completada"
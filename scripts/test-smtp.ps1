# ============================================================
# test-smtp.ps1 — Prueba de SMTP desde Windows/PowerShell local.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File scripts/test-smtp.ps1 [destinatario]
#
# Lee MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD /
# MAIL_FROM desde el entorno (o .env local). El destinatario se toma
# del argumento o de MAIL_DIAGNOSTIC_TO.
# ============================================================
param(
    [string]$To
)

$ErrorActionPreference = "Stop"

if (-not $env:MAIL_HOST) { throw "MAIL_HOST no definido en el entorno" }
if (-not $env:MAIL_USERNAME) { throw "MAIL_USERNAME no definido en el entorno" }
if (-not $env:MAIL_PASSWORD) { throw "MAIL_PASSWORD no definido en el entorno" }
if (-not $env:MAIL_FROM) { throw "MAIL_FROM no definido en el entorno" }

$hostName = $env:MAIL_HOST
$port = if ($env:MAIL_PORT) { $env:MAIL_PORT } else { "587" }
$from = $env:MAIL_FROM
$username = $env:MAIL_USERNAME
$password = $env:MAIL_PASSWORD
$target = $To
if (-not $target -and $env:MAIL_DIAGNOSTIC_TO) { $target = $env:MAIL_DIAGNOSTIC_TO }
if (-not $target) { throw "Uso: test-smtp.ps1 <destinatario> (o define MAIL_DIAGNOSTIC_TO)" }

Write-Host "Probando SMTP $hostName`:$port desde=$from a=$target"

$body = "From: KIN Platform <$from>`r`n" +
        "To: <$target>`r`n" +
        "Subject: KIN — prueba SMTP (test-smtp.ps1)`r`n`r`n" +
        "Correo de prueba del script test-smtp.ps1 de KIN.`r`n"

# SMTPClient con STARTTLS (usar SmtpSslDefault si el puerto es 465 = SMTPS).
$smtp = New-Object System.Net.Mail.SmtpClient($hostName, [int]$port)
$smtp.EnableSsl = $true
$smtp.Credentials = New-Object System.Net.NetworkCredential($username, $password)
$smtp.Timeout = 15000

$message = New-Object System.Net.Mail.MailMessage($from, $target, "KIN — prueba SMTP (test-smtp.ps1)", $body)
try {
    $smtp.Send($message)
    Write-Host "SMTP OK: el servidor aceptó el mensaje."
} finally {
    $message.Dispose()
    $smtp.Dispose()
}

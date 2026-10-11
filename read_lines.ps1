$textPath = "$env:TEMP\anexo_2275_extract\anexo_texto.txt"
$lines = Get-Content $textPath
Write-Host "Total lineas: " $lines.Count
for ($i = 0; $i -lt [Math]::Min(200, $lines.Count); $i++) {
    Write-Host ("{0}: {1}" -f $i, $lines[$i])
}
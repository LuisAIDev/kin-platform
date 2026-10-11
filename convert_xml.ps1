$xmlPath = "$env:TEMP\anexo_2275_extract\word\document.xml"
$rawXml = Get-Content $xmlPath -Raw -Encoding UTF8
$rawXml = $rawXml -replace '</w:p>', "`n"
$rawXml = $rawXml -replace '<[^>]+>', ''
$rawXml = $rawXml -replace '&', '&'
$rawXml = $rawXml -replace '<', '<'
$rawXml = $rawXml -replace '>', '>'
$rawXml = $rawXml -replace '"', '"'
$textPath = "$env:TEMP\anexo_2275_extract\anexo_texto.txt"
$rawXml | Set-Content $textPath -Encoding UTF8
Get-Item $textPath | Select-Object Length
Write-Host "Total de lineas: $((Get-Content $textPath).Count)"
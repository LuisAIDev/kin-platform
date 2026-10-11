$textPath = "$env:TEMP\anexo_2275_extract\anexo_texto.txt"
Write-Host "===== PRIMERAS 50 LINEAS ====="
Get-Content $textPath -TotalCount 50
Write-Host "`n===== BUSCANDO SECCIONES CLAVE ====="
Select-String -Path $textPath -Pattern "CustomTagGeneral|UBLExtension|DianExtensions|Interoperabilidad|Firma digital" | Select-Object LineNumber, Line -First 30
Write-Host "`n===== BUSCANDO ESTRUCTURA RIPS ====="
Select-String -Path $textPath -Pattern "tipoOperacion|codigoPrestador|numDocumento|tipoDocumento|usuarios|consultas|procedimientos|urgencias|hospitalizacion|recienNacidos" | Select-Object LineNumber, Line -First 40
Write-Host "`n===== BUSCANDO EJEMPLOS JSON/XML ====="
Select-String -Path $textPath -Pattern '^\s*\{|^\s*"|^\s*<' | Select-Object LineNumber, Line -First 50
Write-Host "`n===== BUSCANDO TABLAS DE CAMPOS ====="
Select-String -Path $textPath -Pattern "obligatorio|Obligatorio|OBLIGATORIO|condicional|Condicional" | Select-Object LineNumber, Line -First 30
Write-Host "`n===== BUSCANDO POR TIPO (AC, AP, AU, AT, AF, US) ====="
Select-String -Path $textPath -Pattern "\bAC\b|\bAP\b|\bAU\b|\bAT\b|\bAF\b|\bUS\b|Consulta|Procedimiento|Urgencia|Hospitalizaci" | Select-Object LineNumber, Line -First 50
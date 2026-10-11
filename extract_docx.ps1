$docx = 'docs\rips\Anexo_Tecnico_2275_2023.docx'
$tempDir = "$env:TEMP\anexo_2275_extract"
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$fullPath = Resolve-Path $docx
[System.IO.Compression.ZipFile]::ExtractToDirectory($fullPath, $tempDir)
Get-ChildItem -Path $tempDir -Recurse | Select-Object FullName, Length
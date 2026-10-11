Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('docs\rips\Anexo_Tecnico_2275_2023.docx')
$entry = $zip.GetEntry('word/document.xml')
$sr = New-Object System.IO.StreamReader($entry.Open())
$xml = $sr.ReadToEnd()
$sr.Close()
$zip.Dispose()
$text = $xml -replace '<[^>]+>', ''
$len = $text.Length
$text.Substring(13000, [Math]::Min(5000, $len - 13000))
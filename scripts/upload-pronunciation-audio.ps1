<#
Uploads the pronunciation mp3 files for the vocabulary catalog to the Supabase "media" bucket.

Expected layout under -SourceDir (default: $env:PRONUNCIATION_AUDIO_DIR):
  uk\<slug>.mp3
  us\<slug>.mp3
Each file is stored at vocabulary/pronunciation/{uk,us}/<slug>.mp3, the key V35 writes into vocabulary_items.

Environment:
  SUPABASE_URL          project URL, e.g. https://<project>.supabase.co
  SUPABASE_SECRET_KEY   secret (service) key with Storage write access; never write it to a file

Use -DryRun to list the uploads without calling Supabase.
#>
param(
    [string]$SourceDir = $env:PRONUNCIATION_AUDIO_DIR,
    [string]$Bucket = 'media',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

if (-not $SourceDir -or -not (Test-Path $SourceDir)) { throw 'Set -SourceDir or PRONUNCIATION_AUDIO_DIR to the folder that holds uk\ and us\.' }
if (-not $DryRun) {
    if (-not $env:SUPABASE_URL) { throw 'SUPABASE_URL is not set.' }
    if (-not $env:SUPABASE_SECRET_KEY) { throw 'SUPABASE_SECRET_KEY is not set.' }
}

$base = if ($env:SUPABASE_URL) { $env:SUPABASE_URL.TrimEnd('/') } else { '' }
$uploaded = 0
foreach ($accent in 'uk', 'us') {
    $folder = Join-Path $SourceDir $accent
    if (-not (Test-Path $folder)) { throw "Missing folder: $folder" }
    foreach ($file in Get-ChildItem $folder -Filter *.mp3 -File) {
        $key = "vocabulary/pronunciation/$accent/$($file.Name)"
        if ($DryRun) { Write-Output "would upload $key"; $uploaded++; continue }
        $headers = @{ Authorization = "Bearer $env:SUPABASE_SECRET_KEY"; apikey = $env:SUPABASE_SECRET_KEY; 'x-upsert' = 'true' }
        Invoke-RestMethod -Method Put -Uri "$base/storage/v1/object/$Bucket/$key" -Headers $headers `
            -ContentType 'audio/mpeg' -InFile $file.FullName | Out-Null
        $uploaded++
        if ($uploaded % 50 -eq 0) { Write-Output "uploaded $uploaded" }
    }
}
Write-Output "done: $uploaded files"
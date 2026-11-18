<#
Uploads the twelve placement Listening recordings to the Supabase media bucket under the keys the seed migrations
store in content_assets.media_reference. The database never holds the audio or a URL, only the key, so there is no
database step: once the files are in the bucket, the migrations (V138 to V140) already point at them.

Expected layout under -SourceDir (default services\content-service\media, ignored by Git):
  listening\placement\<package code in lower case>\part<N>.mp3
for example listening\placement\placement-b03-a\part1.mp3. The list of expected files comes from
generate-placement-seed.mjs --keys, so it always matches the seeds.

Environment (read from the process, or from the repository root .env when not set; never printed):
  SUPABASE_URL          project URL, e.g. https://<project>.supabase.co
  SUPABASE_SECRET_KEY   secret (service) key with Storage write access
  CONTENT_MEDIA_BUCKET  bucket name, default "media"

Nothing is uploaded unless all twelve files are present. An object that already exists is not replaced unless
-Overwrite is given. Use -DryRun to list what would happen without calling Supabase.
#>
param(
    [string]$SourceDir,
    [string]$Bucket,
    [switch]$DryRun,
    [switch]$Overwrite
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $SourceDir) { $SourceDir = Join-Path $root 'services\content-service\media' }

# Fills only the variables this script needs from the root .env, and only when they are not already set.
function Import-DotEnv([string[]]$Names) {
    $file = Join-Path $root '.env'
    if (-not (Test-Path $file)) { return }
    foreach ($line in Get-Content $file) {
        if ($line -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)$' -and $Names -contains $Matches[1]) {
            if (-not [Environment]::GetEnvironmentVariable($Matches[1])) {
                [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"))
            }
        }
    }
}
Import-DotEnv @('SUPABASE_URL', 'SUPABASE_SECRET_KEY', 'CONTENT_MEDIA_BUCKET')

if (-not $Bucket) { $Bucket = if ($env:CONTENT_MEDIA_BUCKET) { $env:CONTENT_MEDIA_BUCKET } else { 'media' } }
if (-not $DryRun) {
    if (-not $env:SUPABASE_URL) { throw 'SUPABASE_URL is not set.' }
    if (-not $env:SUPABASE_SECRET_KEY) { throw 'SUPABASE_SECRET_KEY is not set.' }
}
$base = if ($env:SUPABASE_URL) { $env:SUPABASE_URL.TrimEnd('/') } else { '<SUPABASE_URL>' }

$keys = @(& node (Join-Path $PSScriptRoot 'generate-placement-seed.mjs') --keys) | Where-Object { $_ }
if ($keys.Count -ne 12) { throw "Expected 12 keys from the generator, got $($keys.Count)." }

$missing = @()
$tooSmall = @()
foreach ($key in $keys) {
    $local = Join-Path $SourceDir ($key -replace '/', '\')
    if (-not (Test-Path $local)) { $missing += $key }
    elseif ((Get-Item $local).Length -lt 1024) { $tooSmall += $key }
}
if ($missing.Count -gt 0) {
    Write-Output "Missing $($missing.Count) of 12 files under ${SourceDir}:"
    $missing | ForEach-Object { Write-Output "  $_" }
}
if ($tooSmall.Count -gt 0) {
    Write-Output 'Files that are too small to be recordings:'
    $tooSmall | ForEach-Object { Write-Output "  $_" }
}
if (($missing.Count -gt 0 -or $tooSmall.Count -gt 0) -and -not $DryRun) { throw 'Nothing was uploaded.' }

$uploaded = 0
foreach ($key in $keys) {
    $local = Join-Path $SourceDir ($key -replace '/', '\')
    if (-not (Test-Path $local)) { continue }
    if ($DryRun) { Write-Output "would upload $key"; continue }
    $headers = @{
        Authorization = "Bearer $env:SUPABASE_SECRET_KEY"
        apikey        = $env:SUPABASE_SECRET_KEY
        'x-upsert'    = if ($Overwrite) { 'true' } else { 'false' }
    }
    try {
        Invoke-RestMethod -Method Post -Uri "$base/storage/v1/object/$Bucket/$key" -Headers $headers `
            -ContentType 'audio/mpeg' -InFile $local | Out-Null
        $uploaded++
        Write-Output "uploaded $key"
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        if ($status -eq 400 -or $status -eq 409) { throw "$key already exists in the bucket; rerun with -Overwrite to replace it." }
        throw "Upload of $key failed (HTTP $status)."
    }
}

if ($DryRun) { Write-Output "dry run: $($keys.Count - $missing.Count) of 12 files found"; return }
Write-Output "done: $uploaded files uploaded to bucket $Bucket"
Write-Output "next: node scripts/placement-exams/generate-placement-seed.mjs --audio <CONTENT_MEDIA_BASE_URL>"

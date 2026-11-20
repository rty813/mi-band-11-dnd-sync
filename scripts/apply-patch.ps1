param(
    [Parameter(Mandatory = $true)]
    [string]$DecodedApk,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"
$patch = Join-Path $PSScriptRoot "..\patches\mi-health-3.61.0\patch.diff"
$decodedPath = (Resolve-Path -LiteralPath $DecodedApk).Path
$smali = Join-Path $decodedPath "smali_classes11\com\xiaomi\fitness\devicesettings\utils\ZenUtils.smali"
if (-not (Test-Path -LiteralPath $smali -PathType Leaf)) {
    throw "Expected Mi Fitness 3.61.0 Smali files were not found."
}

$previousCeiling = $env:GIT_CEILING_DIRECTORIES
$env:GIT_CEILING_DIRECTORIES = Split-Path -Parent $decodedPath

Push-Location $decodedPath
try {
    git apply --check -- $patch
    if ($LASTEXITCODE -ne 0) { throw "Patch does not match the decoded APK." }
    if ($CheckOnly) { return }
    git apply -- $patch
    if ($LASTEXITCODE -ne 0) { throw "Patch application failed." }
} finally {
    Pop-Location
    $env:GIT_CEILING_DIRECTORIES = $previousCeiling
}

Write-Host "Patch applied to $decodedPath"

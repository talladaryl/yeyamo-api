[CmdletBinding()]
param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\docs\postman\YeYamo_API.postman_collection.json'),
    [string]$EnvironmentPath = (Join-Path $PSScriptRoot '..\docs\postman\YeYamo_Local.postman_environment.json')
)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$scriptPy = Join-Path $PSScriptRoot 'generate_api_specs.py'

Write-Host "Running YeYamo Unified API Specification & Postman Generator..."
python $scriptPy

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to generate API specifications."
}

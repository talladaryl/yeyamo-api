<#
.SYNOPSIS
Builds YeYamo JARs, warms Docker base images serially, then builds or starts Compose.

.DESCRIPTION
Docker Hub can close a metadata request (EOF) when dozens of Docker builds request
the same Temurin manifest at once. This script first builds the Maven reactor and
pulls every Java/Maven base image one by one with bounded retries. Compose then
builds with a deliberately small parallelism level and reuses the local cache.
#>
[CmdletBinding()]
param(
    [ValidateRange(1, 4)]
    [int]$Parallelism = 1,

    [ValidateRange(1, 5)]
    [int]$PullAttempts = 4,

    [switch]$BuildOnly,

    [switch]$SkipJarBuild
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Invoke-CheckedCommand {
    param(
        [string]$File,
        [string[]]$Arguments,
        [string]$Description
    )

    & $File @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description a échoué (code $LASTEXITCODE)."
    }
}

function Pull-BaseImage {
    param([string]$Image)

    for ($attempt = 1; $attempt -le $PullAttempts; $attempt++) {
        Write-Host "[$attempt/$PullAttempts] Préchargement de $Image"
        & docker pull $Image
        if ($LASTEXITCODE -eq 0) {
            return
        }

        if ($attempt -lt $PullAttempts) {
            $delaySeconds = [Math]::Min(10, [int][Math]::Pow(2, $attempt))
            Write-Warning "Échec de téléchargement de $Image. Nouvelle tentative dans $delaySeconds s."
            Start-Sleep -Seconds $delaySeconds
        }
    }

    throw "Impossible de télécharger $Image après $PullAttempts tentatives. Vérifiez Docker Desktop, le réseau et l'accès à Docker Hub."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven est requis pour construire les JARs avant le build Docker.'
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker Desktop (CLI docker) est requis.'
}

if (-not $SkipJarBuild) {
    Write-Host 'Construction du reactor Maven (tests ignorés) ...'
    # `-DskipTests` is parsed reliably by PowerShell and still avoids executing tests.
    Invoke-CheckedCommand -File 'mvn' -Arguments @('-B', '-DskipTests', 'package') -Description 'La construction Maven'
}

# Garder cette liste alignée sur les FROM de tous les Dockerfile du dépôt.
# Les pulls séquentiels évitent les appels HEAD concurrents vers registry-1.docker.io.
$baseImages = @(
    'eclipse-temurin:21-jre-jammy'
)

foreach ($baseImage in $baseImages) {
    Pull-BaseImage -Image $baseImage
}

$env:COMPOSE_PARALLEL_LIMIT = "$Parallelism"
Write-Host "Build Compose avec une parallélisation limitée à $Parallelism ..."
Invoke-CheckedCommand -File 'docker' -Arguments @('compose', '--parallel', "$Parallelism", 'build') -Description 'Le build Docker Compose'

if (-not $BuildOnly) {
    Write-Host 'Démarrage de la stack Compose ...'
    Invoke-CheckedCommand -File 'docker' -Arguments @('compose', '--parallel', "$Parallelism", 'up', '-d') -Description 'Le démarrage Docker Compose'
}

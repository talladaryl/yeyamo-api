param(
    [int]$ServiceTimeoutSeconds = 900,
    [switch]$Resume
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$allApplicationServices = @(
    "admin-service",
    "analytics-service",
    "api-gateway",
    "auth-service",
    "booking-service",
    "catalog-service",
    "content-service",
    "discovery-service",
    "event-service",
    "feed-service",
    "gamification-service",
    "ingestion-service",
    "interaction-service",
    "media-service",
    "messaging-service",
    "mission-reward-service",
    "moderation-trust-service",
    "notification-service",
    "partner-service",
    "payment-service",
    "place-service",
    "recommendation-service",
    "referral-service",
    "user-service"
)

$mobileGroups = @(
    @("auth-service", "user-service"),
    @("place-service", "catalog-service", "discovery-service"),
    @("media-service", "content-service", "interaction-service", "feed-service"),
    @("event-service", "booking-service", "payment-service"),
    @("notification-service", "messaging-service"),
    @("gamification-service", "mission-reward-service", "referral-service"),
    @("partner-service", "recommendation-service", "moderation-trust-service"),
    @("ingestion-service", "admin-service", "analytics-service"),
    @("api-gateway")
)

$servicePorts = @{
    "auth-service" = 8082
    "api-gateway" = 8083
    "place-service" = 8084
    "event-service" = 8085
    "user-service" = 8086
    "partner-service" = 8087
    "catalog-service" = 8088
    "ingestion-service" = 8089
    "content-service" = 8090
    "interaction-service" = 8091
    "feed-service" = 8092
    "discovery-service" = 8093
    "notification-service" = 8094
    "recommendation-service" = 8095
    "admin-service" = 8096
    "analytics-service" = 8097
    "mission-reward-service" = 8098
    "referral-service" = 8099
    "moderation-trust-service" = 8100
    "media-service" = 8101
    "booking-service" = 8102
    "payment-service" = 8103
    "messaging-service" = 8104
    "gamification-service" = 8105
}

function Invoke-Compose {
    param([string[]]$Arguments)

    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') a échoué avec le code $LASTEXITCODE."
    }
}

function Get-ContainerState {
    param([string]$Service)

    $containerIds = @(& docker compose ps -a -q $Service)
    $containerId = $containerIds |
        Where-Object { $_ } |
        Select-Object -First 1
    if (-not $containerId) {
        return "absent"
    }
    $containerId = ([string]$containerId).Trim()

    $stateLines = @(& docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $containerId)
    if ($LASTEXITCODE -ne 0 -or $stateLines.Count -eq 0) {
        return "unknown"
    }
    return ([string]($stateLines | Select-Object -First 1)).Trim()
}

function Get-ContainerRuntimeState {
    param([string]$Service)

    $containerIds = @(& docker compose ps -a -q $Service)
    $containerId = $containerIds |
        Where-Object { $_ } |
        Select-Object -First 1
    if (-not $containerId) {
        return "absent"
    }

    $stateLines = @(& docker inspect -f "{{.State.Status}}" ([string]$containerId).Trim())
    if ($LASTEXITCODE -ne 0 -or $stateLines.Count -eq 0) {
        return "unknown"
    }
    return ([string]($stateLines | Select-Object -First 1)).Trim()
}

function Test-ServiceReadiness {
    param([string]$Service)

    if (-not $servicePorts.ContainsKey($Service)) {
        return $false
    }
    $port = $servicePorts[$Service]
    $status = & curl.exe -s -o NUL --max-time 5 -w "%{http_code}" "http://127.0.0.1:$port/actuator/health/readiness"
    return $status -eq "200"
}

function Wait-Healthy {
    param(
        [string]$Service,
        [int]$TimeoutSeconds = $ServiceTimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastState = ""

    while ((Get-Date) -lt $deadline) {
        if (Test-ServiceReadiness $Service) {
            Write-Host "[$Service] readiness: UP"
            return
        }
        $state = Get-ContainerState $Service
        if ($state -ne $lastState) {
            Write-Host "[$Service] état: $state"
            $lastState = $state
        }
        if ($state -eq "healthy" -or $state -eq "running") {
            return
        }
        if ($state -eq "exited" -or $state -eq "dead") {
            & docker compose logs --tail=120 $Service
            throw "$Service s'est arrêté pendant son démarrage."
        }
        Start-Sleep -Seconds 5
    }

    & docker compose logs --tail=120 $Service
    throw "$Service n'est pas devenu healthy en $TimeoutSeconds secondes."
}

if (-not $Resume) {
    Write-Host "Arrêt temporaire des JVM applicatives pour supprimer la saturation CPU..."
    Invoke-Compose (@("stop") + $allApplicationServices)

    Write-Host "Démarrage et validation de l'infrastructure..."
    Invoke-Compose @("up", "-d", "postgres", "redis", "kafka", "opensearch")
    Invoke-Compose @("up", "-d", "--force-recreate", "cassandra", "config-server", "registry-service")

    foreach ($service in @("postgres", "redis", "kafka", "opensearch", "cassandra", "config-server", "registry-service")) {
        Wait-Healthy $service
    }
} else {
    Write-Host "Reprise du démarrage existant sans recréer l'infrastructure."
}

foreach ($group in $mobileGroups) {
    Write-Host "Démarrage du lot mobile : $($group -join ', ')"
    $servicesToStart = @()
    foreach ($service in $group) {
        if (Test-ServiceReadiness $service) {
            Write-Host "[$service] déjà prêt"
            continue
        }
        $runtimeState = Get-ContainerRuntimeState $service
        if ($runtimeState -ne "running") {
            $servicesToStart += $service
        } else {
            Write-Host "[$service] déjà en cours de démarrage"
        }
    }
    if ($servicesToStart.Count -gt 0) {
        Invoke-Compose (@("up", "-d", "--no-deps", "--force-recreate") + $servicesToStart)
    }
    foreach ($service in $group) {
        Wait-Healthy $service
    }
}

Write-Host ""
Write-Host "Stack mobile démarré avec succès."
Invoke-Compose @("ps")

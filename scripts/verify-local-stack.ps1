param(
    [string]$ComposeFile = "docker-compose.yml"
)

$ErrorActionPreference = "Stop"

docker compose -f $ComposeFile config --quiet

$requiredServices = @(
    "api-gateway",
    "auth-service",
    "admin-service",
    "user-service",
    "booking-service",
    "payment-service",
    "place-service",
    "event-service",
    "campaign-service",
    "commerce-service",
    "moderation-trust-service",
    "analytics-service",
    "catalog-service",
    "country-config-service",
    "ingestion-service",
    "mission-reward-service",
    "support-service",
    "notification-service",
    "discovery-service",
    "postgres",
    "redis",
    "kafka",
    "opensearch"
)

$configuredServices = docker compose -f $ComposeFile config --services
$missing = $requiredServices | Where-Object { $_ -notin $configuredServices }
if ($missing) {
    throw "Missing Compose services: $($missing -join ', ')"
}

$running = docker compose -f $ComposeFile ps --format json | ConvertFrom-Json
$unhealthy = $running | Where-Object {
    $_.Health -and $_.Health -notin @("healthy", "")
}
if ($unhealthy) {
    $names = $unhealthy | ForEach-Object { "$($_.Name)=$($_.Health)" }
    throw "Unhealthy containers: $($names -join ', ')"
}

Write-Host "Compose configuration and required service coverage are valid."

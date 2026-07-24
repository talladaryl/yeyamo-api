param(
    [string]$GatewayBaseUrl = "http://127.0.0.1:8083",
    [string]$MessagingBaseUrl = "http://127.0.0.1:8104",
    [switch]$RunAuthFlow
)

$ErrorActionPreference = "Stop"
$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-SmokeRequest {
    param(
        [string]$Name,
        [string]$Uri,
        [int[]]$ExpectedStatus,
        [hashtable]$Headers = @{}
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -Method Get -Headers $Headers -UseBasicParsing
        $status = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        } else {
            $failures.Add("${Name}: connexion impossible ($($_.Exception.Message))")
            return
        }
    }

    if ($status -notin $ExpectedStatus) {
        $failures.Add("${Name}: HTTP $status, attendu $($ExpectedStatus -join '/')")
    } else {
        Write-Host "[OK] $Name -> HTTP $status"
    }
}

$gateway = $GatewayBaseUrl.TrimEnd("/")
$messaging = $MessagingBaseUrl.TrimEnd("/")

Invoke-SmokeRequest "Gateway health" "$gateway/actuator/health" @(200)
Invoke-SmokeRequest "OpenAPI mobile" "$gateway/mobile-api/openapi.json" @(200)
Invoke-SmokeRequest "Regions publiques" "$gateway/api/v1/regions" @(200)
Invoke-SmokeRequest "Categories publiques" "$gateway/api/v1/categories" @(200)
Invoke-SmokeRequest "Evenements publics" "$gateway/api/v1/events/upcoming" @(200)
Invoke-SmokeRequest "Catalogue public" "$gateway/api/v1/catalog/assets" @(200)
Invoke-SmokeRequest "Auth/me sans JWT" "$gateway/api/v1/auth/me" @(401)
Invoke-SmokeRequest "WebSocket handshake HTTP" "$messaging/ws/messaging" @(400, 426)

$token = [Environment]::GetEnvironmentVariable("YEYAMO_SMOKE_TOKEN")
$logoutAfterSmoke = $false
if ($RunAuthFlow) {
    $suffix = Get-Random -Minimum 10000000 -Maximum 99999999
    $phone = "+2376$suffix"
    $password = "Yeyamo-Smoke-$suffix!"

    try {
        $registerBody = @{
            email = $null
            phone = $phone
            password = $password
            displayName = "Mobile Smoke"
        } | ConvertTo-Json
        $registered = Invoke-RestMethod `
            -Uri "$gateway/api/v1/auth/register" `
            -Method Post `
            -ContentType "application/json" `
            -Body $registerBody
        if (-not $registered.accessToken -or -not $registered.refreshToken) {
            throw "accessToken ou refreshToken absent"
        }
        Write-Host "[OK] Auth register -> tokens reçus"

        $loginBody = @{
            identifier = $phone
            password = $password
        } | ConvertTo-Json
        $loggedIn = Invoke-RestMethod `
            -Uri "$gateway/api/v1/auth/login" `
            -Method Post `
            -ContentType "application/json" `
            -Body $loginBody
        if (-not $loggedIn.accessToken -or -not $loggedIn.refreshToken) {
            throw "tokens absents après login"
        }
        Write-Host "[OK] Auth login -> tokens reçus"

        $refreshBody = @{ refreshToken = $loggedIn.refreshToken } | ConvertTo-Json
        $refreshed = Invoke-RestMethod `
            -Uri "$gateway/api/v1/auth/refresh" `
            -Method Post `
            -ContentType "application/json" `
            -Body $refreshBody
        if (-not $refreshed.accessToken -or -not $refreshed.refreshToken) {
            throw "tokens absents après refresh"
        }
        Write-Host "[OK] Auth refresh -> rotation reçue"

        $token = $refreshed.accessToken
        $authHeaders = @{ Authorization = "Bearer $token" }
        Invoke-SmokeRequest "Auth/me après refresh" "$gateway/api/v1/auth/me" @(200) $authHeaders
        $logoutAfterSmoke = $true
    } catch {
        $failures.Add("Flux auth complet: $($_.Exception.Message)")
    }
}

if ($token) {
    $headers = @{ Authorization = "Bearer $token" }
    Invoke-SmokeRequest "Auth/me avec JWT" "$gateway/api/v1/auth/me" @(200) $headers
    Invoke-SmokeRequest "Profil courant" "$gateway/api/v1/users/me" @(200) $headers
    Invoke-SmokeRequest "Feed" "$gateway/api/v1/feed?page=0&size=5" @(200) $headers
    Invoke-SmokeRequest "Notifications" "$gateway/api/v1/notifications?page=0&size=5" @(200) $headers
    Invoke-SmokeRequest "Collections" "$gateway/api/v1/collections?page=0&size=5" @(200) $headers
} else {
    Write-Host "[INFO] YEYAMO_SMOKE_TOKEN absent : les lectures authentifiees sont ignorees."
}

if ($logoutAfterSmoke -and $token) {
    try {
        Invoke-WebRequest `
            -Uri "$gateway/api/v1/auth/logout" `
            -Method Post `
            -Headers @{ Authorization = "Bearer $token" } `
            -UseBasicParsing | Out-Null
        Write-Host "[OK] Auth logout -> HTTP 200/204"
    } catch {
        $failures.Add("Auth logout: $($_.Exception.Message)")
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Host "[FAIL] $_" -ForegroundColor Red }
    throw "$($failures.Count) smoke test(s) en echec."
}

Write-Host "Tous les smoke tests locaux demandes ont reussi."

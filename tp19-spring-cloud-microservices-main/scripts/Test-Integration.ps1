param(
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21'
)

# Executer apres les quatre builds Maven. Chaque JVM reste dans sa propre console.
# Le script laisse les quatre applications actives, Gateway en profil discovery.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$root = Split-Path -Parent $PSScriptRoot
$java = Join-Path $JavaHome 'bin\java.exe'
$script:consoles = @{}
$script:checks = 0
$http = New-Object System.Net.Http.HttpClient
$http.Timeout = [TimeSpan]::FromSeconds(10)

function Assert-True($Condition, [string]$Message) {
    if (-not $Condition) { throw "ECHEC : $Message" }
    $script:checks++
    "OK : $Message"
}

function Send-Http([string]$Url, [string]$Method = 'GET', [string]$Body = $null) {
    $request = New-Object System.Net.Http.HttpRequestMessage
    $request.RequestUri = [Uri]$Url
    $request.Method = New-Object System.Net.Http.HttpMethod($Method)
    $request.Headers.Accept.ParseAdd('application/json')
    if ($null -ne $Body -and $Body -ne '') {
        $request.Content = New-Object System.Net.Http.StringContent($Body, [Text.Encoding]::UTF8, 'application/json')
    }
    try {
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        try {
            $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            return [PSCustomObject]@{ Status = [int]$response.StatusCode; Body = $text; Location = [string]$response.Headers.Location }
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}

function Wait-Http([string]$Url) {
    $deadline = (Get-Date).AddSeconds(120)
    do {
        try {
            $response = Send-Http $Url
            if ($response.Status -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Delai de demarrage depasse : $Url"
}

function Start-App([string]$Name, [int]$Port, [string]$Profile = '') {
    $directory = Join-Path $root $Name
    $jar = Join-Path $directory "target\$Name-0.0.1.jar"
    if (-not (Test-Path -LiteralPath $directory)) { throw "Dossier absent : $directory" }
    if (-not (Test-Path -LiteralPath $jar)) { throw "Construire d'abord : $jar" }
    $command = "& '$java' -jar '$jar' --logging.file.name=target/integration.log"
    if ($Profile) { $command += " --spring.profiles.active=$Profile" }
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    $script:consoles[$Port] = Start-Process powershell.exe -WorkingDirectory $directory -ArgumentList @('-NoProfile', '-NoExit', '-EncodedCommand', $encoded) -PassThru
    Wait-Http "http://localhost:$Port/actuator/health"
    "DEMARRE : $Name ($Profile) port $Port"
}

function Stop-App([int]$Port) {
    # N'arreter que les applications lancees par cette execution.
    if (-not $script:consoles.ContainsKey($Port)) { throw "Application non geree : $Port" }
    $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($ownerId in @($listeners.OwningProcess | Sort-Object -Unique)) {
        Stop-Process -Id $ownerId
        Wait-Process -Id $ownerId -Timeout 20 -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $script:consoles[$Port].Id -ErrorAction SilentlyContinue
    $script:consoles.Remove($Port)
    Start-Sleep -Seconds 2
}

function Check-Get([string]$Url, [int]$Expected = 200) {
    $response = Send-Http $Url
    Assert-True ($response.Status -eq $Expected) "GET $Url => $Expected"
}

function Check-Gateway([string]$Profile) {
    foreach ($path in @('/patients', '/patient/1', '/patients/1', '/rdv', '/rdv/1', '/rdv/patient/1')) {
        Check-Get "http://localhost:8888$path"
    }
    Check-Get 'http://localhost:8888/patient/9999' 404
    Check-Get 'http://localhost:8888/rdv/9999' 404
    $rdvs = (Send-Http 'http://localhost:8888/rdv').Body | ConvertFrom-Json
    Assert-True ($rdvs.Count -eq 3 -and $rdvs[0].patient.nom -eq 'Salma BENNANI') "Gateway $Profile : 3 rendez-vous avec patient enrichi par Feign"
}

try {
    foreach ($port in @(8761, 8088, 8089, 8888)) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
            throw "Le port $port est deja occupe. Arreter l'instance existante avant ce test."
        }
    }
    Start-App 'eureka-server' 8761
    $dashboard = Invoke-WebRequest -UseBasicParsing 'http://localhost:8761/'
    Assert-True ($dashboard.StatusCode -eq 200 -and $dashboard.Content -match 'Eureka') 'Page Eureka disponible'
    Start-App 'service-patient' 8088
    Check-Get 'http://localhost:8088/patients'
    Check-Get 'http://localhost:8088/patient/1'
    Check-Get 'http://localhost:8088/patients/1'
    Check-Get 'http://localhost:8088/patient/9999' 404
    $patients = (Send-Http 'http://localhost:8088/patients').Body | ConvertFrom-Json
    Assert-True ($patients.Count -eq 3 -and $patients[0].id -eq 1 -and $patients[2].nom -eq 'Khadija ALAOUI') 'Trois patients de demonstration avec IDs JPA'
    Start-App 'service-rdv' 8089
    Wait-Http 'http://localhost:8089/rdv'
    Check-Get 'http://localhost:8089/rdv'
    Check-Get 'http://localhost:8089/rdv/1'
    Check-Get 'http://localhost:8089/rdv/patient/1'
    Check-Get 'http://localhost:8089/rdv/9999' 404
    Check-Get 'http://localhost:8089/rdv/patient/9999' 404
    Start-App 'api-gateway' 8888 'static'
    Check-Gateway 'static'

    # Retirer la premiere JVM avant de reutiliser 8888.
    Stop-App 8888
    Start-App 'api-gateway' 8888 'discovery'
    Wait-Http 'http://localhost:8888/rdv'
    Check-Gateway 'discovery'

    $registry = (Send-Http 'http://localhost:8761/eureka/apps').Body | ConvertFrom-Json
    foreach ($name in @('SERVICE-PATIENT', 'SERVICE-RDV', 'API-GATEWAY')) {
        $application = @($registry.applications.application | Where-Object { $_.name -eq $name })
        Assert-True ($application.Count -eq 1 -and @($application[0].instance | Where-Object { $_.status -eq 'UP' }).Count -ge 1) "Eureka : $name UP"
    }
    $dashboard = Invoke-WebRequest -UseBasicParsing 'http://localhost:8761/'
    Assert-True ($dashboard.Content -match 'SERVICE-PATIENT' -and $dashboard.Content -match 'SERVICE-RDV' -and $dashboard.Content -match 'API-GATEWAY') 'Page Eureka affiche les trois applications'

    $created = Send-Http 'http://localhost:8888/rdv/1' 'POST' '{"medecin":"Dr Test","specialite":"INTEGRATION","motif":"Controle"}'
    Assert-True ($created.Status -eq 201 -and $created.Location -match '/rdv/\d+') 'POST via Gateway => 201 + Location'
    $id = ($created.Body | ConvertFrom-Json).id
    $updated = Send-Http "http://localhost:8888/rdv/$id" 'PUT' '{"medecin":"Dr Test","specialite":"INTEGRATION-UPDATE","motif":"Suivi","patientId":2}'
    Assert-True ($updated.Status -eq 200) 'PUT via Gateway => 200'
    $read = (Send-Http "http://localhost:8888/rdv/$id").Body | ConvertFrom-Json
    Assert-True ($read.motif -eq 'Suivi' -and $read.patient.nom -eq 'Youssef TAZI') 'Modification persistee et nouveau patient enrichi'

    # Panne reelle du service distant : aucun mock dans cette validation.
    Stop-App 8088
    Check-Get 'http://localhost:8089/rdv/1' 503
    Check-Get 'http://localhost:8888/rdv/1' 503
    Start-App 'service-patient' 8088
    Wait-Http 'http://localhost:8888/rdv/1'
    Check-Get 'http://localhost:8888/rdv/1'

    # Reinitialiser H2 Rdv pour laisser uniquement les trois exemples du TP.
    Stop-App 8089
    Start-App 'service-rdv' 8089
    Wait-Http 'http://localhost:8888/rdv'
    $final = (Send-Http 'http://localhost:8888/rdv').Body | ConvertFrom-Json
    Assert-True ($final.Count -eq 3) 'Etat final : trois rendez-vous de demonstration'
    "INTEGRATION REUSSIE : $script:checks verifications. Applications actives, Gateway discovery."
    Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in @(8761, 8088, 8089, 8888) } | Format-Table LocalAddress, LocalPort, OwningProcess
} finally {
    $http.Dispose()
}

param(
  [string]$BackendHealthUrl = "http://localhost:8090/api/health",
  [string]$FrontendUrl = "http://localhost:3001/",
  [int]$TimeoutSeconds = 180,
  [int]$IntervalSeconds = 2
)

$ErrorActionPreference = "Stop"

function Invoke-HttpGetText {
  param([string]$Url)
  try {
    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
    return [string]$response.Content
  } catch {
    return $null
  }
}

function Wait-Until {
  param(
    [string]$Name,
    [scriptblock]$Probe,
    [scriptblock]$IsReady
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    $value = & $Probe
    if ($null -ne $value -and (& $IsReady $value)) {
      Write-Host ("[OK] {0}" -f $Name)
      return
    }
    Start-Sleep -Seconds $IntervalSeconds
  }

  throw ("[FAIL] {0} not ready within {1}s" -f $Name, $TimeoutSeconds)
}

Write-Host ("Smoke check starting (timeout {0}s)..." -f $TimeoutSeconds)

Wait-Until -Name "backend health ($BackendHealthUrl)" -Probe { Invoke-HttpGetText -Url $BackendHealthUrl } -IsReady {
  param($body)
  return ($body -match '"status"\s*:\s*"UP"')
}

Wait-Until -Name "frontend ($FrontendUrl)" -Probe { Invoke-HttpGetText -Url $FrontendUrl } -IsReady {
  param($body)
  return ($body -match '<div id="app"')
}

Write-Host "Smoke check passed."

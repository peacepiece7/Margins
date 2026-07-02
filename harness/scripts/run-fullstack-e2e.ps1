param(
  [int] $MysqlPort = 3307,
  [int] $BackendPort = 18080,
  [int] $FrontendPort = 15173,
  [int] $TimeoutSeconds = 180,
  [switch] $ReuseExistingServices,
  [switch] $KeepStartedProcesses
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$frontRoot = Join-Path $repoRoot "front"
$artifactRoot = Join-Path $repoRoot "harness\artifacts\e2e"

New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null

function Test-IsWindows {
  return [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
}

function Get-PowerShellExecutable {
  $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
  if ($pwsh) { return $pwsh.Source }

  $windowsPowerShell = Get-Command powershell -ErrorAction SilentlyContinue
  if ($windowsPowerShell) { return $windowsPowerShell.Source }

  throw "PowerShell executable not found. On macOS, use the Node npm commands documented in README.md; this legacy script is optional."
}

function Invoke-PowerShellFile {
  param(
    [string] $Path,
    [string[]] $Arguments = @()
  )

  $powerShell = Get-PowerShellExecutable
  $powerShellArgs = @("-NoProfile")
  if ((Split-Path -Leaf $powerShell) -ieq "powershell.exe" -or (Split-Path -Leaf $powerShell) -ieq "powershell") {
    $powerShellArgs += @("-ExecutionPolicy", "Bypass")
  }
  $powerShellArgs += @("-File", $Path)
  $powerShellArgs += $Arguments

  & $powerShell @powerShellArgs
}

function Test-HttpOk {
  param([string] $Url)

  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
  }
  catch {
    return $false
  }
}

function Test-TcpOpen {
  param([int] $Port)

  $client = New-Object System.Net.Sockets.TcpClient
  try {
    $asyncResult = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
    if (-not $asyncResult.AsyncWaitHandle.WaitOne(1000, $false)) {
      return $false
    }
    $client.EndConnect($asyncResult)
    return $true
  }
  catch {
    return $false
  }
  finally {
    $client.Close()
  }
}

function Wait-HttpOk {
  param(
    [string] $Name,
    [string] $Url,
    [int] $Timeout
  )

  $deadline = (Get-Date).AddSeconds($Timeout)
  do {
    if (Test-HttpOk -Url $Url) {
      Write-Host "$Name is ready: $Url"
      return
    }

    Start-Sleep -Seconds 2
  } while ((Get-Date) -lt $deadline)

  throw "Timed out waiting for $Name at $Url"
}

function Get-ChildProcessIds {
  param([int] $ParentProcessId)

  if (Test-IsWindows) {
    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId=$ParentProcessId" -ErrorAction SilentlyContinue)
    foreach ($child in $children) {
      Get-ChildProcessIds -ParentProcessId $child.ProcessId
      $child.ProcessId
    }
    return
  }

  $pgrep = Get-Command pgrep -ErrorAction SilentlyContinue
  if (-not $pgrep) {
    return
  }

  $childIds = @(& $pgrep.Source -P $ParentProcessId 2>$null | Where-Object { $_ -match '^\d+$' } | ForEach-Object { [int]$_ })
  foreach ($childId in $childIds) {
    Get-ChildProcessIds -ParentProcessId $childId
    $childId
  }
}

function Stop-ProcessTree {
  param([System.Diagnostics.Process] $Process)

  if (-not $Process -or $Process.HasExited) {
    return
  }

  $childIds = @(Get-ChildProcessIds -ParentProcessId $Process.Id)
  foreach ($childId in $childIds) {
    Stop-Process -Id $childId -Force -ErrorAction SilentlyContinue
  }

  Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
}

function Start-BackgroundPowerShell {
  param(
    [string] $Name,
    [string] $Command,
    [string] $LogName
  )

  $stdoutPath = Join-Path $artifactRoot $LogName
  $stderrPath = Join-Path $artifactRoot ($LogName -replace "\.log$", ".err.log")
  foreach ($path in @($stdoutPath, $stderrPath)) {
    if (Test-Path -LiteralPath $path) {
      Remove-Item -LiteralPath $path -Force
    }
  }

  Write-Host "Starting $Name; logs: $stdoutPath, $stderrPath"
  $powerShell = Get-PowerShellExecutable
  $powerShellArgs = @("-NoProfile")
  if ((Split-Path -Leaf $powerShell) -ieq "powershell.exe" -or (Split-Path -Leaf $powerShell) -ieq "powershell") {
    $powerShellArgs += @("-ExecutionPolicy", "Bypass")
  }
  $powerShellArgs += @("-Command", $Command)

  $startArgs = @{
    FilePath = $powerShell
    ArgumentList = $powerShellArgs
    WorkingDirectory = $repoRoot
    RedirectStandardOutput = $stdoutPath
    RedirectStandardError = $stderrPath
    PassThru = $true
  }
  if (Test-IsWindows) {
    $startArgs.WindowStyle = "Hidden"
  }

  return Start-Process @startArgs
}

$startedProcesses = @()

Push-Location $repoRoot
try {
  $env:MARGINS_MYSQL_PORT = "$MysqlPort"
  $env:SPRING_PROFILES_ACTIVE = if ($env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE } else { "local" }
  $env:SERVER_PORT = "$BackendPort"
  $env:MARGINS_BACKEND_URL = "http://localhost:$BackendPort"
  $env:MARGINS_FRONTEND_PORT = "$FrontendPort"
  $env:MARGINS_FRONT_URL = "http://localhost:$FrontendPort"

  Write-Host "Starting MySQL on host port $MysqlPort and applying schema."
  Invoke-PowerShellFile -Path "infra/scripts/mysql-up.ps1" -Arguments @("-ApplySchema", "-TimeoutSeconds", "$TimeoutSeconds")
  if ($LASTEXITCODE -ne 0) {
    throw "MySQL bootstrap failed with exit code $LASTEXITCODE"
  }

  $backendHealthUrl = "http://localhost:$BackendPort/api/health"
  if (Test-TcpOpen -Port $BackendPort) {
    if ($ReuseExistingServices) {
      if (-not (Test-HttpOk -Url $backendHealthUrl)) {
        throw "Backend port $BackendPort is in use but $backendHealthUrl is not healthy. Stop it or choose another -BackendPort."
      }
      Write-Host "Backend is already reachable and will be reused: $backendHealthUrl"
    }
    else {
      throw "Backend port $BackendPort is already in use. Stop it, choose another -BackendPort, or pass -ReuseExistingServices when reuse is intentional."
    }
  }
  else {
    $backendCommand = "`$env:SPRING_PROFILES_ACTIVE='$($env:SPRING_PROFILES_ACTIVE)'; `$env:MARGINS_MYSQL_PORT='$MysqlPort'; `$env:SERVER_PORT='$BackendPort'; & 'back/scripts/test.ps1' -Task bootRun"
    $startedProcesses += Start-BackgroundPowerShell -Name "backend" -Command $backendCommand -LogName "backend.log"
    Wait-HttpOk -Name "Backend" -Url $backendHealthUrl -Timeout $TimeoutSeconds
  }

  $frontendUrl = "http://localhost:$FrontendPort"
  if (Test-TcpOpen -Port $FrontendPort) {
    if ($ReuseExistingServices) {
      if (-not (Test-HttpOk -Url $frontendUrl)) {
        throw "Frontend port $FrontendPort is in use but $frontendUrl is not serving HTTP 2xx. Stop it or choose another -FrontendPort."
      }
      Write-Host "Frontend is already reachable and will be reused: $frontendUrl"
    }
    else {
      throw "Frontend port $FrontendPort is already in use. Stop it, choose another -FrontendPort, or pass -ReuseExistingServices when reuse is intentional."
    }
  }
  else {
    $frontendCommand = "`$env:MARGINS_BACKEND_URL='http://localhost:$BackendPort'; `$env:MARGINS_FRONTEND_PORT='$FrontendPort'; Set-Location 'front'; npm run dev"
    $startedProcesses += Start-BackgroundPowerShell -Name "frontend" -Command $frontendCommand -LogName "frontend.log"
    Wait-HttpOk -Name "Frontend" -Url $frontendUrl -Timeout $TimeoutSeconds
  }

  Push-Location $frontRoot
  try {
    npm run e2e
    if ($LASTEXITCODE -ne 0) {
      throw "Frontend E2E failed with exit code $LASTEXITCODE"
    }
  }
  finally {
    Pop-Location
  }

  Write-Host "PASS: full-stack E2E completed."
}
finally {
  Pop-Location

  if (-not $KeepStartedProcesses) {
    foreach ($process in $startedProcesses) {
      Stop-ProcessTree -Process $process
    }
  }
}

param(
  [string] $ArtifactPath = "infra/artifacts/margins-release.zip",
  [int] $MysqlPort = 3307,
  [int] $BackendPort = 18081,
  [int] $TimeoutSeconds = 120,
  [switch] $SkipArtifactVerify,
  [switch] $SkipMysqlBootstrap
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$artifact = Resolve-Path -LiteralPath (Join-Path $repoRoot $ArtifactPath)
$smokeRoot = Join-Path $repoRoot ("harness\artifacts\release-artifact-runtime\" + [Guid]::NewGuid().ToString("N"))
$stdoutPath = Join-Path $smokeRoot "backend.out.log"
$stderrPath = Join-Path $smokeRoot "backend.err.log"
$backendProcess = $null

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
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
  }
  catch {
    return $false
  }
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

function Wait-Health {
  param(
    [string] $Url,
    [System.Diagnostics.Process] $Process,
    [int] $Timeout
  )

  $deadline = (Get-Date).AddSeconds($Timeout)
  do {
    if ($Process.HasExited) {
      $stdout = if (Test-Path -LiteralPath $stdoutPath) { Get-Content -LiteralPath $stdoutPath -Tail 40 -ErrorAction SilentlyContinue } else { @() }
      $stderr = if (Test-Path -LiteralPath $stderrPath) { Get-Content -LiteralPath $stderrPath -Tail 40 -ErrorAction SilentlyContinue } else { @() }
      throw "Artifact backend process exited before health passed. stdout: $($stdout -join ' ') stderr: $($stderr -join ' ')"
    }

    if (Test-HttpOk -Url $Url) {
      return
    }

    Start-Sleep -Seconds 2
  } while ((Get-Date) -lt $deadline)

  throw "Timed out waiting for release artifact backend health at configured smoke URL."
}

New-Item -ItemType Directory -Force -Path $smokeRoot | Out-Null

$oldEnv = @{
  MARGINS_MYSQL_PORT = $env:MARGINS_MYSQL_PORT
  MARGINS_DB_URL = $env:MARGINS_DB_URL
  MARGINS_MYSQL_USER = $env:MARGINS_MYSQL_USER
  MARGINS_MYSQL_PASSWORD = $env:MARGINS_MYSQL_PASSWORD
  SPRING_PROFILES_ACTIVE = $env:SPRING_PROFILES_ACTIVE
  SERVER_PORT = $env:SERVER_PORT
  OPENAI_API_KEY = $env:OPENAI_API_KEY
}

Push-Location $repoRoot
try {
  if (-not $SkipArtifactVerify) {
    Invoke-PowerShellFile -Path "infra/scripts/verify-artifacts.ps1" -Arguments @("-ArtifactPath", $ArtifactPath)
    if ($LASTEXITCODE -ne 0) {
      throw "Release artifact verification failed with exit code $LASTEXITCODE"
    }
  }

  if (-not $SkipMysqlBootstrap) {
    $env:MARGINS_MYSQL_PORT = "$MysqlPort"
    Invoke-PowerShellFile -Path "infra/scripts/mysql-up.ps1" -Arguments @("-ApplySchema", "-TimeoutSeconds", "$TimeoutSeconds")
    if ($LASTEXITCODE -ne 0) {
      throw "MySQL bootstrap failed with exit code $LASTEXITCODE"
    }
  }

  $healthUrl = "http://127.0.0.1:$BackendPort/api/health"
  if (Test-HttpOk -Url $healthUrl) {
    throw "Backend smoke port $BackendPort is already serving /api/health. Choose a different -BackendPort."
  }

  Expand-Archive -LiteralPath $artifact.Path -DestinationPath $smokeRoot -Force
  $jarPath = Join-Path $smokeRoot "back\margins-back.jar"
  if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Expanded artifact is missing backend jar: back/margins-back.jar"
  }

  $java = Get-Command java -ErrorAction SilentlyContinue
  if (-not $java) {
    throw "java command is required for release artifact runtime smoke."
  }

  $env:MARGINS_MYSQL_PORT = "$MysqlPort"
  $env:MARGINS_DB_URL = "jdbc:mysql://127.0.0.1:$MysqlPort/margins?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  $env:MARGINS_MYSQL_USER = "margins"
  $env:MARGINS_MYSQL_PASSWORD = "margins-pass"
  $env:SPRING_PROFILES_ACTIVE = "local"
  $env:SERVER_PORT = "$BackendPort"
  $env:OPENAI_API_KEY = ""

  $startArgs = @{
    FilePath = $java.Source
    ArgumentList = @("-jar", $jarPath)
    WorkingDirectory = $smokeRoot
    RedirectStandardOutput = $stdoutPath
    RedirectStandardError = $stderrPath
    PassThru = $true
  }
  if (Test-IsWindows) {
    $startArgs.WindowStyle = "Hidden"
  }

  $backendProcess = Start-Process @startArgs

  Wait-Health -Url $healthUrl -Process $backendProcess -Timeout $TimeoutSeconds

  Write-Output "# Release Artifact Runtime Audit"
  Write-Output ""
  Write-Output "Artifact checked: $($artifact.Path)"
  Write-Output "Backend jar launched from expanded release artifact."
  Write-Output "Health smoke passed at configured local port without printing secrets."
  Write-Output ""
  Write-Output "PASS: release artifact backend runtime smoke passed."
}
finally {
  Pop-Location

  if ($backendProcess) {
    Stop-ProcessTree -Process $backendProcess
  }

  foreach ($key in $oldEnv.Keys) {
    Set-Item -Path "env:$key" -Value $oldEnv[$key] -ErrorAction SilentlyContinue
    if ($null -eq $oldEnv[$key]) {
      Remove-Item -Path "env:$key" -ErrorAction SilentlyContinue
    }
  }

  if (Test-Path -LiteralPath $smokeRoot) {
    Remove-Item -LiteralPath $smokeRoot -Recurse -Force
  }
}

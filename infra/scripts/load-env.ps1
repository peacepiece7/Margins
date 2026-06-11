param(
  [string] $EnvPath = ".env"
)

if (-not (Test-Path -LiteralPath $EnvPath)) {
  return
}

Get-Content -LiteralPath $EnvPath | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') {
    return
  }

  $parts = $_ -split '=', 2
  $name = $parts[0].Trim()
  $value = $parts[1].Trim().Trim('"').Trim("'")
  if ($name) {
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
}

<#
  TestPaperGen local acceptance runner.

  Starts three local processes:
    1. MySQL 8 (portable, D:\WorkBuddy\tools\mysql)  -> 127.0.0.1:3306
    2. Spring Boot backend (packaged jar)            -> http://localhost:8081
    3. Umi dev server                                -> http://localhost:8000

  Usage:
    powershell -ExecutionPolicy Bypass -File scripts\local-start.ps1
    powershell -ExecutionPolicy Bypass -File scripts\local-start.ps1 -AiKey sk-xxx -AiBaseUrl https://api.deepseek.com/v1 -AiModel deepseek-chat

  Notes:
    - Without -AiKey the backend runs in Mock mode (ai.enabled=false) and returns placeholder analysis text.
    - The frontend maps its API base to port 8081 (see src/config/requestConfig.js), so the backend must stay on 8081.
#>
param(
  [string]$AiKey = "",
  [string]$AiBaseUrl = "",
  [string]$AiModel = "",
  [switch]$SkipBrowser
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 cannot build the child environment when the process has
# duplicate case-insensitive variables (e.g. HTTPS_PROXY and https_proxy, common with
# Clash / v2ray). It throws "dictionary contains no key: HTTPS_PROXY".
# Normalise them to the upper-case form so Start-Process works and proxy settings survive.
foreach ($n in @('http_proxy', 'https_proxy', 'all_proxy', 'no_proxy')) {
  $v = [Environment]::GetEnvironmentVariable($n, 'Process')
  if ($v) {
    [Environment]::SetEnvironmentVariable($n, $null, 'Process')
    $upper = $n.ToUpperInvariant()
    if (-not [Environment]::GetEnvironmentVariable($upper, 'Process')) {
      [Environment]::SetEnvironmentVariable($upper, $v, 'Process')
    }
  }
}

$Root       = Split-Path -Parent $PSScriptRoot
$ToolsRoot  = 'D:\WorkBuddy\tools'
$MysqlHome  = Join-Path $ToolsRoot 'mysql'
$MysqlData  = Join-Path $ToolsRoot 'mysqldata'
$JavaHome   = Join-Path $ToolsRoot 'jdk8'
$BackendDir = Join-Path $Root 'TestPapaerGen-Backend'
$FrontendDir= Join-Path $Root 'TestPapaerGen-Frontend'
$LogDir     = Join-Path $Root 'logs'
$JarPath    = Join-Path $BackendDir 'target\test-paper-generation-3.0.0.jar'

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Test-Port {
  param([int]$Port)
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port {
  param([int]$Port, [int]$TimeoutSec = 60, [string]$Name = 'service')
  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $deadline) {
    if (Test-Port -Port $Port) { Write-Host ("[ok]   {0} listening on {1}" -f $Name, $Port); return $true }
    Start-Sleep -Milliseconds 800
  }
  Write-Host ("[fail] {0} did not open port {1} within {2}s, check logs in {3}" -f $Name, $Port, $TimeoutSec, $LogDir)
  return $false
}

# ---------------- 1. MySQL ----------------
if (Test-Port -Port 3306) {
  Write-Host '[skip] MySQL already listening on 3306'
} else {
  $mysqld = Join-Path $MysqlHome 'bin\mysqld.exe'
  if (-not (Test-Path $mysqld)) { throw "mysqld not found: $mysqld" }
  if (-not (Test-Path $MysqlData)) { throw "MySQL data dir not found: $MysqlData" }
  Write-Host '[run]  starting MySQL ...'
  Start-Process -FilePath $mysqld `
    -ArgumentList @("--defaults-file=$MysqlHome\my.ini", '--console') `
    -RedirectStandardOutput (Join-Path $LogDir 'mysql.out.log') `
    -RedirectStandardError  (Join-Path $LogDir 'mysql.err.log') `
    -WindowStyle Hidden | Out-Null
  if (-not (Wait-Port -Port 3306 -TimeoutSec 60 -Name 'MySQL')) { exit 1 }
}

# ---------------- 2. Backend ----------------
if (Test-Port -Port 8081) {
  Write-Host '[skip] backend already listening on 8081'
} else {
  if (-not (Test-Path $JarPath)) {
    throw "backend jar not found: $JarPath`nBuild it first: cd TestPapaerGen-Backend; mvn -DskipTests package"
  }
  $java = Join-Path $JavaHome 'bin\java.exe'
  if (-not (Test-Path $java)) { throw "java not found: $java" }

  $jvmArgs = @('-jar', $JarPath, '--server.port=8081')
  if ($AiKey -ne '') {
    $jvmArgs += '--ai.enabled=true'
    $jvmArgs += "--ai.api-key=$AiKey"
    if ($AiBaseUrl -ne '') { $jvmArgs += "--ai.base-url=$AiBaseUrl" }
    if ($AiModel   -ne '') { $jvmArgs += "--ai.model=$AiModel" }
    Write-Host '[cfg]  AI enabled for this run'
  } else {
    Write-Host '[cfg]  AI disabled (Mock mode), pass -AiKey to enable real model calls'
  }

  Write-Host '[run]  starting backend on 8081 ...'
  $env:DB_PASSWORD = 'root'
  $env:JAVA_HOME   = $JavaHome
  Start-Process -FilePath $java `
    -ArgumentList $jvmArgs `
    -WorkingDirectory $BackendDir `
    -RedirectStandardOutput (Join-Path $LogDir 'backend.out.log') `
    -RedirectStandardError  (Join-Path $LogDir 'backend.err.log') `
    -WindowStyle Hidden | Out-Null
  if (-not (Wait-Port -Port 8081 -TimeoutSec 120 -Name 'backend')) { exit 1 }
}

# ---------------- 3. Frontend ----------------
if (Test-Port -Port 8000) {
  Write-Host '[skip] frontend already listening on 8000'
} else {
  if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
    throw "node_modules missing, run first: cd TestPapaerGen-Frontend; npm install --legacy-peer-deps --registry=https://registry.npmmirror.com"
  }
  Write-Host '[run]  starting umi dev server on 8000 (first start takes 30-60s) ...'
  $env:NODE_OPTIONS = '--openssl-legacy-provider'
  $cmd = 'npm start > "{0}" 2>&1' -f (Join-Path $LogDir 'frontend.log')
  Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', $cmd) `
    -WorkingDirectory $FrontendDir -WindowStyle Hidden | Out-Null
  if (-not (Wait-Port -Port 8000 -TimeoutSec 180 -Name 'frontend')) { exit 1 }
}

Write-Host ''
Write-Host 'Ready:'
Write-Host '  UI       http://localhost:8000'
Write-Host '  API      http://localhost:8081'
Write-Host '  accounts admin/123456 (admin), teacher001/123456 (teacher)'
Write-Host "  logs     $LogDir"
Write-Host ''

if (-not $SkipBrowser) { Start-Process 'http://localhost:8000' }

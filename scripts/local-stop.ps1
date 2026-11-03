<#
  Stop the three local processes started by scripts\local-start.ps1.

  Only kills processes that currently listen on 3306 (mysqld), 8081 (java) and 8000 (node/cmd).
  Data lives in D:\WorkBuddy\tools\mysqldata and is NOT touched.
#>
param([switch]$Force)

$ErrorActionPreference = 'SilentlyContinue'

$targets = @(
  @{ Port = 8000; Name = 'frontend'; Expected = @('node.exe') },
  @{ Port = 8081; Name = 'backend';  Expected = @('java.exe') },
  @{ Port = 3306; Name = 'mysql';    Expected = @('mysqld.exe') }
)

foreach ($t in $targets) {
  $conns = Get-NetTCPConnection -LocalPort $t.Port -State Listen -ErrorAction SilentlyContinue
  if (-not $conns) {
    Write-Host ("[skip] {0} : nothing on port {1}" -f $t.Name, $t.Port)
    continue
  }
  foreach ($procId in ($conns.OwningProcess | Select-Object -Unique)) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if (-not $proc) { continue }
    $exe = $proc.ProcessName + '.exe'
    if ($t.Expected -notcontains $exe) {
      Write-Host ("[warn] port {0} held by {1} (pid {2}), not stopping automatically" -f $t.Port, $exe, $procId)
      continue
    }
    Write-Host ("[stop] {0} pid {1} ({2})" -f $t.Name, $procId, $exe)
    if ($t.Name -eq 'mysql') {
      # graceful shutdown keeps InnoDB consistent
      $mysqlAdmin = 'D:\WorkBuddy\tools\mysql\bin\mysqladmin.exe'
      if (Test-Path $mysqlAdmin) {
        & $mysqlAdmin -u root -proot shutdown 2>$null
      } else {
        Stop-Process -Id $procId -Force
      }
      if (-not $Force) { Start-Sleep -Seconds 2 }
    } else {
      Stop-Process -Id $procId -Force
    }
  }
}

Write-Host 'Done.'

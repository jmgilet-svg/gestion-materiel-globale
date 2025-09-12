<# 
  Dead classes finder for Windows (PowerShell) 
  Requires: PowerShell 5+, jdeps (JDK 11+)
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Ensure-Command($name){
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)){
    throw "[ERROR] $name not found in PATH."
  }
}

Ensure-Command jdeps

$root = (Resolve-Path "$PSScriptRoot\..\..").Path
$reportDir = Join-Path $root "tools\deadcode\report"
$excludesFile = Join-Path $root "tools\deadcode\excludes.txt"
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

Write-Host "🔎 Audit des classes non utilisées (jdeps)" -ForegroundColor Cyan

function Class-ToFqn([string]$classPath, [string]$classesRoot){
  $rel = Resolve-Path $classPath | ForEach-Object { $_.Path.Substring($classesRoot.Length).TrimStart('\','/') }
  $rel = $rel -replace '\\','.' -replace '/','.'
  $rel = $rel -replace '\.class$',''
  return $rel
}

function Matches-Exclude([string]$fqn, [string[]]$patterns){
  foreach($pat in $patterns){
    if ([string]::IsNullOrWhiteSpace($pat) -or $pat.Trim().StartsWith("#")) { continue }
    $p = $pat.Trim()
    if ($p.Contains("*")){
      if ($fqn -like $p) { return $true }
    } else {
      if ($fqn -eq $p) { return $true }
    }
  }
  return $false
}

function Analyze-Module([string]$module){
  $classesDir = Join-Path $root "$module\target\classes"
  $report = Join-Path $reportDir "dead-classes-$module.txt"
  if (-not (Test-Path $classesDir)){
    Write-Host "⚠  $module: pas de classes compilées (build d'abord)." -ForegroundColor Yellow
    return
  }
  Write-Host "→ Module $module" -ForegroundColor Green

  # list defined classes (outer class names)
  $defined = Get-ChildItem -Recurse -Path $classesDir -Filter *.class |
    ForEach-Object {
      $fqn = Class-ToFqn $_.FullName $classesDir
      # drop inner class suffix
      $fqn -replace '\$.*$',''
    } | Sort-Object -Unique

  # referenced via jdeps
  $jdepsOut = & jdeps -verbose:class -cp $classesDir -R $classesDir 2>$null
  $referenced = @()
  if ($LASTEXITCODE -eq 0 -and $jdepsOut){
    $referenced = ($jdepsOut | Select-String -Pattern '->\s+([A-Za-z0-9_\$\.]+)' | ForEach-Object {
      $m = $_.Matches[0].Groups[1].Value
      $m -replace '\$','.'
    } | Sort-Object -Unique)
  } else {
    Write-Host "   ⚠  jdeps a échoué pour $module (ignoré)." -ForegroundColor Yellow
  }

  $patterns = @()
  if (Test-Path $excludesFile){
    $patterns = Get-Content $excludesFile -ErrorAction SilentlyContinue
  }

  if (Test-Path $report){ Remove-Item $report -Force }
  $count = 0
  foreach($fqn in $defined){
    if ($fqn.Contains('$')){ continue }
    if (Matches-Exclude $fqn $patterns){ continue }
    if (-not ($referenced -contains $fqn)){
      Add-Content -Path $report -Value $fqn
      $count++
    }
  }
  if ($count -eq 0){
    if (Test-Path $report){ Remove-Item $report -Force }
    Write-Host "   ✅ Aucune classe orpheline détectée."
  } else {
    Write-Host ("   ⚠  {0} classe(s) potentiellement non utilisée(s). Rapport: {1}" -f $count, $report) -ForegroundColor Yellow
  }
}

Analyze-Module "client"
Analyze-Module "backend"

Write-Host "🏁 Audit terminé." -ForegroundColor Cyan

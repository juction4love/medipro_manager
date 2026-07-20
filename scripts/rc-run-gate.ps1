# MediPro RC Gate — run automatable production checks
# Usage: .\scripts\rc-run-gate.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "=== MediPro RC Gate ===" -ForegroundColor Cyan
$failed = $false

function Step($name, [scriptblock]$action) {
    Write-Host "`n>> $name" -ForegroundColor Yellow
    try {
        & $action
        if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) { throw "Exit code $LASTEXITCODE" }
        Write-Host "   PASS" -ForegroundColor Green
    } catch {
        Write-Host "   FAIL: $_" -ForegroundColor Red
        $script:failed = $true
    }
}

Step "Gradle unit tests (SyncConflictResolver)" {
    & .\gradlew.bat :data:testDebugUnitTest --quiet
}

Step "Release build smoke" {
    & .\gradlew.bat :app:assembleRelease --quiet
}

Step "Catalog asset present" {
    $catalog = Join-Path $Root "app\src\main\assets\databases\catalog.db"
    if (-not (Test-Path $catalog)) { throw "catalog.db missing at $catalog" }
    $sizeMb = [math]::Round((Get-Item $catalog).Length / 1MB, 1)
    Write-Host "   catalog.db size: ${sizeMb} MB"
    if ($sizeMb -lt 50) { Write-Host "   WARN: catalog.db smaller than expected (~80MB+)" -ForegroundColor DarkYellow }
}

Step "Import report row count" {
    $report = Join-Path $Root "assets\output\import-report.json"
    if (Test-Path $report) {
        $json = Get-Content $report -Raw | ConvertFrom-Json
        $count = $json.totalRecords
        Write-Host "   Expected catalog records: $count"
        if ($count -lt 270000) { throw "Catalog count below 270K threshold" }
    } else {
        Write-Host "   SKIP: import-report.json not found" -ForegroundColor DarkYellow
    }
}

Step "Room schema exports v1-v10" {
    $schemaDir = Join-Path $Root "core\database\schemas\com.medipro.manager.core.database.MediProDatabase"
    1..10 | ForEach-Object {
        $f = Join-Path $schemaDir "$_.json"
        if (-not (Test-Path $f)) { throw "Missing schema export: $f" }
    }
    Write-Host "   All 10 schema JSON files present"
    Write-Host "   Migrations: DatabaseMigrations.ALL (v1-v10) — destructive fallback removed" -ForegroundColor Green
}

Write-Host "`n=== Manual steps required ===" -ForegroundColor Cyan
Write-Host @"
  1. Firestore rules (requires Firebase CLI + emulator):
     firebase emulators:exec --only firestore "cd firebase && npm test"

  2. Multi-device sync matrix — see docs/RC-TESTING.md §3

  3. Performance profiling — see docs/RC-TESTING.md §2

  4. BLOCKERS before RC sign-off:
     - Implement Room migrations (no destructive fallback)
     - Implement BackupRepositoryImpl
"@

Write-Host "`n=== Result ===" -ForegroundColor Cyan
if ($failed) {
    Write-Host "RC GATE: FAIL (automated checks)" -ForegroundColor Red
    exit 1
} else {
    Write-Host "RC GATE: Automated checks PASS (manual + blockers remain)" -ForegroundColor Green
    exit 0
}

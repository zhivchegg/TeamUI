#!/usr/bin/env pwsh
# TeamUI Full Setup — GitHub Push + Docker Run
# Usage: .\full-setup.ps1 -GitHubEmail "you@example.com"

Param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubEmail,

    [string]$GitHubUser = "zhivchegg",
    [string]$RepoName = "TeamUI",
    [int]$LogDaysBack = 7
)

$ErrorActionPreference = "Stop"
$LogFile = Join-Path $PSScriptRoot "teamui-setup.log"
$StartTime = Get-Date

function Write-Log {
    param(
        [string]$Level = "INFO",
        [string]$Message,
        [int]$ExitCode = 0,
        [string]$Stderr = ""
    )
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$ts] [$Level] $Message"
    if ($ExitCode -ne 0) { $line += " [EXIT:$ExitCode]" }
    if ($Stderr -and ($Stderr -notmatch '^\s*$')) { $line += " [STDERR: $Stderr]" }
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
    switch ($Level) {
        "ERROR"   { Write-Host $line -ForegroundColor Red }
        "WARN"    { Write-Host $line -ForegroundColor Yellow }
        "SUCCESS" { Write-Host $line -ForegroundColor Green }
        default   { Write-Host $line }
    }
}

function Invoke-Cmd {
    param(
        [string]$Description,
        [scriptblock]$ScriptBlock
    )
    Write-Log -Level "INFO" -Message "STEP START: $Description"
    try {
        $out = & $ScriptBlock 2>&1
        $exit = $LASTEXITCODE
        $joined = $out -join "`n"
        if ($exit -ne 0) {
            Write-Log -Level "ERROR" -Message "STEP FAILED: $Description" -ExitCode $exit -Stderr $joined
            throw "Command '$Description' failed with exit code $exit. Output: $joined"
        }
        Write-Log -Level "SUCCESS" -Message "STEP OK: $Description"
        return $joined
    } catch {
        Write-Log -Level "ERROR" -Message "STEP EXCEPTION: $Description — $_"
        throw
    }
}

try {
    # UTF-8 fix
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    chcp 65001 | Out-Null
    Write-Log -Level "INFO" -Message "=== TeamUI Full Setup Started ==="
    Write-Log -Level "INFO" -Message "Repository: $GitHubUser/$RepoName"
    Write-Log -Level "INFO" -Message "Email: $GitHubEmail"

    # Resolve project dir via $env:USERPROFILE (fix Cyrillic problem)
    $projectDir = Join-Path $env:USERPROFILE "Documents\teamui"
    Write-Log -Level "INFO" -Message "Project directory resolved to: $projectDir"
    if (-not (Test-Path $projectDir)) {
        throw "Project directory not found: $projectDir"
    }
    Set-Location $projectDir

    # ─── Part 1: GitHub Push ───
    Write-Log -Level "INFO" -Message "--- Part 1: GitHub Push ---"

    # 1.1 Check git
    $gitVer = Invoke-Cmd "Check git version" { git --version }
    Write-Log -Level "INFO" -Message "Git detected: $gitVer"

    # 1.2 Check gh
    $ghVer = Invoke-Cmd "Check gh version" { gh --version }
    Write-Log -Level "INFO" -Message "gh detected: $ghVer"

    # 1.3 Auth check
    $ghAuth = gh auth status 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Log -Level "WARN" -Message "gh not authenticated. Starting gh auth login..."
        gh auth login | ForEach-Object { Write-Log -Level "INFO" -Message $_ }
    } else {
        Write-Log -Level "INFO" -Message "gh authenticated"
    }

    # 1.4 Git init if needed
    if (-not (Test-Path "$projectDir\.git")) {
        Invoke-Cmd "git init" { git init }
    } else {
        Write-Log -Level "INFO" -Message ".git already exists, skipping init"
    }

    # 1.5 Configure user
    Invoke-Cmd "git config user.name" { git config user.name "Zhivchegg" }
    Invoke-Cmd "git config user.email" { git config user.email "$GitHubEmail" }
    Write-Log -Level "INFO" -Message "Git identity set: Zhivchegg <$GitHubEmail>"

    # 1.6 Commit
    Invoke-Cmd "git add" { git add . }
    $msg = "Initial commit: TeamUI MVP`n`n" +
           "Modules: Auth, Meetings, Timeline Events, Competency Radar, Bus Factor, Pulse Surveys`n" +
           "Includes: Docker, Flyway, JWT auth, optimistic locking`n" +
           "Co-Authored-By: Claude Code <noreply@anthropic.com>"
    Invoke-Cmd "git commit" { git commit -m "$msg" }

    # 1.7 Create GitHub repo
    $repoCheck = gh repo view "$GitHubUser/$RepoName" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Log -Level "INFO" -Message "Repository $GitHubUser/$RepoName already exists"
    } else {
        Invoke-Cmd "gh repo create" {
            gh repo create "$RepoName" --public `
                --description="Self-hosted people management platform for support engineering teams -- 1:1, competencies, bus factor, pulse surveys"
        }
    }

    # 1.8 Push
    Invoke-Cmd "git remote remove origin" { git remote remove origin } | Out-Null
    Invoke-Cmd "git remote add origin" { git remote add origin "https://github.com/$GitHubUser/$RepoName.git" }
    Invoke-Cmd "git push" { git push -u origin main }
    Write-Log -Level "SUCCESS" -Message "GitHub push complete: https://github.com/$GitHubUser/$RepoName"

    # ─── Part 2: Docker ───
    Write-Log -Level "INFO" -Message "--- Part 2: Docker Build & Run ---"

    # 2.1 Check docker
    $dockerVer = Invoke-Cmd "Check docker version" { docker --version }
    Write-Log -Level "INFO" -Message "Docker detected: $dockerVer"

    # 2.2 Docker Compose build + up
    Invoke-Cmd "docker-compose build" { docker-compose build }
    Invoke-Cmd "docker-compose up -d" { docker-compose up -d }

    # 2.3 Wait for DB healthcheck
    Write-Log -Level "INFO" -Message "Waiting for PostgreSQL to be healthy..."
    $maxWait = 60
    $ waited = 0
    while ($waited -lt $maxWait) {
        $health = docker inspect --format="{{.State.Health.Status}}" teamui-postgres 2>$null
        if ($health -eq "healthy") {
            Write-Log -Level "SUCCESS" -Message "PostgreSQL is healthy"
            break
        }
        Start-Sleep -Seconds 5
        $waited += 5
        Write-Log -Level "INFO" -Message "Waiting for DB... ($waited/$maxWait s)"
    }
    if ($waited -ge $maxWait) {
        Write-Log -Level "WARN" -Message "PostgreSQL health check timeout — app may still start after Flyway migrations"
    }

    # 2.4 Check app logs
    $appLogs = docker-compose logs --tail=20 app 2>&1 | Out-String
    Write-Log -Level "INFO" -Message "App logs:`n$appLogs"

    # ─── Summary ───
    $elapsed = (Get-Date) - $StartTime
    Write-Log -Level "SUCCESS" -Message "=== Full Setup Complete in $($elapsed.ToString('mm\:ss')) ==="
    Write-Log -Level "INFO" -Message "API Base URL: http://localhost:8080/api"
    Write-Log -Level "INFO" -Message "GitHub Repo:  https://github.com/$GitHubUser/$RepoName"
    Write-Log -Level "INFO" -Message "Log file:     $LogFile"

} catch {
    $elapsed = (Get-Date) - $StartTime
    Write-Log -Level "ERROR" -Message "=== ABORTED after $($elapsed.ToString('mm\:ss')) ==="
    Write-Log -Level "ERROR" -Message "Exception: $_"
    Write-Log -Level "ERROR" -Message "Log file location: $LogFile"
    throw
}

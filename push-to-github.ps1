#!/usr/bin/env pwsh
# TeamUI — GitHub Push Script
# Run from PowerShell: .\push-to-github.ps1

Param(
    [string]$RepoName = "TeamUI",
    [string]$GitHubUser = "zhivchegg"
)

$ErrorActionPreference = "Stop"

# Fix UTF-8 encoding for Cyrillic paths
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$projectDir = Join-Path $env:USERPROFILE "Documents\teamui"
Set-Location $projectDir

Write-Host "=== TeamUI GitHub Push Script ===" -ForegroundColor Cyan
Write-Host "  Project dir: $projectDir" -ForegroundColor Gray
Write-Host ""

# 1. Check git
Write-Host "[1/6] Checking git..." -ForegroundColor Yellow
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "git not found in PATH"
}

# 2. Check gh CLI
Write-Host "[2/6] Checking GitHub CLI (gh)..." -ForegroundColor Yellow
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "gh CLI not found. Install: winget install GitHub.cli"
}

$ghStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "gh is not authenticated. Running: gh auth login" -ForegroundColor Red
    gh auth login
}

# 3. Detect author from existing repo (fallback: prompt)
Write-Host "[3/6] Detecting author identity..." -ForegroundColor Yellow
$fallbackName = "Zhivchegg"
$fallbackEmail = Read-Host "Enter author email (or press Enter to skip and use system git config)"

if ([string]::IsNullOrWhiteSpace($fallbackEmail)) {
    $fallbackEmail = ""
    Write-Host "  (using system default git identity)" -ForegroundColor Gray
} else {
    git config user.email "$fallbackEmail"
    git config user.name "$fallbackName"
}

# 4. Init, add, commit
Write-Host "[4/6] Initializing git and committing..." -ForegroundColor Yellow
if (-not (Test-Path "$projectDir\.git")) {
    git init
}
git branch -M main 2>$null
git add .
$commitMessage = "Initial commit: TeamUI MVP`n`nModules: Auth, Meetings, Timeline Events, Competency Radar, Bus Factor, Pulse Surveys`nIncludes: Docker, Flyway, JWT auth, Optimistic locking`nCo-Authored-By: Claude Code <noreply@anthropic.com>"
git commit -m $commitMessage

# 5. Create GitHub repo
Write-Host "[5/6] Creating GitHub repository..." -ForegroundColor Yellow
$repoExists = gh repo view "$GitHubUser/$RepoName" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Repository already exists. Skipping creation." -ForegroundColor Green
} else {
    gh repo create "$RepoName" --public --description="Self-hosted people management platform for support engineering teams -- 1:1, competencies, bus factor, pulse surveys"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create repository"
    }
    Write-Host "  Repository created!" -ForegroundColor Green
}

# 6. Push
Write-Host "[6/6] Pushing to GitHub..." -ForegroundColor Yellow
git remote remove origin 2>$null
git remote add origin "https://github.com/$GitHubUser/$RepoName.git"
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== SUCCESS! ===" -ForegroundColor Green
    Write-Host "Repository: https://github.com/$GitHubUser/$RepoName" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Error "Push failed. Check errors above."
}

Set-Location -

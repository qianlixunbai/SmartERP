# ============================================================
# SmartERP — Local Development Startup Script
# ============================================================
# Usage:  .\scripts\start-dev.ps1
#
# This script loads env vars from the project-root .env file,
# validates them, and launches the Spring Boot backend.
#
# Prerequisites:
#   - Java 21+
#   - MySQL 8.0+ running locally
#   - .env file in the project root (copy from .env.example)
#
# Spring Boot does NOT auto-load .env — this script bridges that.
# ============================================================

param(
    [switch]$SkipEnvCheck
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Get-Item "$PSScriptRoot\..").FullName
$BackendDir  = Join-Path $ProjectRoot "backend"
$EnvFile     = Join-Path $ProjectRoot ".env"

# --------------------------------------------------
# 1. Load .env into current process
# --------------------------------------------------
if (Test-Path $EnvFile) {
    Write-Host "[start-dev] Loading environment from $EnvFile ..."
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -match '^\s*([^#][^=]+?)\s*=\s*(.*)$') {
            $name  = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Support quoted values (strip surrounding quotes)
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
} else {
    if (-not $SkipEnvCheck) {
        Write-Error @"

ERROR: .env file not found at:
  $EnvFile

Please create it from the template:
  cp .env.example .env
Then edit it with your local database password and JWT secret.
"@
        exit 1
    }
    Write-Host "[start-dev] .env not found (--SkipEnvCheck), relying on existing env vars."
}

# --------------------------------------------------
# 2. Validate required variables
# --------------------------------------------------
$Missing = @()
$TooShort = @()

if ([string]::IsNullOrWhiteSpace($env:SMARTERP_DB_URL))           { $Missing += "SMARTERP_DB_URL" }
if ([string]::IsNullOrWhiteSpace($env:SMARTERP_DB_USERNAME))      { $Missing += "SMARTERP_DB_USERNAME" }
if ($null -eq $env:SMARTERP_DB_PASSWORD)                          { $Missing += "SMARTERP_DB_PASSWORD" }
if ([string]::IsNullOrWhiteSpace($env:SMARTERP_JWT_SECRET))       { $Missing += "SMARTERP_JWT_SECRET" }
elseif ($env:SMARTERP_JWT_SECRET.Length -lt 32)                   { $TooShort += "SMARTERP_JWT_SECRET ($($env:SMARTERP_JWT_SECRET.Length) chars, need >= 32)" }

if ($Missing.Count -gt 0) {
    Write-Error @"

ERROR: Missing required environment variables:

$($Missing -join "`n")

Please set them in your .env file:
  $EnvFile
"@
    exit 1
}

if ($TooShort.Count -gt 0) {
    $msg = @"

ERROR: Environment variable(s) too short:

$($TooShort -join "`n")

JWT secret must be at least 32 characters.
Generate one with PowerShell:
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  [Convert]::ToBase64String($bytes)
"@
    Write-Error $msg
    exit 1
}

# --------------------------------------------------
# 3. Confirm (values hidden)
# --------------------------------------------------
Write-Host "[start-dev] SMARTERP_DB_URL     = $env:SMARTERP_DB_URL"
Write-Host "[start-dev] SMARTERP_DB_USERNAME = $env:SMARTERP_DB_USERNAME"
Write-Host "[start-dev] SMARTERP_DB_PASSWORD = *** (length=$($env:SMARTERP_DB_PASSWORD.Length))"
Write-Host "[start-dev] SMARTERP_JWT_SECRET  = *** (length=$($env:SMARTERP_JWT_SECRET.Length))"
Write-Host "[start-dev] SMARTERP_JWT_EXPIRATION = $($env:SMARTERP_JWT_EXPIRATION)"
Write-Host "[start-dev] SPRING_PROFILES_ACTIVE = $($env:SPRING_PROFILES_ACTIVE)"
Write-Host ""

# --------------------------------------------------
# 4. Start backend
# --------------------------------------------------
$Mvnw = Join-Path $BackendDir "mvnw.cmd"

if (-not (Test-Path $Mvnw)) {
    Write-Error "Maven Wrapper not found at: $Mvnw"
    exit 1
}

Write-Host "[start-dev] Starting Spring Boot (profile=$env:SPRING_PROFILES_ACTIVE) ..."
Write-Host ""

Push-Location $BackendDir
try {
    & $Mvnw spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "[start-dev] Application exited with code $LASTEXITCODE"
        Write-Host ""
        Write-Host "Common issues:"
        Write-Host "  1. MySQL not running — start your MySQL 8.0 service."
        Write-Host "  2. Wrong password  — check SMARTERP_DB_PASSWORD in .env"
        Write-Host "  3. Database missing — CREATE DATABASE smarterp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        Write-Host "  4. Tables missing  — import docs/mysql-p*.sql scripts."
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

try {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    $dockerCli = if ($command) { $command.Source } else { $null }

    if (-not $dockerCli) {
        $dockerCli = @(
            "C:\Program Files\Docker\Docker\resources\bin\docker.exe",
            "$env:LOCALAPPDATA\Programs\Docker\Docker\resources\bin\docker.exe",
            "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe"
        ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    }

    if (-not $dockerCli) {
        throw "Docker CLI was not found."
    }

    if (Test-Path -LiteralPath ".env.docker") {
        & $dockerCli compose --env-file .env.docker down
    } else {
        & $dockerCli compose down
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed to stop."
    }

    Write-Host "[Layover] Stopped. Database and upload volumes were preserved."
    exit 0
} catch {
    Write-Host "[Layover] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

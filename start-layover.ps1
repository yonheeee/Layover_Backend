$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

function Find-DockerCli {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        "C:\Program Files\Docker\Docker\resources\bin\docker.exe",
        "$env:LOCALAPPDATA\Programs\Docker\Docker\resources\bin\docker.exe",
        "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe"
    )

    return $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}

function Test-DockerEngine([string]$DockerCli) {
    & $DockerCli info *> $null
    return $LASTEXITCODE -eq 0
}

try {
    $dockerCli = Find-DockerCli
    if (-not $dockerCli) {
        throw "Docker CLI was not found. Check the Docker Desktop installation."
    }

    if (-not (Test-Path -LiteralPath "..\Layover\package.json")) {
        throw "Layover and Layover_Backend must be in the same parent folder."
    }

    if (-not (Test-Path -LiteralPath ".env.docker")) {
        Copy-Item -LiteralPath ".env.docker.example" -Destination ".env.docker"
        Write-Host "[Layover] Created .env.docker from the example file."
    }

    if ([string]::IsNullOrWhiteSpace($env:VITE_KAKAO_JS_KEY) -and (Test-Path -LiteralPath "..\Layover\.env")) {
        $kakaoLine = Get-Content -LiteralPath "..\Layover\.env" |
            Where-Object { $_ -match '^VITE_KAKAO_JS_KEY=' } |
            Select-Object -First 1
        if ($kakaoLine) {
            $env:VITE_KAKAO_JS_KEY = ($kakaoLine -split '=', 2)[1].Trim()
        }
    }

    if (-not (Test-DockerEngine $dockerCli)) {
        $desktopCandidates = @(
            "C:\Program Files\Docker\Docker\Docker Desktop.exe",
            "$env:LOCALAPPDATA\Programs\Docker\Docker\Docker Desktop.exe",
            "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
        )
        $desktopExe = $desktopCandidates |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1

        if (-not $desktopExe) {
            throw "Docker Desktop executable was not found."
        }

        Write-Host "[Layover] Starting Docker Desktop..."
        Start-Process -FilePath $desktopExe

        $dockerDeadline = (Get-Date).AddMinutes(2)
        do {
            Start-Sleep -Seconds 2
            if (Test-DockerEngine $dockerCli) { break }
        } while ((Get-Date) -lt $dockerDeadline)

        if (-not (Test-DockerEngine $dockerCli)) {
            throw "Docker Engine was not ready within two minutes."
        }
    }

    Write-Host "[Layover] Building and starting frontend, backend, and MySQL..."
    & $dockerCli compose --env-file .env.docker up --build -d
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed to start."
    }

    Write-Host "[Layover] Waiting for the web service..."
    $serviceDeadline = (Get-Date).AddMinutes(3)
    $ready = $false
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:5173" -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $serviceDeadline)

    if (-not $ready) {
        throw "Containers started, but the web page was not ready within three minutes."
    }

    Write-Host "[Layover] Ready: http://localhost:5173"
    Start-Process "http://localhost:5173"
    exit 0
} catch {
    Write-Host "[Layover] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

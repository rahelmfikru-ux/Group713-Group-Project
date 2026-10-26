# Builds the API + website and starts it on http://localhost:8080/project-task-manager/
# (Windows PowerShell). macOS / Linux: use run-app.sh.
#
# Run from this folder:   .\run-app.ps1
# If scripts are blocked:  powershell -ExecutionPolicy Bypass -File .\run-app.ps1
#
# The H2 database is a local file (data\), created automatically on
# first start, so no database server or password is needed.

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$PayaraVersion = "6.2025.10"
$Payara = "payara-micro-$PayaraVersion.jar"

if (-not (Test-Path $Payara)) {
    Write-Host "Downloading Payara Micro $PayaraVersion (first run only)..."
    mvn -q dependency:copy "-Dartifact=fish.payara.extras:payara-micro:$PayaraVersion" "-DoutputDirectory=."
    if ($LASTEXITCODE -ne 0) { throw "Could not download Payara Micro." }
}

Write-Host "Building project..."
mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "Build failed." }

Write-Host "Starting Project & Task Manager on port 8080..."
java -jar $Payara --port 8080 --deploy target\project-task-manager.war
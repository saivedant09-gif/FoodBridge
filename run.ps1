# FoodBridge - PowerShell Launcher Script
Write-Host "========================================================" -ForegroundColor Green
Write-Host "  FoodBridge - From Surplus to Sustenance" -ForegroundColor Green
Write-Host "  Starting Backend Server on http://localhost:8080 ...  " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Green

$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"
$env:PATH = "D:\apache-maven-3.9.6\bin;$env:JAVA_HOME\bin;$env:PATH"
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "foodbridge"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "Lupinasairaj@11"
$env:MONGO_ENABLED = "false"
$env:APP_PORT = "8080"

Set-Location "d:\FootBridge\backend"

if (Test-Path "target\foodbridge-1.0.0.jar") {
    Write-Host "Launching pre-built JAR package..." -ForegroundColor Cyan
    & "$env:JAVA_HOME\bin\java.exe" -jar target\foodbridge-1.0.0.jar
} else {
    Write-Host "Building package with Maven..." -ForegroundColor Cyan
    mvn package -DskipTests
    & "$env:JAVA_HOME\bin\java.exe" -jar target\foodbridge-1.0.0.jar
}

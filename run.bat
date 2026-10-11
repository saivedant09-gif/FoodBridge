@echo off
title FoodBridge - Backend Server
echo ========================================================
echo   FoodBridge - From Surplus to Sustenance
echo   Starting Backend Server on http://localhost:8080 ...
echo ========================================================

set "JAVA_HOME=C:\Program Files\BlueJ\jdk"
set "PATH=D:\apache-maven-3.9.6\bin;%JAVA_HOME%\bin;%PATH%"
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=foodbridge"
set "DB_USERNAME=root"
set "DB_PASSWORD=Lupinasairaj@11"
set "MONGO_ENABLED=false"
set "APP_PORT=8080"

cd /d "d:\FootBridge\backend"

if exist "target\foodbridge-1.0.0.jar" (
    echo Starting existing JAR package...
    "%JAVA_HOME%\bin\java.exe" -jar target\foodbridge-1.0.0.jar
) else (
    echo Building package with Maven first...
    call mvn package -DskipTests
    "%JAVA_HOME%\bin\java.exe" -jar target\foodbridge-1.0.0.jar
)

pause

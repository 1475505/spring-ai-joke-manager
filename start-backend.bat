@echo off
echo Starting Spring Boot Backend...
echo.

REM Check if Java is installed
java -version
if %errorlevel% neq 0 (
    echo Java is not installed or not in PATH
    pause
    exit /b 1
)

echo.
echo Starting application on port 8080...
echo Press Ctrl+C to stop
echo.

mvn spring-boot:run

pause

@echo off
echo Starting React Frontend...
echo.

REM Check if Node.js is installed
node --version
if %errorlevel% neq 0 (
    echo Node.js is not installed or not in PATH
    pause
    exit /b 1
)

echo.
echo Installing dependencies...
npm install

if %errorlevel% neq 0 (
    echo Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo Starting development server on port 3000...
echo Press Ctrl+C to stop
echo.

npm run dev

pause

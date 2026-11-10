@echo off
if "%~2"=="" (
    echo Usage: addsite ^<Name^> ^<URL^>
    echo Example: addsite Twitch https://twitch.tv
    exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0addsite.ps1" "%~1" "%~2"


@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-layover.ps1"
if errorlevel 1 pause

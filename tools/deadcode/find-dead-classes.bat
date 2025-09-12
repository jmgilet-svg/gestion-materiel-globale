@echo off
setlocal enabledelayedexpansion
REM Windows wrapper for the dead classes audit (requires PowerShell and jdeps from a JDK)

where jdeps >NUL 2>&1
if errorlevel 1 (
  echo [ERROR] jdeps not found. Ensure a JDK (11+) is installed and on PATH.
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find-dead-classes.ps1"
exit /b %errorlevel%

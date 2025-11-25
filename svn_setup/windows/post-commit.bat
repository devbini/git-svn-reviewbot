@echo off
SET REPOS=%1
SET REV=%2

REM PowerShell 스크립트 실행 (보안 정책 우회)
powershell.exe -ExecutionPolicy Bypass -File "%~dp0\post-commit.ps1" -REPOS "%REPOS%" -REV %REV%
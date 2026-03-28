@echo off
cd /d "%~dp0"
cmd /k mvnw clean package
pause


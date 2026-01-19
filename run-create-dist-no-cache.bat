@echo off
REM Run createWindowsDistributionWithJRE with configuration cache disabled
setlocal
cd /d %~dp0
call gradlew.bat :app:createWindowsDistributionWithJRE --no-configuration-cache --no-daemon %*
endlocal

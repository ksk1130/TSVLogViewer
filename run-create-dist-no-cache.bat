@echo off
REM Run createWindowsDistributionWithJRE and createWindowsAppImage with configuration cache disabled
setlocal
cd /d %~dp0
call gradlew.bat :app:createWindowsDistributionWithJRE :app:createWindowsAppImage --no-configuration-cache --no-daemon %*
endlocal

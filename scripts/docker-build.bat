@echo off
setlocal
set "ROOT=%~dp0.."
cd /d "%ROOT%"

docker build --platform linux/amd64 --tag tv-menu-build .
if errorlevel 1 exit /b 1

docker run --rm --platform linux/amd64 ^
  --volume "%ROOT%:/workspace" ^
  --workdir /workspace ^
  --env GRADLE_USER_HOME=/workspace/.gradle-ci ^
  tv-menu-build
endlocal

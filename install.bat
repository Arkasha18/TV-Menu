@echo off
setlocal
set "ROOT=%~dp0"
set "ADB=%ROOT%tools\android-sdk\platform-tools\adb.exe"
set "APK=%ROOT%TvQuickMenu\app\build\outputs\apk\debug\app-debug.apk"

if not exist "%APK%" (
  echo APK not found. Run build.bat first.
  exit /b 1
)

"%ADB%" devices
"%ADB%" install -r "%APK%"
echo Done.
endlocal

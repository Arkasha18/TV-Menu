@echo off
setlocal
set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%tools\jdk"
set "ANDROID_HOME=%ROOT%tools\android-sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ROOT%tools\gradle\gradle-8.9\bin;%PATH%"
set "GRADLE_USER_HOME=%ROOT%tools\gradle-home"

cd /d "%ROOT%TvQuickMenu"

if "%~1"=="" (
  echo Building signed release APK...
  "%ROOT%tools\gradle\gradle-8.9\bin\gradle.bat" assembleRelease
) else (
  "%ROOT%tools\gradle\gradle-8.9\bin\gradle.bat" %*
)

if exist "%ROOT%TvQuickMenu\app\build\outputs\apk\release\app-release.apk" (
  copy /Y "%ROOT%TvQuickMenu\app\build\outputs\apk\release\app-release.apk" "%ROOT%TV-Menu-release.apk" >nul
  echo.
  echo Release APK: %ROOT%TV-Menu-release.apk
)

endlocal

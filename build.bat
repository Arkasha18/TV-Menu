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
  "%ROOT%tools\gradle\gradle-8.9\bin\gradle.bat" assembleDebug
) else (
  "%ROOT%tools\gradle\gradle-8.9\bin\gradle.bat" %*
)

endlocal

@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "ADB=%ROOT%tools\android-sdk\platform-tools\adb.exe"
set "SVC=com.h9.tvquickmenu/com.h9.tvquickmenu.HotkeyAccessibilityService"

echo === Поиск устройства ===
"%ADB%" devices -l
echo.

set "TARGET="
for /f "tokens=1" %%D in ('"%ADB%" devices ^| findstr /R "device$"') do (
  if not defined TARGET set "TARGET=%%D"
)

if not defined TARGET (
  echo Устройство не найдено.
  set /p TVIP=Или введите IP приставки для adb connect: 
  if not "%TVIP%"=="" (
    "%ADB%" connect %TVIP%:5555
    for /f "tokens=1" %%D in ('"%ADB%" devices ^| findstr /R "device$"') do (
      if not defined TARGET set "TARGET=%%D"
    )
  )
)

if not defined TARGET (
  echo Всё ещё нет устройства. Выход.
  exit /b 1
)

echo Использую: %TARGET%
echo.
echo === Включаю службу кнопок TV Меню ===
"%ADB%" -s %TARGET% shell settings put secure enabled_accessibility_services %SVC%
"%ADB%" -s %TARGET% shell settings put secure accessibility_enabled 1

echo.
echo === Проверка ===
"%ADB%" -s %TARGET% shell settings get secure accessibility_enabled
"%ADB%" -s %TARGET% shell settings get secure enabled_accessibility_services

echo.
echo Готово. Служба TV Меню включена.
endlocal

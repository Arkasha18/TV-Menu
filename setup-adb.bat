@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "ADB=%ROOT%tools\android-sdk\platform-tools\adb.exe"

echo === TV Quick Menu: разовая настройка ADB ===
echo.
echo 1) На ТВ включите: Параметры -^> О устройстве -^> 7 раз по номеру сборки
echo 2) Параметры -^> Для разработчиков -^> Отладка по USB = ВКЛ
echo 3) Если есть пункт сетевой отладки / ADB по сети - включите
echo.
set /p TVIP=Введите IP приставки (например 192.168.1.50): 
if "%TVIP%"=="" (
  echo IP не задан.
  exit /b 1
)

echo Подключение...
"%ADB%" connect %TVIP%:5555
"%ADB%" devices
echo.
echo Перевожу ADB в сетевой режим tcpip 5555 ...
"%ADB%" -s %TVIP%:5555 tcpip 5555
"%ADB%" connect %TVIP%:5555
echo.
echo Готово. ПК можно отключить после проверки в приложении кнопкой
echo "4. Проверить локальный ADB".
echo После перезагрузки ТВ иногда нужно снова выполнить этот скрипт.
endlocal

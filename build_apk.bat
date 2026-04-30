@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.1.12-hotspot"
set "GRADLE=C:\Users\El Yisus Pai\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle"
set "PROJECT_DIR=%~dp0"

echo ====================================
echo  ARGtv - Build APK
echo ====================================
echo.

echo Limpiando build anterior...
call "%GRADLE%" clean --no-daemon

echo.
echo Compilando debug APK...
call "%GRADLE%" assembleDebug --no-daemon

echo.
echo ====================================
if %ERRORLEVEL%==0 (
    echo  BUILD EXITOSO
    echo  APK: %PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk
) else (
    echo  BUILD FALLIDO (error %ERRORLEVEL%)
)
echo ====================================
echo.

pause
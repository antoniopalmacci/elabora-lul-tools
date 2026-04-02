@echo off
setlocal

echo ============================================
echo   BUILD & PACKAGE - Elabora LUL Tools
echo ============================================

REM ----- CONFIGURAZIONE -----
set APP_NAME=Elabora LUL Tools
set APP_VERSION=1.0.0
set MAIN_CLASS=it.ecubit.elabora.lul.tools.ApplicationConfig
set ICON_PATH=installer\elabora-lul.ico
set JAR_NAME=elabora-lul-tools-%APP_VERSION%.jar
set DIST_DIR=dist

echo.
echo [1/4] Pulizia progetto...
call mvnw.cmd clean

echo.
echo [2/4] Compilazione progetto...
call mvnw.cmd package

if not exist target\%JAR_NAME% (
    echo ERRORE: Il file target\%JAR_NAME% non esiste.
    echo Assicurati che la versione nel pom.xml sia %APP_VERSION%.
    pause
    exit /b 1
)

echo.
echo [3/4] Creazione installer MSI con jpackage...

jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --input target ^
  --main-jar %JAR_NAME% ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --icon %ICON_PATH% ^
  --win-menu ^
  --win-shortcut ^
  --vendor "Ecubit S.p.A."

if %errorlevel% neq 0 (
    echo ERRORE: jpackage ha fallito.
    pause
    exit /b 1
)

echo.
echo [4/4] Spostamento MSI nella cartella dist...

if not exist %DIST_DIR% (
    mkdir %DIST_DIR%
)

for %%f in (*.msi) do (
    move "%%f" "%DIST_DIR%\"
)

echo.
echo ============================================
echo   OPERAZIONE COMPLETATA CON SUCCESSO
echo   Installer disponibile in: %DIST_DIR%
echo ============================================

pause

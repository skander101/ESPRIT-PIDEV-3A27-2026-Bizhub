@echo off
cd /d "c:\Users\Mega Pc\Downloads\wetransfer_new-folder-4_2026-02-25_1653\New folder (4)\gestion_investissement_crud\gestion_investissement_crud"
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Testing Java...
java -version

echo.
echo Compiling with Maven...
call mvnw.cmd clean compile -q

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running JavaFX application...
call mvnw.cmd exec:java -Dexec.mainClass="com.bizhub.Investistment.MainApplication" -q

pause

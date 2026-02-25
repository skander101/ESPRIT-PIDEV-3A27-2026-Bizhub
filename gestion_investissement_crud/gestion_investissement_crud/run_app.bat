@echo off
echo Setting up environment...
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Java version:
java -version

echo.
echo Compiling project...
call mvnw.cmd clean compile

echo.
echo Running application...
call mvnw.cmd javafx:run

pause

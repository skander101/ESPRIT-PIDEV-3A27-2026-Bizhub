@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Java version:
java -version
echo.
echo Running application...
mvnw.cmd javafx:run
pause

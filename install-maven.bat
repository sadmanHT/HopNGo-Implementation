@echo off
echo === MAVEN INSTALLATION SCRIPT ===
echo.

set MAVEN_ZIP=D:\softwares\apache-maven-3.9.11-bin.zip
set INSTALL_DIR=D:\softwares
set MAVEN_HOME=D:\softwares\apache-maven-3.9.11

echo Checking for Maven zip file...
if exist "%MAVEN_ZIP%" (
    echo Found Maven zip file. Extracting...
    
    REM Extract using PowerShell
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%INSTALL_DIR%' -Force"
    
    echo Maven extracted successfully to %MAVEN_HOME%
    
    REM Add to PATH
    set MAVEN_BIN=%MAVEN_HOME%\bin
    
    echo Adding Maven to system PATH...
    setx MAVEN_HOME "%MAVEN_HOME%"
    setx PATH "%PATH%;%MAVEN_BIN%"
    
    echo Maven has been installed and added to PATH.
    echo Please restart your terminal or IDE to use Maven.
    echo.
    echo Testing Maven installation...
    "%MAVEN_BIN%\mvn" --version
    
) else (
    echo Maven zip file not found at %MAVEN_ZIP%
    echo Please download apache-maven-3.9.11-bin.zip from:
    echo https://maven.apache.org/download.cgi
    echo And save it to D:\softwares\
)

echo.
echo Press any key to continue...
pause >nul
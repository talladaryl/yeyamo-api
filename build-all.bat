@echo off
echo ========================================
echo Building all microservices...
echo ========================================
echo.
echo NOTE: Tests are skipped to speed up the build
echo Using -Dmaven.test.skip=true
echo.

echo.
echo Checking for Maven or Maven Wrapper...

REM Check if Maven is installed
where mvn >nul 2>&1
if not errorlevel 1 (
    echo Maven found in PATH, using mvn command
    call mvn clean install -Dmaven.test.skip=true
    goto :check_result
)

REM Maven not found, try to use wrapper from a service
echo Maven not found in PATH, trying to use Maven Wrapper...

if exist user-service\mvnw.cmd (
    echo Using Maven Wrapper from user-service
    call user-service\mvnw.cmd clean install -Dmaven.test.skip=true
    goto :check_result
)

if exist auth-service\mvnw.cmd (
    echo Using Maven Wrapper from auth-service
    call auth-service\mvnw.cmd clean install -Dmaven.test.skip=true
    goto :check_result
)

if exist catalog-service\mvnw.cmd (
    echo Using Maven Wrapper from catalog-service
    call catalog-service\mvnw.cmd clean install -Dmaven.test.skip=true
    goto :check_result
)

echo.
echo ERROR: Neither Maven nor Maven Wrapper found
echo.
echo Please either:
echo   1. Install Maven from https://maven.apache.org/download.cgi
echo   2. Or use build-all-no-maven.bat to build each service individually
echo.
exit /b 1

:check_result
if errorlevel 1 (
    echo.
    echo ERROR: Build failed
    echo Check the error messages above
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo All JAR files have been created in target/ directories
echo.
echo You can now run: docker compose up -d --build

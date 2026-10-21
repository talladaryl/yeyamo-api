@echo off
echo ========================================
echo Building all microservices through the Maven reactor
echo ========================================

if "%MAVEN_THREADS%"=="" set MAVEN_THREADS=1

if not exist auth-service\mvnw.cmd (
    echo ERROR: Maven Wrapper not found in auth-service
    exit /b 1
)

call auth-service\mvnw.cmd -f pom.xml -B -ntp -T %MAVEN_THREADS% install -DskipTests
if errorlevel 1 (
    echo.
    echo ERROR: Reactor build failed
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo Shared modules were compiled once for all selected services.

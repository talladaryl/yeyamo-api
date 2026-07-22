@echo off
echo ========================================
echo Building all microservices...
echo Using individual Maven wrappers
echo ========================================

echo.
echo This script will build each service individually using their mvnw.cmd wrapper
echo This is slower but doesn't require Maven to be installed
echo.

set FAILED=0

echo Building security-hardening-starter...
cd security-hardening-starter
if exist mvnw.cmd (
    call mvnw.cmd clean install -DskipTests
    if errorlevel 1 set FAILED=1
) else (
    echo WARNING: No mvnw.cmd found in security-hardening-starter
)
cd ..

if %FAILED%==1 (
    echo ERROR: Failed to build security-hardening-starter
    exit /b 1
)

for /d %%i in (*-service) do (
    echo.
    echo Building %%i...
    cd %%i
    if exist mvnw.cmd (
        call mvnw.cmd clean package -DskipTests
        if errorlevel 1 (
            echo ERROR: Failed to build %%i
            cd ..
            exit /b 1
        )
    ) else (
        echo WARNING: No mvnw.cmd found in %%i, skipping...
    )
    cd ..
)

echo.
echo Building config-server and registry-service...
for %%i in (config-server registry-service api-gateway) do (
    echo Building %%i...
    cd %%i
    if exist mvnw.cmd (
        call mvnw.cmd clean package -DskipTests
        if errorlevel 1 (
            echo ERROR: Failed to build %%i
            cd ..
            exit /b 1
        )
    ) else (
        echo WARNING: No mvnw.cmd found in %%i, skipping...
    )
    cd ..
)

echo.
echo ========================================
echo Build completed!
echo ========================================
echo You can now run: docker compose up -d --build

@echo off 
echo [INFO] Starting clean build of all services... 
set DOCKER_BUILDKIT=1 
set COMPOSE_DOCKER_CLI_BUILD=1 
docker-compose -f docker-compose.optimized.yml build --no-cache --parallel 
if %0% equ 0 ( 
    echo [INFO] Clean build completed successfully! 
) else ( 
    echo [ERROR] Clean build failed! 
    exit /b 1 
) 

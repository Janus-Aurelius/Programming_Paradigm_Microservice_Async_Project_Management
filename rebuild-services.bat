@echo off 
echo [INFO] Building and starting all services... 
set DOCKER_BUILDKIT=1 
set COMPOSE_DOCKER_CLI_BUILD=1 
docker-compose -f docker-compose.optimized.yml up -d 
if %0% equ 0 ( 
    echo [INFO] All services started successfully! 
    echo Services available at: 
    echo   - API Gateway: http://localhost:8080 
    echo   - User Service: http://localhost:8083 
    echo   - Project Service: http://localhost:8082 
    echo   - Task Service: http://localhost:8081 
    echo   - WebSocket Service: http://localhost:8085 
    echo   - Notification Service: http://localhost:8086 
    echo   - Comment Service: http://localhost:8088 
) else ( 
    echo [ERROR] Failed to start services! 
    exit /b 1 
) 

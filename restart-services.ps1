# PowerShell script to restart all microservices with authentication fixes
# Run this from the project root directory

Write-Host "=== Restarting Microservices with Authentication Fixes ===" -ForegroundColor Green

$services = @(
    "user-service",
    "notification-service", 
    "project-service",
    "task-service",
    "comment-service"
)

Write-Host "Stopping services..." -ForegroundColor Yellow
foreach ($service in $services) {
    Write-Host "Stopping $service..." -ForegroundColor Gray
    docker-compose stop $service
}

Write-Host "`nStarting services with new configuration..." -ForegroundColor Yellow
foreach ($service in $services) {
    Write-Host "Starting $service..." -ForegroundColor Gray
    docker-compose start $service
    Start-Sleep -Seconds 2
}

Write-Host "`n=== Services Restarted ===" -ForegroundColor Green
Write-Host "The following authentication fixes have been applied:" -ForegroundColor Cyan
Write-Host "✅ All services now use jwt.enabled=true" -ForegroundColor Green
Write-Host "✅ All services read authentication from API Gateway headers" -ForegroundColor Green
Write-Host "✅ WebSocket authentication via query parameters" -ForegroundColor Green
Write-Host "✅ Role-based permissions for user access" -ForegroundColor Green

Write-Host "`nYou can now test:" -ForegroundColor Cyan
Write-Host "- WebSocket connections should work" -ForegroundColor White
Write-Host "- User service endpoints should work with admin/project manager roles" -ForegroundColor White
Write-Host "- Notification mark-read should work" -ForegroundColor White
Write-Host "- Project creation with user filtering should work" -ForegroundColor White

Write-Host "`nTo monitor logs for any issues:" -ForegroundColor Yellow
Write-Host "docker-compose logs -f user-service notification-service project-service task-service comment-service" -ForegroundColor Gray

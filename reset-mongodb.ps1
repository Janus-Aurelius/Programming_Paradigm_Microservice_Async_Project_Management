# MongoDB Data Reset Script for Development
# This script provides easy ways to reset your MongoDB data

Write-Host "MongoDB Development Reset Options:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Quick Reset (Recommended for development)" -ForegroundColor Green
Write-Host "   This removes all containers and volumes, giving you a fresh start"
Write-Host ""
Write-Host "2. Soft Reset (Keep containers, reset data only)" -ForegroundColor Cyan
Write-Host "   This keeps containers but clears all database data"
Write-Host ""

$choice = Read-Host "Choose option (1 or 2, or 'q' to quit)"

switch ($choice) {
    "1" {
        Write-Host "Performing Quick Reset..." -ForegroundColor Green
        Write-Host "Stopping and removing all containers and volumes..." -ForegroundColor Yellow
        
        docker-compose down -v --remove-orphans
        
        Write-Host "Removing any leftover containers..." -ForegroundColor Yellow
        docker container prune -f
        
        Write-Host "Removing unused volumes..." -ForegroundColor Yellow
        docker volume prune -f
        
        Write-Host "✓ Quick Reset completed!" -ForegroundColor Green
        Write-Host "You can now run 'docker-compose up -d' for a fresh start" -ForegroundColor Cyan
    }
    "2" {
        Write-Host "Performing Soft Reset..." -ForegroundColor Cyan
        Write-Host "Stopping services..." -ForegroundColor Yellow
        
        docker-compose stop
        
        Write-Host "Removing only MongoDB data volume..." -ForegroundColor Yellow
        docker volume rm project_management_mongo_data 2>$null
        
        Write-Host "Restarting services..." -ForegroundColor Yellow
        docker-compose up -d
        
        Write-Host "✓ Soft Reset completed!" -ForegroundColor Green
        Write-Host "MongoDB has been reset with fresh data" -ForegroundColor Cyan
    }
    "q" {
        Write-Host "Reset cancelled." -ForegroundColor Yellow
        exit
    }
    default {
        Write-Host "Invalid option. Please choose 1, 2, or q" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Available commands:" -ForegroundColor Magenta
Write-Host "  docker-compose up -d          # Start all services"
Write-Host "  docker-compose down -v        # Stop and remove everything"
Write-Host "  docker-compose logs mongo     # View MongoDB logs"
Write-Host "  docker-compose ps             # Show service status"

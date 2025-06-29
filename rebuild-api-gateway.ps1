# Rebuild and restart API Gateway
Write-Host "Rebuilding API Gateway..."

# Build the API Gateway
cd api-gateway
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Go back to root directory
cd ..

# Restart the API Gateway container
Write-Host "Restarting API Gateway container..."
docker-compose stop api-gateway
docker-compose up -d api-gateway

Write-Host "API Gateway rebuilt and restarted successfully!" -ForegroundColor Green

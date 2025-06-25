# Docker Compose JWT Configuration Guide

## 🚨 Important: JWT is DISABLED by Default in Docker

When you run `docker-compose up` or `docker-compose up --build`, **JWT authentication is DISABLED by default** to make development easier.

## Quick Commands

### Development Mode (JWT Disabled - Default)

```bash
# Standard Docker Compose - JWT DISABLED
docker-compose up
docker-compose up --build
docker-compose up -d --build

# All endpoints accessible without authentication
# Mock user headers automatically added by API Gateway
```

### Production Mode (JWT Enabled)

```bash
# Production Docker Compose - JWT ENABLED
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build

# Full JWT authentication required
# Real user tokens must be provided
```

## Configuration Details

### Default Docker Compose (JWT Disabled)

- **File**: `docker-compose.yml`
- **Environment**: `JWT_ENABLED=false` for all services
- **Profile**: `SPRING_PROFILES_ACTIVE=docker`
- **Behavior**: All endpoints accessible without JWT tokens

### Production Docker Compose (JWT Enabled)

- **Files**: `docker-compose.yml` + `docker-compose.prod.yml`
- **Environment**: `JWT_ENABLED=true` for all services
- **Profile**: `SPRING_PROFILES_ACTIVE=prod`
- **Behavior**: JWT authentication required for protected endpoints

## Service Configuration

### All Services Include:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker # or prod
  - JWT_ENABLED=false # or true in production
  - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

### JWT-Related Services Also Include:

```yaml
# API Gateway and User Service
environment:
  - JWT_SECRET=${JWT_SECRET:-my-very-secret-key-for-jwt-signing}
```

## Testing JWT Status

### Verify JWT is Disabled (Default)

```bash
# Start services
docker-compose up -d

# Test JWT status
curl http://localhost:8080/api/users/test/dev-mode

# Expected response:
{
  "jwtEnabled": false,
  "message": "Development mode endpoint accessed successfully",
  "devHeaders": {
    "X-User-Id": "dev-user-123",
    "X-User-Email": "dev@example.com",
    "X-User-Role": "ROLE_ADMIN"
  }
}
```

### Verify JWT is Enabled (Production)

```bash
# Start services in production mode
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Test JWT status
curl http://localhost:8080/api/users/test/dev-mode

# Expected: 401 Unauthorized (JWT required)
```

## Automated Testing

### Test Scripts

```bash
# Windows
./test-docker-jwt-disabled.bat

# Linux/Mac
chmod +x test-docker-jwt-disabled.sh
./test-docker-jwt-disabled.sh
```

## Application Profiles

### Docker Profile (`application-docker.yml`)

- **User Service**: MongoDB connects to `mongo:27017`, Kafka to `kafka:9092`
- **API Gateway**: Routes to internal service names (user-service, project-service, etc.)
- **JWT**: `enabled: ${JWT_ENABLED:false}` (disabled by default)

### Production Profile (`application-prod.yml`)

- **JWT**: `enabled: true` (explicitly enabled)
- **Security**: Additional production security settings
- **Secrets**: Uses environment variables for sensitive data

## Port Mapping

| Service              | Internal Port | External Port |
| -------------------- | ------------- | ------------- |
| API Gateway          | 8080          | 8080          |
| User Service         | 8083          | 8083          |
| Project Service      | 8082          | 8082          |
| Task Service         | 8081          | 8081          |
| WebSocket Service    | 8085          | 8085          |
| Notification Service | 8086          | 8086          |
| Comment Service      | 8088          | 8088          |
| MongoDB              | 27017         | 27017         |
| Kafka                | 9092          | 9092          |

## Common Use Cases

### 1. Development with Docker

```bash
# Start all services without JWT
docker-compose up --build

# All endpoints work without authentication
curl http://localhost:8080/api/users
curl http://localhost:8080/api/projects
curl http://localhost:8080/api/tasks
```

### 2. Production Testing

```bash
# Start with JWT enabled
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up

# Login to get token
curl -X POST http://localhost:8080/api/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# Use token for requests
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Mixed Development

```bash
# Override JWT for specific services
JWT_ENABLED=true docker-compose up api-gateway user-service
JWT_ENABLED=false docker-compose up project-service task-service
```

## Environment Variables

### For Development (Default)

```bash
# No environment variables needed
# JWT_ENABLED defaults to false
```

### For Production

```bash
export JWT_ENABLED=true
export JWT_SECRET=your-production-secret-key
export SPRING_PROFILES_ACTIVE=prod

docker-compose up
```

## Troubleshooting

### Issue: JWT still required after docker-compose up

**Solution**: Check environment variables

```bash
docker-compose config | grep JWT_ENABLED
# Should show JWT_ENABLED=false
```

### Issue: Services can't communicate

**Solution**: Verify network configuration

```bash
docker network ls
docker network inspect project_management_default
```

### Issue: Database connection errors

**Solution**: Wait for healthchecks

```bash
docker-compose ps
# Wait for mongo and kafka to show "healthy"
```

## Security Notes

- 🟢 **Development**: JWT disabled for easy testing
- 🔴 **Production**: JWT must be explicitly enabled
- 🟡 **Docker**: Uses internal network for service communication
- 🔵 **Secrets**: Use environment variables in production

This configuration ensures that `docker-compose up` always runs with JWT disabled for development convenience, while production deployments require explicit JWT enabling.

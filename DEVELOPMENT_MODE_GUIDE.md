# Development Mode Configuration Guide

This project supports disabling JWT authentication for development purposes using Spring profiles and configuration properties.

## Quick Start - Development Mode

### Option 1: Using Startup Scripts

```bash
# Windows
./start-dev-mode.bat

# Linux/Mac
chmod +x start-dev-mode.sh
./start-dev-mode.sh
```

### Option 2: Manual Startup

```bash
# User Service
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Djwt.enabled=false

# API Gateway
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Djwt.enabled=false
```

### Option 3: Environment Variables

```bash
export SPRING_PROFILES_ACTIVE=dev
export JWT_ENABLED=false

# Then start services normally
mvn spring-boot:run
```

## Configuration Details

### JWT Disabled Configuration

When JWT is disabled:

- **User Service**: All endpoints become accessible without authentication
- **API Gateway**: JWT validation is bypassed, mock user headers are added
- **Mock User Headers**: Automatically added for downstream services
  - `X-User-Id: dev-user-123`
  - `X-User-Email: dev@example.com`
  - `X-User-Role: ROLE_ADMIN`

### Configuration Files

#### User Service Development Config

File: `user-service/src/main/resources/application-dev.yml`

```yaml
jwt:
  enabled: false
security:
  dev-mode: true
```

#### API Gateway Development Config

File: `api-gateway/src/main/resources/application-dev.yml`

```yaml
jwt:
  enabled: false
security:
  dev-mode: true
```

### Security Components

#### Development Security Configurations

- **DevSecurityConfig** (User Service): Permits all requests without authentication
- **DevJwtAuthenticationFilter** (API Gateway): Bypasses JWT validation, adds mock headers

#### Conditional Activation

```java
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "false", matchIfMissing = false)
```

## Testing in Development Mode

### Test Endpoints

```bash
# Test user service directly
curl -X GET http://localhost:8083/api/users/test

# Test through API Gateway (recommended)
curl -X GET http://localhost:8080/api/users/test

# Test login endpoint (no JWT required)
curl -X POST http://localhost:8080/api/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

### Verify Development Mode

Check logs for these messages:

- User Service: `"DEV MODE: Security disabled - all endpoints permitted"`
- API Gateway: `"DEV MODE: JWT Filter bypassed for request"`

## Production Mode (Default)

To run with JWT enabled (production mode):

```bash
# Method 1: No special flags (default)
mvn spring-boot:run

# Method 2: Explicit JWT enabled
mvn spring-boot:run -Djwt.enabled=true

# Method 3: Production profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Important Notes

1. **Development Only**: This configuration should only be used for development/testing
2. **Security Warning**: Never deploy to production with JWT disabled
3. **Mock Headers**: Development mode adds mock user headers for testing downstream services
4. **Profile Isolation**: Development and production configurations are isolated
5. **Easy Toggle**: Switch between modes using configuration properties

## Troubleshooting

### Common Issues

1. **JWT still required**: Ensure `jwt.enabled=false` is set correctly
2. **Profile not active**: Verify `spring.profiles.active=dev` is set
3. **Port conflicts**: Check if ports 8080 (Gateway) and 8083 (User Service) are available

### Verification Commands

```bash
# Check active profile
curl http://localhost:8080/actuator/info

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8083/actuator/health
```

## Security Considerations

- Development mode bypasses all authentication
- Mock admin user is used for all requests
- All endpoints become publicly accessible
- Use only in development environments
- Never commit with JWT disabled in production configurations

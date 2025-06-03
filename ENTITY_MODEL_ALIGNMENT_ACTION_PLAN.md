# Entity Model Alignment Action Plan

## Executive Summary

This document outlines the specific steps needed to resolve the identified discrepancies between frontend TypeScript models and backend Java DTOs in the project management microservices system.

## Phase 1: High Priority Fixes (Critical - Complete First)

### 1.1 Backend UserDto Enhancement

**File**: `common-contracts/src/main/java/com/pm/commoncontracts/dto/UserDto.java`

**Changes Required**:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String password; // For input only, will not be stored as is
    private String firstName;
    private String lastName;
    private UserRole role;

    // NEW FIELDS TO ADD:
    private Boolean enabled;
    private Boolean active;
    private Instant lastLogin;
    private String profilePictureUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**Impact**: Enables full user management functionality in frontend.

### 1.2 Backend ProjectDto Priority Field Fix

**File**: `common-contracts/src/main/java/com/pm/commoncontracts/dto/ProjectDto.java`

**Changes Required**:

```java
// CHANGE FROM:
private String priority;

// CHANGE TO:
private ProjectPriority priority;

// REMOVE THESE METHODS:
public String getPriority() { return priority; }
public void setPriority(String priority) { this.priority = priority; }
```

**Impact**: Fixes type safety and validation for project priorities.

### 1.3 Backend NotificationDto Channel Field Fix

**File**: `common-contracts/src/main/java/com/pm/commoncontracts/dto/NotificationDto.java`

**Changes Required**:

```java
// CHANGE FROM:
private String channel;

// CHANGE TO:
private NotificationChannel channel;
```

**Import Required**:

```java
import com.pm.commoncontracts.domain.NotificationChannel;
```

**Impact**: Ensures type safety for notification channels.

### 1.4 Date Handling Standardization

**Decision Required**: Choose one approach:

**Option A: Backend uses ISO Strings**

- Change all `Instant` and `Date` fields to `String` in backend DTOs
- Use ISO 8601 format consistently
- Simpler for JSON serialization

**Option B: Frontend handles Date objects**

- Update frontend models to use `Date` objects
- Configure Angular HTTP interceptors for date conversion
- More type-safe but requires more frontend work

**Recommended**: Option A (ISO Strings) for consistency with existing CommentDto pattern.

## Phase 2: Medium Priority Fixes

### 2.1 Backend Service Layer Updates

Update all service classes that use the modified DTOs:

**Files to Update**:

- `user-service/src/main/java/com/pm/userservice/service/UserService.java`
- `user-service/src/main/java/com/pm/userservice/controller/UserController.java`
- `project-service/src/main/java/com/pm/projectservice/service/ProjectService.java`
- `project-service/src/main/java/com/pm/projectservice/controller/ProjectController.java`
- `notification-service/src/main/java/com/pm/notificationservice/service/NotificationService.java`

### 2.2 Database Entity Updates

Update entity classes to include new fields:

**Files to Update**:

- `user-service/src/main/java/com/pm/userservice/entity/User.java`
- Add corresponding database migration scripts

### 2.3 Frontend Type Safety Improvements

**File**: `frontend-angular/src/app/models/user/user.model.ts`

Add type guards:

```typescript
export function isValidUserDto(obj: any): obj is UserDto {
  return obj &&
    typeof obj.username === 'string' &&
    typeof obj.email === 'string' &&
    // Add validation for all required fields
}
```

## Phase 3: Implementation Steps

### Step 1: Backend DTO Updates

1. **Update UserDto** - Add missing fields
2. **Update ProjectDto** - Fix priority type
3. **Update NotificationDto** - Fix channel type
4. **Rebuild common-contracts** - `mvn clean install`

### Step 2: Dependent Service Updates

1. **User Service Updates**:

   ```bash
   # Update User entity
   # Update UserService class
   # Update UserController class
   # Create database migration
   ```

2. **Project Service Updates**:

   ```bash
   # Update Project entity
   # Update ProjectService class
   # Update ProjectController class
   ```

3. **Notification Service Updates**:
   ```bash
   # Update Notification entity
   # Update NotificationService class
   # Update NotificationController class
   ```

### Step 3: Database Migrations

Create migration scripts for each service:

**User Service Migration**:

```sql
ALTER TABLE users
ADD COLUMN enabled BOOLEAN DEFAULT true,
ADD COLUMN active BOOLEAN DEFAULT true,
ADD COLUMN last_login TIMESTAMP,
ADD COLUMN profile_picture_url VARCHAR(255),
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

### Step 4: Frontend Updates

1. **Update API Services** - Handle new response fields
2. **Update Components** - Use new user management fields
3. **Add Error Handling** - For type mismatches
4. **Update Tests** - Mock new fields

### Step 5: Testing

1. **Unit Tests** - Update for new DTO fields
2. **Integration Tests** - Test API communication
3. **End-to-End Tests** - Verify full functionality

## Phase 4: Quality Assurance

### 4.1 Automated Model Validation

Create a tool to validate model alignment:

**File**: `scripts/model-validator.js`

```javascript
// Script to compare TypeScript interfaces with Java DTOs
// Detect mismatches automatically
// Run as part of CI/CD pipeline
```

### 4.2 API Documentation Updates

Update OpenAPI/Swagger specifications to reflect changes:

- `api-gateway/src/main/resources/openapi.yml`
- Generate TypeScript types from OpenAPI specs

### 4.3 Integration Test Suite

Create comprehensive tests covering:

- Date serialization/deserialization
- Enum validation
- Field presence validation
- Type conversion accuracy

## Risk Assessment & Mitigation

### Risks

1. **Breaking Changes**: DTO modifications may break existing clients
2. **Database Migration**: Risk of data loss during migration
3. **Deployment Coordination**: All services must be updated together

### Mitigation Strategies

1. **Versioned APIs**: Maintain backward compatibility during transition
2. **Database Backups**: Full backup before migration
3. **Staged Deployment**: Deploy in development → staging → production
4. **Rollback Plan**: Prepare rollback scripts for each change

## Success Criteria

### Definition of Done

- [ ] All High Priority discrepancies resolved
- [ ] Backend DTOs include all frontend-expected fields
- [ ] Type consistency achieved across all models
- [ ] Date handling standardized
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Zero type-related runtime errors in frontend

### Validation Checklist

- [ ] UserDto includes: enabled, active, lastLogin, profilePictureUrl, audit fields
- [ ] ProjectDto uses ProjectPriority enum instead of String
- [ ] NotificationDto uses NotificationChannel enum instead of String
- [ ] All date fields use consistent format
- [ ] Frontend can successfully consume all API responses
- [ ] API documentation reflects actual implementation

## Timeline Estimate

### Phase 1: Backend DTO Updates (2-3 days)

- Day 1: Update DTOs and rebuild common-contracts
- Day 2-3: Update dependent services and test

### Phase 2: Service & Database Updates (3-4 days)

- Day 1-2: Update service layers and controllers
- Day 3: Database migrations
- Day 4: Testing and fixes

### Phase 3: Frontend Updates (2 days)

- Day 1: Update TypeScript models and services
- Day 2: Update components and add error handling

### Phase 4: QA & Documentation (1-2 days)

- Day 1: Comprehensive testing
- Day 2: Documentation updates and final validation

**Total Estimated Time**: 8-11 days

## Post-Implementation Monitoring

### Metrics to Track

1. **API Error Rates** - Monitor for type-related errors
2. **Frontend Error Logs** - Check for serialization issues
3. **Database Performance** - Monitor after schema changes
4. **User Experience** - Validate new features work correctly

### Maintenance Tasks

1. **Regular Model Sync Checks** - Monthly validation
2. **API Contract Testing** - Automated in CI/CD
3. **Documentation Updates** - Keep models documented
4. **Type Safety Reviews** - Code review focus area

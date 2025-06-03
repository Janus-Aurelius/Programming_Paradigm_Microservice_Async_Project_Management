# Role-Based Access Control (RBAC) Documentation

## Overview

This document provides a comprehensive overview of the Role-Based Access Control (RBAC) implementation in the Project Management System. The RBAC system ensures that users can only access resources and perform actions that are appropriate to their assigned roles.

## Architecture

### Backend Authority

- **Primary Principle**: The backend is the single source of truth for all authorization decisions
- **Security Annotations**: All endpoints use `@PreAuthorize` annotations to enforce permissions
- **Permission Evaluators**: Custom evaluators handle resource-specific access control
- **JWT Integration**: User roles are extracted from JWT tokens for authentication

### Frontend Integration

- **AuthService**: Frontend service that queries backend for permission checks
- **Permission Endpoints**: Dedicated HTTP endpoints return permission status as JSON
- **Error Handling**: Graceful handling of 403 Forbidden and 401 Unauthorized responses
- **UI Controls**: Dynamic UI element visibility based on user permissions

## Role Hierarchy

### ROLE_USER (Basic User)

**Permissions:**

- `USER_SELF_READ` - Read own profile
- `USER_SELF_UPDATE` - Update own profile
- `CMT_CREATE` - Create comments
- `CMT_UPDATE_OWN` - Update own comments
- `CMT_DELETE_OWN` - Delete own comments
- `NOTI_READ` - Read notifications
- `NOTI_MARK_READ` - Mark notifications as read

**Restrictions:**

- Cannot access projects directly
- Cannot create or manage tasks
- Cannot manage other users
- Limited to self-service operations and commenting

### ROLE_DEVELOPER (Developer)

**Inherits:** All USER permissions

**Additional Permissions:**

- `PRJ_READ` - Read project information
- `TASK_CREATE` - Create tasks
- `TASK_READ` - Read task information
- `TASK_UPDATE` - Update tasks (own or assigned)
- `TASK_STATUS_CHANGE` - Change task status

**Restrictions:**

- Cannot create, update, or delete projects
- Cannot assign tasks to others
- Cannot delete tasks
- Cannot moderate comments from others

### ROLE_PROJECT_MANAGER (Project Manager)

**Inherits:** All DEVELOPER permissions

**Additional Permissions:**

- `PRJ_CREATE` - Create projects
- `PRJ_UPDATE` - Update project information
- `PRJ_DELETE` - Delete projects
- `PRJ_ARCHIVE` - Archive projects
- `TASK_ASSIGN` - Assign tasks to users
- `TASK_DELETE` - Delete tasks
- `CMT_DELETE_ANY` - Delete any comment (moderation)

**Restrictions:**

- Cannot manage users (create/delete users)
- Cannot access system-wide administration

### ROLE_ADMIN (Administrator)

**Permissions:** `**` (Wildcard - All permissions)

**Capabilities:**

- Full access to all system resources
- User management (create, read, update, delete users)
- System configuration
- Override any access control restriction

## Access Control Matrix

| Resource/Action    | USER | DEVELOPER         | PROJECT_MANAGER | ADMIN |
| ------------------ | ---- | ----------------- | --------------- | ----- |
| **Projects**       |      |                   |                 |       |
| Create Project     | ❌   | ❌                | ✅              | ✅    |
| Read Project       | ❌   | ✅                | ✅              | ✅    |
| Update Project     | ❌   | ❌                | ✅              | ✅    |
| Delete Project     | ❌   | ❌                | ✅              | ✅    |
| Archive Project    | ❌   | ❌                | ✅              | ✅    |
| **Tasks**          |      |                   |                 |       |
| Create Task        | ❌   | ✅                | ✅              | ✅    |
| Read Task          | ❌   | ✅                | ✅              | ✅    |
| Update Task        | ❌   | ✅ (own/assigned) | ✅              | ✅    |
| Delete Task        | ❌   | ❌                | ✅              | ✅    |
| Assign Task        | ❌   | ❌                | ✅              | ✅    |
| Change Task Status | ❌   | ✅                | ✅              | ✅    |
| **Comments**       |      |                   |                 |       |
| Create Comment     | ✅   | ✅                | ✅              | ✅    |
| Read Comments      | ✅   | ✅                | ✅              | ✅    |
| Update Own Comment | ✅   | ✅                | ✅              | ✅    |
| Update Any Comment | ❌   | ❌                | ✅              | ✅    |
| Delete Own Comment | ✅   | ✅                | ✅              | ✅    |
| Delete Any Comment | ❌   | ❌                | ✅              | ✅    |
| **Users**          |      |                   |                 |       |
| Read Own Profile   | ✅   | ✅                | ✅              | ✅    |
| Update Own Profile | ✅   | ✅                | ✅              | ✅    |
| Read Other Users   | ❌   | ❌                | ❌              | ✅    |
| Create Users       | ❌   | ❌                | ❌              | ✅    |
| Delete Users       | ❌   | ❌                | ❌              | ✅    |
| **Notifications**  |      |                   |                 |       |
| Read Notifications | ✅   | ✅                | ✅              | ✅    |
| Mark as Read       | ✅   | ✅                | ✅              | ✅    |

## Permission Check Endpoints

### Project Service

- `GET /projects/{id}/permissions/check`
  - Returns: `{"hasAccess": true}` if authorized
  - Status: 200 OK if authorized, 403 Forbidden if not

### Task Service

- `GET /api/tasks/{id}/permissions/check`
  - Returns: `{"hasAccess": true}` if authorized
  - Status: 200 OK if authorized, 403 Forbidden if not

### Comment Service

- `GET /comments/{commentId}/permissions/check`
  - Returns: `{"hasAccess": true}` if authorized
  - Status: 200 OK if authorized, 403 Forbidden if not

## Resource-Specific Permissions

### Project Ownership

- Project managers can only manage projects they created or are assigned to manage
- Implemented via `ProjectPermissionEvaluator`

### Task Assignment

- Developers can update tasks they are assigned to
- Project managers can assign tasks and update any task in their projects
- Implemented via `TaskPermissionEvaluator`

### Comment Ownership

- Users can only update/delete their own comments
- Project managers can moderate (update/delete) any comment
- Implemented via `CommentPermissionEvaluator`

## Error Handling

### Backend Error Responses

- **401 Unauthorized**: User is not authenticated
- **403 Forbidden**: User is authenticated but lacks required permissions
- **404 Not Found**: Resource does not exist or user lacks read permissions

### Frontend Error Handling

- **AuthService**: Catches HTTP errors and provides fallback behavior
- **UI Components**: Hide/disable elements based on permission status
- **Error Messages**: User-friendly messages for access denied scenarios

## Security Configuration

### JWT Token Structure

```json
{
  "sub": "user-id",
  "roles": ["ROLE_DEVELOPER", "ROLE_USER"],
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Security Filter Chain

1. JWT Authentication Filter
2. Role Extraction
3. Permission Evaluation
4. Endpoint Access Control

## Testing Strategy

### Unit Tests

- `PermissionEvaluatorTest`: Tests role-based permission logic
- Individual service unit tests for permission evaluators

### Integration Tests

- `ProjectRbacIntegrationTest`: End-to-end project access control
- `TaskRbacIntegrationTest`: End-to-end task access control
- `CommentRbacIntegrationTest`: End-to-end comment access control

### Test Scenarios

- ✅ Authorized access returns expected data
- ✅ Unauthorized access returns 403 Forbidden
- ✅ Unauthenticated access returns 401 Unauthorized
- ✅ Resource-specific permissions (own vs others)
- ✅ Role hierarchy (higher roles inherit lower permissions)

## Performance Considerations

### Permission Check Optimization

- **Caching**: Role permissions are cached to reduce database queries
- **Batch Checks**: Frontend can batch multiple permission checks
- **Minimal Data**: Permission endpoints return minimal JSON payload

### Database Impact

- Permission evaluators use efficient queries
- Indexes on user_id, project_id, task_id for fast lookups
- Connection pooling for high-concurrency scenarios

## Monitoring and Auditing

### Security Events

- Failed authorization attempts are logged
- Permission check patterns are monitored
- Unusual access patterns trigger alerts

### Audit Trail

- All permission-related decisions are logged
- User role changes are tracked
- Resource access patterns are recorded

## Troubleshooting

### Common Issues

1. **403 on valid requests**: Check JWT token expiration and role assignment
2. **Permission endpoint 404**: Verify service is running and endpoint exists
3. **Frontend UI not updating**: Check AuthService error handling and caching

### Debug Steps

1. Verify JWT token contains correct roles
2. Check backend logs for permission evaluation details
3. Test permission endpoints directly with curl/Postman
4. Validate frontend AuthService is calling correct endpoints

## Future Enhancements

### Planned Features

- **Fine-grained Permissions**: More specific action permissions
- **Dynamic Role Assignment**: Runtime role changes without re-authentication
- **Permission Delegation**: Temporary permission grants
- **Resource-based Roles**: Roles scoped to specific projects/teams

### Scalability Improvements

- **Permission Caching**: Redis-based permission cache
- **Async Permission Checks**: Non-blocking permission validation
- **Bulk Operations**: Batch permission checks for large datasets

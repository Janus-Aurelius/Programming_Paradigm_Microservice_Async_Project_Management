Current RBAC Architecture:
Role System:
Uses 4 roles: ROLE_USER, ROLE_ADMIN, ROLE_PROJECT_MANAGER, ROLE_DEVELOPER
Roles are stored in AuthService and managed via BehaviorSubjects
Permission System:
Fine-grained permissions defined in Action enum (e.g., USER_READ, PRJ_CREATE, TASK_UPDATE, etc.)
Permissions cover Users, Projects, Tasks, Comments, and Notifications
Guards:
authGuard - Basic authentication check
roleGuard - Role-based access control for routes
permissionGuard - Permission-based access control for routes
Directives:
HasRoleDirective - Show/hide elements based on user roles
HasPermissionDirective - Show/hide elements based on permissions (supports resource-specific checks)
Pipes:
PermissionPipe - For permission checks in templates
AuthService:
Manages authentication state, user info, roles, and permissions
Provides methods like hasRole(), hasPermission(), hasAllPermissions(), etc.
Supports resource-specific permission checks for projects and tasks
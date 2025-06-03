# Complete RBAC Implementation Guide: Frontend Integration Roadmap

## Overview

This guide provides a step-by-step implementation roadmap to complete the RBAC integration between your existing backend microservices and Angular frontend. Based on the analysis of your current implementation, this guide focuses on bridging the gaps to create a fully functional RBAC system.

## Current Implementation Status

### ✅ Backend - Fully Implemented
Your backend RBAC system is comprehensive and production-ready with:
- JWT-based authentication with role/permission claims
- Hierarchical role system (ADMIN > PROJECT_MANAGER > DEVELOPER > USER)
- Granular permissions system (44+ specific actions)
- API Gateway security filtering
- Service-specific permission evaluators
- YAML-based configuration

### 🔄 Frontend - Partially Implemented
Your Angular frontend has basic authentication but needs enhancement:
- ✅ Basic AuthService with JWT handling
- ✅ Token storage and retrieval
- ✅ Basic role checking methods (hasRole, hasAnyRole, hasAllRoles)
- ✅ Basic HasRoleDirective structure
- ❌ Missing permission-based authorization
- ❌ Incomplete directive implementations
- ❌ No route guards for roles/permissions
- ❌ No resource-specific access control

## Implementation Roadmap

### Phase 1: Enhance AuthService for Full RBAC Support

#### 1.1 Update AuthService with Permission Methods

Your current `AuthService` needs to be enhanced to handle permissions from JWT tokens and provide permission-checking methods.

**File:** `frontend-angular/src/app/core/services/auth.service.ts`

**Changes needed:**
1. Add permission extraction from JWT tokens
2. Add permission-based checking methods
3. Add resource-specific permission checking
4. Add token refresh mechanism

#### 1.2 Create Permission Models

**File:** `frontend-angular/src/app/models/permission.model.ts` (NEW)

Create TypeScript interfaces that match your backend Action enum and permission structure.

#### 1.3 Update Enums Model

**File:** `frontend-angular/src/app/models/enums.model.ts`

Ensure the Action enum matches your backend Action.java exactly.

### Phase 2: Implement Route Guards

#### 2.1 Create Role-Based Route Guard

**File:** `frontend-angular/src/app/core/guards/role.guard.ts` (NEW)

#### 2.2 Create Permission-Based Route Guard

**File:** `frontend-angular/src/app/core/guards/permission.guard.ts` (NEW)

#### 2.3 Create Resource-Specific Guards

**File:** `frontend-angular/src/app/core/guards/resource.guard.ts` (NEW)

### Phase 3: Complete Directive Implementations

#### 3.1 Complete HasRoleDirective

**File:** `frontend-angular/src/app/shared/directives/has-role.directive.ts`

Your current directive is incomplete. It needs proper implementation.

#### 3.2 Create HasPermissionDirective

**File:** `frontend-angular/src/app/shared/directives/has-permission.directive.ts` (NEW)

#### 3.3 Create Permission Pipes

**File:** `frontend-angular/src/app/shared/pipes/permission.pipe.ts` (NEW)

### Phase 4: Add HTTP Interceptors

#### 4.1 Create Auth Interceptor

**File:** `frontend-angular/src/app/core/interceptors/auth.interceptor.ts`

#### 4.2 Create Error Handling Interceptor

**File:** `frontend-angular/src/app/core/interceptors/error.interceptor.ts`

### Phase 5: Implement Advanced Features

#### 5.1 Token Refresh Service

**File:** `frontend-angular/src/app/core/services/token-refresh.service.ts` (NEW)

#### 5.2 Permission Cache Service

**File:** `frontend-angular/src/app/core/services/permission-cache.service.ts` (NEW)

## Detailed Implementation Steps

### Step 1: Enhance AuthService

First, let's enhance your existing AuthService to extract and handle permissions from JWT tokens:

```typescript
// Add these methods to your existing auth.service.ts

export interface DecodedToken {
  sub: string;
  userId: string;
  email: string;
  roles: UserRole[];
  permissions: string[];
  exp: number;
  iat: number;
}

// Add to AuthService class:
private userPermissions$ = new BehaviorSubject<string[]>([]);

private extractTokenData(token: string): DecodedToken | null {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    
    // Extract permissions from roles based on your permissions.yml
    const permissions = this.extractPermissionsFromRoles(decoded.roles || []);
    
    return {
      ...decoded,
      permissions: [...(decoded.permissions || []), ...permissions]
    };
  } catch (error) {
    console.error('Error decoding token:', error);
    return null;
  }
}

private extractPermissionsFromRoles(roles: UserRole[]): string[] {
  // This should match your permissions.yml structure
  const rolePermissions: Record<UserRole, string[]> = {
    [UserRole.ROLE_ADMIN]: [
      'USER_READ', 'USER_UPDATE', 'USER_DELETE', 'USER_CREATE',
      'PRJ_READ', 'PRJ_UPDATE', 'PRJ_DELETE', 'PRJ_CREATE', 'PRJ_MANAGE_MEMBERS',
      'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_CREATE', 'TASK_ASSIGN',
      'CMT_READ', 'CMT_UPDATE', 'CMT_DELETE', 'CMT_CREATE',
      'NOTI_READ', 'NOTI_UPDATE', 'NOTI_DELETE', 'NOTI_CREATE'
    ],
    [UserRole.ROLE_PROJECT_MANAGER]: [
      'USER_READ', 'PRJ_READ', 'PRJ_UPDATE', 'PRJ_CREATE', 'PRJ_MANAGE_MEMBERS',
      'TASK_READ', 'TASK_UPDATE', 'TASK_CREATE', 'TASK_ASSIGN',
      'CMT_READ', 'CMT_UPDATE', 'CMT_CREATE',
      'NOTI_READ', 'NOTI_CREATE'
    ],
    [UserRole.ROLE_DEVELOPER]: [
      'USER_READ', 'PRJ_READ', 'TASK_READ', 'TASK_UPDATE', 'TASK_CREATE',
      'CMT_READ', 'CMT_UPDATE', 'CMT_CREATE', 'NOTI_READ'
    ],
    [UserRole.ROLE_USER]: [
      'USER_READ', 'PRJ_READ', 'TASK_READ', 'CMT_READ', 'NOTI_READ'
    ]
  };

  const allPermissions = new Set<string>();
  roles.forEach(role => {
    rolePermissions[role]?.forEach(permission => allPermissions.add(permission));
  });
  
  return Array.from(allPermissions);
}

// Permission checking methods
hasPermission(permission: string): Observable<boolean> {
  return this.userPermissions$.pipe(
    map(permissions => permissions.includes(permission))
  );
}

hasAnyPermission(permissions: string[]): Observable<boolean> {
  return this.userPermissions$.pipe(
    map(userPermissions => 
      permissions.some(permission => userPermissions.includes(permission))
    )
  );
}

hasAllPermissions(permissions: string[]): Observable<boolean> {
  return this.userPermissions$.pipe(
    map(userPermissions => 
      permissions.every(permission => userPermissions.includes(permission))
    )
  );
}

// Resource-specific permission checking
canAccessProject(projectId: number, action: string): Observable<boolean> {
  return this.http.get<boolean>(`/api/projects/${projectId}/permissions/check`, {
    params: { action }
  });
}

canAccessTask(taskId: number, action: string): Observable<boolean> {
  return this.http.get<boolean>(`/api/tasks/${taskId}/permissions/check`, {
    params: { action }
  });
}
```

### Step 2: Complete HasRoleDirective Implementation

Your current `HasRoleDirective` is incomplete. Here's the complete implementation:

```typescript
// Complete implementation for has-role.directive.ts
@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective implements OnInit, OnDestroy {
  @Input() appHasRole: UserRole | UserRole[] = [];
  @Input() requireAll: boolean = false;

  private destroy$ = new Subject<void>();
  private hasCreatedView = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.checkPermissions();
  }

  private checkPermissions(): void {
    const roles = Array.isArray(this.appHasRole) ? this.appHasRole : [this.appHasRole];
    
    if (roles.length === 0) {
      this.updateView(false);
      return;
    }

    const hasRole$ = this.requireAll 
      ? this.authService.hasAllRoles(roles)
      : this.authService.hasAnyRole(roles);

    hasRole$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(hasRole => {
      this.updateView(hasRole);
    });
  }

  private updateView(show: boolean): void {
    if (show && !this.hasCreatedView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasCreatedView = true;
    } else if (!show && this.hasCreatedView) {
      this.viewContainer.clear();
      this.hasCreatedView = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### Step 3: Create Route Guards

#### Role Guard
```typescript
// Create: frontend-angular/src/app/core/guards/role.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../../models/enums.model';

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.hasAnyRole(allowedRoles).pipe(
      take(1),
      map(hasRole => {
        if (hasRole) {
          return true;
        } else {
          router.navigate(['/forbidden'], { 
            queryParams: { reason: 'insufficient_role' } 
          });
          return false;
        }
      })
    );
  };
};
```

#### Permission Guard
```typescript
// Create: frontend-angular/src/app/core/guards/permission.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

export const permissionGuard = (requiredPermissions: string[], requireAll: boolean = true): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const hasPermission$ = requireAll 
      ? authService.hasAllPermissions(requiredPermissions)
      : authService.hasAnyPermission(requiredPermissions);

    return hasPermission$.pipe(
      take(1),
      map(hasPermissions => {
        if (hasPermissions) {
          return true;
        } else {
          router.navigate(['/forbidden'], { 
            queryParams: { 
              reason: 'insufficient_permissions',
              required: requiredPermissions.join(',')
            } 
          });
          return false;
        }
      })
    );
  };
};
```

### Step 4: Create Permission Directive

```typescript
// Create: frontend-angular/src/app/shared/directives/has-permission.directive.ts
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input() appHasPermission: string | string[] = [];
  @Input() requireAll: boolean = false;
  @Input() resourceId?: number;
  @Input() resourceType?: 'project' | 'task';

  private destroy$ = new Subject<void>();
  private hasCreatedView = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.checkPermissions();
  }

  private checkPermissions(): void {
    const permissions = Array.isArray(this.appHasPermission) 
      ? this.appHasPermission 
      : [this.appHasPermission];
    
    if (permissions.length === 0) {
      this.updateView(false);
      return;
    }

    let hasPermission$: Observable<boolean>;

    // Resource-specific permission check
    if (this.resourceId && this.resourceType && permissions.length === 1) {
      hasPermission$ = this.resourceType === 'project'
        ? this.authService.canAccessProject(this.resourceId, permissions[0])
        : this.authService.canAccessTask(this.resourceId, permissions[0]);
    } else {
      // General permission check
      hasPermission$ = this.requireAll 
        ? this.authService.hasAllPermissions(permissions)
        : this.authService.hasAnyPermission(permissions);
    }

    hasPermission$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(hasPermission => {
      this.updateView(hasPermission);
    });
  }

  private updateView(show: boolean): void {
    if (show && !this.hasCreatedView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasCreatedView = true;
    } else if (!show && this.hasCreatedView) {
      this.viewContainer.clear();
      this.hasCreatedView = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### Step 5: Update Action Enum

Ensure your frontend Action enum matches the backend exactly:

```typescript
// Update: frontend-angular/src/app/models/enums.model.ts
export enum Action {
  // User actions
  USER_READ = 'USER_READ',
  USER_UPDATE = 'USER_UPDATE',
  USER_DELETE = 'USER_DELETE',
  USER_CREATE = 'USER_CREATE',
  
  // Project actions
  PRJ_READ = 'PRJ_READ',
  PRJ_UPDATE = 'PRJ_UPDATE',
  PRJ_DELETE = 'PRJ_DELETE',
  PRJ_CREATE = 'PRJ_CREATE',
  PRJ_MANAGE_MEMBERS = 'PRJ_MANAGE_MEMBERS',
  
  // Task actions
  TASK_READ = 'TASK_READ',
  TASK_UPDATE = 'TASK_UPDATE',
  TASK_DELETE = 'TASK_DELETE',
  TASK_CREATE = 'TASK_CREATE',
  TASK_ASSIGN = 'TASK_ASSIGN',
  
  // Comment actions
  CMT_READ = 'CMT_READ',
  CMT_UPDATE = 'CMT_UPDATE',
  CMT_DELETE = 'CMT_DELETE',
  CMT_CREATE = 'CMT_CREATE',
  
  // Notification actions
  NOTI_READ = 'NOTI_READ',
  NOTI_UPDATE = 'NOTI_UPDATE',
  NOTI_DELETE = 'NOTI_DELETE',
  NOTI_CREATE = 'NOTI_CREATE'
}
```

## Usage Examples

### Template Usage
```html
<!-- Role-based visibility -->
<button *appHasRole="UserRole.ROLE_ADMIN" (click)="deleteUser()">
  Delete User
</button>

<!-- Multiple roles (any) -->
<div *appHasRole="[UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER]">
  Management Panel
</div>

<!-- Permission-based visibility -->
<button *appHasPermission="Action.PRJ_UPDATE" [resourceId]="projectId" resourceType="project">
  Edit Project
</button>

<!-- Multiple permissions (all required) -->
<div *appHasPermission="[Action.PRJ_DELETE, Action.PRJ_UPDATE]" [requireAll]="true">
  Advanced Project Controls
</div>
```

### Route Configuration
```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: 'projects/:id/edit',
    component: ProjectEditComponent,
    canActivate: [
      authGuard, 
      permissionGuard([Action.PRJ_UPDATE])
    ]
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes').then(m => m.routes),
    canActivate: [
      authGuard, 
      roleGuard([UserRole.ROLE_ADMIN])
    ]
  }
];
```

### Component Usage
```typescript
// component.ts
export class ProjectComponent {
  canEditProject$ = this.authService.hasPermission(Action.PRJ_UPDATE);
  canDeleteProject$ = this.authService.canAccessProject(this.projectId, Action.PRJ_DELETE);
  
  constructor(private authService: AuthService) {}
}
```

## Testing Your Implementation

### 1. Test Authentication Flow
1. Login with different user roles
2. Verify JWT token contains correct roles and permissions
3. Check that permissions are properly extracted and stored

### 2. Test Route Guards
1. Try accessing protected routes without proper roles/permissions
2. Verify redirects to forbidden page
3. Test with different user roles

### 3. Test UI Components
1. Login with different roles and verify UI elements show/hide correctly
2. Test permission directives with resource-specific permissions
3. Verify role-based navigation visibility

### 4. Test API Integration
1. Verify that HTTP requests include proper authorization headers
2. Test that 401/403 responses are handled correctly
3. Test resource-specific permission checks

## Next Steps

1. **Implement the enhanced AuthService** - Start with the permission extraction and checking methods
2. **Complete the HasRoleDirective** - Fix the current incomplete implementation
3. **Create the missing directives and guards** - Implement HasPermissionDirective and route guards
4. **Update your routes** - Add appropriate guards to your route configuration
5. **Test thoroughly** - Verify all functionality works with your backend
6. **Add error handling** - Implement proper error handling for authorization failures
7. **Add caching** - Implement permission caching for better performance

This implementation will give you a complete, production-ready RBAC system that seamlessly integrates with your existing backend architecture.

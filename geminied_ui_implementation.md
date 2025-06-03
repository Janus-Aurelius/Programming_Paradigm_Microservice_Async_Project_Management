Okay, this is an excellent starting point for a UI roadmap! The fact that you have a `rolePermissions` matrix defined on the frontend is a **huge advantage** and will make implementing RBAC much cleaner and more maintainable.

Let's enhance this guide, focusing on leveraging that `rolePermissions` matrix effectively.

---

**Enhanced & Prioritized UI Implementation Roadmap**

**Project Management System with RBAC (Leveraging Frontend `rolePermissions` Matrix)**

### 📊 Implementation Overview

This roadmap details building the UI components, prioritizing core functionality and systematically integrating Role-Based Access Control (RBAC) using the provided frontend `rolePermissions` matrix.

- **Key Principle:** UI elements and actions will primarily be controlled by checking **permissions** derived from the user's roles via the `rolePermissions` matrix, rather than direct role checks in most cases.
- **Business Value**: Core functionality first.
- **RBAC Complexity**: Start with global permissions, then address resource-specific nuances.
- **User Dependencies**: Foundational components before feature-specific ones.
- **Risk Mitigation**: Essential features before nice-to-have.

---

**🔑 Key Changes Based on Your `rolePermissions` Matrix:**

1.  **`AuthService` Enhancement:**
    *   `AuthService` will store the current user's roles (e.g., `['ROLE_PROJECT_MANAGER']`).
    *   It will have a core method: `hasPermission(permission: string, resource?: any): boolean`. This method will:
        1.  Get the user's current roles.
        2.  Iterate through these roles.
        3.  For each role, look up its associated permissions in your `rolePermissions` matrix.
        4.  Check if the requested `permission` exists in the aggregated list of permissions for the user.
        5.  (Future) The `resource` parameter will be for more fine-grained, resource-specific permission checks (e.g., "can edit *this specific* project").
2.  **Primary RBAC Directive: `*appHasPermission`:**
    *   You'll create a structural directive (e.g., `*appHasPermission="'PRJ_CREATE'"`) that uses `authService.hasPermission()` to show/hide elements.
3.  **Role Checks for Broad Areas:** Direct role checks (`*appHasRole="'ROLE_ADMIN'"`) are still useful for gating entire sections or very high-level functionalities (like an "Admin Panel").

---

## 🎯 Phase 0: RBAC Foundation Setup (Before UI Components)

**Priority: CRITICAL** | **RBAC Complexity: LOW-MEDIUM**

### 0.1 `AuthService` with Permission Logic

**File:** `frontend-angular/src/app/core/services/auth.service.ts`
**Status:** Basic auth logic exists, needs permission checking.

#### Implementation Steps:

```typescript
// frontend-angular/src/app/core/services/auth.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UserDTO } from '../../models/user/user.dto'; // Your UserDTO model
import { UserRole } from '../../models/enums.model'; // Your UserRole enum

// Assume rolePermissions is imported or defined here
const rolePermissions: Record<UserRole, string[]> = {
  [UserRole.ROLE_ADMIN]: [
    'USER_READ', 'USER_UPDATE', /* ...all other admin permissions... */
    'PRJ_CREATE', 'PRJ_READ', 'PRJ_UPDATE', 'PRJ_DELETE', 'PRJ_MANAGE_MEMBERS',
    'TASK_CREATE', 'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_ASSIGN',
    // ... etc.
  ],
  [UserRole.ROLE_PROJECT_MANAGER]: [
    'USER_READ', 'PRJ_CREATE', 'PRJ_READ', 'PRJ_UPDATE', 'PRJ_MANAGE_MEMBERS',
    'TASK_CREATE', 'TASK_READ', 'TASK_UPDATE', 'TASK_ASSIGN',
    // ... etc.
  ],
  [UserRole.ROLE_DEVELOPER]: [ /* ... developer permissions ... */ ],
  [UserRole.ROLE_USER]: [ /* ... basic user permissions ... */ ],
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<UserDTO | null>(null); // Initialize appropriately
  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.currentUser$.pipe(map(user => !!user));

  // ... login, logout, storage methods ...

  // Call this after successful login with user data from backend
  setCurrentUser(user: UserDTO | null): void {
    this.currentUserSubject.next(user);
    // Store user in localStorage/sessionStorage if needed
  }

  public get currentUserRoles(): UserRole[] {
    return (this.currentUserSubject.value?.roles as UserRole[]) || [];
  }

  public hasRole(roleOrRoles: UserRole | UserRole[]): boolean {
    const userRoles = this.currentUserRoles;
    if (!userRoles.length) return false;
    const rolesToCheck = Array.isArray(roleOrRoles) ? roleOrRoles : [roleOrRoles];
    return rolesToCheck.some(r => userRoles.includes(r));
  }

  public hasPermission(permission: string, _resource?: any): boolean { // _resource for future use
    const userRoles = this.currentUserRoles;
    if (!userRoles.length) return false;

    for (const role of userRoles) {
      const permissionsForRole = rolePermissions[role];
      if (permissionsForRole && permissionsForRole.includes(permission)) {
        return true;
      }
    }
    return false;
  }

  // Observable version for reactive checks in templates if needed
  public hasPermission$(permission: string, _resource?: any): Observable<boolean> {
    return this.currentUser$.pipe(
      map(user => {
        if (!user || !user.roles) return false;
        const userRoles = user.roles as UserRole[];
        for (const role of userRoles) {
          const permissionsForRole = rolePermissions[role];
          if (permissionsForRole && permissionsForRole.includes(permission)) {
            return true;
          }
        }
        return false;
      })
    );
  }
}
```

### 0.2 `HasPermission` Structural Directive

**File:** `frontend-angular/src/app/shared/directives/has-permission.directive.ts`
**Status:** Needs creation.

#### Implementation Steps:

```bash
ng generate directive shared/directives/hasPermission --standalone
```
```typescript
// frontend-angular/src/app/shared/directives/has-permission.directive.ts
import { Directive, Input, OnDestroy, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '../../core/services/auth.service'; // Adjust path
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  private permission: string = '';
  private resource: any; // For future resource-specific checks
  private destroy$ = new Subject<void>();
  private hasView = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  @Input()
  set appHasPermission(value: string) { // Input is the permission string
    this.permission = value;
    this.updateView();
  }

  @Input()
  set appHasPermissionResource(resource: any) { // Optional resource context
    this.resource = resource;
    this.updateView();
  }

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.updateView(); // Re-evaluate when user context changes
    });
  }

  private updateView(): void {
    if (!this.permission) { // If no permission is specified, don't show
        this.viewContainer.clear();
        this.hasView = false;
        return;
    }
    const condition = this.authService.hasPermission(this.permission, this.resource);
    if (condition && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!condition && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### 0.3 (Optional) `HasRole` Structural Directive

*   Similar to `HasPermissionDirective` but calls `authService.hasRole()`. Useful for gating entire sections like an "Admin Panel".

**Testing Criteria (Phase 0):**
- [ ] `AuthService.hasPermission()` correctly returns true/false based on user roles and the `rolePermissions` matrix.
- [ ] `*appHasPermission` directive correctly shows/hides elements based on `AuthService.hasPermission()`.

---

## 🎯 Phase 1: Foundation & Core Navigation (Week 1-2)

**Priority: CRITICAL** | **RBAC Complexity: LOW (using permissions)** | **Business Value: HIGH**

### 1.1 Enhanced Sidebar Navigation ⭐⭐⭐

**File:** `frontend-angular/src/app/layouts/main-layout/sidebar/sidebar.component.ts` & `.html`
**Status:** Basic structure exists, needs RBAC integration using `*appHasPermission`.

#### Implementation Steps:

1.  **Import `HasPermissionDirective`** into `SidebarComponent`'s `imports` array.
2.  Update `navItems` in `sidebar.component.ts` to use `permissions` primarily. Use `roles` only if a section is strictly role-gated and not covered by a permission.
    ```typescript
    // sidebar.component.ts
    interface NavItem { /* ... as before ... */ }
    navItems: NavItem[] = [
      { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' /* All authenticated users */ },
      { label: 'Projects', icon: 'workspaces', route: '/projects', permissions: ['PRJ_READ'] },
      { label: 'My Tasks', icon: 'assignment_turned_in', route: '/my-tasks', permissions: ['TASK_READ'] }, // Assuming all users can read their own tasks
      { label: 'Team Management', icon: 'group', route: '/users', permissions: ['USER_READ'] }, // PMs and Admins have USER_READ
      { label: 'Admin Panel', icon: 'admin_panel_settings', route: '/admin', roles: ['ROLE_ADMIN'] } // Role-gated
    ];
    ```
3.  Update `sidebar.component.html` to use `*appHasPermission` or `*appHasRole`.
    ```html
    <!-- sidebar.component.html -->
    <mat-nav-list>
      <ng-container *ngFor="let item of navItems">
        <a *ngIf="!item.permissions && !item.roles" mat-list-item [routerLink]="item.route" routerLinkActive="active-link">...</a>
        <a *appHasPermission="item.permissions ? item.permissions[0] : ''" <!-- Simplified: assumes single permission for now -->
           *ngIf="item.permissions"
           mat-list-item [routerLink]="item.route" routerLinkActive="active-link">...</a>
        <a *appHasRole="item.roles ? item.roles[0] : ''" <!-- Simplified: assumes single role for now -->
           *ngIf="item.roles && !item.permissions" <!-- Only use role if no permission specified -->
           mat-list-item [routerLink]="item.route" routerLinkActive="active-link">...</a>
        <!-- Add logic for multiple permissions/roles if needed, or enhance directive -->
      </ng-container>
    </mat-nav-list>
    ```
    *Self-correction: The template logic for checking permissions/roles in the `*ngFor` needs to be robust. The directive handles the core logic, so the template can be simpler if the `navItems` structure is well-defined.*
    **Revised Template Snippet for Sidebar:**
    ```html
    <mat-nav-list>
        <ng-container *ngFor="let item of navItems">
            <!-- If item has specific permissions defined, use HasPermissionDirective -->
            <a *ngIf="item.permissions && item.permissions.length > 0; else roleCheckOrPublic"
               [appHasPermission]="item.permissions[0]" <!-- Enhance directive to take array -->
               mat-list-item [routerLink]="item.route" routerLinkActive="active-link">
                <mat-icon matListItemIcon>{{item.icon}}</mat-icon> {{item.label}}
            </a>
            <ng-template #roleCheckOrPublic>
                <!-- If item has specific roles defined (and no permissions), use HasRoleDirective -->
                <a *ngIf="item.roles && item.roles.length > 0; else publicItem"
                   [appHasRole]="item.roles" <!-- Enhance directive to take array -->
                   mat-list-item [routerLink]="item.route" routerLinkActive="active-link">
                    <mat-icon matListItemIcon>{{item.icon}}</mat-icon> {{item.label}}
                </a>
            </ng-template>
            <ng-template #publicItem>
                <!-- If no permissions or roles, assume public for authenticated users -->
                <a *ngIf="!item.permissions && !item.roles"
                   mat-list-item [routerLink]="item.route" routerLinkActive="active-link">
                    <mat-icon matListItemIcon>{{item.icon}}</mat-icon> {{item.label}}
                </a>
            </ng-template>
        </ng-container>
    </mat-nav-list>
    ```
    *(Note: The `HasPermissionDirective` and `HasRoleDirective` would ideally be enhanced to accept an array of permissions/roles and check if the user has *any* of them.)*

**Testing Criteria:**
- [x] `AuthService.hasPermission()` is functional.
- [x] `*appHasPermission` directive is created and works.
- [ ] Navigation items show/hide based on user's derived permissions.
- [ ] "Admin Panel" link shows only for `ROLE_ADMIN`.
- [ ] Responsive design works.
- [ ] Visual feedback for active routes.

---

### 1.2 Enhanced Dashboard Page ⭐⭐⭐

**File:** `frontend-angular/src/app/features/dashboard/dashboard-page/dashboard-page.component.ts` & `.html`
**Status:** Basic template exists, needs permission-based content.

#### Implementation Steps:

1.  **Import `HasPermissionDirective` and `HasRoleDirective`** into `DashboardPageComponent`'s `imports` array.
2.  Inject `AuthService` into `DashboardPageComponent`.
3.  Fetch `currentUser$` from `AuthService`.
4.  Fetch `projectStats`, `taskStats`, `userStats` from respective API services (create stubs if services not ready).
5.  Update template to use `*appHasPermission` and `*appHasRole` as per your original example.

```html
<!-- dashboard-page.component.html -->
<div class="dashboard-container">
  <div class="welcome-section">
    <h1>Welcome, {{ (authService.currentUser$ | async)?.username }}!</h1>
    <p>Roles: {{ (authService.currentUser$ | async)?.roles?.join(', ') }}</p>
  </div>

  <div class="stats-grid">
    <mat-card *appHasPermission="'PRJ_READ'" class="stat-card">...</mat-card>
    <mat-card *appHasPermission="'TASK_READ'" class="stat-card">...</mat-card>
    <mat-card *appHasRole="'ROLE_ADMIN'" class="stat-card">...</mat-card> <!-- Or use a specific admin permission -->
  </div>

  <div class="quick-actions">
    <button *appHasPermission="'PRJ_CREATE'" mat-raised-button color="primary" (click)="navigateToCreateProject()">New Project</button>
    <button *appHasPermission="'TASK_CREATE'" mat-raised-button color="accent" (click)="navigateToCreateTask()">New Task</button>
  </div>
</div>
```

**Testing Criteria:**
- [ ] Dashboard shows relevant data/cards based on user's permissions.
- [ ] Quick actions appear only for users with corresponding permissions.
- [ ] Statistics load correctly (or show appropriate message if data not available).

---

## 🎯 Phase 2: Core Project Management (Week 3-4)

**Priority: HIGH** | **RBAC Complexity: MEDIUM (introducing resource context)** | **Business Value: HIGH**

### 2.1 Project List Component ⭐⭐⭐

**Path:** `frontend-angular/src/app/features/projects/project-list/`
**Status:** Directory exists, component needs creation.

#### Implementation Steps:

1.  **Create `project-list.component.ts` and `.html`.**
2.  **Import `HasPermissionDirective`** into `ProjectListComponent`'s `imports`.
3.  Inject `ProjectApiService`, `AuthService`.
4.  Fetch projects. **Backend API for `getProjects()` MUST filter projects based on the authenticated user's access.** The frontend should not be solely responsible for filtering which projects are *visible* in the list from an "all projects" firehose.
5.  Use `*appHasPermission="'PRJ_CREATE'"` for the "New Project" button.
6.  For actions per row (View, Edit, Delete):
    *   Use `*appHasPermission="'PRJ_READ'"` (or `'PRJ_UPDATE'`, `'PRJ_DELETE'`).
    *   **Resource-Specific Consideration:** True resource-specific checks (e.g., "can edit *this specific* project because I am its manager") are more complex.
        *   **Option A (Simpler for now):** The backend API for update/delete will enforce this. The UI can show the button if the user has the *general* permission (e.g., `PRJ_UPDATE`). If they click it for a project they can't edit, the backend returns 403.
        *   **Option B (Better UX, more complex):** The `ProjectDTO` from the backend could include a flag like `canEdit: boolean` based on the current user. Or, the `appHasPermissionResource` input on the directive could be used, and `AuthService.hasPermission` would need more sophisticated logic (potentially involving checking `project.managerId === currentUser.id`). **Start with Option A and enhance to Option B later if needed.**

```html
<!-- project-list.component.html -->
<div class="project-list-container">
  <div class="list-header">
    <h2>Projects</h2>
    <button *appHasPermission="'PRJ_CREATE'" mat-raised-button color="primary" (click)="openCreateDialog()">
      <mat-icon>add</mat-icon> New Project
    </button>
  </div>

  <mat-table [dataSource]="projects$" class="projects-table">
    <!-- ... Name, Status columns ... -->
    <ng-container matColumnDef="actions">
      <mat-header-cell *matHeaderCellDef>Actions</mat-header-cell>
      <mat-cell *matCellDef="let project">
        <button mat-icon-button (click)="viewProject(project)" *appHasPermission="'PRJ_READ'">
          <mat-icon>visibility</mat-icon>
        </button>
        <button mat-icon-button (click)="editProject(project)" *appHasPermission="'PRJ_UPDATE'"> <!-- General permission -->
          <mat-icon>edit</mat-icon>
        </button>
        <button mat-icon-button color="warn" (click)="deleteProject(project)" *appHasPermission="'PRJ_DELETE'"> <!-- General permission -->
          <mat-icon>delete</mat-icon>
        </button>
      </mat-cell>
    </ng-container>
    <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
    <mat-row *matRowDef="let row; columns: displayedColumns;"></mat-row>
  </mat-table>
</div>
```

**Testing Criteria:**
- [ ] Project list correctly displays projects accessible to the user (backend filtered).
- [ ] "New Project" button visibility controlled by `PRJ_CREATE` permission.
- [ ] Action buttons (View, Edit, Delete) visibility controlled by general permissions.
- [ ] Clicking Edit/Delete on a project the user *can't* modify (due to resource-specific rules) should be gracefully handled by the backend (403), and the UI should show an error.

---

### 2.2 Project Detail Component ⭐⭐

**Path:** `frontend-angular/src/app/features/projects/project-detail/`
**Status:** Needs creation.

#### Implementation Features:

-   Fetch project details (backend ensures user has access).
-   Display project info.
-   **RBAC Elements (using `*appHasPermission`):**
    -   "Edit Project Details" button: `*appHasPermission="'PRJ_UPDATE'"` (and potentially resource-specific check later).
    -   "Manage Team Members" section/button: `*appHasPermission="'PRJ_MANAGE_MEMBERS'"`.
    -   "Change Project Status" control: `*appHasPermission="'PRJ_UPDATE'"` (or a more specific `PRJ_STATUS_CHANGE` if you have it).
    -   Task list/board within this component will have its own task-level permissions.

---

## 🎯 Phase 3: Task Management System (Week 5-6)

**Priority: HIGH** | **RBAC Complexity: HIGH (resource & context-aware)** | **Business Value: HIGH**

### 3.1 Task Board/Kanban Component ⭐⭐⭐

**Path:** `frontend-angular/src/app/features/tasks/task-board/` (likely part of `ProjectDetailComponent`)
**Status:** Needs creation.

#### Key RBAC Features:

-   **Task Visibility:** Backend API for tasks within a project should filter based on user.
-   **Task Creation Button:** `*appHasPermission="'TASK_CREATE'"`.
-   **Drag-and-Drop (Status Change):**
    *   The ability to drag might be generally allowed if the user can see tasks.
    *   The API call to update status will be permission-checked by the backend.
    *   UI could visually restrict dragging if the user lacks `TASK_UPDATE` or a specific `TASK_STATUS_CHANGE` permission for *that task* (e.g., if not assignee or PM). This requires more complex logic in the component or a resource-aware permission check.
-   **Editing Task Details (e.g., from a task card quick edit):** `*appHasPermission="'TASK_UPDATE'"` (and resource-specific).
-   **Changing Task Priority:** `*appHasPermission="'TASK_PRIORITY_CHANGE'"` (or similar).

### 3.2 My Tasks Component ⭐⭐

**Path:** `frontend-angular/src/app/features/tasks/my-tasks/`
**Status:** Needs creation.
-   Fetches tasks assigned to the current user. Backend API handles this.
-   Actions on tasks (e.g., quick status update) will use `*appHasPermission="'TASK_UPDATE'"` (and the backend will verify they are indeed the assignee or have override).

---

## 🎯 Phase 4: User Management (Week 7-8)

**Priority: MEDIUM** | **RBAC Complexity: HIGH** | **Business Value: MEDIUM**

### 4.1 User List Component ⭐⭐

**Path:** `frontend-angular/src/app/features/user/user-list/` (Likely in an `/admin/users` route)
**Status:** Needs creation.

#### RBAC Considerations:

-   **Component Access:** Route guarded by `*appHasRole="'ROLE_ADMIN'"` or `*appHasPermission="'USER_READ_ALL'"`.
-   **User List Data:** Backend API for listing users should respect roles (Admin sees all, PM might see their team - requires backend logic).
-   **"Create User" Button:** `*appHasPermission="'USER_CREATE'"`.
-   **"Edit User Roles" Action:** `*appHasPermission="'USER_UPDATE_ROLES'"` (a very specific admin permission).
-   **"Disable/Enable User" Action:** `*appHasPermission="'USER_UPDATE_STATUS'"`.

### 4.2 User Profile Component ⭐

**Path:** `frontend-angular/src/app/features/user/user-profile/`
**Status:** Needs creation.
-   Users can always view their own profile.
-   Editing own profile fields (name, password) is a standard user action (no special permission beyond being authenticated).

---

## 🎯 Phase 5: Advanced Features (Week 9-10)

### 5.1 Comments System ⭐

**Path:** `frontend-angular/src/app/features/comments/` (likely components integrated into Task/Project Detail)
-   **Create Comment:** `*appHasPermission="'CMT_CREATE'"`.
-   **Edit Own Comment:** Logic in component + backend check.
-   **Delete Own/Any Comment:** `*appHasPermission="'CMT_DELETE_OWN'"` or `*appHasPermission="'CMT_DELETE_ANY'"`.

### 5.2 Notifications Center ⭐

**Path:** `frontend-angular/src/app/features/notifications/` (Notification Bell in Header, potentially a full list page)
-   **View Notifications:** `*appHasPermission="'NOTI_READ'"`.
-   **Mark as Read/Delete:** `*appHasPermission="'NOTI_UPDATE'"` or `*appHasPermission="'NOTI_DELETE'"`.

### 5.3 Admin Dashboard ⭐

**Path:** `frontend-angular/src/app/features/admin/admin-dashboard/`
-   Route guarded by `*appHasRole="'ROLE_ADMIN'"`.
-   Components within will use specific admin-level permissions.

---

## 🧪 Testing Strategy by Phase (Enhanced)

### Phase 0 Testing:
- [ ] Unit test `AuthService.hasPermission()` with various roles and permissions from the matrix.
- [ ] Unit test `HasPermissionDirective` (mock `AuthService`).

### Phase 1 Testing:
- [ ] Sidebar: Log in as each role, verify correct links appear/are hidden.
- [ ] Dashboard: Log in as each role, verify correct cards/actions appear.
- [ ] Test with a user having NO roles (should see minimal UI).
- [ ] Test with a user having a role NOT in the `rolePermissions` matrix (should gracefully have no permissions).

### Subsequent Phases:
- For each component with RBAC:
    - [ ] Test with a user who HAS the required permission(s).
    - [ ] Test with a user who LACKS the required permission(s) – verify element is hidden/disabled.
    - [ ] Test edge cases for combined permissions if applicable.
- **E2E Testing:** Crucial for RBAC. Create user personas for each role and script journeys:
    - `npm run e2e:admin` (can do everything)
    - `npm run e2e:project-manager` (can manage projects, assign tasks, etc.)
    - `npm run e2e:developer` (can work on assigned tasks, create tasks, comment)
    - `npm run e2e:basic-user` (can view, maybe comment on things they have access to)

---

## 📋 Implementation Checklist (Enhanced)

### Before Starting Each Phase:
- [x] `rolePermissions` matrix is defined and accessible in `AuthService`.
- [x] `AuthService.hasPermission()` method is implemented and unit tested.
- [x] `*appHasPermission` directive is implemented and unit tested.
- [ ] Backend API endpoints are working and **enforce permissions correctly**.
- [ ] Test data includes users with different roles.

### During Implementation:
- [ ] Consistently use `*appHasPermission` for UI element control.
- [ ] Use `*appHasRole` for broad section gating (like Admin areas) if simpler.
- [ ] Provide clear visual cues or messages for disabled actions or hidden content where appropriate (don't just make things disappear without context if it's confusing).
- [ ] Ensure loading states and error handling (especially for 403 Forbidden from backend) are in place.

### After Each Phase:
- [ ] Unit tests for new components/services pass.
- [ ] Manual testing with different roles for the implemented features.
- [ ] E2E tests for key user flows of that phase pass.

---

## 🔧 Quick Commands (Remain largely the same)

---

## 🎯 Success Metrics (Enhanced)

### Phase 0 Success:
- [ ] `AuthService` correctly determines permissions for all defined roles.
- [ ] `*appHasPermission` directive functions as expected.

### Overall Success:
- [ ] UI accurately reflects the permissions granted by a user's role(s) as defined in the `rolePermissions` matrix.
- [ ] Zero permission bypass vulnerabilities found in UI (backend is still the ultimate guard).
- [ ] Intuitive user experience: users only see and interact with what they are allowed to.

---

## 🚨 Critical Notes (Enhanced)

1.  **Backend is the Authority**: Frontend RBAC is for UX. The backend **MUST** enforce all permission checks on every API call.
2.  **Sync `rolePermissions`**: If the `rolePermissions` matrix can change, ensure the frontend version is kept in sync with the backend's definition. For many apps, this matrix is fairly static and can be a frontend constant. If it's dynamic, consider fetching it after login.
3.  **Test Thoroughly**: RBAC is critical for security. Test with all roles and combinations of permissions relevant to each feature.
4.  **Resource-Specific Permissions**: Start with general permissions. Implementing true resource-specific checks (e.g., "can edit *this specific* project because I am its manager") in `AuthService.hasPermission()` and the directive is an advanced step. Initially, rely on the backend for this fine-grained check after a general permission allows the UI element to be visible.
5.  **Clear Fallbacks**: When an action is not permitted, the UI should ideally make it clear why (if appropriate) or simply not present the option. Avoid dead buttons without explanation if possible.

---

This enhanced roadmap leverages your `rolePermissions` matrix to create a more robust, maintainable, and permission-driven UI. Good luck!
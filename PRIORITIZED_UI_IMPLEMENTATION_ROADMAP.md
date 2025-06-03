# 🚀 Prioritized UI Implementation Roadmap

## Project Management System with RBAC

### 📊 Implementation Overview

This roadmap provides a step-by-step approach to building the project management UI components, prioritizing based on:

- **Business Value**: Core functionality first
- **RBAC Complexity**: Simpler role checks before complex permissions
- **User Dependencies**: Foundation components before feature-specific ones
- **Risk Mitigation**: Essential features before nice-to-have

---

## 🎯 Phase 1: Foundation & Core Navigation (Week 1-2)

**Priority: CRITICAL** | **RBAC Complexity: LOW** | **Business Value: HIGH**

### 1.1 Enhanced Sidebar Navigation ⭐⭐⭐

**File:** `frontend-angular/src/app/layouts/main-layout/sidebar/sidebar.component.ts`
**Status:** Basic structure exists, needs RBAC integration

#### Implementation Steps:

```typescript
// 1. Inject AuthService and implement role-based navigation
interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
  permissions?: string[];
  children?: NavItem[];
}

// 2. Define role-based navigation structure
navItems: NavItem[] = [
  {
    label: 'Dashboard',
    icon: 'dashboard',
    route: '/dashboard',
    // Visible to all authenticated users
  },
  {
    label: 'Projects',
    icon: 'workspaces',
    route: '/projects',
    permissions: ['PRJ_READ']
  },
  {
    label: 'My Tasks',
    icon: 'assignment_turned_in',
    route: '/my-tasks',
    permissions: ['TASK_READ']
  },
  {
    label: 'Team Management',
    icon: 'group',
    route: '/users',
    roles: ['ROLE_ADMIN', 'ROLE_PROJECT_MANAGER'],
    permissions: ['USER_READ']
  },
  {
    label: 'Admin Panel',
    icon: 'admin_panel_settings',
    route: '/admin',
    roles: ['ROLE_ADMIN']
  }
];
```

**Testing Criteria:**

- [ ] Navigation items show/hide based on user roles
- [ ] Permissions properly filter menu options
- [ ] Responsive design works on mobile
- [ ] Visual feedback for active routes

---

### 1.2 Enhanced Dashboard Page ⭐⭐⭐

**File:** `frontend-angular/src/app/features/dashboard/dashboard-page/dashboard-page.component.html`
**Status:** Basic template exists, needs role-based content

#### Implementation Steps:

```html
<!-- Role-based dashboard sections -->
<div class="dashboard-container">
  <!-- Welcome section for all users -->
  <div class="welcome-section">
    <h1>Welcome, {{ (currentUser$ | async)?.username }}!</h1>
    <p>Role: {{ (currentUser$ | async)?.roles?.join(', ') }}</p>
  </div>

  <!-- Quick Stats Cards (role-based visibility) -->
  <div class="stats-grid">
    <!-- For Project Managers & Admins -->
    <mat-card *hasPermission="'PRJ_READ'" class="stat-card">
      <mat-card-content>
        <h3>Total Projects</h3>
        <div class="stat-number">{{ projectStats.total }}</div>
      </mat-card-content>
    </mat-card>

    <!-- For all users with task access -->
    <mat-card *hasPermission="'TASK_READ'" class="stat-card">
      <mat-card-content>
        <h3>My Tasks</h3>
        <div class="stat-number">{{ taskStats.myTasks }}</div>
      </mat-card-content>
    </mat-card>

    <!-- Admin only -->
    <mat-card *hasRole="'ROLE_ADMIN'" class="stat-card">
      <mat-card-content>
        <h3>Total Users</h3>
        <div class="stat-number">{{ userStats.total }}</div>
      </mat-card-content>
    </mat-card>
  </div>

  <!-- Quick Actions (permission-based) -->
  <div class="quick-actions">
    <button *hasPermission="'PRJ_CREATE'" mat-raised-button color="primary">
      New Project
    </button>
    <button *hasPermission="'TASK_CREATE'" mat-raised-button color="accent">
      New Task
    </button>
  </div>
</div>
```

**Testing Criteria:**

- [ ] Dashboard shows relevant data based on user role
- [ ] Quick actions appear only for authorized users
- [ ] Statistics load correctly for each role type
- [ ] Real-time updates work properly

---

## 🎯 Phase 2: Core Project Management (Week 3-4)

**Priority: HIGH** | **RBAC Complexity: MEDIUM** | **Business Value: HIGH**

### 2.1 Project List Component ⭐⭐⭐

**Path:** `frontend-angular/src/app/features/projects/project-list/`
**Status:** Directory exists, component needs creation

#### Implementation Steps:

```typescript
// 1. Create project-list.component.ts with role-based filtering
export class ProjectListComponent implements OnInit {
  projects$ = this.projectService.getProjects();
  canCreate$ = this.authService.hasPermission("PRJ_CREATE");
  canEdit$ = this.authService.hasPermission("PRJ_UPDATE");

  // 2. Implement role-based project filtering
  getVisibleProjects() {
    if (this.authService.hasRole("ROLE_ADMIN")) {
      return this.projects$; // All projects
    }
    if (this.authService.hasRole("ROLE_PROJECT_MANAGER")) {
      return this.projects$.pipe(
        map((projects) =>
          projects.filter((p) => p.managerId === this.currentUser.id)
        )
      );
    }
    // Developers see assigned projects only
    return this.projects$.pipe(
      map((projects) =>
        projects.filter((p) => p.teamMembers.includes(this.currentUser.id))
      )
    );
  }
}
```

```html
<!-- 3. Template with permission-based actions -->
<div class="project-list-container">
  <div class="list-header">
    <h2>Projects</h2>
    <button
      *hasPermission="'PRJ_CREATE'"
      mat-raised-button
      color="primary"
      (click)="openCreateDialog()"
    >
      <mat-icon>add</mat-icon>
      New Project
    </button>
  </div>

  <mat-table [dataSource]="visibleProjects$" class="projects-table">
    <!-- Standard columns -->
    <ng-container matColumnDef="name">
      <mat-header-cell *matHeaderCellDef>Name</mat-header-cell>
      <mat-cell *matCellDef="let project">{{ project.name }}</mat-cell>
    </ng-container>

    <!-- Actions column with permission checks -->
    <ng-container matColumnDef="actions">
      <mat-header-cell *matHeaderCellDef>Actions</mat-header-cell>
      <mat-cell *matCellDef="let project">
        <button *hasPermission="'PRJ_READ'; resource: project" mat-icon-button>
          <mat-icon>visibility</mat-icon>
        </button>
        <button
          *hasPermission="'PRJ_UPDATE'; resource: project"
          mat-icon-button
        >
          <mat-icon>edit</mat-icon>
        </button>
        <button
          *hasPermission="'PRJ_DELETE'; resource: project"
          mat-icon-button
          color="warn"
        >
          <mat-icon>delete</mat-icon>
        </button>
      </mat-cell>
    </ng-container>
  </mat-table>
</div>
```

**Testing Criteria:**

- [ ] Projects filtered correctly by user role
- [ ] Actions appear based on permissions
- [ ] Resource-specific permissions work
- [ ] Create/Edit dialogs respect permissions

---

### 2.2 Project Detail Component ⭐⭐

**Path:** `frontend-angular/src/app/features/projects/project-detail/`
**Status:** Needs creation

#### Implementation Features:

- **RBAC Elements:**
  - View: All members can see project details
  - Edit: Only Project Managers and Admins
  - Delete: Only Admins
  - Team Management: Only Project Managers and Admins
  - Status Changes: Project Managers and Admins

```typescript
// Key RBAC implementation points
canEditProject$ = this.authService.hasPermission("PRJ_UPDATE", this.project);
canManageTeam$ = this.authService.hasRole([
  "ROLE_ADMIN",
  "ROLE_PROJECT_MANAGER",
]);
canChangeStatus$ = this.authService.hasPermission(
  "PRJ_STATUS_CHANGE",
  this.project
);
```

---

## 🎯 Phase 3: Task Management System (Week 5-6)

**Priority: HIGH** | **RBAC Complexity: HIGH** | **Business Value: HIGH**

### 3.1 Task Board/Kanban Component ⭐⭐⭐

**Path:** `frontend-angular/src/app/features/tasks/task-board/`
**Status:** Needs creation

#### Key RBAC Features:

- **Task Visibility:** Users see only assigned tasks, PMs see project tasks, Admins see all
- **Task Creation:** Developers can create, PMs can assign to others
- **Task Status:** Assignees can update status, PMs can change any
- **Task Priority:** Only PMs and Admins can change priority

```typescript
// Complex permission logic for task operations
canCreateTask$ = this.authService.hasPermission('TASK_CREATE');
canAssignTasks$ = this.authService.hasPermission('TASK_ASSIGN');
canChangePriority$ = this.authService.hasPermission('TASK_PRIORITY_CHANGE');

// Resource-specific task permissions
canEditTask(task: Task) {
  return this.authService.hasPermission('TASK_UPDATE', task) &&
         (task.assigneeId === this.currentUser.id ||
          this.authService.hasRole(['ROLE_PROJECT_MANAGER', 'ROLE_ADMIN']));
}
```

### 3.2 My Tasks Component ⭐⭐

**Path:** `frontend-angular/src/app/features/tasks/my-tasks/`
**Status:** Needs creation

#### Features:

- Personal task dashboard
- Task filtering and sorting
- Quick status updates
- Time tracking (if required)

---

## 🎯 Phase 4: User Management (Week 7-8)

**Priority: MEDIUM** | **RBAC Complexity: HIGH** | **Business Value: MEDIUM**

### 4.1 User List Component ⭐⭐

**Path:** `frontend-angular/src/app/features/user/user-list/`
**Status:** Needs creation

#### RBAC Considerations:

- **Visibility:** Admins see all users, PMs see team members only
- **User Creation:** Only Admins
- **Role Assignment:** Only Admins
- **User Profile:** Users can edit own profile

### 4.2 User Profile Component ⭐

**Path:** `frontend-angular/src/app/features/user/user-profile/`
**Status:** Needs creation

---

## 🎯 Phase 5: Advanced Features (Week 9-10)

**Priority: LOW** | **RBAC Complexity: MEDIUM** | **Business Value: MEDIUM**

### 5.1 Comments System ⭐

**Path:** `frontend-angular/src/app/features/comments/`
**Status:** Directory exists, needs components

### 5.2 Notifications Center ⭐

**Path:** `frontend-angular/src/app/features/notifications/`
**Status:** Directory exists, needs components

### 5.3 Admin Dashboard ⭐

**Path:** `frontend-angular/src/app/features/admin/`
**Status:** Needs creation (Admin-only)

---

## 🧪 Testing Strategy by Phase

### Phase 1 Testing:

```bash
# Navigation tests
ng test --include="**/sidebar.component.spec.ts"
ng test --include="**/dashboard-page.component.spec.ts"

# RBAC directive tests
ng test --include="**/has-role.directive.spec.ts"
ng test --include="**/has-permission.directive.spec.ts"
```

### Phase 2 Testing:

```bash
# Project feature tests
ng test --include="**/projects/**/*.spec.ts"

# Permission-based component tests
ng test --include="**/project-list.component.spec.ts"
```

### End-to-End Testing:

```bash
# Role-based user journeys
npm run e2e:admin-user
npm run e2e:project-manager
npm run e2e:developer
npm run e2e:basic-user
```

---

## 📋 Implementation Checklist

### Before Starting Each Phase:

- [ ] Backend API endpoints are working
- [ ] Required permissions are defined
- [ ] Test data is available
- [ ] RBAC infrastructure is tested

### During Implementation:

- [ ] Component follows Angular best practices
- [ ] RBAC directives are properly used
- [ ] Error handling for permission failures
- [ ] Loading states for async operations
- [ ] Responsive design considerations

### After Each Phase:

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing with different roles
- [ ] Performance testing
- [ ] Accessibility testing
- [ ] Code review completed

---

## 🔧 Quick Commands

### Generate Components:

```bash
# Phase 1
ng generate component layouts/main-layout/sidebar --standalone
ng generate component features/dashboard/dashboard-page --standalone

# Phase 2
ng generate component features/projects/project-list --standalone
ng generate component features/projects/project-detail --standalone
ng generate component features/projects/project-create-edit --standalone

# Phase 3
ng generate component features/tasks/task-board --standalone
ng generate component features/tasks/my-tasks --standalone
ng generate component features/tasks/task-detail --standalone
```

### Run Development Server:

```bash
# Frontend
cd frontend-angular && npm start

# Backend services (via Docker)
docker-compose up -d
```

### Test Commands:

```bash
# Unit tests
npm test

# E2E tests
npm run e2e

# Test specific component
ng test --include="**/component-name.component.spec.ts"
```

---

## 🎯 Success Metrics

### Phase 1 Success:

- [ ] All user roles can navigate correctly
- [ ] Dashboard shows appropriate content per role
- [ ] No unauthorized access attempts

### Phase 2 Success:

- [ ] Project operations work per role permissions
- [ ] Resource-specific permissions function correctly
- [ ] Project managers can manage their projects

### Phase 3 Success:

- [ ] Task assignment workflow functions
- [ ] Status changes respect permissions
- [ ] Kanban board shows correct tasks per user

### Overall Success:

- [ ] Zero permission bypass vulnerabilities
- [ ] Intuitive user experience for each role
- [ ] Performance meets requirements
- [ ] 95%+ test coverage on RBAC logic

---

## 🚨 Critical Notes

1. **Always Test Permissions**: Each component should be tested with all 4 user roles
2. **Resource-Specific Security**: Use resource parameters in permission checks
3. **Fallback UI**: Always provide meaningful feedback when users lack permissions
4. **Performance**: Cache permission checks where possible
5. **Security**: Never rely on frontend-only permission checks

---

This roadmap ensures systematic implementation of RBAC-compliant UI components while maintaining code quality and security standards. Each phase builds upon the previous one, allowing for incremental testing and validation.

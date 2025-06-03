# RBAC-Based UI Creation Guide for Angular Project Management Application

## 📋 Table of Contents

1. [Overview](#overview)
2. [RBAC System Summary](#rbac-system-summary)
3. [UI Architecture](#ui-architecture)
4. [Role-Based Navigation](#role-based-navigation)
5. [Component Design Patterns](#component-design-patterns)
6. [Feature Module Guidelines](#feature-module-guidelines)
7. [Implementation Examples](#implementation-examples)
8. [Best Practices](#best-practices)
9. [Testing Strategies](#testing-strategies)

## 📖 Overview

This guide provides comprehensive instructions for creating a role-based user interface for the Angular Project Management application. The frontend is fully equipped with RBAC infrastructure and ready for UI implementation.

### ✅ RBAC System Status

- **Complete**: All guards, directives, and pipes implemented
- **Tested**: JWT integration and permission extraction verified
- **Ready**: 4 user roles with 22 distinct permissions mapped

## 🔐 RBAC System Summary

### User Roles & Permissions

| Role                | Permissions Count | Key Capabilities                                         |
| ------------------- | ----------------- | -------------------------------------------------------- |
| **ADMIN**           | 22                | Full system access, user management, all CRUD operations |
| **PROJECT_MANAGER** | 14                | Project lifecycle, team management, reporting            |
| **DEVELOPER**       | 9                 | Task management, code-related activities                 |
| **USER**            | 5                 | Basic read access, own profile management                |

### Permission Categories

```typescript
// Core Operations
USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE;
PRJ_READ, PRJ_CREATE, PRJ_UPDATE, PRJ_DELETE, PRJ_STATUS_CHANGE;
TASK_READ,
  TASK_CREATE,
  TASK_UPDATE,
  TASK_DELETE,
  TASK_STATUS_CHANGE,
  TASK_PRIORITY_CHANGE,
  TASK_ASSIGN;
CMT_READ, CMT_CREATE, CMT_UPDATE, CMT_DELETE;
NOTI_READ, NOTI_UPDATE, NOTI_DELETE;
USER_PROFILE_UPDATE;
```

## 🏗️ UI Architecture

### Layout Structure

```
main-layout/
├── header.component.ts/html/scss
├── sidebar.component.ts/html/scss
└── main-layout.component.ts/html/scss
```

### Current Implementation

- **Angular Material**: Modern, responsive design system
- **Lazy Loading**: Feature modules loaded on demand
- **Responsive**: Mobile-first design with mat-sidenav
- **RBAC-Ready**: All necessary guards and directives in place

## 🧭 Role-Based Navigation

### Sidebar Navigation Implementation

#### 1. Update Sidebar Component

```typescript
// layouts/main-layout/sidebar/sidebar.component.ts
import { Component, OnInit } from "@angular/core";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { AuthService } from "../../../core/services/auth.service";
import { UserRole, Permission } from "../../../models/enums.model";

interface NavItem {
  label: string;
  icon: string;
  route: string;
  requiredPermission?: Permission;
  requiredRole?: UserRole;
  children?: NavItem[];
}

@Component({
  selector: "app-sidebar",
  templateUrl: "./sidebar.component.html",
  styleUrls: ["./sidebar.component.scss"],
})
export class SidebarComponent implements OnInit {
  navItems: NavItem[] = [];
  filteredNavItems$: Observable<NavItem[]>;

  private allNavItems: NavItem[] = [
    {
      label: "Dashboard",
      icon: "dashboard",
      route: "/dashboard",
    },
    {
      label: "Projects",
      icon: "workspaces",
      route: "/projects",
      requiredPermission: Permission.PRJ_READ,
      children: [
        {
          label: "All Projects",
          icon: "list",
          route: "/projects",
          requiredPermission: Permission.PRJ_READ,
        },
        {
          label: "Create Project",
          icon: "add",
          route: "/projects/create",
          requiredPermission: Permission.PRJ_CREATE,
        },
      ],
    },
    {
      label: "Tasks",
      icon: "assignment",
      route: "/tasks",
      requiredPermission: Permission.TASK_READ,
      children: [
        {
          label: "My Tasks",
          icon: "person",
          route: "/tasks/my-tasks",
          requiredPermission: Permission.TASK_READ,
        },
        {
          label: "All Tasks",
          icon: "list",
          route: "/tasks",
          requiredPermission: Permission.TASK_READ,
        },
        {
          label: "Create Task",
          icon: "add",
          route: "/tasks/create",
          requiredPermission: Permission.TASK_CREATE,
        },
      ],
    },
    {
      label: "Users",
      icon: "people",
      route: "/users",
      requiredPermission: Permission.USER_READ,
      children: [
        {
          label: "All Users",
          icon: "list",
          route: "/users",
          requiredPermission: Permission.USER_READ,
        },
        {
          label: "Create User",
          icon: "person_add",
          route: "/users/create",
          requiredPermission: Permission.USER_CREATE,
        },
      ],
    },
    {
      label: "Notifications",
      icon: "notifications",
      route: "/notifications",
      requiredPermission: Permission.NOTI_READ,
    },
    {
      label: "Comments",
      icon: "comment",
      route: "/comments",
      requiredPermission: Permission.CMT_READ,
    },
  ];

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.filteredNavItems$ = this.authService.currentUser$.pipe(
      map((user) => this.filterNavItems(this.allNavItems, user))
    );
  }

  private filterNavItems(items: NavItem[], user: any): NavItem[] {
    if (!user) return [];

    return items.filter((item) => {
      if (
        item.requiredPermission &&
        !this.authService.hasPermission(item.requiredPermission)
      ) {
        return false;
      }
      if (item.requiredRole && user.role !== item.requiredRole) {
        return false;
      }

      // Filter children recursively
      if (item.children) {
        item.children = this.filterNavItems(item.children, user);
      }

      return true;
    });
  }
}
```

#### 2. Update Sidebar Template

```html
<!-- layouts/main-layout/sidebar/sidebar.component.html -->
<mat-nav-list>
  <ng-container *ngFor="let item of filteredNavItems$ | async">
    <!-- Main navigation item -->
    <mat-list-item
      *ngIf="!item.children; else hasChildren"
      [routerLink]="item.route"
      routerLinkActive="active-list-item"
    >
      <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
      <span matListItemTitle>{{ item.label }}</span>
    </mat-list-item>

    <!-- Navigation item with children -->
    <ng-template #hasChildren>
      <mat-expansion-panel class="nav-expansion-panel">
        <mat-expansion-panel-header>
          <mat-panel-title>
            <mat-icon>{{ item.icon }}</mat-icon>
            <span>{{ item.label }}</span>
          </mat-panel-title>
        </mat-expansion-panel-header>

        <mat-nav-list class="nested-nav">
          <mat-list-item
            *ngFor="let child of item.children"
            [routerLink]="child.route"
            routerLinkActive="active-list-item"
          >
            <mat-icon matListItemIcon>{{ child.icon }}</mat-icon>
            <span matListItemTitle>{{ child.label }}</span>
          </mat-list-item>
        </mat-nav-list>
      </mat-expansion-panel>
    </ng-template>
  </ng-container>
</mat-nav-list>
```

## 🎨 Component Design Patterns

### 1. Role-Based Dashboard Cards

```typescript
// features/dashboard/dashboard-page/dashboard-page.component.ts
interface DashboardCard {
  title: string;
  icon: string;
  description: string;
  route: string;
  color: string;
  requiredPermission?: Permission;
  requiredRole?: UserRole;
  count?: number;
}

@Component({
  selector: "app-dashboard-page",
  templateUrl: "./dashboard-page.component.html",
})
export class DashboardPageComponent implements OnInit {
  visibleCards$: Observable<DashboardCard[]>;

  private allCards: DashboardCard[] = [
    {
      title: "Projects",
      icon: "workspaces",
      description: "Manage and track project progress",
      route: "/projects",
      color: "primary",
      requiredPermission: Permission.PRJ_READ,
    },
    {
      title: "My Tasks",
      icon: "assignment_ind",
      description: "View and manage your assigned tasks",
      route: "/tasks/my-tasks",
      color: "accent",
      requiredPermission: Permission.TASK_READ,
    },
    {
      title: "All Tasks",
      icon: "assignment",
      description: "Overview of all project tasks",
      route: "/tasks",
      color: "warn",
      requiredPermission: Permission.TASK_READ,
    },
    {
      title: "Team Members",
      icon: "people",
      description: "Manage team members and roles",
      route: "/users",
      color: "primary",
      requiredPermission: Permission.USER_READ,
    },
    {
      title: "Reports",
      icon: "analytics",
      description: "Project analytics and reports",
      route: "/reports",
      color: "accent",
      requiredRole: UserRole.PROJECT_MANAGER,
    },
    {
      title: "System Settings",
      icon: "settings",
      description: "Configure system settings",
      route: "/settings",
      color: "warn",
      requiredRole: UserRole.ADMIN,
    },
  ];

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.visibleCards$ = this.authService.currentUser$.pipe(
      map((user) => this.filterCards(user))
    );
  }

  private filterCards(user: any): DashboardCard[] {
    return this.allCards.filter((card) => {
      if (
        card.requiredPermission &&
        !this.authService.hasPermission(card.requiredPermission)
      ) {
        return false;
      }
      if (card.requiredRole && user?.role !== card.requiredRole) {
        return false;
      }
      return true;
    });
  }
}
```

### 2. Dynamic Action Buttons

```typescript
// shared/components/action-buttons/action-buttons.component.ts
interface ActionButton {
  label: string;
  icon: string;
  color: string;
  action: () => void;
  requiredPermission?: Permission;
  disabled?: boolean;
}

@Component({
  selector: "app-action-buttons",
  template: `
    <div class="action-buttons">
      <button
        *ngFor="let button of visibleButtons"
        mat-raised-button
        [color]="button.color"
        [disabled]="button.disabled"
        (click)="button.action()"
      >
        <mat-icon>{{ button.icon }}</mat-icon>
        {{ button.label }}
      </button>
    </div>
  `,
})
export class ActionButtonsComponent implements OnInit {
  @Input() buttons: ActionButton[] = [];
  visibleButtons: ActionButton[] = [];

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.visibleButtons = this.buttons.filter(
      (button) =>
        !button.requiredPermission ||
        this.authService.hasPermission(button.requiredPermission)
    );
  }
}
```

### 3. Conditional Form Fields

```typescript
// shared/components/dynamic-form/dynamic-form.component.ts
interface FormField {
  name: string;
  type: "text" | "select" | "textarea" | "date";
  label: string;
  required?: boolean;
  options?: any[];
  requiredPermission?: Permission;
  readOnly?: boolean;
}

@Component({
  selector: "app-dynamic-form",
  template: `
    <form [formGroup]="formGroup">
      <mat-form-field *ngFor="let field of visibleFields" appearance="outline">
        <mat-label>{{ field.label }}</mat-label>

        <!-- Text Input -->
        <input
          *ngIf="field.type === 'text'"
          matInput
          [formControlName]="field.name"
          [readonly]="field.readOnly"
        />

        <!-- Select Dropdown -->
        <mat-select
          *ngIf="field.type === 'select'"
          [formControlName]="field.name"
        >
          <mat-option
            *ngFor="let option of field.options"
            [value]="option.value"
          >
            {{ option.label }}
          </mat-option>
        </mat-select>

        <!-- Textarea -->
        <textarea
          *ngIf="field.type === 'textarea'"
          matInput
          [formControlName]="field.name"
          [readonly]="field.readOnly"
        >
        </textarea>
      </mat-form-field>
    </form>
  `,
})
export class DynamicFormComponent implements OnInit {
  @Input() fields: FormField[] = [];
  @Input() formGroup: FormGroup;

  visibleFields: FormField[] = [];

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.visibleFields = this.fields.filter(
      (field) =>
        !field.requiredPermission ||
        this.authService.hasPermission(field.requiredPermission)
    );
  }
}
```

## 📦 Feature Module Guidelines

### 1. Projects Module

```typescript
// features/projects/projects.module.ts
@NgModule({
  declarations: [
    ProjectListComponent,
    ProjectDetailComponent,
    ProjectCreateComponent,
    ProjectEditComponent,
  ],
  imports: [CommonModule, ProjectsRoutingModule, SharedModule, MaterialModule],
})
export class ProjectsModule {}

// features/projects/projects-routing.module.ts
const routes: Routes = [
  {
    path: "",
    component: ProjectListComponent,
    canActivate: [permissionGuard],
    data: { requiredPermission: Permission.PRJ_READ },
  },
  {
    path: "create",
    component: ProjectCreateComponent,
    canActivate: [permissionGuard],
    data: { requiredPermission: Permission.PRJ_CREATE },
  },
  {
    path: ":id",
    component: ProjectDetailComponent,
    canActivate: [permissionGuard],
    data: { requiredPermission: Permission.PRJ_READ },
  },
  {
    path: ":id/edit",
    component: ProjectEditComponent,
    canActivate: [permissionGuard],
    data: { requiredPermission: Permission.PRJ_UPDATE },
  },
];
```

### 2. Project List Component Example

```typescript
// features/projects/project-list/project-list.component.ts
@Component({
  selector: "app-project-list",
  templateUrl: "./project-list.component.html",
})
export class ProjectListComponent implements OnInit {
  projects$: Observable<Project[]>;
  displayedColumns: string[] = ["name", "status", "owner", "createdAt"];

  // Permission-based columns
  get visibleColumns(): string[] {
    const columns = [...this.displayedColumns];

    if (this.authService.hasPermission(Permission.PRJ_UPDATE)) {
      columns.push("actions");
    }

    return columns;
  }

  constructor(
    private projectService: ProjectService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.projects$ = this.projectService.getProjects();
  }

  canEdit(): boolean {
    return this.authService.hasPermission(Permission.PRJ_UPDATE);
  }

  canDelete(): boolean {
    return this.authService.hasPermission(Permission.PRJ_DELETE);
  }

  canChangeStatus(): boolean {
    return this.authService.hasPermission(Permission.PRJ_STATUS_CHANGE);
  }
}
```

```html
<!-- features/projects/project-list/project-list.component.html -->
<div class="project-list-container">
  <div class="header-section">
    <h2>Projects</h2>
    <div class="header-actions">
      <button
        *hasPermission="'PRJ_CREATE'"
        mat-raised-button
        color="primary"
        routerLink="/projects/create"
      >
        <mat-icon>add</mat-icon>
        Create Project
      </button>
    </div>
  </div>

  <mat-card>
    <mat-table [dataSource]="projects$ | async" class="projects-table">
      <!-- Name Column -->
      <ng-container matColumnDef="name">
        <mat-header-cell *matHeaderCellDef>Name</mat-header-cell>
        <mat-cell *matCellDef="let project">
          <a [routerLink]="['/projects', project.id]">{{ project.name }}</a>
        </mat-cell>
      </ng-container>

      <!-- Status Column -->
      <ng-container matColumnDef="status">
        <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
        <mat-cell *matCellDef="let project">
          <mat-chip-set>
            <mat-chip [color]="getStatusColor(project.status)">
              {{ project.status }}
            </mat-chip>
          </mat-chip-set>
        </mat-cell>
      </ng-container>

      <!-- Owner Column -->
      <ng-container matColumnDef="owner">
        <mat-header-cell *matHeaderCellDef>Owner</mat-header-cell>
        <mat-cell *matCellDef="let project">{{ project.owner }}</mat-cell>
      </ng-container>

      <!-- Created Date Column -->
      <ng-container matColumnDef="createdAt">
        <mat-header-cell *matHeaderCellDef>Created</mat-header-cell>
        <mat-cell *matCellDef="let project">
          {{ project.createdAt | date:'short' }}
        </mat-cell>
      </ng-container>

      <!-- Actions Column -->
      <ng-container matColumnDef="actions">
        <mat-header-cell *matHeaderCellDef>Actions</mat-header-cell>
        <mat-cell *matCellDef="let project">
          <button
            *hasPermission="'PRJ_UPDATE'"
            mat-icon-button
            [routerLink]="['/projects', project.id, 'edit']"
            matTooltip="Edit Project"
          >
            <mat-icon>edit</mat-icon>
          </button>

          <button
            *hasPermission="'PRJ_STATUS_CHANGE'"
            mat-icon-button
            (click)="openStatusDialog(project)"
            matTooltip="Change Status"
          >
            <mat-icon>swap_horiz</mat-icon>
          </button>

          <button
            *hasPermission="'PRJ_DELETE'"
            mat-icon-button
            color="warn"
            (click)="confirmDelete(project)"
            matTooltip="Delete Project"
          >
            <mat-icon>delete</mat-icon>
          </button>
        </mat-cell>
      </ng-container>

      <mat-header-row *matHeaderRowDef="visibleColumns"></mat-header-row>
      <mat-row *matRowDef="let row; columns: visibleColumns;"></mat-row>
    </mat-table>
  </mat-card>
</div>
```

### 3. User Management Module

```typescript
// features/users/user-list/user-list.component.ts
@Component({
  selector: "app-user-list",
  templateUrl: "./user-list.component.html",
})
export class UserListComponent implements OnInit {
  users$: Observable<User[]>;
  userRole = UserRole; // Make enum available in template

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.users$ = this.userService.getUsers();
  }

  isAdmin(): boolean {
    return this.authService.hasRole(UserRole.ADMIN);
  }

  isProjectManager(): boolean {
    return this.authService.hasRole(UserRole.PROJECT_MANAGER);
  }

  canManageUser(user: User): boolean {
    const currentUser = this.authService.getCurrentUser();

    // Admins can manage all users
    if (this.isAdmin()) return true;

    // Project managers can manage developers and users
    if (this.isProjectManager()) {
      return user.role === UserRole.DEVELOPER || user.role === UserRole.USER;
    }

    // Users can only manage their own profile
    return currentUser?.id === user.id;
  }
}
```

## 🎯 Implementation Examples

### 1. Task Management with Role-Based Actions

```html
<!-- features/tasks/task-detail/task-detail.component.html -->
<mat-card class="task-detail-card">
  <mat-card-header>
    <mat-card-title>{{ task.name }}</mat-card-title>
    <mat-card-subtitle>
      Project: {{ task.projectName }} | Status: {{ task.status }}
    </mat-card-subtitle>
  </mat-card-header>

  <mat-card-content>
    <!-- Task Information -->
    <div class="task-info">
      <p><strong>Description:</strong> {{ task.description }}</p>
      <p><strong>Priority:</strong> {{ task.priority }}</p>
      <p><strong>Assigned to:</strong> {{ task.assigneeName }}</p>
      <p><strong>Due Date:</strong> {{ task.dueDate | date }}</p>
    </div>

    <!-- Status Actions -->
    <div class="status-actions" *hasPermission="'TASK_STATUS_CHANGE'">
      <h3>Change Status</h3>
      <mat-button-toggle-group
        [(value)]="task.status"
        (change)="updateStatus($event)"
      >
        <mat-button-toggle value="TODO">To Do</mat-button-toggle>
        <mat-button-toggle value="IN_PROGRESS">In Progress</mat-button-toggle>
        <mat-button-toggle value="DONE">Done</mat-button-toggle>
      </mat-button-toggle-group>
    </div>

    <!-- Assignment Section -->
    <div class="assignment-section" *hasPermission="'TASK_ASSIGN'">
      <h3>Assignment</h3>
      <mat-form-field>
        <mat-select
          [(value)]="task.assigneeId"
          (selectionChange)="updateAssignee($event)"
        >
          <mat-option *ngFor="let member of teamMembers" [value]="member.id">
            {{ member.name }}
          </mat-option>
        </mat-select>
      </mat-form-field>
    </div>

    <!-- Priority Change -->
    <div class="priority-section" *hasPermission="'TASK_PRIORITY_CHANGE'">
      <h3>Priority</h3>
      <mat-button-toggle-group
        [(value)]="task.priority"
        (change)="updatePriority($event)"
      >
        <mat-button-toggle value="LOW">Low</mat-button-toggle>
        <mat-button-toggle value="MEDIUM">Medium</mat-button-toggle>
        <mat-button-toggle value="HIGH">High</mat-button-toggle>
        <mat-button-toggle value="CRITICAL">Critical</mat-button-toggle>
      </mat-button-toggle-group>
    </div>
  </mat-card-content>

  <mat-card-actions>
    <button
      *hasPermission="'TASK_UPDATE'"
      mat-raised-button
      color="primary"
      [routerLink]="['/tasks', task.id, 'edit']"
    >
      Edit Task
    </button>

    <button
      *hasPermission="'TASK_DELETE'"
      mat-raised-button
      color="warn"
      (click)="deleteTask()"
    >
      Delete Task
    </button>
  </mat-card-actions>
</mat-card>

<!-- Comments Section -->
<mat-card class="comments-section" *hasPermission="'CMT_READ'">
  <mat-card-header>
    <mat-card-title>Comments</mat-card-title>
  </mat-card-header>

  <mat-card-content>
    <!-- Add Comment Form -->
    <div class="add-comment" *hasPermission="'CMT_CREATE'">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Add a comment</mat-label>
        <textarea matInput [(ngModel)]="newComment" rows="3"></textarea>
      </mat-form-field>
      <button mat-raised-button color="primary" (click)="addComment()">
        Add Comment
      </button>
    </div>

    <!-- Comments List -->
    <div class="comments-list">
      <div *ngFor="let comment of comments" class="comment-item">
        <div class="comment-header">
          <strong>{{ comment.authorName }}</strong>
          <span class="comment-date"
            >{{ comment.createdAt | date:'short' }}</span
          >
        </div>
        <p class="comment-content">{{ comment.content }}</p>

        <!-- Comment Actions -->
        <div class="comment-actions">
          <button
            *hasPermission="'CMT_UPDATE'"
            mat-icon-button
            (click)="editComment(comment)"
            [disabled]="!canEditComment(comment)"
          >
            <mat-icon>edit</mat-icon>
          </button>

          <button
            *hasPermission="'CMT_DELETE'"
            mat-icon-button
            color="warn"
            (click)="deleteComment(comment)"
            [disabled]="!canDeleteComment(comment)"
          >
            <mat-icon>delete</mat-icon>
          </button>
        </div>
      </div>
    </div>
  </mat-card-content>
</mat-card>
```

### 2. Admin Panel with System Management

```html
<!-- features/admin/admin-dashboard/admin-dashboard.component.html -->
<div class="admin-dashboard" *hasRole="'ADMIN'">
  <h1>System Administration</h1>

  <div class="admin-grid">
    <!-- User Management -->
    <mat-card class="admin-card">
      <mat-card-header>
        <mat-card-title>User Management</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p>Total Users: {{ userStats.total }}</p>
        <p>Active: {{ userStats.active }}</p>
        <p>Pending: {{ userStats.pending }}</p>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button routerLink="/admin/users">
          Manage Users
        </button>
      </mat-card-actions>
    </mat-card>

    <!-- System Statistics -->
    <mat-card class="admin-card">
      <mat-card-header>
        <mat-card-title>System Statistics</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p>Projects: {{ systemStats.projects }}</p>
        <p>Tasks: {{ systemStats.tasks }}</p>
        <p>Comments: {{ systemStats.comments }}</p>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button routerLink="/admin/reports">
          View Reports
        </button>
      </mat-card-actions>
    </mat-card>

    <!-- System Configuration -->
    <mat-card class="admin-card">
      <mat-card-header>
        <mat-card-title>System Configuration</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p>Configure system-wide settings and preferences</p>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button routerLink="/admin/settings">
          System Settings
        </button>
      </mat-card-actions>
    </mat-card>
  </div>
</div>
```

### 3. Project Manager Dashboard

```html
<!-- features/manager/manager-dashboard/manager-dashboard.component.html -->
<div class="manager-dashboard" *hasRole="'PROJECT_MANAGER'">
  <h1>Project Manager Dashboard</h1>

  <div class="manager-grid">
    <!-- Project Overview -->
    <mat-card class="manager-card">
      <mat-card-header>
        <mat-card-title>My Projects</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div *ngFor="let project of myProjects" class="project-summary">
          <h4>{{ project.name }}</h4>
          <mat-progress-bar [value]="project.progress"></mat-progress-bar>
          <p>
            {{ project.tasksCompleted }}/{{ project.totalTasks }} tasks
            completed
          </p>
        </div>
      </mat-card-content>
    </mat-card>

    <!-- Team Performance -->
    <mat-card class="manager-card">
      <mat-card-header>
        <mat-card-title>Team Performance</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div class="team-stats">
          <p>Active Team Members: {{ teamStats.active }}</p>
          <p>Tasks Completed This Week: {{ teamStats.weeklyTasks }}</p>
          <p>Average Task Completion: {{ teamStats.avgCompletion }}%</p>
        </div>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button routerLink="/manager/team">
          Manage Team
        </button>
      </mat-card-actions>
    </mat-card>

    <!-- Quick Actions -->
    <mat-card class="manager-card">
      <mat-card-header>
        <mat-card-title>Quick Actions</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div class="quick-actions">
          <button mat-stroked-button routerLink="/projects/create">
            <mat-icon>add</mat-icon>
            New Project
          </button>

          <button mat-stroked-button routerLink="/tasks/assign">
            <mat-icon>assignment_ind</mat-icon>
            Assign Tasks
          </button>

          <button mat-stroked-button routerLink="/reports">
            <mat-icon>analytics</mat-icon>
            Generate Report
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  </div>
</div>
```

## ✅ Best Practices

### 1. Directive Usage

```html
<!-- Use HasPermissionDirective for simple permission checks -->
<button *hasPermission="'PRJ_CREATE'" mat-raised-button>Create Project</button>

<!-- Use HasRoleDirective for role-based visibility -->
<div *hasRole="'ADMIN'" class="admin-panel">Admin Tools</div>

<!-- Combine multiple conditions -->
<div *ngIf="(hasPermission('PRJ_UPDATE') | async) && isOwner">
  Owner-specific actions
</div>
```

### 2. Guard Implementation

```typescript
// Always protect routes with appropriate guards
{
  path: 'admin',
  loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
  canActivate: [roleGuard],
  data: { requiredRole: UserRole.ADMIN }
}
```

### 3. Error Handling

```typescript
// Handle permission errors gracefully
@Component({
  template: `
    <div *ngIf="hasAccess; else noAccess">
      <!-- Main content -->
    </div>

    <ng-template #noAccess>
      <mat-card class="access-denied">
        <mat-card-content>
          <mat-icon color="warn">block</mat-icon>
          <p>You don't have permission to access this resource.</p>
        </mat-card-content>
      </mat-card>
    </ng-template>
  `,
})
export class SecureComponent {
  hasAccess: boolean;

  ngOnInit() {
    this.hasAccess = this.authService.hasPermission(
      Permission.REQUIRED_PERMISSION
    );
  }
}
```

### 4. Loading States

```typescript
// Show loading states while checking permissions
@Component({
  template: `
    <div *ngIf="loading" class="loading">
      <mat-spinner></mat-spinner>
      <p>Checking permissions...</p>
    </div>

    <div *ngIf="!loading && hasAccess">
      <!-- Main content -->
    </div>

    <div *ngIf="!loading && !hasAccess">
      <!-- Access denied message -->
    </div>
  `,
})
export class PermissionAwareComponent {
  loading = true;
  hasAccess = false;

  ngOnInit() {
    this.authService.currentUser$
      .pipe(
        take(1),
        finalize(() => (this.loading = false))
      )
      .subscribe((user) => {
        this.hasAccess = this.checkPermissions(user);
      });
  }
}
```

## 🧪 Testing Strategies

### 1. Component Testing with RBAC

```typescript
describe("ProjectListComponent", () => {
  let component: ProjectListComponent;
  let fixture: ComponentFixture<ProjectListComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj("AuthService", [
      "hasPermission",
      "hasRole",
    ]);

    await TestBed.configureTestingModule({
      declarations: [ProjectListComponent],
      providers: [{ provide: AuthService, useValue: authSpy }],
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it("should show create button for users with PRJ_CREATE permission", () => {
    authService.hasPermission.and.returnValue(true);

    fixture.detectChanges();

    const createButton = fixture.debugElement.query(
      By.css('[data-test="create-project-btn"]')
    );
    expect(createButton).toBeTruthy();
  });

  it("should hide create button for users without PRJ_CREATE permission", () => {
    authService.hasPermission.and.returnValue(false);

    fixture.detectChanges();

    const createButton = fixture.debugElement.query(
      By.css('[data-test="create-project-btn"]')
    );
    expect(createButton).toBeFalsy();
  });
});
```

### 2. E2E Testing with Different Roles

```typescript
describe("Project Management E2E", () => {
  it("should allow admin to access all features", () => {
    cy.loginAs("admin");
    cy.visit("/projects");

    cy.get('[data-test="create-project-btn"]').should("be.visible");
    cy.get('[data-test="edit-project-btn"]').should("be.visible");
    cy.get('[data-test="delete-project-btn"]').should("be.visible");
  });

  it("should restrict developer access to appropriate features", () => {
    cy.loginAs("developer");
    cy.visit("/projects");

    cy.get('[data-test="create-project-btn"]').should("not.exist");
    cy.get('[data-test="edit-project-btn"]').should("not.exist");
    cy.get('[data-test="delete-project-btn"]').should("not.exist");
  });
});
```

## 📚 Additional Resources

### Style Guidelines

- Use Angular Material Design principles
- Implement consistent spacing and typography
- Apply role-based color coding where appropriate
- Ensure responsive design for mobile devices

### Performance Considerations

- Lazy load feature modules based on user permissions
- Cache permission checks to avoid repeated API calls
- Use OnPush change detection strategy where possible
- Implement virtual scrolling for large data sets

### Accessibility

- Ensure proper ARIA labels for role-based content
- Provide keyboard navigation for all interactive elements
- Use semantic HTML elements appropriately
- Test with screen readers for permission-based content

---

## 🎉 Conclusion

This guide provides a comprehensive foundation for building a sophisticated, role-based user interface for the Angular Project Management application. The RBAC system is fully implemented and tested, providing a secure and scalable foundation for feature development.

### Next Steps:

1. Implement the sidebar navigation updates
2. Create role-specific dashboard components
3. Build feature modules following the provided patterns
4. Add comprehensive error handling and loading states
5. Implement thorough testing for all permission scenarios

The application is ready for full UI development with complete RBAC integration! 🚀

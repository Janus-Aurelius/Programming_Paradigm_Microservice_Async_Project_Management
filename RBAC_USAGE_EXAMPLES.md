# RBAC Implementation Examples

This file provides practical examples of how to use the newly implemented RBAC features in your Angular application.

## Route Protection Examples

### Basic Route Configuration (app.routes.ts)

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard, adminGuard, managerGuard } from './core/guards/role.guard';
import { permissionGuard, canCreateProjectGuard, canUpdateProjectGuard } from './core/guards/permission.guard';
import { resourceGuard, canEditProjectGuard, canDeleteProjectGuard } from './core/guards/resource.guard';
import { UserRole } from './models/enums.model';
import { Action } from './models/enums.model';

export const routes: Routes = [
  // Public routes
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/register', component: RegisterComponent },
  
  // Protected routes requiring authentication only
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  
  // Role-based protection
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.routes)
  },
  
  {
    path: 'projects/create',
    component: CreateProjectComponent,
    canActivate: [authGuard, canCreateProjectGuard]
  },
  
  // Resource-specific protection
  {
    path: 'projects/:id/edit',
    component: EditProjectComponent,
    canActivate: [authGuard, canEditProjectGuard()]
  },
  
  {
    path: 'projects/:projectId/settings',
    component: ProjectSettingsComponent,
    canActivate: [authGuard, canEditProjectGuard('projectId')]
  },
  
  // Combined protection (role + permission)
  {
    path: 'projects/:id/members',
    component: ProjectMembersComponent,
    canActivate: [
      authGuard, 
      roleGuard([UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER]),
      resourceGuard('project', Action.PRJ_MANAGE_MEMBERS)
    ]
  },
  
  // Multiple permission requirements
  {
    path: 'users',
    component: UserManagementComponent,
    canActivate: [
      authGuard,
      permissionGuard([Action.USER_READ, Action.USER_CREATE, Action.USER_UPDATE])
    ]
  }
];
```

## Template Usage Examples

### Using Directives

```html
<!-- project-detail.component.html -->
<div class="project-detail">
  <h1>{{ project.name }}</h1>
  
  <!-- Role-based visibility -->
  <button *appHasRole="UserRole.ROLE_ADMIN" 
          class="btn btn-danger" 
          (click)="deleteProject()">
    Delete Project
  </button>
  
  <!-- Multiple roles (any) -->
  <div *appHasRole="[UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER]" 
       class="management-panel">
    <h3>Project Management</h3>
    <!-- Management controls -->
  </div>
  
  <!-- Permission-based visibility -->
  <button *appHasPermission="Action.PRJ_UPDATE" 
          class="btn btn-primary" 
          (click)="editProject()">
    Edit Project
  </button>
  
  <!-- Resource-specific permission -->
  <button *appHasPermission="Action.PRJ_UPDATE" 
          [resourceId]="project.id" 
          resourceType="project"
          class="btn btn-warning" 
          (click)="editProjectDetails()">
    Edit This Project
  </button>
  
  <!-- Multiple permissions (all required) -->
  <div *appHasPermission="[Action.PRJ_UPDATE, Action.PRJ_DELETE]" 
       [requireAll]="true" 
       class="advanced-controls">
    Advanced Project Controls
  </div>
  
  <!-- Multiple permissions (any) -->
  <div *appHasPermission="[Action.PRJ_READ, Action.PRJ_UPDATE]" 
       [requireAll]="false" 
       class="project-info">
    Project Information Panel
  </div>
</div>

<!-- task-list.component.html -->
<div class="task-list">
  <div *ngFor="let task of tasks" class="task-item">
    <h4>{{ task.title }}</h4>
    
    <!-- Task-specific permissions -->
    <div class="task-actions">
      <button *appHasPermission="Action.TASK_UPDATE" 
              [resourceId]="task.id" 
              resourceType="task"
              (click)="editTask(task)">
        Edit
      </button>
      
      <button *appHasPermission="Action.TASK_DELETE" 
              [resourceId]="task.id" 
              resourceType="task"
              class="btn-danger"
              (click)="deleteTask(task)">
        Delete
      </button>
      
      <button *appHasPermission="Action.TASK_ASSIGN" 
              [resourceId]="task.id" 
              resourceType="task"
              (click)="assignTask(task)">
        Assign
      </button>
    </div>
  </div>
</div>
```

### Using Pipes

```html
<!-- user-profile.component.html -->
<div class="user-profile">
  <!-- Simple role check -->
  <div *ngIf="UserRole.ROLE_ADMIN | hasRole | async" class="admin-badge">
    Administrator
  </div>
  
  <!-- Multiple roles -->
  <div *ngIf="[UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER] | hasRole | async" 
       class="manager-section">
    Management Tools
  </div>
  
  <!-- Permission checks -->
  <button *ngIf="Action.USER_UPDATE | hasPermission | async" 
          (click)="editProfile()">
    Edit Profile
  </button>
  
  <!-- Resource-specific permission with pipe -->
  <button *ngIf="project.id | canAccessProject:Action.PRJ_UPDATE | async" 
          (click)="editProject()">
    Edit Project
  </button>
  
  <!-- Multiple permissions (all required) -->
  <div *ngIf="[Action.USER_CREATE, Action.USER_DELETE] | hasPermission:true | async" 
       class="user-management">
    User Management Panel
  </div>
</div>
```

## Component Usage Examples

### In TypeScript Components

```typescript
// project.component.ts
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { UserRole, Action } from '../models/enums.model';

@Component({
  selector: 'app-project',
  templateUrl: './project.component.html'
})
export class ProjectComponent implements OnInit {
  // Expose enums to template
  readonly UserRole = UserRole;
  readonly Action = Action;
  
  // Permission observables
  canCreateProject$ = this.authService.hasPermission(Action.PRJ_CREATE);
  canUpdateProject$ = this.authService.hasPermission(Action.PRJ_UPDATE);
  canDeleteProject$ = this.authService.hasPermission(Action.PRJ_DELETE);
  
  // Role observables
  isAdmin$ = this.authService.hasRole(UserRole.ROLE_ADMIN);
  isManager$ = this.authService.hasAnyRole([UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER]);
  
  // Resource-specific permissions
  canEditThisProject$!: Observable<boolean>;
  canDeleteThisProject$!: Observable<boolean>;
  
  constructor(private authService: AuthService) {}
  
  ngOnInit(): void {
    // Initialize resource-specific permissions
    if (this.projectId) {
      this.canEditThisProject$ = this.authService.canAccessProject(this.projectId, Action.PRJ_UPDATE);
      this.canDeleteThisProject$ = this.authService.canAccessProject(this.projectId, Action.PRJ_DELETE);
    }
  }
  
  // Method-level permission checks
  async editProject(): Promise<void> {
    const canEdit = await this.authService.hasPermission(Action.PRJ_UPDATE).toPromise();
    if (!canEdit) {
      alert('You do not have permission to edit projects');
      return;
    }
    
    // Resource-specific check
    const canEditThis = await this.authService.canAccessProject(this.projectId, Action.PRJ_UPDATE).toPromise();
    if (!canEditThis) {
      alert('You do not have permission to edit this specific project');
      return;
    }
    
    // Proceed with edit
    this.router.navigate(['/projects', this.projectId, 'edit']);
  }
  
  // Combined permission and role checks
  showAdvancedFeatures(): Observable<boolean> {
    return this.authService.hasAllPermissions([
      Action.PRJ_UPDATE, 
      Action.PRJ_DELETE, 
      Action.PRJ_MANAGE_MEMBERS
    ]);
  }
}
```

### Service Usage in Components

```typescript
// task-management.component.ts
export class TaskManagementComponent implements OnInit {
  tasks: Task[] = [];
  
  constructor(
    private authService: AuthService,
    private taskService: TaskService
  ) {}
  
  ngOnInit(): void {
    this.loadTasks();
  }
  
  async loadTasks(): Promise<void> {
    // Check if user can read tasks
    const canRead = await this.authService.hasPermission(Action.TASK_READ).toPromise();
    if (!canRead) {
      this.router.navigate(['/forbidden']);
      return;
    }
    
    this.tasks = await this.taskService.getTasks().toPromise();
  }
  
  async deleteTask(taskId: number): Promise<void> {
    // Check resource-specific delete permission
    const canDelete = await this.authService.canAccessTask(taskId, Action.TASK_DELETE).toPromise();
    if (!canDelete) {
      alert('You cannot delete this task');
      return;
    }
    
    // Proceed with deletion
    await this.taskService.deleteTask(taskId).toPromise();
    this.loadTasks(); // Refresh list
  }
  
  // Get filtered tasks based on permissions
  getEditableTasks(): Observable<Task[]> {
    return this.authService.hasPermission(Action.TASK_UPDATE).pipe(
      switchMap(canUpdate => {
        if (canUpdate) {
          return this.taskService.getTasks();
        } else {
          // Return only tasks user can read
          return this.taskService.getReadOnlyTasks();
        }
      })
    );
  }
}
```

## Navigation Menu Example

```html
<!-- navigation.component.html -->
<nav class="main-navigation">
  <ul class="nav-menu">
    <!-- Always visible for authenticated users -->
    <li><a routerLink="/dashboard">Dashboard</a></li>
    
    <!-- Role-based navigation -->
    <li *appHasRole="[UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER]">
      <a routerLink="/projects">Projects</a>
    </li>
    
    <!-- Permission-based navigation -->
    <li *appHasPermission="Action.USER_READ">
      <a routerLink="/users">Users</a>
    </li>
    
    <!-- Admin-only section -->
    <li *appHasRole="UserRole.ROLE_ADMIN">
      <a routerLink="/admin">Administration</a>
      <ul class="submenu">
        <li *appHasPermission="Action.USER_CREATE">
          <a routerLink="/admin/users/create">Create User</a>
        </li>
        <li *appHasPermission="[Action.PRJ_CREATE, Action.PRJ_MANAGE_MEMBERS]" [requireAll]="false">
          <a routerLink="/admin/projects">Manage Projects</a>
        </li>
      </ul>
    </li>
    
    <!-- Complex permission example -->
    <li *appHasPermission="[Action.TASK_CREATE, Action.TASK_ASSIGN]" [requireAll]="true">
      <a routerLink="/tasks/create">Create & Assign Tasks</a>
    </li>
  </ul>
</nav>
```

## Testing Your RBAC Implementation

### Unit Test Example

```typescript
// auth.service.spec.ts
describe('AuthService RBAC', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService, StorageService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  it('should extract permissions from token correctly', () => {
    const mockToken = 'mock.jwt.token';
    const mockUser = { id: '1', email: 'test@test.com', role: UserRole.ROLE_ADMIN };
    
    service.login({ email: 'test@test.com', password: 'password' }).subscribe();
    
    const req = httpMock.expectOne('/api/users/auth/login');
    req.flush({ token: mockToken, user: mockUser });
    
    service.hasPermission(Action.USER_CREATE).subscribe(hasPermission => {
      expect(hasPermission).toBeTruthy();
    });
  });
  
  it('should check resource-specific permissions', () => {
    service.canAccessProject(1, Action.PRJ_UPDATE).subscribe();
    
    const req = httpMock.expectOne('/api/projects/1/permissions/check?action=PRJ_UPDATE');
    expect(req.request.method).toBe('GET');
    req.flush(true);
  });
});
```

This implementation provides a complete RBAC system that integrates seamlessly with your existing backend architecture. The guards, directives, and pipes work together to provide both route-level and UI-level authorization control.

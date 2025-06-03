# Role-Based Access Control Implementation Plan

## CRITICAL ISSUES FOUND

### 1. Backend Security Issues
- Permission evaluators return `true` (bypass all security)
- No role-based method security in controllers
- Custom permissions instead of standard role checks

### 2. Frontend Security Issues  
- Empty HasRoleDirective implementation
- No role guards for routes
- Missing role-based UI component visibility

## REQUIRED FIXES

### Backend Fixes (Priority: CRITICAL)

#### 1. Implement Real Permission Evaluators
Replace placeholder `return true` with actual role-based logic:

```java
// CommentPermissionEvaluator.java
@Override
public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    String userRole = authentication.getAuthorities().stream()
        .findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_USER");
    
    return switch (permission.toString()) {
        case "CREATE", "EDIT", "DELETE" -> 
            userRole.equals("ROLE_ADMIN") || userRole.equals("ROLE_PROJECT_MANAGER");
        case "VIEW" -> true; // All authenticated users can view
        default -> false;
    };
}
```

#### 2. Add Role-Based Controller Security
```java
// Example for admin-only endpoints
@PreAuthorize("hasRole('ROLE_ADMIN')")
@PostMapping("/admin/users")
public Mono<UserDto> createUserAsAdmin(@RequestBody UserDto userDto) { ... }

// Example for project managers and admins
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROJECT_MANAGER')")
@PostMapping("/projects")
public Mono<ProjectDto> createProject(@RequestBody ProjectDto projectDto) { ... }
```

#### 3. Service-Level Role Mapping
```java
// UserIdHeaderWebFilter.java - Fix authority creation
UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
    userId, null, 
    role != null ? Collections.singletonList(new SimpleGrantedAuthority(role)) : 
                  Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
);
```

### Frontend Fixes (Priority: HIGH)

#### 1. Implement HasRoleDirective
```typescript
@Directive({
  selector: '[appHasRole]'
})
export class HasRoleDirective implements OnInit, OnDestroy {
  @Input() appHasRole: UserRole | UserRole[];
  
  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}
  
  ngOnInit() {
    this.authService.hasAnyRole(Array.isArray(this.appHasRole) ? this.appHasRole : [this.appHasRole])
      .subscribe(hasRole => {
        if (hasRole) {
          this.viewContainer.createEmbeddedView(this.templateRef);
        } else {
          this.viewContainer.clear();
        }
      });
  }
}
```

#### 2. Create Role Guards
```typescript
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}
  
  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const requiredRoles = route.data['roles'] as UserRole[];
    
    return this.authService.hasAnyRole(requiredRoles).pipe(
      tap(hasRole => {
        if (!hasRole) {
          this.router.navigate(['/access-denied']);
        }
      })
    );
  }
}
```

#### 3. Apply Guards to Routes
```typescript
{
  path: 'admin',
  component: AdminComponent,
  canActivate: [RoleGuard],
  data: { roles: [UserRole.ROLE_ADMIN] }
},
{
  path: 'projects/manage',
  component: ProjectManagementComponent,
  canActivate: [RoleGuard],
  data: { roles: [UserRole.ROLE_ADMIN, UserRole.ROLE_PROJECT_MANAGER] }
}
```

## RECOMMENDED ROLE HIERARCHY

```
ROLE_ADMIN (Full system access)
├── Can manage users, system settings
├── Can create/edit/delete any project/task
└── Can access all administrative functions

ROLE_PROJECT_MANAGER (Project management)
├── Can create/edit projects they manage
├── Can assign tasks to team members
├── Can view project reports and analytics
└── Cannot manage system users

ROLE_DEVELOPER (Task execution)
├── Can view assigned projects/tasks
├── Can edit tasks assigned to them
├── Can comment on projects/tasks
└── Cannot create projects or manage users

ROLE_USER (Basic access)
├── Can view projects they're members of
├── Can comment on accessible content
└── Cannot create projects or tasks
```

## TESTING PLAN

1. **Unit Tests**: Test each permission evaluator with different roles
2. **Integration Tests**: Test controller endpoints with different user roles
3. **Frontend Tests**: Test role directive and guards with different role combinations
4. **End-to-End Tests**: Test complete user workflows by role

## IMPLEMENTATION PRIORITY

1. **CRITICAL**: Fix permission evaluators (security bypass)
2. **HIGH**: Implement frontend role guards and directive
3. **MEDIUM**: Add comprehensive role-based controller annotations
4. **LOW**: Enhance gateway-level role routing

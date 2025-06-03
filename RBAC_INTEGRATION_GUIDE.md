# Comprehensive RBAC Integration Guide: Backend to Frontend

## Table of Contents
1. [System Overview](#system-overview)
2. [Backend RBAC Architecture](#backend-rbac-architecture)
3. [JWT Authentication Flow](#jwt-authentication-flow)
4. [Permission System](#permission-system)
5. [API Gateway Security](#api-gateway-security)
6. [Frontend Integration](#frontend-integration)
7. [Authentication Service Implementation](#authentication-service-implementation)
8. [Route Guards and Authorization](#route-guards-and-authorization)
9. [Role-Based UI Components](#role-based-ui-components)
10. [Best Practices](#best-practices)
11. [Security Considerations](#security-considerations)
12. [Testing Strategies](#testing-strategies)

## System Overview

This project implements a comprehensive Role-Based Access Control (RBAC) system across a microservices architecture with Angular frontend integration. The system provides:

- **JWT-based authentication** with role claims
- **Hierarchical role system** with inherited permissions
- **Resource-level authorization** with ownership and membership checks
- **API Gateway filtering** for centralized security
- **Frontend integration** with Angular services and guards

### Architecture Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Angular App   │    │   API Gateway   │    │  Microservices  │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │Auth Service │ │◄──►│ │JWT Filter   │ │◄──►│ │Security     │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │Config       │ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ └─────────────┘ │
│ │Route Guards │ │    │ │CORS Config  │ │    │ ┌─────────────┐ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │Permission   │ │
│ ┌─────────────┐ │    │                 │    │ │Evaluators   │ │
│ │Interceptors │ │    │                 │    │ └─────────────┘ │
│ └─────────────┘ │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Backend RBAC Architecture

### Role Hierarchy

The system implements a hierarchical role structure where higher-level roles inherit permissions from lower-level roles:

```yaml
ROLE_USER → ROLE_DEVELOPER → ROLE_PROJECT_MANAGER → ROLE_ADMIN
```

**Role Definitions:**
- `ROLE_USER`: Basic authenticated user
- `ROLE_DEVELOPER`: Can manage tasks and comments
- `ROLE_PROJECT_MANAGER`: Can manage projects and team members
- `ROLE_ADMIN`: Full system access

### Permission Categories

#### Project Permissions
```yaml
PRJ_CREATE: Create new projects
PRJ_READ: View project details
PRJ_UPDATE: Modify project information
PRJ_DELETE: Remove projects
PRJ_MANAGE_MEMBERS: Add/remove project members
```

#### Task Permissions
```yaml
TASK_CREATE: Create new tasks
TASK_READ: View task details
TASK_UPDATE: Modify task information
TASK_DELETE: Remove tasks
TASK_ASSIGN: Assign tasks to users
```

#### Comment Permissions
```yaml
CMT_CREATE: Add comments
CMT_READ: View comments
CMT_UPDATE: Edit comments
CMT_DELETE: Remove comments
```

#### User Management Permissions
```yaml
USER_READ: View user profiles
USER_UPDATE: Modify user information
USER_DELETE: Remove users
USER_MANAGE_ROLES: Assign/modify user roles
```

#### Notification Permissions
```yaml
NOTI_READ: View notifications
NOTI_UPDATE: Mark notifications as read
NOTI_DELETE: Remove notifications
```

### Service-Level Security Implementation

#### API Gateway JWT Filter
```java
// JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        
        if (token != null && jwtUtil.validateToken(token)) {
            String userId = jwtUtil.extractUserId(token);
            List<String> roles = jwtUtil.extractRoles(token);
            
            // Add user context headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-ID", userId)
                .header("X-User-Roles", String.join(",", roles))
                .build();
                
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        
        return handleUnauthorized(exchange);
    }
}
```

#### Service Method Security
```java
// ProjectController.java
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasRole('ROLE_USER')")
public class ProjectController {
    
    @PostMapping
    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
        // Implementation
    }
    
    @PutMapping("/{projectId}")
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'PRJ_UPDATE')")
    public ResponseEntity<ProjectDto> updateProject(
        @PathVariable Long projectId, 
        @RequestBody UpdateProjectRequest request) {
        // Implementation
    }
}
```

#### Custom Permission Evaluator
```java
// ProjectPermissionEvaluator.java
@Component
public class ProjectPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        String userId = getCurrentUserId();
        String permissionName = (String) permission;
        
        // Check role-based permissions
        if (hasRolePermission(auth, permissionName)) {
            return true;
        }
        
        // Check resource-specific permissions
        if (targetDomainObject instanceof Long) {
            Long projectId = (Long) targetDomainObject;
            return hasProjectAccess(userId, projectId, permissionName);
        }
        
        return false;
    }
    
    private boolean hasProjectAccess(String userId, Long projectId, String permission) {
        // Check if user is project owner or member
        // Implement ownership and membership logic
    }
}
```

## JWT Authentication Flow

### Token Generation (User Service)
```java
// JwtUtil.java
@Component
public class JwtUtil {
    
    public String generateToken(UserDto user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("roles", List.of(user.getRole().name()));
        claims.put("permissions", getPermissionsForRole(user.getRole()));
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
            .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
            .compact();
    }
    
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("roles");
    }
    
    public List<String> extractPermissions(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("permissions");
    }
}
```

### Token Validation Flow
```
1. Client sends request with JWT token
2. API Gateway validates token signature and expiration
3. Gateway extracts user ID and roles from token
4. Gateway forwards request with X-User-ID and X-User-Roles headers
5. Downstream services use headers for authorization decisions
6. Services perform resource-level permission checks
```

## Permission System

### YAML Configuration
```yaml
# permissions.yml
roles:
  ROLE_USER:
    permissions:
      - USER_READ
      - CMT_CREATE
      - CMT_READ
      - TASK_READ
      - PRJ_READ
      - NOTI_READ

  ROLE_DEVELOPER:
    inherits: ROLE_USER
    permissions:
      - TASK_CREATE
      - TASK_UPDATE
      - TASK_DELETE
      - CMT_UPDATE
      - CMT_DELETE

  ROLE_PROJECT_MANAGER:
    inherits: ROLE_DEVELOPER
    permissions:
      - PRJ_CREATE
      - PRJ_UPDATE
      - PRJ_DELETE
      - PRJ_MANAGE_MEMBERS
      - TASK_ASSIGN
      - USER_READ

  ROLE_ADMIN:
    inherits: ROLE_PROJECT_MANAGER
    permissions:
      - USER_UPDATE
      - USER_DELETE
      - USER_MANAGE_ROLES
      - NOTI_UPDATE
      - NOTI_DELETE
```

### Permission Loading Service
```java
// PermissionService.java
@Service
public class PermissionService {
    
    private Map<String, Set<String>> rolePermissions;
    
    @PostConstruct
    public void loadPermissions() {
        // Load permissions from YAML configuration
        this.rolePermissions = loadRolePermissionsFromYaml();
    }
    
    public Set<String> getPermissionsForRole(String role) {
        Set<String> permissions = new HashSet<>();
        collectPermissions(role, permissions);
        return permissions;
    }
    
    private void collectPermissions(String role, Set<String> permissions) {
        if (rolePermissions.containsKey(role)) {
            permissions.addAll(rolePermissions.get(role));
            // Handle inheritance
            String parentRole = getParentRole(role);
            if (parentRole != null) {
                collectPermissions(parentRole, permissions);
            }
        }
    }
}
```

## API Gateway Security

### Security Configuration
```java
// SecurityConfig.java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/users/auth/**").permitAll()
                .pathMatchers("/api/users").permitAll() // Registration
                .anyExchange().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Route-Based Security
```java
// RouteConfig.java
@Configuration
public class RouteConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r.path("/api/users/**")
                .filters(f -> f.filter(jwtAuthenticationFilter()))
                .uri("http://localhost:8081"))
            .route("project-service", r -> r.path("/api/projects/**")
                .filters(f -> f.filter(jwtAuthenticationFilter()))
                .uri("http://localhost:8082"))
            .build();
    }
}
```

## Frontend Integration

### Angular Authentication Service

The current Angular implementation provides a solid foundation for RBAC integration:

```typescript
// auth.service.ts (Enhanced)
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly JWT_KEY = 'jwt_token';
  private readonly USER_KEY = 'current_user';
  private readonly PERMISSIONS_KEY = 'user_permissions';

  isAuthenticated$ = new BehaviorSubject<boolean>(false);
  currentUser$ = new BehaviorSubject<UserDto | null>(null);
  userRoles$ = new BehaviorSubject<UserRole[]>([]);
  userPermissions$ = new BehaviorSubject<string[]>([]);

  constructor(private http: HttpClient, private storage: StorageService) {
    this.initializeAuthState();
  }

  private initializeAuthState(): void {
    const token = this.getToken();
    if (token && !this.isTokenExpired(token)) {
      const decodedToken = this.decodeToken(token);
      this.setAuthState(decodedToken);
    } else {
      this.clearAuthState();
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/users/auth/login', credentials).pipe(
      tap(response => {
        this.storage.setItem(this.JWT_KEY, response.token);
        const decodedToken = this.decodeToken(response.token);
        this.setAuthState(decodedToken);
      })
    );
  }

  private setAuthState(decodedToken: any): void {
    const user = {
      id: decodedToken.userId,
      email: decodedToken.sub,
      role: decodedToken.roles[0]
    };
    
    this.storage.setItem(this.USER_KEY, user);
    this.storage.setItem(this.PERMISSIONS_KEY, decodedToken.permissions || []);
    
    this.isAuthenticated$.next(true);
    this.currentUser$.next(user);
    this.userRoles$.next(decodedToken.roles || []);
    this.userPermissions$.next(decodedToken.permissions || []);
  }

  private clearAuthState(): void {
    this.storage.removeItem(this.JWT_KEY);
    this.storage.removeItem(this.USER_KEY);
    this.storage.removeItem(this.PERMISSIONS_KEY);
    
    this.isAuthenticated$.next(false);
    this.currentUser$.next(null);
    this.userRoles$.next([]);
    this.userPermissions$.next([]);
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
  canAccessProject(projectId: number, permission: string): Observable<boolean> {
    return this.http.get<boolean>(`/api/projects/${projectId}/can-access`, {
      params: { permission }
    });
  }

  canAccessTask(taskId: number, permission: string): Observable<boolean> {
    return this.http.get<boolean>(`/api/tasks/${taskId}/can-access`, {
      params: { permission }
    });
  }

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    const decoded = this.decodeToken(token);
    if (!decoded || !decoded.exp) return true;
    return Date.now() >= decoded.exp * 1000;
  }
}
```

## Route Guards and Authorization

### Enhanced Auth Guard
```typescript
// auth.guard.ts (Enhanced)
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuthenticated => {
      if (isAuthenticated) {
        return true;
      } else {
        router.navigate(['/auth/login'], { 
          queryParams: { returnUrl: state.url } 
        });
        return false;
      }
    })
  );
};
```

### Role-Based Route Guard
```typescript
// role.guard.ts
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
          router.navigate(['/forbidden']);
          return false;
        }
      })
    );
  };
};
```

### Permission-Based Route Guard
```typescript
// permission.guard.ts
export const permissionGuard = (requiredPermissions: string[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.hasAllPermissions(requiredPermissions).pipe(
      take(1),
      map(hasPermissions => {
        if (hasPermissions) {
          return true;
        } else {
          router.navigate(['/forbidden']);
          return false;
        }
      })
    );
  };
};
```

### Resource-Specific Guard
```typescript
// resource.guard.ts
export const resourceGuard = (
  resourceType: 'project' | 'task',
  permission: string
): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    
    const resourceId = Number(route.paramMap.get('id'));
    
    const canAccess$ = resourceType === 'project' 
      ? authService.canAccessProject(resourceId, permission)
      : authService.canAccessTask(resourceId, permission);
    
    return canAccess$.pipe(
      take(1),
      map(canAccess => {
        if (canAccess) {
          return true;
        } else {
          router.navigate(['/forbidden']);
          return false;
        }
      })
    );
  };
};
```

### Route Configuration Examples
```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: 'projects',
    canActivate: [authGuard, roleGuard([UserRole.ROLE_USER])],
    loadChildren: () => import('./features/projects/projects.routes').then(m => m.routes)
  },
  {
    path: 'projects/:id/edit',
    canActivate: [
      authGuard, 
      resourceGuard('project', 'PRJ_UPDATE')
    ],
    component: ProjectEditComponent
  },
  {
    path: 'admin',
    canActivate: [
      authGuard, 
      roleGuard([UserRole.ROLE_ADMIN])
    ],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.routes)
  }
];
```

## Role-Based UI Components

### Enhanced Has-Role Directive
```typescript
// has-role.directive.ts
@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective implements OnInit, OnDestroy {
  @Input() appHasRole: UserRole | UserRole[] = [];
  @Input() requireAll: boolean = false;

  private destroy$ = new Subject<void>();

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const roles = Array.isArray(this.appHasRole) ? this.appHasRole : [this.appHasRole];
    
    const hasRole$ = this.requireAll 
      ? this.authService.hasAllRoles(roles)
      : this.authService.hasAnyRole(roles);

    hasRole$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(hasRole => {
      if (hasRole) {
        this.viewContainer.createEmbeddedView(this.templateRef);
      } else {
        this.viewContainer.clear();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### Has-Permission Directive
```typescript
// has-permission.directive.ts
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input() appHasPermission: string | string[] = [];
  @Input() requireAll: boolean = false;

  private destroy$ = new Subject<void>();

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const permissions = Array.isArray(this.appHasPermission) 
      ? this.appHasPermission 
      : [this.appHasPermission];
    
    const hasPermission$ = this.requireAll 
      ? this.authService.hasAllPermissions(permissions)
      : this.authService.hasAnyPermission(permissions);

    hasPermission$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(hasPermission => {
      if (hasPermission) {
        this.viewContainer.createEmbeddedView(this.templateRef);
      } else {
        this.viewContainer.clear();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### Usage Examples
```html
<!-- Role-based visibility -->
<button *appHasRole="UserRole.ROLE_PROJECT_MANAGER" 
        (click)="createProject()">
  Create Project
</button>

<!-- Multiple roles (any) -->
<div *appHasRole="[UserRole.ROLE_DEVELOPER, UserRole.ROLE_PROJECT_MANAGER]">
  Developer/Manager content
</div>

<!-- Multiple roles (all required) -->
<div *appHasRole="[UserRole.ROLE_ADMIN]; requireAll: true">
  Admin-only content
</div>

<!-- Permission-based visibility -->
<button *appHasPermission="'PRJ_UPDATE'" 
        (click)="editProject()">
  Edit Project
</button>

<!-- Multiple permissions -->
<div *appHasPermission="['TASK_CREATE', 'TASK_UPDATE']; requireAll: false">
  Task management content
</div>
```

### Permission Pipe
```typescript
// has-permission.pipe.ts
@Pipe({
  name: 'hasPermission',
  pure: false,
  standalone: true
})
export class HasPermissionPipe implements PipeTransform {
  constructor(private authService: AuthService) {}

  transform(permission: string | string[], requireAll: boolean = false): Observable<boolean> {
    const permissions = Array.isArray(permission) ? permission : [permission];
    
    return requireAll 
      ? this.authService.hasAllPermissions(permissions)
      : this.authService.hasAnyPermission(permissions);
  }
}
```

### Usage in Templates
```html
<!-- Using pipe in template -->
<button [disabled]="!('PRJ_UPDATE' | hasPermission | async)" 
        (click)="editProject()">
  Edit Project
</button>

<!-- Complex permission logic -->
<div *ngIf="(['TASK_CREATE', 'TASK_ASSIGN'] | hasPermission: false | async)">
  Task creation controls
</div>
```

## Best Practices

### 1. Token Management
- **Automatic Token Refresh**: Implement refresh token mechanism
- **Secure Storage**: Use HttpOnly cookies for sensitive tokens
- **Token Expiration Handling**: Graceful logout on token expiration

```typescript
// token-refresh.interceptor.ts
export const tokenRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && authService.getToken()) {
        // Token expired, attempt refresh
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Retry original request with new token
            const newReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${authService.getToken()}`
              }
            });
            return next(newReq);
          }),
          catchError(() => {
            // Refresh failed, logout user
            authService.logout();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
```

### 2. Permission Caching
```typescript
// permission-cache.service.ts
@Injectable({ providedIn: 'root' })
export class PermissionCacheService {
  private cache = new Map<string, { result: boolean; expiry: number }>();
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  checkPermission(key: string, checkFn: () => Observable<boolean>): Observable<boolean> {
    const cached = this.cache.get(key);
    
    if (cached && Date.now() < cached.expiry) {
      return of(cached.result);
    }
    
    return checkFn().pipe(
      tap(result => {
        this.cache.set(key, {
          result,
          expiry: Date.now() + this.CACHE_DURATION
        });
      })
    );
  }

  clearCache(): void {
    this.cache.clear();
  }
}
```

### 3. Error Handling
```typescript
// error.interceptor.ts (Enhanced)
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          router.navigate(['/auth/login']);
          notificationService.error('Please log in to continue');
          break;
        case 403:
          router.navigate(['/forbidden']);
          notificationService.error('You do not have permission to access this resource');
          break;
        case 404:
          notificationService.error('Resource not found');
          break;
        default:
          notificationService.error('An unexpected error occurred');
      }
      
      return throwError(() => error);
    })
  );
};
```

### 4. Loading States
```typescript
// loading.service.ts
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private loadingSubject = new BehaviorSubject<boolean>(false);
  loading$ = this.loadingSubject.asObservable();

  setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }
}

// loading.interceptor.ts
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);
  
  loadingService.setLoading(true);
  
  return next(req).pipe(
    finalize(() => loadingService.setLoading(false))
  );
};
```

## Security Considerations

### 1. XSS Protection
- **Sanitize User Input**: Use Angular's built-in sanitization
- **Content Security Policy**: Implement strict CSP headers
- **Avoid innerHTML**: Use Angular's safe methods for dynamic content

### 2. CSRF Protection
```typescript
// csrf.interceptor.ts
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') {
    const csrfToken = getCsrfToken(); // Get from meta tag or cookie
    const csrfReq = req.clone({
      setHeaders: { 'X-CSRF-TOKEN': csrfToken }
    });
    return next(csrfReq);
  }
  return next(req);
};
```

### 3. Token Security
```typescript
// Secure token storage
class SecureStorageService {
  setToken(token: string): void {
    // Use httpOnly cookies in production
    if (environment.production) {
      this.setCookie('auth_token', token, { httpOnly: true, secure: true });
    } else {
      sessionStorage.setItem('jwt_token', token);
    }
  }

  getToken(): string | null {
    if (environment.production) {
      return this.getCookie('auth_token');
    }
    return sessionStorage.getItem('jwt_token');
  }
}
```

### 4. Rate Limiting
```typescript
// rate-limit.interceptor.ts
export const rateLimitInterceptor: HttpInterceptorFn = (req, next) => {
  const rateLimitService = inject(RateLimitService);
  
  if (!rateLimitService.canMakeRequest(req.url)) {
    return throwError(() => new Error('Rate limit exceeded'));
  }
  
  return next(req);
};
```

## Testing Strategies

### 1. Authentication Service Tests
```typescript
// auth.service.spec.ts
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should login successfully', () => {
    const mockResponse: LoginResponse = {
      token: 'mock-token',
      user: { id: 1, email: 'test@example.com', role: UserRole.ROLE_USER }
    };

    service.login({ email: 'test@example.com', password: 'password' }).subscribe();

    const req = httpMock.expectOne('/api/users/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(service.isAuthenticated$.value).toBe(true);
    expect(service.currentUser$.value).toEqual(mockResponse.user);
  });

  it('should check permissions correctly', () => {
    service.userPermissions$.next(['PRJ_READ', 'TASK_CREATE']);

    service.hasPermission('PRJ_READ').subscribe(result => {
      expect(result).toBe(true);
    });

    service.hasPermission('PRJ_DELETE').subscribe(result => {
      expect(result).toBe(false);
    });
  });
});
```

### 2. Guard Tests
```typescript
// auth.guard.spec.ts
describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', [], {
      isAuthenticated$: new BehaviorSubject(false)
    });
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should allow access for authenticated users', () => {
    authService.isAuthenticated$.next(true);

    TestBed.runInInjectionContext(() => {
      const result = authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      expect(result).toBe(true);
    });
  });

  it('should redirect unauthenticated users to login', () => {
    authService.isAuthenticated$.next(false);

    TestBed.runInInjectionContext(() => {
      const result = authGuard({} as ActivatedRouteSnapshot, { url: '/dashboard' } as RouterStateSnapshot);
      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { returnUrl: '/dashboard' } });
    });
  });
});
```

### 3. Component Permission Tests
```typescript
// component.spec.ts
describe('ProjectComponent', () => {
  let component: ProjectComponent;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['hasPermission']);

    TestBed.configureTestingModule({
      declarations: [ProjectComponent],
      providers: [{ provide: AuthService, useValue: authSpy }]
    });

    component = TestBed.createComponent(ProjectComponent).componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should show edit button for users with PRJ_UPDATE permission', () => {
    authService.hasPermission.and.returnValue(of(true));

    component.ngOnInit();

    expect(component.canEdit).toBe(true);
  });
});
```

### 4. E2E Permission Tests
```typescript
// auth.e2e-spec.ts
describe('Authentication Flow', () => {
  it('should redirect to login for unauthenticated users', () => {
    cy.visit('/dashboard');
    cy.url().should('include', '/auth/login');
  });

  it('should allow access after login', () => {
    cy.login('test@example.com', 'password');
    cy.visit('/dashboard');
    cy.url().should('include', '/dashboard');
  });

  it('should show role-specific content', () => {
    cy.login('admin@example.com', 'password');
    cy.visit('/dashboard');
    cy.get('[data-cy=admin-panel]').should('be.visible');
  });

  it('should hide restricted actions', () => {
    cy.login('user@example.com', 'password');
    cy.visit('/projects/1');
    cy.get('[data-cy=delete-project]').should('not.exist');
  });
});
```

## Implementation Checklist

### Backend (✅ Complete)
- [x] JWT token generation with role/permission claims
- [x] API Gateway JWT validation and header forwarding
- [x] Role-based method security annotations
- [x] Custom permission evaluators for each service
- [x] YAML-based permission configuration
- [x] CORS configuration for frontend integration

### Frontend (🔄 Partial - Needs Enhancement)
- [x] Basic authentication service
- [x] JWT token storage and retrieval
- [x] Basic auth guard
- [x] HTTP interceptor for token attachment
- [ ] **Enhanced permission checking methods**
- [ ] **Role-based route guards**
- [ ] **Permission-based route guards**
- [ ] **Resource-specific access guards**
- [ ] **Complete has-role directive implementation**
- [ ] **Has-permission directive**
- [ ] **Permission pipes**
- [ ] **Token refresh mechanism**
- [ ] **Permission caching**
- [ ] **Enhanced error handling**

### Recommended Next Steps

1. **Enhance the HasRoleDirective** - Implement the empty directive with proper role checking
2. **Create Permission-Based Directives** - Add directives for fine-grained permission control
3. **Implement Advanced Guards** - Add role and permission-based route guards
4. **Add Token Refresh** - Implement automatic token refresh mechanism
5. **Enhance Error Handling** - Improve error handling for authorization failures
6. **Add Permission Caching** - Implement client-side permission caching for performance
7. **Create Unit Tests** - Add comprehensive tests for all auth-related components

This guide provides a complete foundation for integrating RBAC from your backend microservices to the Angular frontend, ensuring secure and efficient authorization throughout your application.

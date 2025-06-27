package com.pm.projectservice.security;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.projectservice.model.Project;
import com.pm.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive version of ProjectPermissionEvaluator for use in reactive chains.
 * This maintains the same permission logic as ProjectPermissionEvaluator but
 * returns Mono<Boolean>
 * instead of blocking boolean values.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReactiveProjectPermissionEvaluator {

    private final ProjectRepository projectRepository;
    private final PermissionEvaluator basePermissionEvaluator;

    /**
     * Check permission for a specific project reactively
     *
     * @param authentication The user's authentication
     * @param projectId The project ID
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, String projectId, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // Get project reactively and check permissions
            return projectRepository.findById(projectId)
                    .map(project -> hasProjectAccess(authentication, project, actionEnum, currentUserId))
                    .switchIfEmpty(Mono.just(false)) // Project not found = no permission
                    .onErrorResume(e -> {
                        log.error("Error checking permissions for project {} and action {}: {}", projectId, action, e.getMessage());
                        return Mono.just(false);
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        }
    }

    /**
     * Check permission when we already have the project entity
     *
     * @param authentication The user's authentication
     * @param project The project entity
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, Project project, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null || project == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            boolean hasAccess = hasProjectAccess(authentication, project, actionEnum, currentUserId);
            return Mono.just(hasAccess);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error checking permissions for project {} and action {}: {}", project.getId(), action, e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * Check general permissions (not project-specific)
     *
     * @param authentication The user's authentication
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasGeneralPermission(Authentication authentication, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            List<String> userRoles = getUserRoles(authentication);
            boolean hasPermission = basePermissionEvaluator.hasPermission(userRoles, actionEnum);
            return Mono.just(hasPermission);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error checking general permission for action {}: {}", action, e.getMessage());
            return Mono.just(false);
        }
    }

    private boolean hasProjectAccess(Authentication authentication, Project project, Action action, String currentUserId) {
        // Check if user is project creator
        boolean isCreator = currentUserId != null && currentUserId.equals(project.getCreatedBy());

        // Check if user is project member
        boolean isMember = currentUserId != null && project.getMemberIds() != null
                && project.getMemberIds().contains(currentUserId);

        // Project-specific permissions based on membership/ownership
        return switch (action) {
            case PRJ_READ -> {
                // Members and creators can read the project, plus role-based access
                if (isCreator || isMember) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case PRJ_UPDATE, PRJ_STATUS_CHANGE -> {
                // Only creators and project managers can update projects
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case PRJ_DELETE, PRJ_ARCHIVE -> {
                // Only creators and admins can delete/archive projects
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case PRJ_MEMBER_ADD, PRJ_MEMBER_REMOVE -> {
                // Only creators and project managers can manage members
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            // For all other actions, use role-based evaluation
            default -> {
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
        };
    }

    private String getCurrentUserId(Authentication authentication) {
        // Extract user ID from authentication - this depends on your JWT implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof String stringPrincipal) {
            return stringPrincipal;
        }
        return authentication.getName();
    }

    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList();
    }
}

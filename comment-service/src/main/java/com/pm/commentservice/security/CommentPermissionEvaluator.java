package com.pm.commentservice.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pm.commentservice.model.Comment;
import com.pm.commentservice.repository.CommentRepository;
import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Reactive Comment-specific permission evaluator for use in reactive chains.
 * This maintains the same permission logic but returns Mono<Boolean> instead of
 * blocking boolean values.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentPermissionEvaluator {

    private final CommentRepository commentRepository;
    private final PermissionEvaluator basePermissionEvaluator;

    /**
     * Check permission for a specific comment reactively
     *
     * @param authentication The user's authentication
     * @param commentId The comment ID
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, String commentId, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // Get comment reactively and check permissions
            return commentRepository.findById(commentId)
                    .map(comment -> hasCommentAccess(authentication, comment, actionEnum, currentUserId))
                    .switchIfEmpty(Mono.just(false)) // Comment not found = no permission
                    .onErrorResume(e -> {
                        log.error("Error checking permissions for comment {} and action {}: {}", commentId, action, e.getMessage());
                        return Mono.just(false);
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        }
    }

    /**
     * Check permission when we already have the comment entity
     *
     * @param authentication The user's authentication
     * @param comment The comment entity
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, Comment comment, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null || comment == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            boolean hasAccess = hasCommentAccess(authentication, comment, actionEnum, currentUserId);
            return Mono.just(hasAccess);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error checking permissions for comment {} and action {}: {}", comment.getId(), action, e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * Check general permissions (not comment-specific)
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

    /**
     * Evaluates comment-specific access rules
     */
    private boolean hasCommentAccess(Authentication authentication, Comment comment, Action action, String currentUserId) {
        List<String> userRoles = getUserRoles(authentication);
        // Check if the user is the creator of the comment
        boolean isCreator = currentUserId != null && currentUserId.equals(comment.getUserId());

        return switch (action) {
            case CMT_READ -> {
                // Anyone with basic role can read comments if they have access to the parent resource
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case CMT_CREATE -> {
                // Anyone with basic access can create comments (handled at parent project/task level)
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case CMT_UPDATE, CMT_EDIT, CMT_UPDATE_OWN -> {
                // Only creators can update their own comments, or admins/PMs
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case CMT_DELETE, CMT_DELETE_OWN -> {
                // Only creators can delete their own comments, or admins/PMs
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case CMT_DELETE_ANY -> {
                // Only admins/moderators can delete any comment
                yield userRoles.contains("ROLE_ADMIN") || userRoles.contains("ROLE_PROJECT_MANAGER");
            }
            case CMT_REPLY -> {
                // Anyone who can create comments can reply
                yield basePermissionEvaluator.hasPermission(userRoles, Action.CMT_CREATE);
            }
            // For all other actions, use role-based evaluation
            default -> {
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
        };
    }

    /**
     * Extracts user ID from authentication
     */
    private String getCurrentUserId(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * Extracts user roles from authentication
     */
    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .toList();
    }
}

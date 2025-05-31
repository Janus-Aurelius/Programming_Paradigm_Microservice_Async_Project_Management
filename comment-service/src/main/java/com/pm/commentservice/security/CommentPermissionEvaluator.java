package com.pm.commentservice.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pm.commentservice.model.Comment;
import com.pm.commentservice.repository.CommentRepository;
import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Comment-specific permission evaluator that extends the base RBAC system
 * to handle comment-specific access scenarios (creator ownership).
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final CommentRepository commentRepository;
    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // Check if user is trying to access a specific comment
            if (targetDomainObject instanceof Comment comment) {
                return hasCommentAccess(authentication, comment, action, currentUserId);
            }
            
            // Check if targetDomainObject is a comment ID string
            if (targetDomainObject instanceof String commentId) {
                // For operations that need comment context but only have the ID
                Comment comment = commentRepository.findById(commentId).block();
                if (comment != null) {
                    return hasCommentAccess(authentication, comment, action, currentUserId);
                }
            }
            
            // Fall back to role-based permission evaluation for general actions
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return false;
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            if ("COMMENT".equals(targetType) && targetId != null) {
                Comment comment = commentRepository.findById(targetId.toString()).block();
                if (comment != null) {
                    return hasCommentAccess(authentication, comment, action, currentUserId);
                }
            }
            
            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return false;
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
            return false;
        }
    }

    /**
     * Evaluates comment-specific access rules
     */
    private boolean hasCommentAccess(Authentication authentication, Comment comment, Action action, String currentUserId) {
        List<String> userRoles = getUserRoles(authentication);
          // Check if the user is the creator of the comment
        boolean isCreator = currentUserId.equals(comment.getUserId());
        
        switch (action) {
            case CMT_CREATE:
                // Anyone with basic access can create comments (handled at parent project/task level)
                return true;
                
            case CMT_UPDATE_OWN:
                // Only creators can update their own comments, or admins
                return isCreator || userRoles.contains("ROLE_ADMIN");
                
            case CMT_DELETE_OWN:
                // Only creators can delete their own comments, or admins
                return isCreator || userRoles.contains("ROLE_ADMIN");
                
            case CMT_DELETE_ANY:
                // Only admins/moderators can delete any comment
                return userRoles.contains("ROLE_ADMIN") || userRoles.contains("ROLE_PROJECT_MANAGER");
                
            default:
                // For other actions, fall back to role-based permission evaluation
                return basePermissionEvaluator.hasPermission(userRoles, action);
        }
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
    @SuppressWarnings("unchecked")
    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .toList();
    }
}

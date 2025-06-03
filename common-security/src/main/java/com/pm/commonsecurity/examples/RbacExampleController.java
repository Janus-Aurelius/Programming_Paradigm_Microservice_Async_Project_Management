package com.pm.commonsecurity.examples;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Example controller demonstrating RBAC integration patterns.
 * This shows how to properly use @PreAuthorize annotations with the RBAC system.
 */
@RestController
@RequestMapping("/api/examples")
public class RbacExampleController {

    // ═══════════════════════════════════════════════════════════════════════════
    // User Management Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'USER_SELF_READ')")
    @GetMapping("/profile")
    public Mono<String> getOwnProfile() {
        // Any authenticated user can read their own profile
        return Mono.just("User profile data");
    }

    @PreAuthorize("hasPermission(null, 'USER_SELF_UPDATE')")
    @PutMapping("/profile")
    public Mono<String> updateOwnProfile(@RequestBody Object profileData) {
        // Any authenticated user can update their own profile
        return Mono.just("Profile updated");
    }

    @PreAuthorize("hasPermission(null, 'USER_READ')")
    @GetMapping("/admin/users")
    public Flux<String> getAllUsers() {
        // Only admins can view all users
        return Flux.just("user1", "user2", "user3");
    }

    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    @PostMapping("/admin/users")
    public Mono<String> createUser(@RequestBody Object userData) {
        // Only admins can create new users
        return Mono.just("User created");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Project Management Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping("/projects")
    public Flux<String> getProjects() {
        // Developers and above can read projects
        return Flux.just("project1", "project2");
    }

    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    @PostMapping("/projects")
    public Mono<String> createProject(@RequestBody Object projectData) {
        // Project managers and above can create projects
        return Mono.just("Project created");
    }

    @PreAuthorize("hasPermission(#projectId, 'Project', 'PRJ_UPDATE')")
    @PutMapping("/projects/{projectId}")
    public Mono<String> updateProject(@PathVariable String projectId, @RequestBody Object projectData) {
        // Project managers and above can update projects
        // This could be enhanced with ownership checks
        return Mono.just("Project updated");
    }

    @PreAuthorize("hasPermission(null, 'PRJ_DELETE')")
    @DeleteMapping("/projects/{projectId}")
    public Mono<String> deleteProject(@PathVariable String projectId) {
        // Project managers and above can delete projects
        return Mono.just("Project deleted");
    }

    @PreAuthorize("hasPermission(null, 'PRJ_MEMBER_ADD')")
    @PostMapping("/projects/{projectId}/members")
    public Mono<String> addProjectMember(@PathVariable String projectId, @RequestBody Object memberData) {
        // Project managers and above can add members
        return Mono.just("Member added");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Task Management Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @PostMapping("/tasks")
    public Mono<String> createTask(@RequestBody Object taskData) {
        // Developers and above can create tasks
        return Mono.just("Task created");
    }

    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @GetMapping("/tasks")
    public Flux<String> getTasks() {
        // Developers and above can read tasks
        return Flux.just("task1", "task2");
    }

    @PreAuthorize("hasPermission(null, 'TASK_UPDATE')")
    @PutMapping("/tasks/{taskId}")
    public Mono<String> updateTask(@PathVariable String taskId, @RequestBody Object taskData) {
        // Developers and above can update tasks
        return Mono.just("Task updated");
    }

    @PreAuthorize("hasPermission(null, 'TASK_STATUS_CHANGE')")
    @PutMapping("/tasks/{taskId}/status")
    public Mono<String> changeTaskStatus(@PathVariable String taskId, @RequestBody Object statusData) {
        // Developers and above can change task status
        return Mono.just("Task status changed");
    }

    @PreAuthorize("hasPermission(null, 'TASK_ASSIGN')")
    @PutMapping("/tasks/{taskId}/assign")
    public Mono<String> assignTask(@PathVariable String taskId, @RequestBody Object assignmentData) {
        // Project managers and above can assign tasks
        return Mono.just("Task assigned");
    }

    @PreAuthorize("hasPermission(null, 'TASK_DELETE')")
    @DeleteMapping("/tasks/{taskId}")
    public Mono<String> deleteTask(@PathVariable String taskId) {
        // Project managers and above can delete tasks
        return Mono.just("Task deleted");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Comment Management Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'CMT_CREATE')")
    @PostMapping("/comments")
    public Mono<String> createComment(@RequestBody Object commentData) {
        // All authenticated users can create comments
        return Mono.just("Comment created");
    }

    @PreAuthorize("hasPermission(#commentId, 'Comment', 'CMT_UPDATE_OWN')")
    @PutMapping("/comments/{commentId}")
    public Mono<String> updateComment(@PathVariable String commentId, @RequestBody Object commentData) {
        // Users can update their own comments
        // This would need additional logic to check ownership
        return Mono.just("Comment updated");
    }

    @PreAuthorize("hasPermission(#commentId, 'Comment', 'CMT_DELETE_OWN')")
    @DeleteMapping("/comments/{commentId}")
    public Mono<String> deleteOwnComment(@PathVariable String commentId) {
        // Users can delete their own comments
        return Mono.just("Own comment deleted");
    }

    @PreAuthorize("hasPermission(null, 'CMT_DELETE_ANY')")
    @DeleteMapping("/admin/comments/{commentId}")
    public Mono<String> deleteAnyComment(@PathVariable String commentId) {
        // Project managers and above can delete any comment
        return Mono.just("Comment deleted by moderator");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Notification Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'NOTI_READ')")
    @GetMapping("/notifications")
    public Flux<String> getNotifications() {
        // All authenticated users can read their notifications
        return Flux.just("notification1", "notification2");
    }

    @PreAuthorize("hasPermission(null, 'NOTI_MARK_READ')")
    @PutMapping("/notifications/{notificationId}/read")
    public Mono<String> markNotificationRead(@PathVariable String notificationId) {
        // All authenticated users can mark notifications as read
        return Mono.just("Notification marked as read");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Advanced Examples with Multiple Conditions
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'PRJ_READ') and hasPermission(null, 'TASK_READ')")
    @GetMapping("/projects/{projectId}/tasks")
    public Flux<String> getProjectTasks(@PathVariable String projectId) {
        // Requires both project and task read permissions
        return Flux.just("task1", "task2");
    }

    @PreAuthorize("hasPermission(null, 'USER_ROLE_GRANT') or (hasPermission(#projectId, 'Project', 'PRJ_MEMBER_ADD') and #role == 'ROLE_DEVELOPER')")
    @PostMapping("/projects/{projectId}/members/{userId}/roles")
    public Mono<String> grantRoleToProjectMember(
            @PathVariable String projectId, 
            @PathVariable String userId, 
            @RequestParam String role) {
        // Admins can grant any role, or project managers can grant developer role
        return Mono.just("Role granted");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Conditional Access Examples
    // ═══════════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasPermission(null, 'USER_READ') or authentication.name == #userId")
    @GetMapping("/users/{userId}")
    public Mono<String> getUserProfile(@PathVariable String userId) {
        // Admins can read any user, or users can read their own profile
        return Mono.just("User profile");
    }

    @PreAuthorize("hasPermission(null, 'TASK_UPDATE') or (hasPermission(null, 'TASK_STATUS_CHANGE') and #field == 'status')")
    @PutMapping("/tasks/{taskId}/field/{field}")
    public Mono<String> updateTaskField(
            @PathVariable String taskId, 
            @PathVariable String field, 
            @RequestBody Object value) {
        // Full task update permission, or limited to status changes
        return Mono.just("Task field updated");
    }
}

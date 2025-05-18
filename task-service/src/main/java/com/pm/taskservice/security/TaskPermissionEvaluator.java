package com.pm.taskservice.security;

import com.pm.taskservice.model.Task;
import com.pm.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class TaskPermissionEvaluator implements PermissionEvaluator {

    private final TaskRepository taskRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Task task) {
            String userId = authentication.getName();
            return switch (permission.toString()) {
                case "EDIT" -> task.getAssigneeId().equals(userId) || task.getCreatedBy().equals(userId);
                case "VIEW" -> task.getAssigneeId().equals(userId) || task.getCreatedBy().equals(userId);
                default -> false;
            };
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if ("Task".equals(targetType)) {
            String userId = authentication.getName();
            return Boolean.TRUE.equals(taskRepository.findById(targetId.toString())
                    .map(task -> hasPermission(authentication, task, permission))
                    .defaultIfEmpty(false)
                    .block());
        }
        return false;
    }
}

package com.pm.projectservice.security;

import com.pm.projectservice.model.Project;
import com.pm.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements PermissionEvaluator {

    private final ProjectRepository projectRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Project project) {
            String userId = authentication.getName();
            return switch (permission.toString()) {
                case "EDIT" -> project.getCreatedBy().equals(userId) || project.getMemberIds().contains(userId);
                case "VIEW" -> project.getCreatedBy().equals(userId) || project.getMemberIds().contains(userId);
                default -> false;
            };
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if ("Project".equals(targetType)) {
            String userId = authentication.getName();
            return Boolean.TRUE.equals(projectRepository.findById(targetId.toString())
                    .map(project -> hasPermission(authentication, project, permission))
                    .defaultIfEmpty(false)
                    .block());
        }
        return false;
    }
}

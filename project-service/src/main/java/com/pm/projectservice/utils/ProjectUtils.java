package com.pm.projectservice.utils;

import org.springframework.beans.BeanUtils;

import com.pm.commoncontracts.dto.ProjectDto;
import com.pm.projectservice.model.Project;

public class ProjectUtils {
    public static ProjectDto entityToDto(Project project) {
        ProjectDto projectDto = new ProjectDto();
        BeanUtils.copyProperties(project, projectDto);
        // Ensure memberIds and priority are copied if not handled by BeanUtils
        projectDto.setMemberIds(project.getMemberIds());
        projectDto.setPriority(project.getPriority());
        return projectDto;
    }

    public static Project dtoToEntity(ProjectDto projectDto) {
        Project projectEntity = new Project();
        BeanUtils.copyProperties(projectDto, projectEntity);
        // Ensure memberIds and priority are copied if not handled by BeanUtils
        projectEntity.setMemberIds(projectDto.getMemberIds());
        projectEntity.setPriority(projectDto.getPriority());
        return projectEntity;
    }
}

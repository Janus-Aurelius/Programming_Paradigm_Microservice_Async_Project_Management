package com.pm.taskservice.utils;

import com.pm.commoncontracts.dto.TaskDto;
import com.pm.taskservice.model.Task;
import org.springframework.beans.BeanUtils;


public class TaskUtils {
    public static TaskDto entityToDto(com.pm.taskservice.model.Task task) {
        TaskDto taskDto = new TaskDto();
        BeanUtils.copyProperties(task, taskDto);
        return taskDto;

    }

    public static Task dtoToEntity(TaskDto taskDto) {
        Task taskEntity = new Task();
        BeanUtils.copyProperties(taskDto, taskEntity);
        return taskEntity;
    }

}

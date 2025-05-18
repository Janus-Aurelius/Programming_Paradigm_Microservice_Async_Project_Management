package com.pm.notificationservice.utils;

import com.pm.commoncontracts.dto.NotificationDto;
import org.springframework.beans.BeanUtils;
import com.pm.notificationservice.model.Notification;

public class NotificationUtils {
    public static NotificationDto entityToDto(Notification notification) {
        NotificationDto notificationDto = new NotificationDto();
        BeanUtils.copyProperties(notification, notificationDto);
        return notificationDto;

    }

    public static Notification dtoToEntity(NotificationDto notificationDto) {
        Notification notificationEntity = new Notification();
        BeanUtils.copyProperties(notificationDto, notificationEntity);
        return notificationEntity;
    }
}

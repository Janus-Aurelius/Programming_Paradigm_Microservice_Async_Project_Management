package com.pm.commoncontracts.events.notification;

public enum NotificationEvent {
    //user-related
    USER_CREATED,
    USER_DELETED,
    USER_UPDATED,
    //project-related
    PROJECT_CREATED,
    PROJECT_DELETED,
    PROJECT_PRIORITY_CHANGED,
    PROJECT_STATUS_CHANGED,
    PROJECT_UPDATED,
    PROJECT_TASK_CREATED,
    // TODO: check project member added to project or to assign to a task, its events, logic and notif
    PROJECT_MEMBER_ADDED,
    //task-related
    TASK_ASSIGNED,
    TASK_CREATED,
    TASK_DELETED,
    TASK_PRIORITY_CHANGED,
    TASK_STATUS_CHANGED,
    TASK_UPDATED,
    //comment-related
    COMMENT_ADDED,
    COMMENT_EDITED,
    COMMENT_DELETED,
    NOTIFICATION_READ
}

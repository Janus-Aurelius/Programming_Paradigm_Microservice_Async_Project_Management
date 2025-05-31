package com.pm.commonsecurity.security;

public enum Action {    // ─── User service ──────────────────────────────────────────────
    USER_SELF_READ,        // read own profile
    USER_SELF_UPDATE,      // update own profile/password
    USER_READ,             // read any user (admin only)
    USER_UPDATE,           // update any user (admin only)
    USER_CREATE,           // create new user (admin only)
    USER_DELETE,           // delete user (admin only)
    USER_ROLE_GRANT,       // assign/revoke roles (admin only)    // ─── Project service ───────────────────────────────────────────
    PRJ_CREATE,            // create a new project
    PRJ_READ,              // view project metadata & members
    PRJ_UPDATE,            // modify project settings
    PRJ_DELETE,            // permanently delete project
    PRJ_ARCHIVE,           // soft-close / freeze project
    PRJ_STATUS_CHANGE,     // change project status
    PRJ_MEMBER_ADD,        // add member to project
    PRJ_MEMBER_REMOVE,     // remove member from project    // ─── Task service ──────────────────────────────────────────────
    TASK_CREATE,           // create a task
    TASK_READ,             // view task details
    TASK_UPDATE,           // edit task fields
    TASK_STATUS_CHANGE,    // move task through workflow
    TASK_PRIORITY_CHANGE,  // change task priority
    TASK_ASSIGN,           // assign/reassign task owners/watchers
    TASK_DELETE,           // delete a task

    // ─── Comment service ───────────────────────────────────────────
    CMT_CREATE,            // post a new comment
    CMT_UPDATE_OWN,        // edit your own comment
    CMT_DELETE_OWN,        // delete your own comment
    CMT_DELETE_ANY,        // delete any comment (moderator)

    // ─── Notification service ──────────────────────────────────────
    NOTI_SEND,             // emit a notification (system only)
    NOTI_READ,             // read notification stream
    NOTI_MARK_READ         // mark notification(s) read
}

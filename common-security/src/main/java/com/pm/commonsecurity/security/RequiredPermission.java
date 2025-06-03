package com.pm.commonsecurity.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level permission checking.
 * Can be used to declaratively specify required permissions for methods.
 * 
 * Example usage:
 * @RequiredPermission(Action.TASK_CREATE)
 * public Task createTask(TaskRequest request) { ... }
 * 
 * @RequiredPermission("TASK_UPDATE")
 * public Task updateTask(String id, TaskRequest request) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermission {
    
    /**
     * The required action as an Action enum value
     */
    Action[] value() default {};
    
    /**
     * The required action as a string (alternative to value())
     */
    String[] actions() default {};
    
    /**
     * Logical operator for multiple permissions
     * - AND: user must have ALL specified permissions
     * - OR: user must have AT LEAST ONE of the specified permissions
     */
    LogicalOperator operator() default LogicalOperator.OR;
    
    /**
     * Custom error message when permission is denied
     */
    String message() default "Access denied: insufficient permissions";
    
    enum LogicalOperator {
        AND, OR
    }
}

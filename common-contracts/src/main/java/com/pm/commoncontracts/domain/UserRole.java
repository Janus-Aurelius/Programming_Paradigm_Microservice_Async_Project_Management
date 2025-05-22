package com.pm.commoncontracts.domain;

public enum UserRole {
    ROLE_USER("ROLE_USER"),
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_PROJECT_MANAGER("ROLE_PROJECT_MANAGER"),
    ROLE_DEVELOPER("ROLE_DEVELOPER");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return role;
    }
}

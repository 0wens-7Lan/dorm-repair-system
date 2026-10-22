package com.dormrepair.support;

public class CurrentUser {
    private Long id;
    private String username;
    private String name;
    private String roleCode;

    public CurrentUser() {
    }

    public CurrentUser(Long id, String username, String name, String roleCode) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.roleCode = roleCode;
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (roleCode != null && roleCode.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}

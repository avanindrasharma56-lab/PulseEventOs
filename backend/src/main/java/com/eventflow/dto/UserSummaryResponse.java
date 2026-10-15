package com.eventflow.dto;

import java.util.Set;

public class UserSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private Set<String> roles;

    public UserSummaryResponse() {}

    public UserSummaryResponse(Long id, String email, String fullName, String phone, String status, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}

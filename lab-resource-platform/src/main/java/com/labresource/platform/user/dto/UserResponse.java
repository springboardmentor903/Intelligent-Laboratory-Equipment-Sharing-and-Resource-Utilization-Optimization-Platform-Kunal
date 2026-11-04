
package com.labresource.platform.user.dto;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.entity.Role;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.department.entity.Department;

public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String role;
    private String institutionName;
    private String departmentName;
    private String authProvider;
    private Boolean emailVerified;
    private Boolean isActive;

    public UserResponse(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.profileImageUrl = user.getProfileImageUrl();
        this.authProvider = user.getAuthProvider();
        this.emailVerified = user.getEmailVerified();
        this.isActive = user.getIsActive();

        Role userRole = user.getRole();
        if (userRole != null) {
            this.role = userRole.getName();
        }

        Institution institution = user.getInstitution();
        if (institution != null) {
            this.institutionName = institution.getName();
        }

        Department department = user.getDepartment();
        if (department != null) {
            this.departmentName = department.getName();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getRole() {
        return role;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}

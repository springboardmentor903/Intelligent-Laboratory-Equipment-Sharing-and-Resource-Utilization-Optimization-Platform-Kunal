
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.user.entity.User;

public class TechnicianOptionResponse {

    private final Long id;
    private final String fullName;
    private final String email;

    public TechnicianOptionResponse(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
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
}

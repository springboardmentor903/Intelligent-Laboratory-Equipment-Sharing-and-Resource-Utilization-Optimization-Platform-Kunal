
package com.labresource.platform.user.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_roles")
@IdClass(UserRole.UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Short roleId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Short getRoleId() {
        return roleId;
    }

    public void setRoleId(Short roleId) {
        this.roleId = roleId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public static class UserRoleId implements Serializable {

        private Long userId;
        private Short roleId;

        public UserRoleId() {
        }

        public UserRoleId(Long userId, Short roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Short getRoleId() {
            return roleId;
        }

        public void setRoleId(Short roleId) {
            this.roleId = roleId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof UserRoleId)) return false;

            UserRoleId other = (UserRoleId) obj;

            return java.util.Objects.equals(userId, other.userId)
                    && java.util.Objects.equals(roleId, other.roleId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, roleId);
        }
    }
}

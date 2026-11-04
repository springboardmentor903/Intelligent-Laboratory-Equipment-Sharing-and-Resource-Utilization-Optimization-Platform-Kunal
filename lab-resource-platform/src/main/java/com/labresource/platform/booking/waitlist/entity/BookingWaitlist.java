
package com.labresource.platform.booking.waitlist.entity;

import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_waitlist")
public class BookingWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_start", nullable = false)
    private LocalDateTime requestedStart;

    @Column(name = "requested_end", nullable = false)
    private LocalDateTime requestedEnd;

    @Column(nullable = false, length = 20)
    private String status = "WAITING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "offered_at")
    private LocalDateTime offeredAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "WAITING";
        }
    }

    public Long getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getRequestedStart() {
        return requestedStart;
    }

    public void setRequestedStart(LocalDateTime requestedStart) {
        this.requestedStart = requestedStart;
    }

    public LocalDateTime getRequestedEnd() {
        return requestedEnd;
    }

    public void setRequestedEnd(LocalDateTime requestedEnd) {
        this.requestedEnd = requestedEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getOfferedAt() {
        return offeredAt;
    }

    public void setOfferedAt(LocalDateTime offeredAt) {
        this.offeredAt = offeredAt;
    }
}

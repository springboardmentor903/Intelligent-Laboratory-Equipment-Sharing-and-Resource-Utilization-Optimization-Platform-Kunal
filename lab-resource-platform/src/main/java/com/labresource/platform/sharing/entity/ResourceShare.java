
package com.labresource.platform.sharing.entity;

import com.labresource.platform.resource.entity.Resource;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_shares")
public class ResourceShare {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false)
    private Long sharedByUserId;

    @Column(nullable = false)
    private Long sharedWithUserId;

    @Column(name = "shared_by_institution_id")
    private Long sharedByInstitutionId;

    @Column(name = "shared_with_institution_id")
    private Long sharedWithInstitutionId;

    @Column(nullable = false, length = 30)
    private String status = PENDING;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // T6: Hourly sharing fee
    @Column(name = "fee_per_hour", precision = 12, scale = 2)
    private BigDecimal feePerHour = BigDecimal.ZERO;

    @Column(length = 500)
    private String remarks;

    @Column(name = "decision_remarks", length = 500)
    private String decisionRemarks;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null || status.isBlank()) {
            status = PENDING;
        }

        if (feePerHour == null) {
            feePerHour = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public void setSharedByUserId(Long sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }

    public Long getSharedWithUserId() {
        return sharedWithUserId;
    }

    public void setSharedWithUserId(Long sharedWithUserId) {
        this.sharedWithUserId = sharedWithUserId;
    }

    public Long getSharedByInstitutionId() {
        return sharedByInstitutionId;
    }

    public void setSharedByInstitutionId(Long sharedByInstitutionId) {
        this.sharedByInstitutionId = sharedByInstitutionId;
    }

    public Long getSharedWithInstitutionId() {
        return sharedWithInstitutionId;
    }

    public void setSharedWithInstitutionId(Long sharedWithInstitutionId) {
        this.sharedWithInstitutionId = sharedWithInstitutionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getFeePerHour() {
        return feePerHour;
    }

    public void setFeePerHour(BigDecimal feePerHour) {
        this.feePerHour = feePerHour;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getDecisionRemarks() {
        return decisionRemarks;
    }

    public void setDecisionRemarks(String decisionRemarks) {
        this.decisionRemarks = decisionRemarks;
    }

    public Long getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Long approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

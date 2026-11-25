package com.labresource.platform.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "equipment_alert_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_equipment_alert_reference_period",
                columnNames = {"alert_type", "reference_id", "period_key"}
        )
)
public class EquipmentAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type", nullable = false, length = 40)
    private String alertType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "period_key", nullable = false, length = 30)
    private String periodKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EquipmentAlertLog() {}

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

package com.labresource.platform.usage.entity;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "usage_charges",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_usage_charge_booking",
                columnNames = "booking_id"
        ),
        indexes = {
                @Index(name = "idx_usage_charge_user", columnList = "user_id"),
                @Index(name = "idx_usage_charge_institution", columnList = "owning_institution_id"),
                @Index(name = "idx_usage_charge_created", columnList = "created_at")
        }
)
public class UsageCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "owning_institution_id")
    private Long owningInstitutionId;

    @Column(name = "user_institution_id")
    private Long userInstitutionId;

    @Column(name = "department_id")
    private Long departmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 20)
    private ChargeType chargeType;

    @Column(name = "usage_minutes", nullable = false)
    private Long usageMinutes;

    @Column(name = "hourly_rate_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRateSnapshot;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "charge_status", nullable = false, length = 20)
    private String chargeStatus = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (chargeStatus == null) {
            chargeStatus = "PENDING";
        }
    }

    public enum ChargeType {
        INTERNAL,
        EXTERNAL,
        UNASSIGNED
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
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

    public Long getOwningInstitutionId() {
        return owningInstitutionId;
    }

    public void setOwningInstitutionId(Long id) {
        this.owningInstitutionId = id;
    }

    public Long getUserInstitutionId() {
        return userInstitutionId;
    }

    public void setUserInstitutionId(Long id) {
        this.userInstitutionId = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long id) {
        this.departmentId = id;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public void setChargeType(ChargeType chargeType) {
        this.chargeType = chargeType;
    }

    public Long getUsageMinutes() {
        return usageMinutes;
    }

    public void setUsageMinutes(Long usageMinutes) {
        this.usageMinutes = usageMinutes;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public void setHourlyRateSnapshot(BigDecimal rate) {
        this.hourlyRateSnapshot = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getChargeStatus() {
        return chargeStatus;
    }

    public void setChargeStatus(String status) {
        this.chargeStatus = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

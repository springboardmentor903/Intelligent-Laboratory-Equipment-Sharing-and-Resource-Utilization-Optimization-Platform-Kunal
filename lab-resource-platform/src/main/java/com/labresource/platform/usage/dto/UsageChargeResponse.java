
package com.labresource.platform.usage.dto;

import com.labresource.platform.usage.entity.UsageCharge;

import java.math.BigDecimal;
import java.time.Instant;

public class UsageChargeResponse {

    private Long id;
    private Long bookingId;
    private Long resourceId;
    private String equipmentName;
    private Long usageMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String chargeType;
    private String status;
    private Instant createdAt;

    public UsageChargeResponse(UsageCharge charge) {
        this.id = charge.getId();
        this.bookingId = charge.getBooking().getId();
        this.resourceId = charge.getResource().getId();
        this.equipmentName = charge.getResource().getName();
        this.usageMinutes = charge.getUsageMinutes();
        this.hourlyRate = charge.getHourlyRateSnapshot();
        this.amount = charge.getAmount();
        this.chargeType = charge.getChargeType().name();
        this.status = charge.getChargeStatus();
        this.createdAt = charge.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public Long getUsageMinutes() {
        return usageMinutes;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getChargeType() {
        return chargeType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

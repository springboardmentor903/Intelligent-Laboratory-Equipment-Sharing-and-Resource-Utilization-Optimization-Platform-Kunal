package com.labresource.platform.usage.dto;

import java.math.BigDecimal;

public class EquipmentCostResponse {

    private Long resourceId;
    private String equipmentName;
    private Long usageMinutes;
    private BigDecimal usageHours;
    private BigDecimal internalCost;
    private BigDecimal externalCost;
    private BigDecimal unassignedCost;
    private BigDecimal totalUsageCost;
    private BigDecimal maintenanceCost;
    private BigDecimal totalCost;

    public EquipmentCostResponse(
            Long resourceId,
            String equipmentName,
            Long usageMinutes,
            BigDecimal internalCost,
            BigDecimal externalCost,
            BigDecimal unassignedCost,
            BigDecimal maintenanceCost) {

        this.resourceId = resourceId;
        this.equipmentName = equipmentName;
        this.usageMinutes = usageMinutes == null ? 0L : usageMinutes;

        this.usageHours = BigDecimal.valueOf(this.usageMinutes)
                .divide(BigDecimal.valueOf(60), 2,
                        java.math.RoundingMode.HALF_UP);

        this.internalCost = safe(internalCost);
        this.externalCost = safe(externalCost);
        this.unassignedCost = safe(unassignedCost);
        this.maintenanceCost = safe(maintenanceCost);

        this.totalUsageCost = this.internalCost
                .add(this.externalCost)
                .add(this.unassignedCost);

        this.totalCost = this.totalUsageCost.add(this.maintenanceCost);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getResourceId() { return resourceId; }
    public String getEquipmentName() { return equipmentName; }
    public Long getUsageMinutes() { return usageMinutes; }
    public BigDecimal getUsageHours() { return usageHours; }
    public BigDecimal getInternalCost() { return internalCost; }
    public BigDecimal getExternalCost() { return externalCost; }
    public BigDecimal getUnassignedCost() { return unassignedCost; }
    public BigDecimal getTotalUsageCost() { return totalUsageCost; }
    public BigDecimal getMaintenanceCost() { return maintenanceCost; }
    public BigDecimal getTotalCost() { return totalCost; }
}
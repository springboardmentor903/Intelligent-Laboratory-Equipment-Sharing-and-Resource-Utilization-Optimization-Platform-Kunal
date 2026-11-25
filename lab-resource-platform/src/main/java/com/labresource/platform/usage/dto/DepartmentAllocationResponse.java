package com.labresource.platform.usage.dto;

import java.math.BigDecimal;

public class DepartmentAllocationResponse {

    private Long departmentId;
    private String departmentName;
    private BigDecimal internalCost;
    private BigDecimal externalCost;
    private BigDecimal totalCost;
    private BigDecimal percentageShare;

    public DepartmentAllocationResponse(
            Long departmentId,
            String departmentName,
            BigDecimal internalCost,
            BigDecimal externalCost,
            BigDecimal totalCost,
            BigDecimal percentageShare) {

        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.internalCost = safe(internalCost);
        this.externalCost = safe(externalCost);
        this.totalCost = safe(totalCost);
        this.percentageShare = safe(percentageShare);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public BigDecimal getInternalCost() { return internalCost; }
    public BigDecimal getExternalCost() { return externalCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getPercentageShare() { return percentageShare; }
}
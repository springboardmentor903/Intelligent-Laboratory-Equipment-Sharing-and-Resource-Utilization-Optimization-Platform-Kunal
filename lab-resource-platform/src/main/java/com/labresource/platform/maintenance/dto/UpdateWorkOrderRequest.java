
package com.labresource.platform.maintenance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateWorkOrderRequest {

    @Size(max = 2000, message = "Remarks cannot exceed 2000 characters")
    private String remarks;

    @DecimalMin(value = "0.0", message = "Actual cost cannot be negative")
    private BigDecimal actualCost;

    private LocalDateTime expectedEnd;

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public LocalDateTime getExpectedEnd() {
        return expectedEnd;
    }

    public void setExpectedEnd(LocalDateTime expectedEnd) {
        this.expectedEnd = expectedEnd;
    }
}

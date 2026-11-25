
package com.labresource.platform.budget.dto;

import java.math.BigDecimal;

public class BudgetResponse {

    private Long departmentId;
    private String departmentName;
    private Integer fiscalYear;
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercent;
    private String status;

    public BudgetResponse(
            Long departmentId,
            String departmentName,
            Integer fiscalYear,
            BigDecimal budgetAmount,
            BigDecimal usedAmount,
            BigDecimal remainingAmount,
            BigDecimal utilizationPercent,
            String status) {

        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.fiscalYear = fiscalYear;
        this.budgetAmount = budgetAmount;
        this.usedAmount = usedAmount;
        this.remainingAmount = remainingAmount;
        this.utilizationPercent = utilizationPercent;
        this.status = status;
    }

    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Integer getFiscalYear() { return fiscalYear; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public BigDecimal getUsedAmount() { return usedAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public BigDecimal getUtilizationPercent() { return utilizationPercent; }
    public String getStatus() { return status; }
}

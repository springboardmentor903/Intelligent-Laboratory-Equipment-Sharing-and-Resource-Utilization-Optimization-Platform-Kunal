
package com.labresource.platform.budget.dto;

import java.math.BigDecimal;

public class BudgetRequest {

    private Long departmentId;
    private Integer fiscalYear;
    private BigDecimal budgetAmount;

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }
}

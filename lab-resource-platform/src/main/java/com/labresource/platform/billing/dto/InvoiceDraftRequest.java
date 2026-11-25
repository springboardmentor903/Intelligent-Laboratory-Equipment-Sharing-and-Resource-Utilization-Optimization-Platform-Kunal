
package com.labresource.platform.billing.dto;

import com.labresource.platform.billing.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceDraftRequest {

    private InvoiceType invoiceType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal taxRate;
    private Integer dueDays;

    public InvoiceType getInvoiceType() { return invoiceType; }
    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public Integer getDueDays() { return dueDays; }
    public void setDueDays(Integer dueDays) {
        this.dueDays = dueDays;
    }
}

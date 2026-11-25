
package com.labresource.platform.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialSummaryResponse {

    private Long institutionId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private BigDecimal issuedInvoiced;
    private BigDecimal issuedPaid;
    private BigDecimal issuedOutstanding;
    private BigDecimal issuedOverdue;

    private BigDecimal receivedInvoiced;
    private BigDecimal receivedPaid;
    private BigDecimal receivedOutstanding;
    private BigDecimal receivedOverdue;

    private BigDecimal pendingUninvoicedCharges;

    public FinancialSummaryResponse(Long institutionId,
                                    LocalDate periodStart,
                                    LocalDate periodEnd) {
        this.institutionId = institutionId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;

        this.issuedInvoiced = BigDecimal.ZERO;
        this.issuedPaid = BigDecimal.ZERO;
        this.issuedOutstanding = BigDecimal.ZERO;
        this.issuedOverdue = BigDecimal.ZERO;

        this.receivedInvoiced = BigDecimal.ZERO;
        this.receivedPaid = BigDecimal.ZERO;
        this.receivedOutstanding = BigDecimal.ZERO;
        this.receivedOverdue = BigDecimal.ZERO;

        this.pendingUninvoicedCharges = BigDecimal.ZERO;
    }

    public Long getInstitutionId() { return institutionId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }

    public BigDecimal getIssuedInvoiced() { return issuedInvoiced; }
    public void setIssuedInvoiced(BigDecimal value) { this.issuedInvoiced = value; }

    public BigDecimal getIssuedPaid() { return issuedPaid; }
    public void setIssuedPaid(BigDecimal value) { this.issuedPaid = value; }

    public BigDecimal getIssuedOutstanding() { return issuedOutstanding; }
    public void setIssuedOutstanding(BigDecimal value) { this.issuedOutstanding = value; }

    public BigDecimal getIssuedOverdue() { return issuedOverdue; }
    public void setIssuedOverdue(BigDecimal value) { this.issuedOverdue = value; }

    public BigDecimal getReceivedInvoiced() { return receivedInvoiced; }
    public void setReceivedInvoiced(BigDecimal value) { this.receivedInvoiced = value; }

    public BigDecimal getReceivedPaid() { return receivedPaid; }
    public void setReceivedPaid(BigDecimal value) { this.receivedPaid = value; }

    public BigDecimal getReceivedOutstanding() { return receivedOutstanding; }
    public void setReceivedOutstanding(BigDecimal value) { this.receivedOutstanding = value; }

    public BigDecimal getReceivedOverdue() { return receivedOverdue; }
    public void setReceivedOverdue(BigDecimal value) { this.receivedOverdue = value; }

    public BigDecimal getPendingUninvoicedCharges() {
        return pendingUninvoicedCharges;
    }

    public void setPendingUninvoicedCharges(BigDecimal value) {
        this.pendingUninvoicedCharges = value;
    }
}

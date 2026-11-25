
package com.labresource.platform.billing.dto;

import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.usage.entity.UsageCharge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private String invoiceType;
    private String status;
    private Long issuerInstitutionId;
    private Long recipientInstitutionId;
    private Long recipientDepartmentId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant issuedAt;
    private Instant paidAt;
    private boolean overdue;
    private List<ChargeItem> charges;

    public InvoiceResponse(Invoice invoice) {
        this.id = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.invoiceType = invoice.getInvoiceType().name();
        this.status = invoice.getStatus().name();
        this.issuerInstitutionId = invoice.getIssuerInstitutionId();
        this.recipientInstitutionId = invoice.getRecipientInstitutionId();
        this.recipientDepartmentId = invoice.getRecipientDepartmentId();
        this.periodStart = invoice.getPeriodStart();
        this.periodEnd = invoice.getPeriodEnd();
        this.subtotal = invoice.getSubtotal();
        this.taxRate = invoice.getTaxRate();
        this.taxAmount = invoice.getTaxAmount();
        this.totalAmount = invoice.getTotalAmount();
        this.currency = invoice.getCurrency();
        this.dueDate = invoice.getDueDate();
        this.createdAt = invoice.getCreatedAt();
        this.issuedAt = invoice.getIssuedAt();
        this.paidAt = invoice.getPaidAt();
        this.overdue = invoice.isOverdue();

        this.charges = invoice.getCharges().stream()
                .map(ChargeItem::new)
                .toList();
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getInvoiceType() { return invoiceType; }
    public String getStatus() { return status; }
    public Long getIssuerInstitutionId() { return issuerInstitutionId; }
    public Long getRecipientInstitutionId() { return recipientInstitutionId; }
    public Long getRecipientDepartmentId() { return recipientDepartmentId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getPaidAt() { return paidAt; }
    public boolean isOverdue() { return overdue; }
    public List<ChargeItem> getCharges() { return charges; }

    public static class ChargeItem {

        private Long chargeId;
        private Long bookingId;
        private Long resourceId;
        private String equipmentName;
        private Long usageMinutes;
        private BigDecimal amount;
        private String chargeType;
        private String status;

        public ChargeItem(UsageCharge charge) {
            this.chargeId = charge.getId();
            this.bookingId = charge.getBooking().getId();
            this.resourceId = charge.getResource().getId();
            this.equipmentName = charge.getResource().getName();
            this.usageMinutes = charge.getUsageMinutes();
            this.amount = charge.getAmount();
            this.chargeType = charge.getChargeType().name();
            this.status = charge.getChargeStatus();
        }

        public Long getChargeId() { return chargeId; }
        public Long getBookingId() { return bookingId; }
        public Long getResourceId() { return resourceId; }
        public String getEquipmentName() { return equipmentName; }
        public Long getUsageMinutes() { return usageMinutes; }
        public BigDecimal getAmount() { return amount; }
        public String getChargeType() { return chargeType; }
        public String getStatus() { return status; }
    }
}

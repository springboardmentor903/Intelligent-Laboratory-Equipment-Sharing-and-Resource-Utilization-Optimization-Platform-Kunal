
package com.labresource.platform.billing.entity;

import com.labresource.platform.billing.enums.InvoiceStatus;
import com.labresource.platform.billing.enums.InvoiceType;
import com.labresource.platform.usage.entity.UsageCharge;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoice_number",
                        columnNames = "invoice_number"
                )
        }
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 40)
    private InvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issuer_institution_id", nullable = false)
    private Long issuerInstitutionId;

    @Column(name = "recipient_institution_id")
    private Long recipientInstitutionId;

    @Column(name = "recipient_department_id")
    private Long recipientDepartmentId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "invoice_usage_charges",
            joinColumns = @JoinColumn(name = "invoice_id"),
            inverseJoinColumns = @JoinColumn(name = "usage_charge_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_invoice_usage_charge",
                    columnNames = "usage_charge_id"
            )
    )
    private List<UsageCharge> charges = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = InvoiceStatus.DRAFT;
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    public Long getId() { return id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public InvoiceType getInvoiceType() { return invoiceType; }
    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Long getIssuerInstitutionId() { return issuerInstitutionId; }
    public void setIssuerInstitutionId(Long issuerInstitutionId) {
        this.issuerInstitutionId = issuerInstitutionId;
    }

    public Long getRecipientInstitutionId() { return recipientInstitutionId; }
    public void setRecipientInstitutionId(Long recipientInstitutionId) {
        this.recipientInstitutionId = recipientInstitutionId;
    }

    public Long getRecipientDepartmentId() { return recipientDepartmentId; }
    public void setRecipientDepartmentId(Long recipientDepartmentId) {
        this.recipientDepartmentId = recipientDepartmentId;
    }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<UsageCharge> getCharges() { return charges; }
    public void setCharges(List<UsageCharge> charges) {
        this.charges = charges;
    }

    public boolean isOverdue() {
        return status == InvoiceStatus.ISSUED
                && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }
}

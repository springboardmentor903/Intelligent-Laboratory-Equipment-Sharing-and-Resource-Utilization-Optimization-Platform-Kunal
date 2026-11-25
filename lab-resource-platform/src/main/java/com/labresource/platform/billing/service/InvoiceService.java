
package com.labresource.platform.billing.service;

import com.labresource.platform.billing.dto.InvoiceDraftRequest;
import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.billing.enums.InvoiceStatus;
import com.labresource.platform.billing.enums.InvoiceType;
import com.labresource.platform.billing.repository.InvoiceRepository;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UsageChargeRepository usageChargeRepository;

    @Value("${app.billing.default-tax-rate:0.00}")
    private BigDecimal defaultTaxRate;

    @Value("${app.billing.default-due-days:30}")
    private int defaultDueDays;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            UsageChargeRepository usageChargeRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.usageChargeRepository = usageChargeRepository;
    }

    // Generate draft invoices
    @Transactional
    public List<Invoice> generateDrafts(
            Long institutionId,
            Long createdByUserId,
            InvoiceDraftRequest request
    ) {
        validateRequest(request);

        LocalDate start = request.getPeriodStart();
        LocalDate end = request.getPeriodEnd();

        Instant from = start.atStartOfDay(
                ZoneId.systemDefault()
        ).toInstant();

        Instant to = end.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        List<UsageCharge> charges =
                usageChargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId,
                                from,
                                to
                        );

        InvoiceType type = request.getInvoiceType();

        List<UsageCharge> eligible = charges.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getChargeStatus()))
                .filter(c -> c.getAmount() != null
                        && c.getAmount().signum() > 0)
                .filter(c -> !invoiceRepository.existsByCharges_Id(c.getId()))
                .filter(c -> type == InvoiceType.EXTERNAL
                        ? c.getChargeType() == UsageCharge.ChargeType.EXTERNAL
                          && c.getUserInstitutionId() != null
                        : c.getChargeType() == UsageCharge.ChargeType.INTERNAL
                          && c.getDepartmentId() != null)
                .toList();

        Map<Long, List<UsageCharge>> grouped =
                eligible.stream().collect(
                        Collectors.groupingBy(
                                c -> type == InvoiceType.EXTERNAL
                                        ? c.getUserInstitutionId()
                                        : c.getDepartmentId()
                        )
                );

        List<Invoice> generated = new ArrayList<>();

        for (Map.Entry<Long, List<UsageCharge>> entry
                : grouped.entrySet()) {

            Long recipientId = entry.getKey();
            List<UsageCharge> groupCharges = entry.getValue();

            boolean alreadyGenerated = type == InvoiceType.EXTERNAL
                    ? invoiceRepository
                      .existsByIssuerInstitutionIdAndInvoiceTypeAndRecipientInstitutionIdAndPeriodStartAndPeriodEnd(
                              institutionId,
                              type,
                              recipientId,
                              start,
                              end
                      )
                    : invoiceRepository
                      .existsByIssuerInstitutionIdAndInvoiceTypeAndRecipientDepartmentIdAndPeriodStartAndPeriodEnd(
                              institutionId,
                              type,
                              recipientId,
                              start,
                              end
                      );

            if (alreadyGenerated) {
                continue;
            }

            BigDecimal subtotal = groupCharges.stream()
                    .map(UsageCharge::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal taxRate = request.getTaxRate() != null
                    ? request.getTaxRate()
                    : defaultTaxRate;

            int dueDays = request.getDueDays() != null
                    ? request.getDueDays()
                    : defaultDueDays;

            if (taxRate.signum() < 0 || dueDays < 0) {
                throw new IllegalArgumentException(
                        "Tax rate and due days cannot be negative"
                );
            }

            BigDecimal taxAmount = subtotal.multiply(taxRate)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );

            Invoice invoice = new Invoice();

            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setInvoiceType(type);
            invoice.setStatus(InvoiceStatus.DRAFT);
            invoice.setIssuerInstitutionId(institutionId);

            if (type == InvoiceType.EXTERNAL) {
                invoice.setRecipientInstitutionId(recipientId);
            } else {
                invoice.setRecipientDepartmentId(recipientId);
            }

            invoice.setPeriodStart(start);
            invoice.setPeriodEnd(end);
            invoice.setSubtotal(subtotal);
            invoice.setTaxRate(taxRate);
            invoice.setTaxAmount(taxAmount);
            invoice.setTotalAmount(subtotal.add(taxAmount));
            invoice.setDueDate(LocalDate.now().plusDays(dueDays));
            invoice.setCreatedByUserId(createdByUserId);
            invoice.setCharges(new ArrayList<>(groupCharges));

            Invoice saved = invoiceRepository.save(invoice);

            // Initialize lazy collection before transaction closes
            Hibernate.initialize(saved.getCharges());

            for (UsageCharge charge : saved.getCharges()) {
                Hibernate.initialize(charge.getResource());
                Hibernate.initialize(charge.getBooking());
            }

            // Update linked charge status
            groupCharges.forEach(
                    charge -> charge.setChargeStatus("INVOICED")
            );

            generated.add(saved);
        }

        return generated;
    }

    // Issued invoices
    @Transactional(readOnly = true)
    public Page<Invoice> getIssuedInvoices(
            Long institutionId,
            Pageable pageable
    ) {
        return invoiceRepository.findByIssuerInstitutionId(
                institutionId,
                pageable
        );
    }

    // Received invoices
    @Transactional(readOnly = true)
    public Page<Invoice> getReceivedInvoices(
            Long institutionId,
            Pageable pageable
    ) {
        return invoiceRepository.findByRecipientInstitutionId(
                institutionId,
                pageable
        );
    }

    // Department invoices
    @Transactional(readOnly = true)
    public Page<Invoice> getDepartmentInvoices(
            Long departmentId,
            Pageable pageable
    ) {
        return invoiceRepository.findByRecipientDepartmentId(
                departmentId,
                pageable
        );
    }

    // Invoice details
    @Transactional(readOnly = true)
    public Invoice getInvoiceDetails(Long invoiceId) {
        Invoice invoice = invoiceRepository.findDetailedById(invoiceId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invoice not found"
                        )
                );

        initializeInvoiceCharges(invoice);

        return invoice;
    }

    // Issue invoice
    @Transactional
    public Invoice issueInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceForUpdate(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be issued"
            );
        }

        if (invoice.getCharges() == null
                || invoice.getCharges().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot issue an empty invoice"
            );
        }

        invoice.getCharges().forEach(charge -> {
            if (!"INVOICED".equalsIgnoreCase(
                    charge.getChargeStatus())) {
                throw new IllegalStateException(
                        "A linked charge is no longer reserved for this invoice"
                );
            }
        });

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        return invoiceRepository.save(invoice);
    }

    // Mark invoice paid
    @Transactional
    public Invoice markPaid(Long invoiceId) {
        Invoice invoice = getInvoiceForUpdate(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException(
                    "Only ISSUED invoices can be marked paid"
            );
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());

        invoice.getCharges().forEach(
                charge -> charge.setChargeStatus("PAID")
        );

        return invoiceRepository.save(invoice);
    }

    // Cancel invoice
    @Transactional
    public Invoice cancelInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceForUpdate(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException(
                    "Only DRAFT or ISSUED invoices can be cancelled"
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledAt(Instant.now());

        invoice.getCharges().forEach(charge -> {
            if ("INVOICED".equalsIgnoreCase(
                    charge.getChargeStatus())) {
                charge.setChargeStatus("PENDING");
            }
        });

        return invoiceRepository.save(invoice);
    }

    private Invoice getInvoiceForUpdate(Long invoiceId) {
        Invoice invoice = invoiceRepository.findDetailedById(invoiceId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invoice not found"
                        )
                );

        initializeInvoiceCharges(invoice);

        return invoice;
    }

    private void initializeInvoiceCharges(Invoice invoice) {
        Hibernate.initialize(invoice.getCharges());

        for (UsageCharge charge : invoice.getCharges()) {
            Hibernate.initialize(charge.getResource());
            Hibernate.initialize(charge.getBooking());
        }
    }

    private String generateInvoiceNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "INV-" + year + "-";

        long next =
                invoiceRepository.countByInvoiceNumberStartingWith(prefix) + 1;

        return prefix + String.format("%06d", next);
    }

    private void validateRequest(InvoiceDraftRequest request) {
        if (request == null
                || request.getInvoiceType() == null
                || request.getPeriodStart() == null
                || request.getPeriodEnd() == null) {
            throw new IllegalArgumentException(
                    "Invoice type, period start and period end are required"
            );
        }

        if (request.getPeriodEnd().isBefore(
                request.getPeriodStart())) {
            throw new IllegalArgumentException(
                    "Period end cannot be before period start"
            );
        }
    }
}

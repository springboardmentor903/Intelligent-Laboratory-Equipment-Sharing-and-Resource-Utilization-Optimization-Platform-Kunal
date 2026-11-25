
package com.labresource.platform.billing.service;

import com.labresource.platform.billing.dto.FinancialSummaryResponse;
import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.billing.enums.InvoiceStatus;
import com.labresource.platform.billing.repository.InvoiceRepository;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class FinancialSummaryService {

    private final InvoiceRepository invoiceRepository;
    private final UsageChargeRepository usageChargeRepository;

    public FinancialSummaryService(
            InvoiceRepository invoiceRepository,
            UsageChargeRepository usageChargeRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.usageChargeRepository = usageChargeRepository;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse getSummary(
            Long institutionId,
            LocalDate start,
            LocalDate end
    ) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid financial summary period");
        }

        FinancialSummaryResponse response =
                new FinancialSummaryResponse(institutionId, start, end);

        List<Invoice> issued = invoiceRepository
                .findByIssuerInstitutionId(institutionId)
                .stream()
                .filter(i -> inPeriod(i, start, end))
                .toList();

        List<Invoice> received = invoiceRepository
                .findByRecipientInstitutionId(institutionId)
                .stream()
                .filter(i -> inPeriod(i, start, end))
                .toList();

        BigDecimal issuedInvoiced = BigDecimal.ZERO;
        BigDecimal issuedPaid = BigDecimal.ZERO;
        BigDecimal issuedOutstanding = BigDecimal.ZERO;
        BigDecimal issuedOverdue = BigDecimal.ZERO;

        for (Invoice invoice : issued) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED
                    || invoice.getStatus() == InvoiceStatus.DRAFT) {
                continue;
            }

            BigDecimal amount = safe(invoice.getTotalAmount());
            issuedInvoiced = issuedInvoiced.add(amount);

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                issuedPaid = issuedPaid.add(amount);
            } else if (invoice.getStatus() == InvoiceStatus.ISSUED) {
                issuedOutstanding = issuedOutstanding.add(amount);

                if (invoice.isOverdue()) {
                    issuedOverdue = issuedOverdue.add(amount);
                }
            }
        }

        BigDecimal receivedInvoiced = BigDecimal.ZERO;
        BigDecimal receivedPaid = BigDecimal.ZERO;
        BigDecimal receivedOutstanding = BigDecimal.ZERO;
        BigDecimal receivedOverdue = BigDecimal.ZERO;

        for (Invoice invoice : received) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED
                    || invoice.getStatus() == InvoiceStatus.DRAFT) {
                continue;
            }

            BigDecimal amount = safe(invoice.getTotalAmount());
            receivedInvoiced = receivedInvoiced.add(amount);

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                receivedPaid = receivedPaid.add(amount);
            } else if (invoice.getStatus() == InvoiceStatus.ISSUED) {
                receivedOutstanding = receivedOutstanding.add(amount);

                if (invoice.isOverdue()) {
                    receivedOverdue = receivedOverdue.add(amount);
                }
            }
        }

        Instant from = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = end.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<UsageCharge> charges =
                usageChargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId, from, to
                        );

        BigDecimal pending = charges.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getChargeStatus()))
                .filter(c -> c.getChargeType() != UsageCharge.ChargeType.UNASSIGNED)
                .map(UsageCharge::getAmount)
                .map(FinancialSummaryService::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setIssuedInvoiced(issuedInvoiced);
        response.setIssuedPaid(issuedPaid);
        response.setIssuedOutstanding(issuedOutstanding);
        response.setIssuedOverdue(issuedOverdue);

        response.setReceivedInvoiced(receivedInvoiced);
        response.setReceivedPaid(receivedPaid);
        response.setReceivedOutstanding(receivedOutstanding);
        response.setReceivedOverdue(receivedOverdue);

        response.setPendingUninvoicedCharges(pending);

        return response;
    }

    private boolean inPeriod(Invoice invoice, LocalDate start, LocalDate end) {
        return invoice.getPeriodStart() != null
                && invoice.getPeriodEnd() != null
                && !invoice.getPeriodStart().isBefore(start)
                && !invoice.getPeriodEnd().isAfter(end);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}


package com.labresource.platform.billing.service;

import com.labresource.platform.billing.entity.BillingRecord;
import com.labresource.platform.billing.repository.BillingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingRecordService {

    private final BillingRecordRepository billingRepository;

    public BillingRecordService(BillingRecordRepository billingRepository) {
        this.billingRepository = billingRepository;
    }

    @Transactional(readOnly = true)
    public List<BillingRecord> getAllRecords() {
        return billingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BillingRecord getRecordById(Long id) {
        return billingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Billing record not found: " + id));
    }

    @Transactional
    public BillingRecord createRecord(
            Long userId, BigDecimal amount, String currency,
            String paymentReference) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid user ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        String selectedCurrency = (currency == null || currency.isBlank())
                ? "INR" : currency.trim().toUpperCase();

        if (!List.of("INR", "USD", "EUR").contains(selectedCurrency)) {
            throw new IllegalArgumentException("Unsupported currency");
        }

        BillingRecord record = new BillingRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setCurrency(selectedCurrency);
        record.setStatus("PENDING");
        record.setPaymentReference(paymentReference);

        return billingRepository.save(record);
    }

    @Transactional
    public BillingRecord updatePaymentStatus(Long id, String newStatus) {
        BillingRecord record = getRecordById(id);

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Payment status is required");
        }

        String status = newStatus.trim().toUpperCase();

        if (!List.of("PENDING", "PAID", "FAILED", "REFUNDED", "CANCELLED")
                .contains(status)) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }

        record.setStatus(status);

        if ("PAID".equals(status)) {
            record.setPaidAt(LocalDateTime.now());
        }

        return billingRepository.save(record);
    }
}

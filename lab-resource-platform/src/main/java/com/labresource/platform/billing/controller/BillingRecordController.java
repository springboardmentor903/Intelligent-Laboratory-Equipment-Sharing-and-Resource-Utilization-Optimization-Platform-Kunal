
package com.labresource.platform.billing.controller;

import com.labresource.platform.billing.entity.BillingRecord;
import com.labresource.platform.billing.service.BillingRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingRecordController {

    private final BillingRecordService billingService;

    public BillingRecordController(BillingRecordService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public List<BillingRecord> getAllRecords() {
        return billingService.getAllRecords();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public BillingRecord getRecordById(@PathVariable Long id) {
        return billingService.getRecordById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public BillingRecord createRecord(@RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(body.get("userId"));
        BigDecimal amount = new BigDecimal(body.get("amount"));

        return billingService.createRecord(
                userId,
                amount,
                body.get("currency"),
                body.get("paymentReference")
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<BillingRecord> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        BillingRecord updated = billingService.updatePaymentStatus(
                id, body.get("status"));

        return ResponseEntity.ok(updated);
    }
}

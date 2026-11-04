
package com.labresource.platform.billing.repository;

import com.labresource.platform.billing.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRecordRepository
        extends JpaRepository<BillingRecord, Long> {

    List<BillingRecord> findByUserId(Long userId);

    List<BillingRecord> findByStatus(String status);

    List<BillingRecord> findByBookingId(Long bookingId);
}

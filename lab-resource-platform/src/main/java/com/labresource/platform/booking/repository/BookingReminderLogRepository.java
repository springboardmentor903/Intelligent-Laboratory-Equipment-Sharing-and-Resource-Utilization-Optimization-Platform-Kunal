package com.labresource.platform.booking.repository;

import com.labresource.platform.booking.entity.BookingReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingReminderLogRepository extends JpaRepository<BookingReminderLog, Long> {
    boolean existsByBookingId(Long bookingId);
}

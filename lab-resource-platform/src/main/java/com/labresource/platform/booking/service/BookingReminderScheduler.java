package com.labresource.platform.booking.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.BookingReminderLog;
import com.labresource.platform.booking.repository.BookingReminderLogRepository;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(BookingReminderScheduler.class);
    private final BookingRepository bookingRepository;
    private final BookingReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;

    public BookingReminderScheduler(BookingRepository bookingRepository,
                                    BookingReminderLogRepository reminderLogRepository,
                                    NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.notificationService = notificationService;
    }

    // Runs every 5 minutes; each booking receives at most one reminder.
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void sendUpcomingBookingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusMinutes(60);
        List<Booking> bookings = bookingRepository.findAllWithDetails();
        for (Booking booking : bookings) {
            try {
                if (booking.getStartTime() == null || booking.getStartTime().isBefore(now)
                        || booking.getStartTime().isAfter(limit)) continue;
                String status = booking.getStatus() == null ? "" : booking.getStatus().toUpperCase();
                if (!List.of("APPROVED", "CONFIRMED").contains(status)) continue;
                if (reminderLogRepository.existsByBookingId(booking.getId())) continue;
                Long userId = booking.getUser() == null ? null : booking.getUser().getId();
                if (userId == null) continue;
                notificationService.createNotification(userId,
                        "Booking reminder",
                        "Your booking for " + booking.getResource().getName()
                                + " starts at " + booking.getStartTime() + ".",
                        "BOOKING_REMINDER", "BOOKING", booking.getId());
                reminderLogRepository.save(new BookingReminderLog(booking.getId()));
            } catch (Exception ex) {
                log.error("Could not create reminder for booking {}", booking.getId(), ex);
            }
        }
    }
}

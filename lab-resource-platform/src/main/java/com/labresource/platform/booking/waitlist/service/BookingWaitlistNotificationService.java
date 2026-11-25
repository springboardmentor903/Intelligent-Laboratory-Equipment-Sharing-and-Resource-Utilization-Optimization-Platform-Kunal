package com.labresource.platform.booking.waitlist.service;

import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.notification.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class BookingWaitlistNotificationService {
    private final NotificationService notificationService;
    public BookingWaitlistNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    public void notifyOfferCreated(BookingWaitlist entry) {
        if (entry == null || entry.getUser() == null || entry.getUser().getId() == null) return;
        notificationService.createNotification(entry.getUser().getId(),
                "Equipment booking slot available",
                "A slot is now available for " + entry.getResource().getName()
                        + " from " + entry.getRequestedStart() + " to " + entry.getRequestedEnd()
                        + ". Please open your waitlist to respond to the offer.",
                "WAITLIST_OFFER", "WAITLIST", entry.getId());
    }
}

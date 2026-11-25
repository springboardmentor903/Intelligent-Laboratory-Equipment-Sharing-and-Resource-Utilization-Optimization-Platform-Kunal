
package com.labresource.platform.maintenance.service;

import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.booking.waitlist.repository.BookingWaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceWaitlistService {

    private final BookingWaitlistRepository waitlistRepository;

    public MaintenanceWaitlistService(
            BookingWaitlistRepository waitlistRepository
    ) {
        this.waitlistRepository = waitlistRepository;
    }

    /**
     * Expires all active waitlist offers overlapping
     * with the equipment maintenance window.
     */
    @Transactional
    public int expireOffersForMaintenance(
            Long resourceId,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        if (resourceId == null
                || maintenanceStart == null
                || maintenanceEnd == null
                || !maintenanceEnd.isAfter(maintenanceStart)) {
            throw new IllegalArgumentException(
                    "Valid equipment and maintenance time range are required"
            );
        }

        List<BookingWaitlist> overlappingOffers =
                waitlistRepository.findOverlappingOffersForMaintenance(
                        resourceId,
                        maintenanceStart,
                        maintenanceEnd
                );

        if (overlappingOffers.isEmpty()) {
            return 0;
        }

        for (BookingWaitlist offer : overlappingOffers) {
            offer.setStatus("EXPIRED");
        }

        waitlistRepository.saveAll(overlappingOffers);

        return overlappingOffers.size();
    }
}

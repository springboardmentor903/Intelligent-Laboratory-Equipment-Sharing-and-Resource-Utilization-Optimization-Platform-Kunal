package com.labresource.platform.booking.recurring.controller;

import com.labresource.platform.booking.dto.BookingResponse;
import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.recurring.dto.RecurringBookingRequest;
import com.labresource.platform.booking.service.BookingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/bookings/recurring")
public class RecurringBookingController {

    private final BookingService bookingService;

    public RecurringBookingController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT','DEPARTMENT_HEAD')")
    @Transactional
    public Map<String, Object> create(
            Authentication auth,
            @RequestBody RecurringBookingRequest request
    ) {
        if (request.getResourceId() == null
                || request.getStartTime() == null
                || request.getEndTime() == null) {

            throw new IllegalArgumentException(
                    "resourceId, startTime and endTime are required"
            );
        }

        String frequency = request.getFrequency() == null
                ? "WEEKLY"
                : request.getFrequency()
                  .trim()
                  .toUpperCase(Locale.ROOT);

        if (!List.of("DAILY", "WEEKLY").contains(frequency)) {
            throw new IllegalArgumentException(
                    "frequency must be DAILY or WEEKLY"
            );
        }

        int interval = request.getInterval() == null
                ? 1
                : request.getInterval();

        int occurrences = request.getOccurrences() == null
                ? 4
                : request.getOccurrences();

        if (interval < 1 || interval > 4) {
            throw new IllegalArgumentException(
                    "interval must be between 1 and 4"
            );
        }

        if (occurrences < 2 || occurrences > 12) {
            throw new IllegalArgumentException(
                    "occurrences must be between 2 and 12"
            );
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException(
                    "endTime must be after startTime"
            );
        }

        long durationMinutes =
                java.time.Duration.between(
                        request.getStartTime(),
                        request.getEndTime()
                ).toMinutes();

        List<BookingResponse> created = new ArrayList<>();

        for (int i = 0; i < occurrences; i++) {

            LocalDateTime start = frequency.equals("DAILY")
                    ? request.getStartTime().plusDays((long) interval * i)
                    : request.getStartTime().plusWeeks((long) interval * i);

            Booking booking = new Booking();

            booking.setStartTime(start);
            booking.setEndTime(
                    start.plusMinutes(durationMinutes)
            );

            created.add(
                    new BookingResponse(
                            bookingService.createBooking(
                                    auth.getName(),
                                    request.getResourceId(),
                                    booking
                            )
                    )
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("frequency", frequency);
        result.put("interval", interval);
        result.put("requestedOccurrences", occurrences);
        result.put("createdCount", created.size());
        result.put("bookings", created);

        return result;
    }
}
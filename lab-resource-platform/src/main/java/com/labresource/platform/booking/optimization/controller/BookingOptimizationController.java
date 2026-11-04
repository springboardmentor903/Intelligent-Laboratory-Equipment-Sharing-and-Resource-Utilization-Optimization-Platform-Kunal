package com.labresource.platform.booking.optimization.controller;

import com.labresource.platform.booking.service.BookingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/bookings/optimization")
public class BookingOptimizationController {

    private final BookingService bookingService;

    public BookingOptimizationController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT','DEPARTMENT_HEAD')")
    public List<Map<String, Object>> suggestions(
            @RequestParam Long resourceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime preferredStart,
            @RequestParam(defaultValue = "60")
            int durationMinutes
    ) {
        if (durationMinutes < 15 || durationMinutes > 480) {
            throw new IllegalArgumentException(
                    "durationMinutes must be between 15 and 480"
            );
        }

        if (preferredStart == null
                || !preferredStart.isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "preferredStart must be in the future"
            );
        }

        List<Map<String, Object>> results = new ArrayList<>();

        LocalDateTime day =
                preferredStart.toLocalDate().atStartOfDay();

        for (int d = 0; d < 7 && results.size() < 10; d++) {

            LocalDateTime date = day.plusDays(d);

            for (int hour = 8;
                 hour < 20 && results.size() < 10;
                 hour++) {

                LocalDateTime start = date.withHour(hour);
                LocalDateTime end =
                        start.plusMinutes(durationMinutes);

                if (!start.isAfter(LocalDateTime.now())) {
                    continue;
                }

                int available;

                try {
                    available =
                            bookingService.getAvailableQuantity(
                                    resourceId,
                                    start,
                                    end
                            );
                } catch (IllegalArgumentException ex) {
                    continue;
                }

                if (available > 0) {

                    Map<String, Object> slot =
                            new LinkedHashMap<>();

                    slot.put("startTime", start);
                    slot.put("endTime", end);
                    slot.put("availableQuantity", available);
                    slot.put(
                            "preferred",
                            start.equals(preferredStart)
                    );

                    slot.put(
                            "reason",
                            d == 0
                                    ? "Available later today"
                                    : "Alternative available slot"
                    );

                    results.add(slot);
                }
            }
        }

        return results;
    }
}
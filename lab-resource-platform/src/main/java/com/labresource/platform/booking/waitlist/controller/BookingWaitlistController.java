
package com.labresource.platform.booking.waitlist.controller;

import com.labresource.platform.booking.waitlist.service.BookingWaitlistService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/bookings/waitlist")
public class BookingWaitlistController {

    private final BookingWaitlistService waitlistService;

    public BookingWaitlistController(
            BookingWaitlistService waitlistService
    ) {
        this.waitlistService = waitlistService;
    }

    // Student: Join waitlist
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public Map<String, Object> joinWaitlist(
            Authentication authentication,
            @RequestParam Long resourceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime
    ) {
        return waitlistService.join(
                authentication.getName(),
                resourceId,
                startTime,
                endTime
        );
    }

    // Student: My waitlist
    @GetMapping("/my")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public List<Map<String, Object>> getMyWaitlist(
            Authentication authentication
    ) {
        return waitlistService.myWaitlist(
                authentication.getName()
        );
    }

    // Manager: Resource queue
    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('LAB_MANAGER','INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public List<Map<String, Object>> getResourceWaitlist(
            @PathVariable Long resourceId,
            Authentication authentication
    ) {
        return waitlistService.resourceWaitlist(
                resourceId,
                authentication.getName()
        );
    }

    // Student: Cancel own request
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public void cancelWaitlist(
            @PathVariable Long id,
            Authentication authentication
    ) {
        waitlistService.cancel(
                id,
                authentication.getName()
        );
    }

    // Manager: Offer next request. No date/time parameters needed.
    @PostMapping("/offer-next")
    @PreAuthorize("hasAnyRole('LAB_MANAGER','INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public Map<String, Object> offerNext(
            @RequestParam Long resourceId,
            Authentication authentication
    ) {
        return waitlistService.offerNext(
                resourceId,
                authentication.getName()
        );
    }
}


package com.labresource.platform.booking.controller;

import com.labresource.platform.booking.dto.BookingHistoryResponse;
import com.labresource.platform.booking.dto.BookingResponse;
import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.repository.BookingStatusHistoryRepository;
import com.labresource.platform.booking.service.BookingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingStatusHistoryRepository historyRepository;

    public BookingController(
            BookingService bookingService,
            BookingStatusHistoryRepository historyRepository) {

        this.bookingService = bookingService;
        this.historyRepository = historyRepository;
    }

    // Check availability
    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'LAB_TECHNICIAN', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public Map<String, Object> checkAvailability(
            @RequestParam Long resourceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {

        int availableQuantity = bookingService.getAvailableQuantity(
                resourceId, startTime, endTime);

        return Map.of(
                "resourceId", resourceId,
                "startTime", startTime,
                "endTime", endTime,
                "availableQuantity", availableQuantity,
                "available", availableQuantity > 0
        );
    }

    // Student creates internal booking
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public BookingResponse createBooking(
            Authentication authentication,
            @RequestParam Long resourceId,
            @RequestBody Booking booking) {

        Booking savedBooking = bookingService.createBooking(
                authentication.getName(), resourceId, booking);

        return new BookingResponse(savedBooking);
    }

    // Lab staff: View all bookings
    @GetMapping
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public List<BookingResponse> getAllBookings() {

        return bookingService.getAllBookings()
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    // Institution Admin: View own institution bookings
    @GetMapping("/institution")
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    public List<BookingResponse> getInstitutionBookings(
            Authentication authentication) {

        return bookingService
                .getBookingsForInstitutionAdmin(authentication.getName())
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    // Student views own bookings
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public List<BookingResponse> getMyBookings(
            Authentication authentication) {

        return bookingService.getBookingsByEmail(authentication.getName())
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    // Student check-in
    @PutMapping("/{bookingId}/check-in")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public BookingResponse checkIn(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.checkIn(
                        bookingId, authentication.getName()));
    }

    // Student check-out
    @PutMapping("/{bookingId}/check-out")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public BookingResponse checkOut(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.checkOut(
                        bookingId, authentication.getName()));
    }

    // Staff: Approve booking
    @PutMapping("/{bookingId}/approve")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public BookingResponse approveBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatus(
                        bookingId, "APPROVED", authentication.getName()));
    }

    // Institution Admin: Approve own institution booking
    @PutMapping("/{bookingId}/institution-approve")
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    public BookingResponse institutionApproveBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatusForInstitutionAdmin(
                        bookingId, "APPROVED", authentication.getName()));
    }

    // Staff: Reject booking
    @PutMapping("/{bookingId}/reject")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public BookingResponse rejectBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatus(
                        bookingId, "REJECTED", authentication.getName()));
    }

    // Institution Admin: Reject own institution booking
    @PutMapping("/{bookingId}/institution-reject")
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    public BookingResponse institutionRejectBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatusForInstitutionAdmin(
                        bookingId, "REJECTED", authentication.getName()));
    }

    // Start approved booking - staff operation
    @PutMapping("/{bookingId}/start")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public BookingResponse startBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatus(
                        bookingId, "IN_USE", authentication.getName()));
    }

    // Complete booking - staff operation
    @PutMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public BookingResponse completeBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatus(
                        bookingId, "COMPLETED", authentication.getName()));
    }

    // Mark approved booking as no-show
    @PutMapping("/{bookingId}/no-show")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public BookingResponse markNoShow(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.updateBookingStatus(
                        bookingId, "NO_SHOW", authentication.getName()));
    }

    // Student cancels own booking
    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public BookingResponse cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return new BookingResponse(
                bookingService.cancelBooking(
                        bookingId, authentication.getName()));
    }

    // Filter bookings by status - staff
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public List<BookingResponse> getBookingsByStatus(
            @PathVariable String status) {

        return bookingService.getBookingsByStatus(status)
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    // View booking status history
    @GetMapping("/{bookingId}/history")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'LAB_TECHNICIAN', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public List<BookingHistoryResponse> getBookingHistory(
            @PathVariable Long bookingId,
            Authentication authentication) {

        if (!bookingService.bookingExists(bookingId)) {
            throw new IllegalArgumentException("Booking not found");
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_RESEARCHER_STUDENT"));

        boolean isInstitutionAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_INSTITUTION_ADMIN"));

        if (isStudent) {
            boolean isOwner = bookingService
                    .getBookingsByEmail(authentication.getName())
                    .stream()
                    .anyMatch(b -> b.getId().equals(bookingId));

            if (!isOwner) {
                throw new AccessDeniedException(
                        "You can view history only for your own bookings");
            }
        }

        if (isInstitutionAdmin) {
            boolean isOwnInstitutionBooking = bookingService
                    .getBookingsForInstitutionAdmin(authentication.getName())
                    .stream()
                    .anyMatch(b -> b.getId().equals(bookingId));

            if (!isOwnInstitutionBooking) {
                throw new AccessDeniedException(
                        "You can view history only for your institution bookings");
            }
        }

        return historyRepository.findHistoryWithBooking(bookingId)
                .stream()
                .map(BookingHistoryResponse::new)
                .toList();
    }
}

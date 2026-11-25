
package com.labresource.platform.booking.waitlist.service;

import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.booking.waitlist.repository.BookingWaitlistRepository;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingWaitlistService {

    private final BookingWaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public BookingWaitlistService(
            BookingWaitlistRepository waitlistRepository,
            BookingRepository bookingRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository
    ) {
        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // STUDENT: Join waitlist
    @Transactional
    public Map<String, Object> join(
            String email,
            Long resourceId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        validateTimeRange(start, end);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        validateResourceForWaitlist(resource);

        Integer quantity = resource.getQuantity();

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Equipment quantity is not configured"
            );
        }

        long occupied = bookingRepository.countOverlappingBookings(
                resourceId, start, end
        );

        if (occupied < quantity) {
            throw new IllegalArgumentException(
                    "This slot is available. Please create a normal booking."
            );
        }

        Optional<BookingWaitlist> existing =
                waitlistRepository
                        .findFirstByResourceIdAndUserEmailAndRequestedStartAndRequestedEndAndStatusIn(
                                resourceId,
                                email,
                                start,
                                end,
                                List.of("WAITING", "OFFERED")
                        );

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        BookingWaitlist entry = new BookingWaitlist();
        entry.setResource(resource);
        entry.setUser(user);
        entry.setRequestedStart(start);
        entry.setRequestedEnd(end);
        entry.setStatus("WAITING");

        return toResponse(waitlistRepository.save(entry));
    }

    // STUDENT: Get own waitlist
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myWaitlist(String email) {
        return waitlistRepository
                .findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // MANAGER: Get equipment waitlist
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resourceWaitlist(
            Long resourceId,
            String managerEmail
    ) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        User manager = userRepository.findByEmailWithProfile(managerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Manager not found"));

        checkManagerAccess(manager, resource);

        return waitlistRepository
                .findByResourceIdAndStatusOrderByCreatedAtAsc(
                        resourceId,
                        "WAITING"
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // STUDENT: Cancel own waitlist request
    @Transactional
    public void cancel(Long waitlistId, String email) {
        BookingWaitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Waitlist request not found"
                        ));

        if (!entry.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException(
                    "You can cancel only your own waitlist request"
            );
        }

        if (!"WAITING".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalArgumentException(
                    "Only WAITING requests can be cancelled"
            );
        }

        entry.setStatus("CANCELLED");
        waitlistRepository.save(entry);
    }

    // MANAGER: Offer next oldest waiting request
    @Transactional
    public Map<String, Object> offerNext(
            Long resourceId,
            String managerEmail
    ) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        User manager = userRepository.findByEmailWithProfile(managerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Manager not found"));

        checkManagerAccess(manager, resource);

        // Never send offers for equipment that cannot be booked.
        validateResourceForWaitlist(resource);

        List<BookingWaitlist> queue =
                waitlistRepository
                        .findByResourceIdAndStatusOrderByCreatedAtAsc(
                                resourceId,
                                "WAITING"
                        );

        if (queue.isEmpty()) {
            throw new IllegalArgumentException(
                    "No waiting request found for this equipment"
            );
        }

        BookingWaitlist next = queue.get(0);

        LocalDateTime start = next.getRequestedStart();
        LocalDateTime end = next.getRequestedEnd();

        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "The waitlist request has an invalid time slot"
            );
        }

        if (!start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "The requested slot has already started or expired"
            );
        }

        Integer quantity = resource.getQuantity();

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Equipment quantity is not configured"
            );
        }

        long occupied = bookingRepository.countOverlappingBookings(
                resourceId,
                start,
                end
        );

        if (occupied >= quantity) {
            throw new IllegalArgumentException(
                    "The next user's requested slot is still fully booked"
            );
        }

        next.setStatus("OFFERED");
        next.setOfferedAt(LocalDateTime.now());

        return toResponse(waitlistRepository.save(next));
    }

    // Shared equipment eligibility check
    private void validateResourceForWaitlist(Resource resource) {
        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "This equipment is inactive"
            );
        }

        String resourceStatus = resource.getStatus();

        if (!"AVAILABLE".equalsIgnoreCase(resourceStatus)
                && !"BOOKED".equalsIgnoreCase(resourceStatus)) {
            throw new IllegalArgumentException(
                    "This equipment is not accepting waitlist requests"
            );
        }
    }

    // Role and resource ownership check
    private void checkManagerAccess(User manager, Resource resource) {
        String role = manager.getRole() == null
                ? ""
                : manager.getRole().getName();

        if ("SYSTEM_ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        if ("INSTITUTION_ADMIN".equalsIgnoreCase(role)) {
            Long managerInstitution = manager.getInstitution() == null
                    ? null
                    : manager.getInstitution().getId();

            Long resourceInstitution = resource.getInstitution() == null
                    ? null
                    : resource.getInstitution().getId();

            if (managerInstitution == null
                    || !managerInstitution.equals(resourceInstitution)) {
                throw new AccessDeniedException(
                        "You can access only your institution's equipment"
                );
            }
            return;
        }

        if ("LAB_MANAGER".equalsIgnoreCase(role)) {
            Long managerDepartment = manager.getDepartment() == null
                    ? null
                    : manager.getDepartment().getId();

            Long resourceDepartment = resource.getDepartment() == null
                    ? null
                    : resource.getDepartment().getId();

            if (managerDepartment == null
                    || !managerDepartment.equals(resourceDepartment)) {
                throw new AccessDeniedException(
                        "You can access only your department's equipment"
                );
            }
            return;
        }

        throw new AccessDeniedException(
                "You are not allowed to manage this waitlist"
        );
    }

    // API response
    private Map<String, Object> toResponse(BookingWaitlist entry) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", entry.getId());
        response.put("resourceId", entry.getResource().getId());
        response.put("resourceName", entry.getResource().getName());
        response.put("startTime", entry.getRequestedStart());
        response.put("endTime", entry.getRequestedEnd());
        response.put("status", entry.getStatus());
        response.put("createdAt", entry.getCreatedAt());
        response.put("offeredAt", entry.getOfferedAt());
        response.put("userName", entry.getUser().getFullName());
        response.put("userEmail", entry.getUser().getEmail());

        if ("WAITING".equalsIgnoreCase(entry.getStatus())) {
            long position = waitlistRepository.countPosition(
                    entry.getResource().getId(),
                    entry.getRequestedStart(),
                    entry.getRequestedEnd(),
                    entry.getCreatedAt()
            );
            response.put("position", position);
        } else {
            response.put("position", null);
        }

        return response;
    }

    private void validateTimeRange(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Valid startTime and endTime are required"
            );
        }

        if (!start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Waitlist time must be in the future"
            );
        }
    }
}

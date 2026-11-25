package com.labresource.platform.booking.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.BookingStatusHistory;
import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.booking.repository.BookingStatusHistoryRepository;
import com.labresource.platform.booking.repository.UsageLogRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.resource.service.LiveStatusEventPublisher;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import com.labresource.platform.usage.service.UsageChargeService;
import com.labresource.platform.notification.service.NotificationService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final UsageLogRepository usageLogRepository;
    private final LiveStatusEventPublisher liveStatusEventPublisher;
    private final UsageChargeService usageChargeService;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            BookingStatusHistoryRepository historyRepository,
            UsageLogRepository usageLogRepository,
            UsageChargeService usageChargeService,
            LiveStatusEventPublisher liveStatusEventPublisher,
            NotificationService notificationService) {

        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.usageLogRepository = usageLogRepository;
        this.liveStatusEventPublisher = liveStatusEventPublisher;
        this.usageChargeService = usageChargeService;
        this.notificationService = notificationService;
    }

    // Create booking
    @Transactional
    public Booking createBooking(
            String email,
            Long resourceId,
            Booking booking) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        // Lock the resource row so maintenance status cannot change
        // concurrently while a new booking is being created.
        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        validateResourceBookable(resource);

        LocalDateTime startTime = booking.getStartTime();
        LocalDateTime endTime = booking.getEndTime();

        validateTimeSlot(startTime, endTime);

        Integer quantity = resource.getQuantity();

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Resource quantity is not available");
        }

        long overlappingBookings =
                bookingRepository.countOverlappingBookings(
                        resourceId, startTime, endTime);

        if (overlappingBookings >= quantity) {
            throw new IllegalArgumentException(
                    "No workstation available for this time slot");
        }

        booking.setUser(user);
        booking.setResource(resource);
        booking.setStatus("PENDING");
        booking.setStatusRemark("Booking created");

        Booking savedBooking = bookingRepository.save(booking);


        saveStatusHistory(
                savedBooking,
                null,
                "PENDING",
                email,
                "Booking created");


        publishLiveStatusAfterCommit(
                resource.getId(), "BOOKING_CREATED");

        return savedBooking;
    }

    // A resource is bookable only when active and operational.
    // BOOKED is allowed because capacity is checked for the requested time slot.
    private void validateResourceBookable(Resource resource) {
        if (!isResourceBookable(resource)) {
            throw new IllegalArgumentException(
                    "This resource is currently unavailable for booking"
            );
        }
    }

    private boolean isResourceBookable(Resource resource) {
        if (resource == null || Boolean.FALSE.equals(resource.getIsActive())) {
            return false;
        }

        String status = resource.getStatus();

        return "AVAILABLE".equalsIgnoreCase(status)
                || "BOOKED".equalsIgnoreCase(status);
    }

    // Validate booking time
    private void validateTimeSlot(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Booking start time must be in the future");
        }
    }

    // Check resource availability
    @Transactional(readOnly = true)
    public int getAvailableQuantity(
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        validateTimeSlot(startTime, endTime);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        if (!isResourceBookable(resource)) {
            return 0;
        }

        if (resource.getQuantity() == null
                || resource.getQuantity() <= 0) {
            return 0;
        }

        long overlappingBookings =
                bookingRepository.countOverlappingBookings(
                        resourceId, startTime, endTime);

        return (int) Math.max(
                0,
                resource.getQuantity() - overlappingBookings);
    }

    // Get all bookings
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithDetails();
    }

    // Get bookings by user ID
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdWithDetails(userId);
    }

    // Get bookings by logged-in user's email
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return bookingRepository.findByUserIdWithDetails(user.getId());
    }

    // Institution Admin: View own institution bookings
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForInstitutionAdmin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this user");
        }

        Long institutionId = user.getInstitution().getId();

        return bookingRepository.findByInstitutionIdWithDetails(
                institutionId);
    }

    // Student check-in
    @Transactional
    public Booking checkIn(Long bookingId, String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Booking booking = bookingRepository
                .findByIdWithDetailsForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can check in only for your own booking");
        }

        String currentStatus = booking.getStatus();

        if (!"APPROVED".equals(currentStatus)
                && !"CONFIRMED".equals(currentStatus)) {
            throw new IllegalArgumentException(
                    "Only approved or confirmed bookings can be checked in");
        }

        if (booking.getActualStartTime() != null) {
            throw new IllegalArgumentException(
                    "This booking has already been checked in");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestCheckIn =
                booking.getStartTime().minusMinutes(15);

        if (now.isBefore(earliestCheckIn)) {
            throw new IllegalArgumentException(
                    "Check-in is allowed only 15 minutes before booking start");
        }

        if (now.isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException(
                    "Booking time has expired. Check-in is not allowed");
        }

        Resource resource = resourceRepository
                .findByIdForUpdate(booking.getResource().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        if (Boolean.FALSE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "This resource is inactive");
        }

        String resourceStatus = resource.getStatus();

        if ("UNDER_MAINTENANCE".equals(resourceStatus)
                || "OUT_OF_SERVICE".equals(resourceStatus)) {
            throw new IllegalArgumentException(
                    "Resource is currently unavailable");
        }

        Integer availableQuantity = resource.getAvailableQuantity();

        if (availableQuantity == null || availableQuantity <= 0) {
            throw new IllegalArgumentException(
                    "No resource quantity is currently available");
        }

        boolean activeUsageExists = usageLogRepository
                .findByBookingIdAndActualEndTimeIsNull(bookingId)
                .isPresent();

        if (activeUsageExists) {
            throw new IllegalArgumentException(
                    "An active usage session already exists");
        }

        int remainingQuantity = availableQuantity - 1;
        resource.setAvailableQuantity(remainingQuantity);

        if (remainingQuantity == 0) {
            resource.setStatus("BOOKED");
        }

        String oldStatus = booking.getStatus();

        booking.setActualStartTime(now);
        booking.setActualEndTime(null);
        booking.setStatus("IN_USE");
        booking.setStatusRemark("Booking checked in by owner");

        Booking savedBooking = bookingRepository.save(booking);

        UsageLog usageLog = new UsageLog();
        usageLog.setResource(resource);
        usageLog.setUser(user);
        usageLog.setBooking(savedBooking);
        usageLog.setActualStartTime(now);
        usageLog.setActualEndTime(null);
        usageLog.setUsageType("BOOKING");
        usageLog.setRemarks("Check-in completed");

        usageLogRepository.save(usageLog);

        saveStatusHistory(
                savedBooking,
                oldStatus,
                "IN_USE",
                email,
                "Student checked in");

        publishLiveStatusAfterCommit(
                resource.getId(), "CHECKED_IN");

        return savedBooking;
    }

    // Student check-out
    @Transactional
    public Booking checkOut(Long bookingId, String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Booking booking = bookingRepository
                .findByIdWithDetailsForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can check out only your own booking");
        }

        if (!"IN_USE".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only active bookings can be checked out");
        }

        UsageLog usageLog = usageLogRepository
                .findByBookingIdAndActualEndTimeIsNull(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Active usage session not found"));

        LocalDateTime now = LocalDateTime.now();

        if (booking.getActualStartTime() == null) {
            throw new IllegalArgumentException(
                    "Booking check-in time is missing");
        }

        if (now.isBefore(booking.getActualStartTime())) {
            throw new IllegalArgumentException(
                    "Check-out time cannot be before check-in time");
        }

        Resource resource = resourceRepository
                .findByIdForUpdate(booking.getResource().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        usageLog.setActualEndTime(now);
        usageLog.setRemarks("Check-out completed");

        usageLogRepository.save(usageLog);

        String oldStatus = booking.getStatus();

        booking.setActualEndTime(now);
        booking.setStatus("COMPLETED");
        booking.setStatusRemark("Booking checked out by owner");

        Booking savedBooking = bookingRepository.save(booking);
        usageChargeService.createCharge(savedBooking, usageLog);

        Integer availableQuantity = resource.getAvailableQuantity();
        Integer totalQuantity = resource.getQuantity();

        if (availableQuantity == null) {
            availableQuantity = 0;
        }

        if (totalQuantity != null
                && availableQuantity < totalQuantity) {
            resource.setAvailableQuantity(availableQuantity + 1);
        }

        if ("BOOKED".equals(resource.getStatus())
                && resource.getAvailableQuantity() != null
                && resource.getAvailableQuantity() > 0) {
            resource.setStatus("AVAILABLE");
        }

        resourceRepository.save(resource);

        saveStatusHistory(
                savedBooking,
                oldStatus,
                "COMPLETED",
                email,
                "Student checked out");

        publishLiveStatusAfterCommit(
                resource.getId(), "CHECKED_OUT");

        return savedBooking;
    }

    // Update booking status
    @Transactional
    public Booking updateBookingStatus(
            Long bookingId,
            String newStatus,
            String changedByEmail) {

        if (changedByEmail == null || changedByEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        String normalizedStatus = newStatus == null
                ? ""
                : newStatus.trim().toUpperCase(Locale.ROOT);

        String oldStatus = booking.getStatus();

        boolean validTransition = switch (normalizedStatus) {
            case "APPROVED", "REJECTED" ->
                    "PENDING".equals(oldStatus);

            case "IN_USE" ->
                    "APPROVED".equals(oldStatus);

            case "COMPLETED" ->
                    "IN_USE".equals(oldStatus);

            case "NO_SHOW" ->
                    "APPROVED".equals(oldStatus);

            default -> false;
        };

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid status transition: "
                            + oldStatus + " -> " + normalizedStatus);
        }

        String remark = "Status changed from "
                + oldStatus + " to " + normalizedStatus;

        booking.setStatus(normalizedStatus);
        booking.setStatusRemark(remark);

        UsageLog completedUsageLog = null;

        if ("COMPLETED".equals(normalizedStatus)) {
            completedUsageLog = usageLogRepository
                    .findByBookingIdAndActualEndTimeIsNull(bookingId)
                    .orElse(null);

            if (completedUsageLog != null) {
                LocalDateTime completedAt = LocalDateTime.now();

                completedUsageLog.setActualEndTime(completedAt);
                completedUsageLog.setRemarks("Booking completed by staff");

                usageLogRepository.save(completedUsageLog);
                booking.setActualEndTime(completedAt);
            }
        }

        Booking savedBooking = bookingRepository.save(booking);
        if (completedUsageLog != null) {
            usageChargeService.createCharge(savedBooking, completedUsageLog);
        }

        saveStatusHistory(
                savedBooking,
                oldStatus,
                normalizedStatus,
                changedByEmail.trim(),
                remark);

        if ("APPROVED".equals(normalizedStatus)) {
            notifyBookingOwner(
                    savedBooking,
                    "BOOKING_CONFIRMED",
                    "Booking confirmed",
                    "Your booking for " + resourceName(savedBooking)
                            + " has been confirmed.");
        } else if ("REJECTED".equals(normalizedStatus)) {
            notifyBookingOwner(
                    savedBooking,
                    "BOOKING_REJECTED",
                    "Booking rejected",
                    "Your booking for " + resourceName(savedBooking)
                            + " has been rejected.");
        }

        publishLiveStatusAfterCommit(
                savedBooking.getResource().getId(),
                "STATUS_CHANGED");

        return savedBooking;
    }

    // Institution Admin: Approve or reject own institution booking
    @Transactional
    public Booking updateBookingStatusForInstitutionAdmin(
            Long bookingId,
            String newStatus,
            String changedByEmail) {

        User user = userRepository.findByEmail(changedByEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this user");
        }

        Booking booking = bookingRepository
                .findByIdWithDetailsForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        Long adminInstitutionId = user.getInstitution().getId();

        Long resourceInstitutionId =
                booking.getResource().getInstitution() == null
                        ? null
                        : booking.getResource()
                          .getInstitution().getId();

        if (resourceInstitutionId == null
                || !adminInstitutionId.equals(resourceInstitutionId)) {
            throw new AccessDeniedException(
                    "You can manage bookings only for your own institution");
        }

        String normalizedStatus = newStatus == null
                ? ""
                : newStatus.trim().toUpperCase(Locale.ROOT);

        if (!List.of("APPROVED", "REJECTED")
                .contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Institution Admin can only approve or reject bookings");
        }

        String oldStatus = booking.getStatus();

        if (!"PENDING".equals(oldStatus)) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be approved or rejected");
        }

        String remark = "Status changed from "
                + oldStatus + " to " + normalizedStatus;

        booking.setStatus(normalizedStatus);
        booking.setStatusRemark(remark);

        Booking savedBooking = bookingRepository.save(booking);

        saveStatusHistory(
                savedBooking,
                oldStatus,
                normalizedStatus,
                changedByEmail.trim(),
                remark);

        if ("APPROVED".equals(normalizedStatus)) {
            notifyBookingOwner(
                    savedBooking,
                    "BOOKING_CONFIRMED",
                    "Booking confirmed",
                    "Your booking for " + resourceName(savedBooking)
                            + " has been confirmed.");
        } else if ("REJECTED".equals(normalizedStatus)) {
            notifyBookingOwner(
                    savedBooking,
                    "BOOKING_REJECTED",
                    "Booking rejected",
                    "Your booking for " + resourceName(savedBooking)
                            + " has been rejected.");
        }

        publishLiveStatusAfterCommit(
                savedBooking.getResource().getId(),
                "STATUS_CHANGED");

        return savedBooking;
    }

    // Student cancels own booking
    @Transactional
    public Booking cancelBooking(Long bookingId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can cancel only your own bookings");
        }

        if (!"PENDING".equals(booking.getStatus())
                && !"APPROVED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending or approved bookings can be cancelled");
        }

        String oldStatus = booking.getStatus();
        String remark = "Booking cancelled by the student";

        booking.setStatus("CANCELLED");
        booking.setStatusRemark(remark);

        Booking savedBooking = bookingRepository.save(booking);

        saveStatusHistory(
                savedBooking,
                oldStatus,
                "CANCELLED",
                email,
                remark);

        notifyBookingOwner(
                savedBooking,
                "BOOKING_CANCELLED",
                "Booking cancelled",
                "Your booking for " + resourceName(savedBooking)
                        + " has been cancelled.");

        publishLiveStatusAfterCommit(
                savedBooking.getResource().getId(),
                "BOOKING_CANCELLED");

        return savedBooking;
    }

    // Filter bookings by status
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(String status) {

        if (status == null || status.isBlank()) {
            return bookingRepository.findAllWithDetails();
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);

        if (!List.of(
                "PENDING",
                "APPROVED",
                "REJECTED",
                "CANCELLED",
                "CONFIRMED",
                "IN_USE",
                "COMPLETED",
                "NO_SHOW"
        ).contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid booking status: " + status);
        }

        return bookingRepository.findByStatusWithDetails(normalizedStatus);
    }

    // Check booking existence
    @Transactional(readOnly = true)
    public boolean bookingExists(Long bookingId) {
        return bookingRepository.existsById(bookingId);
    }

    private void notifyBookingOwner(
            Booking booking,
            String type,
            String title,
            String message) {

        if (booking.getUser() == null || booking.getUser().getId() == null) {
            return;
        }

        notificationService.createNotification(
                booking.getUser().getId(),
                title,
                message,
                type,
                "BOOKING",
                booking.getId()
        );
    }

    private String resourceName(Booking booking) {
        if (booking.getResource() == null
                || booking.getResource().getName() == null
                || booking.getResource().getName().isBlank()) {
            return "the selected equipment";
        }
        return booking.getResource().getName();
    }

    // Publish live status after transaction commit
    private void publishLiveStatusAfterCommit(
            Long resourceId,
            String eventType) {

        Runnable publishEvent = () ->
                liveStatusEventPublisher.publishResourceUpdate(
                        resourceId,
                        eventType
                );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishEvent.run();
                        }
                    }
            );
        } else {
            publishEvent.run();
        }
    }

    // Save booking status history
    private void saveStatusHistory(
            Booking booking,
            String oldStatus,
            String newStatus,
            String changedBy,
            String remark) {

        BookingStatusHistory history = new BookingStatusHistory();

        history.setBooking(booking);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemark(remark);

        historyRepository.save(history);
    }
}

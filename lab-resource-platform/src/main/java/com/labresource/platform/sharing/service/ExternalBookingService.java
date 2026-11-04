
package com.labresource.platform.sharing.service;

import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.sharing.dto.ExternalBookingResponse;
import com.labresource.platform.sharing.entity.ExternalBooking;
import com.labresource.platform.sharing.entity.ResourceShare;
import com.labresource.platform.sharing.repository.ExternalBookingRepository;
import com.labresource.platform.sharing.repository.ResourceShareRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExternalBookingService {

    private final ExternalBookingRepository externalBookingRepository;
    private final ResourceShareRepository shareRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ExternalBookingService(
            ExternalBookingRepository externalBookingRepository,
            ResourceShareRepository shareRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository) {

        this.externalBookingRepository = externalBookingRepository;
        this.shareRepository = shareRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    private User getAuthorizedManager(String email) {
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getRole() == null || !List.of(
                "LAB_MANAGER",
                "INSTITUTION_ADMIN",
                "SYSTEM_ADMIN"
        ).contains(user.getRole().getName())) {
            throw new IllegalArgumentException(
                    "You are not authorized to manage external bookings");
        }

        if (user.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "User is not associated with an institution");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException(
                    "Inactive users cannot manage external bookings");
        }

        return user;
    }

    private void validateTime(
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

    @Transactional
    public ExternalBookingResponse createRequest(
            Long shareId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String remarks,
            String email) {

        User requester = getAuthorizedManager(email);
        Long institutionId = requester.getInstitution().getId();

        validateTime(startTime, endTime);

        ResourceShare share = shareRepository.findByIdWithResource(shareId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Sharing record not found"));

        if (!ResourceShare.APPROVED.equals(share.getStatus())) {
            throw new IllegalArgumentException(
                    "Only approved shared resources can be booked");
        }

        if (!institutionId.equals(share.getSharedWithInstitutionId())) {
            throw new IllegalArgumentException(
                    "Your institution does not have access to this shared resource");
        }

        if (share.getStartTime() != null
                && startTime.isBefore(share.getStartTime())) {
            throw new IllegalArgumentException(
                    "Requested time is before the sharing period");
        }

        if (share.getEndTime() != null
                && endTime.isAfter(share.getEndTime())) {
            throw new IllegalArgumentException(
                    "Requested time is after the sharing period");
        }

        Resource resource = share.getResource();

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "The shared resource is inactive");
        }

        if ("UNDER_MAINTENANCE".equalsIgnoreCase(resource.getStatus())
                || "OUT_OF_SERVICE".equalsIgnoreCase(resource.getStatus())) {
            throw new IllegalArgumentException(
                    "The shared resource is currently unavailable");
        }

        long internalOverlaps =
                bookingRepository.countOverlappingBookings(
                        resource.getId(), startTime, endTime);

        long externalOverlaps =
                externalBookingRepository.countOverlappingExternalBookings(
                        resource.getId(), startTime, endTime);

        int quantity = resource.getQuantity() == null
                ? 0 : resource.getQuantity();

        if (quantity <= 0
                || internalOverlaps + externalOverlaps >= quantity) {
            throw new IllegalArgumentException(
                    "No resource capacity is available for this time slot");
        }

        ExternalBooking booking = new ExternalBooking();

        booking.setResourceShare(share);
        booking.setRequestedBy(requester);
        booking.setRequestingInstitutionId(institutionId);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setRemarks(remarks);
        booking.setStatus(ExternalBooking.PENDING);

        ExternalBooking saved = externalBookingRepository.save(booking);

        return new ExternalBookingResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExternalBookingResponse> getMyRequests(String email) {

        User user = getAuthorizedManager(email);

        return externalBookingRepository
                .findForRequester(user.getId())
                .stream()
                .map(ExternalBookingResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExternalBookingResponse> getIncomingRequests(String email) {

        User user = getAuthorizedManager(email);
        Long institutionId = user.getInstitution().getId();

        return externalBookingRepository
                .findForResourceOwner(institutionId)
                .stream()
                .map(ExternalBookingResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExternalBookingResponse> getOutgoingRequests(String email) {

        User user = getAuthorizedManager(email);
        Long institutionId = user.getInstitution().getId();

        return externalBookingRepository
                .findForRequestingInstitution(institutionId)
                .stream()
                .map(ExternalBookingResponse::new)
                .toList();
    }

    @Transactional
    public ExternalBookingResponse approve(
            Long id,
            String remarks,
            String email) {

        return decide(id, ExternalBooking.APPROVED, remarks, email);
    }

    @Transactional
    public ExternalBookingResponse reject(
            Long id,
            String remarks,
            String email) {

        return decide(id, ExternalBooking.REJECTED, remarks, email);
    }

    private ExternalBookingResponse decide(
            Long id,
            String newStatus,
            String remarks,
            String email) {

        User reviewer = getAuthorizedManager(email);
        Long institutionId = reviewer.getInstitution().getId();

        ExternalBooking booking =
                externalBookingRepository.findDetailsById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "External booking request not found"));

        ResourceShare share = booking.getResourceShare();

        if (!institutionId.equals(share.getSharedByInstitutionId())) {
            throw new IllegalArgumentException(
                    "Only the resource owner's institution can review this request");
        }

        if (!ExternalBooking.PENDING.equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending external booking requests can be reviewed");
        }

        if (ExternalBooking.APPROVED.equals(newStatus)) {

            long internalOverlaps =
                    bookingRepository.countOverlappingBookings(
                            share.getResource().getId(),
                            booking.getStartTime(),
                            booking.getEndTime());

            long otherExternalOverlaps =
                    externalBookingRepository.countOverlappingExternalBookings(
                            share.getResource().getId(),
                            booking.getStartTime(),
                            booking.getEndTime());

            // Exclude the current pending request from the overlap count.
            if (otherExternalOverlaps > 0) {
                otherExternalOverlaps--;
            }

            int quantity = share.getResource().getQuantity() == null
                    ? 0 : share.getResource().getQuantity();

            if (quantity <= 0
                    || internalOverlaps + otherExternalOverlaps >= quantity) {
                throw new IllegalArgumentException(
                        "The requested time slot is no longer available");
            }

            if (!Boolean.TRUE.equals(share.getResource().getIsActive())
                    || "UNDER_MAINTENANCE".equalsIgnoreCase(
                    share.getResource().getStatus())
                    || "OUT_OF_SERVICE".equalsIgnoreCase(
                    share.getResource().getStatus())) {
                throw new IllegalArgumentException(
                        "The shared resource is currently unavailable");
            }
        }

        booking.setStatus(newStatus);
        booking.setDecisionRemarks(remarks);
        booking.setReviewedByUserId(reviewer.getId());
        booking.setReviewedAt(LocalDateTime.now());

        ExternalBooking saved = externalBookingRepository.save(booking);

        return new ExternalBookingResponse(saved);
    }

    @Transactional
    public ExternalBookingResponse cancel(Long id, String email) {

        User user = getAuthorizedManager(email);

        ExternalBooking booking =
                externalBookingRepository.findDetailsById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "External booking request not found"));

        if (!booking.getRequestedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can cancel only requests created by your account");
        }

        if (!ExternalBooking.PENDING.equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending external booking requests can be cancelled");
        }

        booking.setStatus(ExternalBooking.CANCELLED);
        booking.setDecisionRemarks(
                "Cancelled by requesting institution");
        booking.setReviewedAt(LocalDateTime.now());

        ExternalBooking saved = externalBookingRepository.save(booking);

        return new ExternalBookingResponse(saved);
    }
}

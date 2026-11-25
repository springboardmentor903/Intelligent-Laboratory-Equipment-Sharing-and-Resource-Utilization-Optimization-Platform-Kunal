
package com.labresource.platform.sharing.service;

import com.labresource.platform.notification.service.NotificationService;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.sharing.entity.ResourceShare;
import com.labresource.platform.sharing.repository.ResourceShareRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceShareService {

    private final ResourceShareRepository shareRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ResourceShareService(
            ResourceShareRepository shareRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.shareRepository = shareRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private User getUser(String email) {
        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private Long getInstitutionId(User user) {
        if (user.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "User is not associated with an institution");
        }

        return user.getInstitution().getId();
    }

    private void validateManager(User user) {
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role not found");
        }

        String role = user.getRole().getName();

        if (!List.of(
                "LAB_MANAGER",
                "INSTITUTION_ADMIN",
                "SYSTEM_ADMIN"
        ).contains(role)) {
            throw new IllegalArgumentException(
                    "You are not authorized to manage resource sharing");
        }
    }

    private void validateTime(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if ((startTime == null) != (endTime == null)) {
            throw new IllegalArgumentException(
                    "Both start time and end time are required");
        }

        if (startTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException(
                        "End time must be after start time");
            }

            if (!startTime.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "Sharing start time must be in the future");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ResourceShare> discoverSharedResources(String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        return shareRepository.findApprovedSharesForInstitution(
                institutionId);
    }

    @Transactional(readOnly = true)
    public List<ResourceShare> getSentRequests(String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        return shareRepository.findSentRequestsWithResource(
                institutionId);
    }

    @Transactional(readOnly = true)
    public List<ResourceShare> getReceivedRequests(String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        return shareRepository.findReceivedRequestsWithResource(
                institutionId);
    }

    @Transactional
    public ResourceShare createShare(
            Long resourceId,
            Long targetInstitutionId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal feePerHour,
            String remarks,
            String email) {

        User requester = getUser(email);
        validateManager(requester);

        Long sourceInstitutionId = getInstitutionId(requester);

        if (targetInstitutionId == null) {
            throw new IllegalArgumentException(
                    "Target institution is required");
        }

        if (sourceInstitutionId.equals(targetInstitutionId)) {
            throw new IllegalArgumentException(
                    "You cannot share resources with your own institution");
        }

        validateTime(startTime, endTime);

        if (feePerHour == null ||
                feePerHour.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Fee per hour must be zero or greater");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot share an inactive resource");
        }

        if (resource.getInstitution() == null ||
                !sourceInstitutionId.equals(
                        resource.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "You can only share resources owned by your institution");
        }

        if (!"AVAILABLE".equalsIgnoreCase(resource.getStatus())
                && !"BOOKED".equalsIgnoreCase(resource.getStatus())) {
            throw new IllegalArgumentException(
                    "This resource is not eligible for sharing");
        }

        if (shareRepository
                .existsByResourceIdAndSharedWithInstitutionIdAndStatus(
                        resourceId,
                        targetInstitutionId,
                        ResourceShare.PENDING)) {
            throw new IllegalArgumentException(
                    "A pending sharing request already exists");
        }

        User recipient = userRepository
                .findFirstByInstitution_IdAndRole_NameAndIsActiveTrueOrderByIdAsc(
                        targetInstitutionId,
                        "INSTITUTION_ADMIN")
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active institution administrator found"));

        ResourceShare share = new ResourceShare();

        share.setResource(resource);
        share.setSharedByUserId(requester.getId());
        share.setSharedWithUserId(recipient.getId());
        share.setSharedByInstitutionId(sourceInstitutionId);
        share.setSharedWithInstitutionId(targetInstitutionId);
        share.setStartTime(startTime);
        share.setEndTime(endTime);
        share.setFeePerHour(feePerHour);
        share.setRemarks(remarks);
        share.setStatus(ResourceShare.PENDING);

        ResourceShare savedShare = shareRepository.save(share);

        notificationService.createNotification(
                recipient.getId(),
                "New Resource Sharing Request",
                "A resource sharing request has been submitted for "
                        + resource.getName()
                        + ". Please review the request.",
                "SHARING_REQUEST_SUBMITTED",
                "SHARING_REQUEST",
                savedShare.getId()
        );

        return savedShare;
    }

    /**
     * Forward a pending request to another authorized user in the receiving
     * institution. The request remains pending; only its assigned reviewer changes.
     */
    @Transactional
    public ResourceShare forwardRequest(
            Long id,
            Long targetUserId,
            String remarks,
            String email) {

        User forwarder = getUser(email);
        validateManager(forwarder);

        ResourceShare share = shareRepository.findByIdWithResource(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sharing request not found"));

        Long receivingInstitutionId = getInstitutionId(forwarder);

        if (!receivingInstitutionId.equals(
                share.getSharedWithInstitutionId())) {
            throw new IllegalArgumentException(
                    "Only the receiving institution can forward this request");
        }

        if (!ResourceShare.PENDING.equals(share.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending sharing requests can be forwarded");
        }

        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user is required");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Forward target user not found"));

        if (!Boolean.TRUE.equals(target.getIsActive())
                || target.getInstitution() == null
                || !receivingInstitutionId.equals(
                target.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "Forward target must be an active user in your institution");
        }

        if (target.getRole() == null
                || target.getRole().getName() == null
                || !List.of(
                "INSTITUTION_ADMIN",
                "LAB_MANAGER"
        ).contains(target.getRole().getName())) {
            throw new IllegalArgumentException(
                    "Forward target must be an Institution Admin or Lab Manager");
        }

        if (target.getId().equals(forwarder.getId())) {
            throw new IllegalArgumentException(
                    "You cannot forward a request to yourself");
        }

        share.setSharedWithUserId(target.getId());

        if (remarks != null && !remarks.isBlank()) {
            share.setDecisionRemarks(remarks.trim());
        }

        ResourceShare savedShare = shareRepository.save(share);

        notificationService.createNotification(
                target.getId(),
                "Sharing Request Forwarded to You",
                "A resource sharing request for "
                        + share.getResource().getName()
                        + " has been forwarded to you for review.",
                "SHARING_REQUEST_FORWARDED",
                "SHARING_REQUEST",
                savedShare.getId()
        );

        return savedShare;
    }

    @Transactional
    public ResourceShare approveRequest(
            Long id,
            String decisionRemarks,
            String email) {

        return updateDecision(
                id,
                ResourceShare.APPROVED,
                decisionRemarks,
                email);
    }

    @Transactional
    public ResourceShare rejectRequest(
            Long id,
            String decisionRemarks,
            String email) {

        return updateDecision(
                id,
                ResourceShare.REJECTED,
                decisionRemarks,
                email);
    }

    private ResourceShare updateDecision(
            Long id,
            String newStatus,
            String decisionRemarks,
            String email) {

        User reviewer = getUser(email);
        validateManager(reviewer);

        Long institutionId = getInstitutionId(reviewer);

        ResourceShare share = shareRepository
                .findByIdWithResource(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing request not found"));

        if (!institutionId.equals(
                share.getSharedWithInstitutionId())) {
            throw new IllegalArgumentException(
                    "You cannot manage requests for another institution");
        }

        if (!ResourceShare.PENDING.equals(share.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending requests can be reviewed");
        }

        share.setStatus(newStatus);
        share.setDecisionRemarks(decisionRemarks);
        share.setApprovedByUserId(reviewer.getId());
        share.setDecidedAt(LocalDateTime.now());

        ResourceShare savedShare = shareRepository.save(share);

        String resourceName = share.getResource().getName();

        if (ResourceShare.APPROVED.equals(newStatus)) {
            notificationService.createNotification(
                    share.getSharedByUserId(),
                    "Sharing Request Approved",
                    "Your sharing request for "
                            + resourceName
                            + " has been approved.",
                    "SHARING_REQUEST_APPROVED",
                    "SHARING_REQUEST",
                    savedShare.getId()
            );
        } else if (ResourceShare.REJECTED.equals(newStatus)) {
            notificationService.createNotification(
                    share.getSharedByUserId(),
                    "Sharing Request Rejected",
                    "Your sharing request for "
                            + resourceName
                            + " has been rejected."
                            + (decisionRemarks == null
                            || decisionRemarks.isBlank()
                            ? ""
                            : " Reason: " + decisionRemarks),
                    "SHARING_REQUEST_REJECTED",
                    "SHARING_REQUEST",
                    savedShare.getId()
            );
        }

        return savedShare;
    }

    @Transactional
    public ResourceShare revokeShare(
            Long id,
            String remarks,
            String email) {

        User requester = getUser(email);
        validateManager(requester);

        Long institutionId = getInstitutionId(requester);

        ResourceShare share = shareRepository
                .findByIdWithResource(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing request not found"));

        if (!institutionId.equals(
                share.getSharedByInstitutionId())) {
            throw new IllegalArgumentException(
                    "Only the resource-owning institution can revoke this share");
        }

        if (!ResourceShare.APPROVED.equals(share.getStatus())) {
            throw new IllegalArgumentException(
                    "Only approved sharing agreements can be revoked");
        }

        share.setStatus(ResourceShare.CANCELLED);
        share.setDecisionRemarks(remarks);
        share.setApprovedByUserId(requester.getId());
        share.setDecidedAt(LocalDateTime.now());

        ResourceShare savedShare = shareRepository.save(share);

        notificationService.createNotification(
                share.getSharedWithUserId(),
                "Resource Sharing Revoked",
                "Access to the shared resource "
                        + share.getResource().getName()
                        + " has been revoked by the owning institution."
                        + (remarks == null || remarks.isBlank()
                        ? ""
                        : " Reason: " + remarks),
                "SHARING_ACCESS_REVOKED",
                "AGREEMENT",
                savedShare.getId()
        );

        return savedShare;
    }
}

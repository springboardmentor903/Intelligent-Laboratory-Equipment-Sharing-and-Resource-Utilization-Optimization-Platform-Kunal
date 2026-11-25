
package com.labresource.platform.usage.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.sharing.entity.ResourceShare;
import com.labresource.platform.sharing.repository.ResourceShareRepository;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;
import com.labresource.platform.user.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class UsageChargeService {

    private final UsageChargeRepository usageChargeRepository;
    private final ResourceShareRepository resourceShareRepository;

    @Value("${app.usage.internal-hourly-rate:0.00}")
    private BigDecimal internalHourlyRate;

    public UsageChargeService(
            UsageChargeRepository usageChargeRepository,
            ResourceShareRepository resourceShareRepository) {
        this.usageChargeRepository = usageChargeRepository;
        this.resourceShareRepository = resourceShareRepository;
    }

    @Transactional
    public UsageCharge createCharge(Booking booking, UsageLog usageLog) {

        if (booking == null || usageLog == null) {
            throw new IllegalArgumentException(
                    "Booking and usage log are required");
        }

        if (!"COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Usage charge can only be created for completed bookings");
        }

        if (usageChargeRepository.existsByBookingId(booking.getId())) {
            return usageChargeRepository.findByBookingId(booking.getId())
                    .orElseThrow();
        }

        Long usageMinutes = usageLog.getActualDurationMinutes();

        if (usageMinutes == null || usageMinutes <= 0) {
            return null;
        }

        Resource resource = booking.getResource();
        User user = booking.getUser();

        if (resource == null || user == null) {
            throw new IllegalArgumentException(
                    "Booking resource or user is missing");
        }

        Long owningInstitutionId =
                resource.getInstitution() == null
                        ? null
                        : resource.getInstitution().getId();

        Long userInstitutionId =
                user.getInstitution() == null
                        ? null
                        : user.getInstitution().getId();

        Long departmentId =
                user.getDepartment() == null
                        ? null
                        : user.getDepartment().getId();

        UsageCharge.ChargeType chargeType;
        BigDecimal hourlyRate;

        if (userInstitutionId == null) {
            chargeType = UsageCharge.ChargeType.UNASSIGNED;
            hourlyRate = internalHourlyRate;

        } else if (owningInstitutionId != null
                && owningInstitutionId.equals(userInstitutionId)) {

            chargeType = UsageCharge.ChargeType.INTERNAL;
            hourlyRate = internalHourlyRate;

        } else {
            chargeType = UsageCharge.ChargeType.EXTERNAL;

            List<ResourceShare> approvedShares =
                    resourceShareRepository
                            .findApprovedSharesForInstitution(
                                    userInstitutionId);

            ResourceShare matchingShare = approvedShares.stream()
                    .filter(share ->
                            share.getResource() != null
                                    && share.getResource().getId()
                                    .equals(resource.getId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "No approved sharing agreement found for this booking"));

            hourlyRate = matchingShare.getFeePerHour();

            if (hourlyRate == null) {
                throw new IllegalStateException(
                        "Sharing agreement hourly fee is missing");
            }
        }

        if (hourlyRate == null || hourlyRate.signum() < 0) {
            throw new IllegalStateException(
                    "Invalid hourly usage rate");
        }

        BigDecimal amount = hourlyRate
                .multiply(BigDecimal.valueOf(usageMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        UsageCharge charge = new UsageCharge();
        charge.setBooking(booking);
        charge.setResource(resource);
        charge.setUser(user);
        charge.setOwningInstitutionId(owningInstitutionId);
        charge.setUserInstitutionId(userInstitutionId);
        charge.setDepartmentId(departmentId);
        charge.setChargeType(chargeType);
        charge.setUsageMinutes(usageMinutes);
        charge.setHourlyRateSnapshot(hourlyRate);
        charge.setAmount(amount);
        charge.setChargeStatus("PENDING");

        return usageChargeRepository.save(charge);
    }
}

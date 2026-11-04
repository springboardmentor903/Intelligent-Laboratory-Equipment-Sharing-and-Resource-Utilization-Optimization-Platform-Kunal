package com.labresource.platform.report;

import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.booking.repository.UsageLogRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ResourceUtilizationService {

    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;
    private final UserRepository userRepository;

    public ResourceUtilizationService(
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository,
            UserRepository userRepository) {

        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceUtilizationResponse> getResourceUtilization(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String email) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User profile not found"
                ));

        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User role not configured"
            );
        }

        String role = user.getRole().getName()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        String scopeType;
        Long scopeId = null;

        switch (role) {

            case "SYSTEM_ADMIN":
                scopeType = "ALL";
                break;

            case "INSTITUTION_ADMIN":
                if (user.getInstitution() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Institution not assigned"
                    );
                }

                scopeType = "INSTITUTION";
                scopeId = user.getInstitution().getId();
                break;

            case "DEPARTMENT_HEAD":
            case "LAB_MANAGER":
                if (user.getDepartment() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Department not assigned"
                    );
                }

                scopeType = "DEPARTMENT";
                scopeId = user.getDepartment().getId();
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not allowed to view this report"
                );
        }

        List<Object[]> bookingRows =
                bookingRepository.getResourceUtilizationReport(
                        startDateTime,
                        endDateTime,
                        scopeType,
                        scopeId
                );

        Map<Long, Double> actualUsageHoursByResource =
                new HashMap<>();

        for (Object[] row :
                usageLogRepository.getActualUsageHoursByResource(
                        startDateTime,
                        endDateTime,
                        scopeType,
                        scopeId
                )) {

            Long resourceId =
                    ((Number) row[0]).longValue();

            double actualHours =
                    ((Number) row[1]).doubleValue();

            actualUsageHoursByResource.put(
                    resourceId,
                    actualHours
            );
        }

        return bookingRows.stream()
                .map(row -> {

                    Long resourceId =
                            ((Number) row[0]).longValue();

                    return new ResourceUtilizationResponse(
                            resourceId,
                            (String) row[1],
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).longValue(),
                            ((Number) row[4]).doubleValue(),
                            actualUsageHoursByResource.getOrDefault(
                                    resourceId,
                                    0.0
                            )
                    );
                })
                .toList();
    }
}
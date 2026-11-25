
package com.labresource.platform.institution.service;

import com.labresource.platform.institution.dto.UpdateInstitutionWorkingHoursRequest;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.institution.repository.InstitutionRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class InstitutionWorkingHoursService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    public InstitutionWorkingHoursService(
            UserRepository userRepository,
            InstitutionRepository institutionRepository) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    public Institution updateWorkingHours(
            String email,
            UpdateInstitutionWorkingHoursRequest request) {

        User user = getAuthorizedManager(email);
        Institution institution = user.getInstitution();

        if (institution == null) {
            throw new IllegalArgumentException(
                    "User is not linked to an institution");
        }

        LocalTime openingTime = request.getOpeningTime();
        LocalTime closingTime = request.getClosingTime();

        if (openingTime == null || closingTime == null) {
            throw new IllegalArgumentException(
                    "Opening time and closing time are required");
        }

        if (!closingTime.isAfter(openingTime)) {
            throw new IllegalArgumentException(
                    "Closing time must be after opening time");
        }

        Set<DayOfWeek> days = parseWorkingDays(
                request.getWorkingDays()
        );

        if (days.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one valid working day is required");
        }

        institution.setOpeningTime(openingTime);
        institution.setClosingTime(closingTime);
        institution.setWorkingDays(
                String.join(",", days.stream()
                        .map(DayOfWeek::name)
                        .toList())
        );

        return institutionRepository.save(institution);
    }

    @Transactional(readOnly = true)
    public Institution getWorkingHours(String email) {

        User user = getAuthorizedManager(email);
        Institution institution = user.getInstitution();

        if (institution == null) {
            throw new IllegalArgumentException(
                    "User is not linked to an institution");
        }

        return institution;
    }

    private User getAuthorizedManager(String email) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String role = user.getRole().getName()
                .toUpperCase(Locale.ROOT)
                .replace("ROLE_", "");

        if (!"LAB_MANAGER".equals(role)) {
            throw new AccessDeniedException(
                    "Only LAB_MANAGER can manage institution working hours");
        }

        return user;
    }

    private Set<DayOfWeek> parseWorkingDays(String workingDays) {

        if (workingDays == null || workingDays.isBlank()) {
            throw new IllegalArgumentException(
                    "Working days are required");
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

        for (String value : workingDays.split(",")) {
            try {
                DayOfWeek day = DayOfWeek.valueOf(
                        value.trim().toUpperCase(Locale.ROOT)
                );

                if (!days.add(day)) {
                    throw new IllegalArgumentException(
                            "Duplicate working day: " + day);
                }

            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid or duplicate working day: " + value);
            }
        }

        return days;
    }
}

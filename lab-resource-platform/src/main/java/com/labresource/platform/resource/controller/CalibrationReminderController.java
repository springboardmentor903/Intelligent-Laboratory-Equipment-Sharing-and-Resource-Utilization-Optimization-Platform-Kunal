package com.labresource.platform.resource.controller;

import com.labresource.platform.resource.dto.CalibrationReminderResponse;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.repository.CalibrationReminderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/calibrations/reminders")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
public class CalibrationReminderController {

    private static final int DUE_SOON_DAYS = 30;

    private final CalibrationReminderRepository reminderRepository;
    private final UserRepository userRepository;

    public CalibrationReminderController(
            CalibrationReminderRepository reminderRepository,
            UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<CalibrationReminderResponse> getReminders(
            Authentication authentication,
            @RequestParam(required = false) String status) {

        User user = userRepository.findByEmailWithProfile(
                authentication.getName()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is not associated with an institution"
            );
        }

        String filter = status == null || status.isBlank()
                ? null
                : status.trim().toUpperCase();

        if (filter != null &&
                !filter.matches("VALID|DUE_SOON|EXPIRED|NO_CALIBRATION")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid calibration reminder status"
            );
        }

        // NO_CALIBRATION resources have no CalibrationRecord.
        // Frontend merges these from /api/resources.
        if ("NO_CALIBRATION".equals(filter)) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate dueSoonLimit = today.plusDays(DUE_SOON_DAYS);

        // Fetch latest calibration records, not only due records.
        List<CalibrationRecord> records =
                reminderRepository.findLatestCalibrations();

        return records.stream()

                // Institution-level data isolation
                .filter(record ->
                        record.getResource().getInstitution() != null
                                && record.getResource().getInstitution().getId()
                                .equals(user.getInstitution().getId())
                )

                .map(record -> {
                    LocalDate dueDate = record.getNextDueDate();

                    long days = ChronoUnit.DAYS.between(
                            today,
                            dueDate
                    );

                    String reminderStatus;

                    if (dueDate.isBefore(today)) {
                        reminderStatus = "EXPIRED";
                    } else if (!dueDate.isAfter(dueSoonLimit)) {
                        reminderStatus = "DUE_SOON";
                    } else {
                        reminderStatus = "VALID";
                    }

                    return new CalibrationReminderResponse(
                            record.getId(),
                            record.getResource().getId(),
                            record.getResource().getName(),
                            record.getCalibrationDate(),
                            dueDate,
                            days,
                            reminderStatus
                    );
                })

                .filter(response ->
                        filter == null ||
                                filter.equals(response.reminderStatus())
                )

                .toList();
    }
}
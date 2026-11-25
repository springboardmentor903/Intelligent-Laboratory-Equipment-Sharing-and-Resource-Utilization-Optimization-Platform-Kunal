
package com.labresource.platform.resource.service;

import com.labresource.platform.resource.dto.CalibrationOverviewResponse;
import com.labresource.platform.resource.repository.CalibrationRecordRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class CalibrationOverviewService {

    private static final int DUE_SOON_DAYS = 30;

    private final CalibrationRecordRepository calibrationRepository;
    private final UserRepository userRepository;

    public CalibrationOverviewService(
            CalibrationRecordRepository calibrationRepository,
            UserRepository userRepository) {
        this.calibrationRepository = calibrationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<CalibrationOverviewResponse> getOverview(
            String email,
            Long departmentId,
            String state,
            String keyword,
            Pageable pageable) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is not associated with an institution");
        }

        if (state != null && !state.isBlank()) {
            state = state.trim().toUpperCase();

            if (!state.matches("VALID|DUE_SOON|EXPIRED|NONE")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid calibration state");
            }
        } else {
            state = null;
        }

        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueSoonLimit = today.plusDays(DUE_SOON_DAYS);

        Page<Object[]> result =
                calibrationRepository.findCalibrationOverview(
                        user.getInstitution().getId(),
                        departmentId,
                        keyword,
                        state,
                        today,
                        dueSoonLimit,
                        pageable
                );

        return result.map(row -> {
            LocalDate lastDate = (LocalDate) row[4];
            LocalDate nextDate = (LocalDate) row[5];

            Long daysUntilDue = nextDate == null
                    ? null
                    : ChronoUnit.DAYS.between(today, nextDate);

            return new CalibrationOverviewResponse(
                    (Long) row[0],
                    (String) row[1],
                    (Long) row[2],
                    (String) row[3],
                    lastDate,
                    nextDate,
                    daysUntilDue,
                    (String) row[6]
            );
        });
    }
}

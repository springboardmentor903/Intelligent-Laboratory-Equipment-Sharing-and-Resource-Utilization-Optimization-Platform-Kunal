
package com.labresource.platform.report.preview;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportFilterValidator {

    public void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException(
                    "Both fromDate and toDate are required"
            );
        }

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException(
                    "toDate cannot be before fromDate"
            );
        }

        if (fromDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "fromDate cannot be in the future"
            );
        }

        if (fromDate.plusYears(10).isBefore(toDate)) {
            throw new IllegalArgumentException(
                    "Report date range cannot exceed 10 years"
            );
        }
    }

    public void validateDepartmentId(Long departmentId) {
        if (departmentId != null && departmentId <= 0) {
            throw new IllegalArgumentException(
                    "Department ID must be a positive number"
            );
        }
    }

    public void validateResourceId(Long resourceId) {
        if (resourceId != null && resourceId <= 0) {
            throw new IllegalArgumentException(
                    "Resource ID must be a positive number"
            );
        }
    }

    public void validateKeyword(String keyword) {
        if (keyword != null && keyword.length() > 100) {
            throw new IllegalArgumentException(
                    "Keyword cannot exceed 100 characters"
            );
        }
    }

    public void validateStatus(String status) {
        if (status != null && status.length() > 50) {
            throw new IllegalArgumentException(
                    "Status cannot exceed 50 characters"
            );
        }
    }

    public void validate(
            LocalDate fromDate,
            LocalDate toDate,
            Long departmentId,
            Long resourceId,
            String keyword,
            String status
    ) {
        validateDateRange(fromDate, toDate);
        validateDepartmentId(departmentId);
        validateResourceId(resourceId);
        validateKeyword(keyword);
        validateStatus(status);
    }
}

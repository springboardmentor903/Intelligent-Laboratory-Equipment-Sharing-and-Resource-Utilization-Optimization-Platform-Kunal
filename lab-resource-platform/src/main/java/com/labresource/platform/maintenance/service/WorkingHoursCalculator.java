
package com.labresource.platform.maintenance.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

public final class WorkingHoursCalculator {

    private WorkingHoursCalculator() {
    }

    public static long calculateOpenMinutes(
            LocalDateTime start,
            LocalDateTime end,
            LocalTime openingTime,
            LocalTime closingTime,
            String workingDays) {

        if (start == null || end == null
                || openingTime == null || closingTime == null
                || workingDays == null || workingDays.isBlank()) {
            return 0;
        }

        if (!closingTime.isAfter(openingTime) || !end.isAfter(start)) {
            return 0;
        }

        Set<DayOfWeek> openDays = parseWorkingDays(workingDays);

        if (openDays.isEmpty()) {
            return 0;
        }

        long totalMinutes = 0;

        LocalDate currentDate = start.toLocalDate();
        LocalDate lastDate = end.toLocalDate();

        while (!currentDate.isAfter(lastDate)) {

            if (openDays.contains(currentDate.getDayOfWeek())) {

                LocalDateTime openingDateTime =
                        LocalDateTime.of(currentDate, openingTime);

                LocalDateTime closingDateTime =
                        LocalDateTime.of(currentDate, closingTime);

                LocalDateTime overlapStart =
                        start.isAfter(openingDateTime)
                                ? start : openingDateTime;

                LocalDateTime overlapEnd =
                        end.isBefore(closingDateTime)
                                ? end : closingDateTime;

                if (overlapEnd.isAfter(overlapStart)) {
                    totalMinutes += Duration.between(
                            overlapStart,
                            overlapEnd
                    ).toMinutes();
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return totalMinutes;
    }

    private static Set<DayOfWeek> parseWorkingDays(String workingDays) {

        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);

        for (String day : workingDays.split(",")) {
            try {
                result.add(DayOfWeek.valueOf(
                        day.trim().toUpperCase()
                ));
            } catch (IllegalArgumentException ignored) {
                // Invalid day names are ignored.
            }
        }

        return result;
    }
}

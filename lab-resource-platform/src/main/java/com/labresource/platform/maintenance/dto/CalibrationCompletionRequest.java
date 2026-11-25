
package com.labresource.platform.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalibrationCompletionRequest(
        @NotNull LocalDate calibrationDate,
        @NotNull LocalDate nextDueDate,
        @Size(max = 150) String certificateNumber,
        @Size(max = 200) String certifiedBy,
        @Size(max = 1000) String remarks
) {}

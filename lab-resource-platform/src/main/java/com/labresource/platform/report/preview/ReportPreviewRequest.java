
package com.labresource.platform.report.preview;

import java.time.LocalDate;

public record ReportPreviewRequest(
        LocalDate fromDate,
        LocalDate toDate
) {
}


package com.labresource.platform.report.preview;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportPreviewResponse(
        String reportType,
        String title,
        LocalDate fromDate,
        LocalDate toDate,
        List<String> columns,
        List<Map<String, Object>> rows,
        Map<String, Object> totals,
        long totalRows,
        boolean truncated,
        List<Map<String, Object>> chartSeries
) {
    public ReportPreviewResponse(
            String reportType,
            String title,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> columns,
            List<Map<String, Object>> rows,
            Map<String, Object> totals,
            long totalRows,
            boolean truncated
    ) {
        this(
                reportType,
                title,
                fromDate,
                toDate,
                columns,
                rows,
                totals,
                totalRows,
                truncated,
                List.of()
        );
    }
}

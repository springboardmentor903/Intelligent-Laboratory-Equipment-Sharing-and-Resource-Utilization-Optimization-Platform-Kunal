
package com.labresource.platform.report.preview;

public record ReportFilters(
        Long departmentId,
        Long resourceId,
        String keyword,
        String status
) {
    public boolean isEmpty() {
        return departmentId == null
                && resourceId == null
                && (keyword == null || keyword.isBlank())
                && (status == null || status.isBlank());
    }
}

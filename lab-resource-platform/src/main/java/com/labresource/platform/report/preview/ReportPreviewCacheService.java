
package com.labresource.platform.report.preview;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReportPreviewCacheService {

    private final ReportPreviewService previewService;

    public ReportPreviewCacheService(ReportPreviewService previewService) {
        this.previewService = previewService;
    }

    @Cacheable(
            cacheNames = "reportPreviews",
            key = "#type + ':' + #fromDate + ':' + #toDate + ':' + #email + ':' + #filters"
    )
    public ReportPreviewResponse preview(
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String email,
            ReportFilters filters
    ) {
        return previewService.preview(
                type,
                fromDate,
                toDate,
                email,
                filters
        );
    }

    public ReportPreviewResponse preview(
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String email
    ) {
        return preview(
                type,
                fromDate,
                toDate,
                email,
                new ReportFilters(null, null, null, null)
        );
    }
}

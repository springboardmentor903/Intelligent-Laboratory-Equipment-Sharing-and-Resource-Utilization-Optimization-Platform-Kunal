
package com.labresource.platform.report.export;

import com.labresource.platform.report.preview.ReportPreviewCacheService;
import com.labresource.platform.report.preview.ReportPreviewResponse;
import com.labresource.platform.report.registry.ReportRegistryService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportPreviewCacheService cacheService;
    private final ReportExportService exportService;
    private final StreamingExcelExportService streamingExcelExportService;
    private final ReportRegistryService registryService;

    public ReportExportController(
            ReportPreviewCacheService cacheService,
            ReportExportService exportService,
            StreamingExcelExportService streamingExcelExportService,
            ReportRegistryService registryService
    ) {
        this.cacheService = cacheService;
        this.exportService = exportService;
        this.streamingExcelExportService = streamingExcelExportService;
        this.registryService = registryService;
    }

    @GetMapping("/{type}/export")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','LAB_MANAGER','DEPARTMENT_HEAD','SYSTEM_ADMIN')")
    public ResponseEntity<StreamingResponseBody> exportReport(
            @PathVariable String type,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "pdf") String format,
            Authentication authentication
    ) {

        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "From date cannot be later than To date"
            );
        }

        String normalizedFormat = format.toLowerCase(Locale.ROOT);

        if (!normalizedFormat.equals("pdf")
                && !normalizedFormat.equals("xlsx")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Supported formats are pdf and xlsx"
            );
        }

        String email = authentication.getName();

        registryService.getReportDefinition(type, email);

        ReportPreviewResponse report = cacheService.preview(
                type,
                fromDate,
                toDate,
                email
        );

        String safeType = type.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");

        String extension = normalizedFormat.equals("xlsx")
                ? "xlsx"
                : "pdf";

        String filename = safeType + "_"
                + fromDate + "_to_" + toDate + "." + extension;

        StreamingResponseBody responseBody;
        MediaType contentType;

        if (normalizedFormat.equals("xlsx")) {
            responseBody =
                    streamingExcelExportService.streamExcel(report);

            contentType = MediaType.parseMediaType(EXCEL_CONTENT_TYPE);

        } else {
            byte[] pdfContent = exportService.exportPdf(report);

            responseBody = outputStream -> {
                outputStream.write(pdfContent);
                outputStream.flush();
            };

            contentType = MediaType.APPLICATION_PDF;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(responseBody);
    }
}

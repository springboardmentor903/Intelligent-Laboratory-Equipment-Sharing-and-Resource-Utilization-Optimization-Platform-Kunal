
package com.labresource.platform.report.preview;

import com.labresource.platform.report.registry.ReportRegistryService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportPreviewController {

    private final ReportPreviewCacheService cacheService;
    private final ReportRegistryService registryService;
    private final ReportFilterValidator filterValidator;

    public ReportPreviewController(
            ReportPreviewCacheService cacheService,
            ReportRegistryService registryService,
            ReportFilterValidator filterValidator
    ) {
        this.cacheService = cacheService;
        this.registryService = registryService;
        this.filterValidator = filterValidator;
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','LAB_MANAGER','DEPARTMENT_HEAD','SYSTEM_ADMIN')")
    public ReportPreviewResponse getReportPreview(
            @PathVariable String type,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        String email = authentication.getName();

        registryService.getReportDefinition(type, email);

        filterValidator.validate(
                fromDate,
                toDate,
                departmentId,
                resourceId,
                keyword,
                status
        );

        ReportFilters filters = new ReportFilters(
                departmentId,
                resourceId,
                keyword,
                status
        );

        return cacheService.preview(
                type,
                fromDate,
                toDate,
                email,
                filters
        );
    }
}

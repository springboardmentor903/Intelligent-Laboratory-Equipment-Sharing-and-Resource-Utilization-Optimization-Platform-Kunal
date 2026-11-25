
package com.labresource.platform.report.registry;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportRegistryController {

    private final ReportRegistryService registryService;

    public ReportRegistryController(
            ReportRegistryService registryService
    ) {
        this.registryService = registryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','LAB_MANAGER','DEPARTMENT_HEAD','SYSTEM_ADMIN')")
    public List<ReportDefinition> getAvailableReports(
            Authentication authentication
    ) {
        return registryService.getAvailableReports(
                authentication.getName()
        );
    }
}
